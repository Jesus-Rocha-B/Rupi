package pe.rupi.api.shared;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * ARQUITECTURA DE IDENTIDAD Y ESCRITOR ÚNICO (Single Writer):
 * De acuerdo con AGENTS.md y database/mysql/README.md, el módulo administrativo Django
 * es el dueño del catálogo maestro de identidades (identidad_cuenta_usuario, roles y permisos).
 * Para la interacción de estudiantes en la plataforma web (y desarrollo local integrado),
 * Spring Boot actúa como emisor y validador de sesiones activas (identidad_sesion_usuario)
 * y supervisor de intentos de acceso (identidad_intento_acceso), garantizando:
 *  1. Rotación y revocación segura de tokens criptográficos de alta entropía.
 *  2. Mitigación de fuerza bruta con bloqueo temporal por combinación de cuenta + IP.
 *  3. Verificación de credenciales compatible con Django (BCrypt y PBKDF2) sin timing attacks.
 *  4. Almacenamiento exclusivo del hash SHA-256 de los tokens en MySQL.
 */
@Service
public class StudentSessionService {
    public static final String COOKIE = "rupi_session";

    private final JdbcTemplate jdbc;
    private final PasswordService passwordService;
    private final SecureRandom secureRandom = new SecureRandom();

    public StudentSessionService(JdbcTemplate jdbc, PasswordService passwordService) {
        this.jdbc = jdbc;
        this.passwordService = passwordService;
    }

    public record Student(String id, String name) {}

    public record AuthenticationResult(
            boolean authenticated,
            boolean rateLimited,
            Student student,
            String token
    ) {
        public static AuthenticationResult success(Student student, String token) {
            return new AuthenticationResult(true, false, student, token);
        }

        public static AuthenticationResult throttled() {
            return new AuthenticationResult(false, true, null, null);
        }

        public static AuthenticationResult badCredentials() {
            return new AuthenticationResult(false, false, null, null);
        }
    }

    private record UserAccount(
            String id,
            String username,
            String passwordHash,
            String state,
            Timestamp deactivatedAt,
            Timestamp lockedUntil,
            String displayName
    ) {}

    public Optional<Student> resolve(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Optional.empty();
        }
        return find(token);
    }

    public String extractToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public Optional<Student> find(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43,128}")) {
            return Optional.empty();
        }
        byte[] hash = sha256(token);
        return jdbc.query("""
                SELECT u.id, COALESCE(p.nombre_preferido, p.nombre, u.nombre_usuario) AS nombre
                FROM identidad_sesion_usuario s
                JOIN identidad_cuenta_usuario u ON u.id = s.usuario_id
                LEFT JOIN identidad_perfil_usuario p ON p.usuario_id = u.id
                WHERE s.token_hash = ? AND s.revocada_en IS NULL
                  AND s.expira_en > UTC_TIMESTAMP(6) AND s.cliente = 'WEB_USUARIO'
                  AND u.estado = 'ACTIVO' AND u.desactivado_en IS NULL
                  AND (u.bloqueado_hasta IS NULL OR u.bloqueado_hasta <= UTC_TIMESTAMP(6))
                """, (rs, row) -> new Student(rs.getString("id"), rs.getString("nombre")), hash)
                .stream().findFirst();
    }

    @org.springframework.transaction.annotation.Transactional
    public AuthenticationResult authenticate(String rawUsername, String rawPassword, String clientIp) {
        if (rawUsername == null || rawUsername.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return AuthenticationResult.badCredentials();
        }

        String username = rawUsername.trim();
        byte[] idHash = sha256(username.toLowerCase());
        byte[] originHash = clientIp != null && !clientIp.isBlank() ? sha256(clientIp) : null;

        // 1. Mitigación de fuerza bruta: límite por cuenta e IP combinados
        Integer accountIpFailures = originHash != null
                ? jdbc.queryForObject("""
                        SELECT COUNT(*) FROM identidad_intento_acceso
                        WHERE identificador_hash = ? AND origen_hash = ?
                          AND resultado IN ('INCORRECTO', 'BLOQUEADO')
                          AND ocurrido_en > UTC_TIMESTAMP(6) - INTERVAL 5 MINUTE
                        """, Integer.class, idHash, originHash)
                : 0;

        Integer ipFailures = originHash != null
                ? jdbc.queryForObject("""
                        SELECT COUNT(*) FROM identidad_intento_acceso
                        WHERE origen_hash = ?
                          AND resultado IN ('INCORRECTO', 'BLOQUEADO')
                          AND ocurrido_en > UTC_TIMESTAMP(6) - INTERVAL 5 MINUTE
                        """, Integer.class, originHash)
                : 0;

        if ((accountIpFailures != null && accountIpFailures >= 5) || (ipFailures != null && ipFailures >= 20)) {
            jdbc.update("""
                    INSERT INTO identidad_intento_acceso (usuario_id, identificador_hash, resultado, origen_hash, ocurrido_en)
                    VALUES (NULL, ?, 'BLOQUEADO', ?, UTC_TIMESTAMP(6))
                    """, idHash, originHash);
            return AuthenticationResult.throttled();
        }

        // 2. Consulta de cuenta de usuario
        var accounts = jdbc.query("""
                SELECT u.id, u.nombre_usuario, u.hash_clave, u.estado, u.desactivado_en, u.bloqueado_hasta,
                       COALESCE(p.nombre_preferido, p.nombre, u.nombre_usuario) AS display_name
                FROM identidad_cuenta_usuario u
                LEFT JOIN identidad_perfil_usuario p ON p.usuario_id = u.id
                WHERE u.nombre_usuario = ?
                """, (rs, rowNum) -> new UserAccount(
                rs.getString("id"),
                rs.getString("nombre_usuario"),
                rs.getString("hash_clave"),
                rs.getString("estado"),
                rs.getTimestamp("desactivado_en"),
                rs.getTimestamp("bloqueado_hasta"),
                rs.getString("display_name")
        ), username);

        if (accounts.isEmpty()) {
            passwordService.performDummyCheck(rawPassword);
            jdbc.update("""
                    INSERT INTO identidad_intento_acceso (usuario_id, identificador_hash, resultado, origen_hash, ocurrido_en)
                    VALUES (NULL, ?, 'INCORRECTO', ?, UTC_TIMESTAMP(6))
                    """, idHash, originHash);
            return AuthenticationResult.badCredentials();
        }

        UserAccount account = accounts.get(0);
        boolean passwordValid = passwordService.matches(rawPassword, account.passwordHash());

        if (!passwordValid) {
            jdbc.update("""
                    INSERT INTO identidad_intento_acceso (usuario_id, identificador_hash, resultado, origen_hash, ocurrido_en)
                    VALUES (?, ?, 'INCORRECTO', ?, UTC_TIMESTAMP(6))
                    """, account.id(), idHash, originHash);
            return AuthenticationResult.badCredentials();
        }

        // 3. Verificación de estado de cuenta
        Instant now = Instant.now();
        boolean isActive = "ACTIVO".equalsIgnoreCase(account.state())
                && account.deactivatedAt() == null
                && (account.lockedUntil() == null || !account.lockedUntil().toInstant().isAfter(now));

        if (!isActive) {
            jdbc.update("""
                    INSERT INTO identidad_intento_acceso (usuario_id, identificador_hash, resultado, origen_hash, ocurrido_en)
                    VALUES (?, ?, 'BLOQUEADO', ?, UTC_TIMESTAMP(6))
                    """, account.id(), idHash, originHash);
            return AuthenticationResult.badCredentials(); // Mismo código para no enumerar
        }

        // 4. Registro de acceso exitoso
        jdbc.update("""
                INSERT INTO identidad_intento_acceso (usuario_id, identificador_hash, resultado, origen_hash, ocurrido_en)
                VALUES (?, ?, 'CORRECTO', ?, UTC_TIMESTAMP(6))
                """, account.id(), idHash, originHash);

        // 5. Rotación de sesiones: revocar sesiones activas previas del estudiante
        jdbc.update("""
                UPDATE identidad_sesion_usuario
                SET revocada_en = UTC_TIMESTAMP(6)
                WHERE usuario_id = ? AND revocada_en IS NULL
                """, account.id());

        // 6. Generación de nuevo token de sesión de alta entropía (32 bytes = 256 bits)
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        byte[] tokenHash = sha256(token);
        String sessionId = UUID.randomUUID().toString();

        jdbc.update("""
                INSERT INTO identidad_sesion_usuario (id, usuario_id, token_hash, cliente, creada_en, actividad_en, expira_en)
                VALUES (?, ?, ?, 'WEB_USUARIO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) + INTERVAL 30 DAY)
                """, sessionId, account.id(), tokenHash);

        return AuthenticationResult.success(new Student(account.id(), account.displayName()), token);
    }

    public void revokeSession(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null && token.matches("[A-Za-z0-9_-]{43,128}")) {
            byte[] hash = sha256(token);
            jdbc.update("""
                    UPDATE identidad_sesion_usuario
                    SET revocada_en = UTC_TIMESTAMP(6)
                    WHERE token_hash = ? AND revocada_en IS NULL
                    """, hash);
        }
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
