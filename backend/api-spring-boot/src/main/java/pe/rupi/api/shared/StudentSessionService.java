package pe.rupi.api.shared;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    /** HU-43: el celular se identifica al iniciar sesión para que la consola y la web sepan dónde quedó abierta. */
    public static final String CLIENT_HEADER = "X-Rupi-Cliente";
    public static final String CLIENT_WEB = "WEB_USUARIO";
    public static final String CLIENT_MOBILE = "MOVIL";
    /** La última actividad se guarda como mucho cada 5 minutos por sesión: basta para la consola y evita una escritura por petición. */
    private static final long ACTIVITY_EVERY_MS = 5 * 60 * 1000L;

    /**
     * Un solo rol y una sola aula por usuario, elegidos siempre igual. Con un JOIN directo, un usuario con dos roles
     * o varias aulas activas producía varias filas y la sesión tomaba una cualquiera: el grado, el colegio y hasta el
     * rol podían cambiar entre inicios de sesión. Se prefiere DOCENTE (necesita el panel de autoría) y el aula activa
     * a la que ingresó más recientemente.
     */
    private static final String ROLE_AND_CLASSROOM_COLUMNS = """
            COALESCE((
                SELECT inst.nombre 
                FROM escuela_membresia_aula ma 
                JOIN escuela_aula aa ON aa.id = ma.aula_id AND aa.activa = TRUE 
                JOIN escuela_institucion inst ON inst.id = aa.institucion_id 
                WHERE ma.usuario_id = u.id AND ma.estado = 'ACTIVO' 
                ORDER BY ma.ingreso_en DESC, ma.aula_id 
                LIMIT 1
            ), 'I.E. 38001 Mariscal Sucre') AS school_name,
            COALESCE((
                SELECT rr.codigo 
                FROM identidad_usuario_rol ur 
                JOIN identidad_rol rr ON rr.id = ur.rol_id 
                WHERE ur.usuario_id = u.id 
                ORDER BY rr.codigo = 'DOCENTE' DESC, rr.codigo 
                LIMIT 1
            ), 'ESTUDIANTE') AS rol_codigo,
            COALESCE((
                SELECT g.numero_grado 
                FROM escuela_membresia_aula ma 
                JOIN escuela_aula aa ON aa.id = ma.aula_id AND aa.activa = TRUE 
                JOIN curriculo_grado g ON g.id = aa.grado_id 
                WHERE ma.usuario_id = u.id AND ma.estado = 'ACTIVO' 
                ORDER BY ma.ingreso_en DESC, ma.aula_id 
                LIMIT 1
            ), 2) AS numero_grado,
            COALESCE((
                SELECT g.nombre 
                FROM escuela_membresia_aula ma 
                JOIN escuela_aula aa ON aa.id = ma.aula_id AND aa.activa = TRUE 
                JOIN curriculo_grado g ON g.id = aa.grado_id 
                WHERE ma.usuario_id = u.id AND ma.estado = 'ACTIVO' 
                ORDER BY ma.ingreso_en DESC, ma.aula_id 
                LIMIT 1
            ), '2.° de primaria') AS grado_nombre
            """;

    private final JdbcTemplate jdbc;
    private final PasswordService passwordService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Long> lastActivity = new ConcurrentHashMap<>();

    public StudentSessionService(JdbcTemplate jdbc, PasswordService passwordService) {
        this.jdbc = jdbc;
        this.passwordService = passwordService;
    }

    public record Student(
            String id,
            String name,
            String schoolName,
            String role,
            Integer gradeNumber,
            String gradeName
    ) {
        public Student(String id, String name, String schoolName, String role) {
            this(id, name, schoolName, role, 2, "2.° de primaria");
        }

        public Student(String id, String name) {
            this(id, name, "I.E. 38001 Mariscal Sucre", "ESTUDIANTE", 2, "2.° de primaria");
        }

        public Student(String id, String name, String schoolName) {
            this(id, name, schoolName, "ESTUDIANTE", 2, "2.° de primaria");
        }
    }

    public record AuthenticationResult(
            boolean authenticated,
            boolean rateLimited,
            boolean consoleOnly,
            boolean locked,
            Student student,
            String token
    ) {
        public static AuthenticationResult success(Student student, String token) {
            return new AuthenticationResult(true, false, false, false, student, token);
        }

        public static AuthenticationResult throttled() {
            return new AuthenticationResult(false, true, false, false, null, null);
        }

        public static AuthenticationResult badCredentials() {
            return new AuthenticationResult(false, false, false, false, null, null);
        }

        public static AuthenticationResult consoleAccount() {
            return new AuthenticationResult(false, false, true, false, null, null);
        }

        public static AuthenticationResult lockedAccount() {
            return new AuthenticationResult(false, false, false, true, null, null);
        }
    }

    private record UserAccount(
            String id,
            String username,
            String passwordHash,
            String state,
            Timestamp deactivatedAt,
            Timestamp lockedUntil,
            String displayName,
            String schoolName,
            String role,
            Integer gradeNumber,
            String gradeName
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
                SELECT u.id,
                       COALESCE(p.nombre_preferido, p.nombre, u.nombre_usuario) AS nombre,
                """ + ROLE_AND_CLASSROOM_COLUMNS + """
                FROM identidad_sesion_usuario s
                JOIN identidad_cuenta_usuario u ON u.id = s.usuario_id
                LEFT JOIN identidad_perfil_usuario p ON p.usuario_id = u.id
                WHERE s.token_hash = ? AND s.revocada_en IS NULL
                  AND s.expira_en > UTC_TIMESTAMP(6) AND s.cliente IN ('WEB_USUARIO', 'MOVIL')
                  AND u.estado = 'ACTIVO' AND u.desactivado_en IS NULL
                  AND (u.bloqueado_hasta IS NULL OR u.bloqueado_hasta <= UTC_TIMESTAMP(6))
                """, (rs, row) -> new Student(
                        rs.getString("id"),
                        rs.getString("nombre"),
                        rs.getString("school_name"),
                        rs.getString("rol_codigo"),
                        rs.getInt("numero_grado"),
                        rs.getString("grado_nombre")
                ), hash)
                .stream().findFirst()
                .map(student -> {
                    touchActivity(hash);
                    return student;
                });
    }

    private void touchActivity(byte[] tokenHash) {
        String key = Base64.getEncoder().encodeToString(tokenHash);
        long now = System.currentTimeMillis();
        Long previous = lastActivity.get(key);
        if (previous != null && now - previous < ACTIVITY_EVERY_MS) {
            return;
        }
        if (lastActivity.size() > 20_000) {
            lastActivity.clear();
        }
        lastActivity.put(key, now);
        jdbc.update("""
                UPDATE identidad_sesion_usuario SET actividad_en = UTC_TIMESTAMP(6)
                WHERE token_hash = ? AND revocada_en IS NULL
                """, tokenHash);
    }

    /**
     * Por qué ya no vale un token (HU-43). Una sesión revocada justo cuando se abrió otra de la misma cuenta
     * se cerró por la sesión única: la web y el celular lo explican en vez de mostrar solo el ingreso.
     */
    public record SessionEnd(String reason, String newClient) {
        public static final String OTHER_DEVICE = "OTRO_DISPOSITIVO";
        public static final String CLOSED = "CERRADA";
        public static final String EXPIRED = "EXPIRADA";
    }

    public Optional<SessionEnd> sessionEnd(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !token.matches("[A-Za-z0-9_-]{43,128}")) {
            return Optional.empty();
        }
        return jdbc.query("""
                SELECT s.revocada_en IS NOT NULL AS revocada,
                       s.expira_en <= UTC_TIMESTAMP(6) AS expirada,
                       (SELECT n.cliente FROM identidad_sesion_usuario n
                         WHERE n.usuario_id = s.usuario_id AND n.id <> s.id AND s.revocada_en IS NOT NULL
                           AND n.creada_en > s.creada_en
                           AND n.creada_en BETWEEN s.revocada_en - INTERVAL 1 SECOND AND s.revocada_en + INTERVAL 5 SECOND
                         ORDER BY n.creada_en LIMIT 1) AS nuevo_cliente
                FROM identidad_sesion_usuario s
                WHERE s.token_hash = ?
                """, (rs, row) -> {
                    String newClient = rs.getString("nuevo_cliente");
                    if (newClient != null) {
                        return new SessionEnd(SessionEnd.OTHER_DEVICE, newClient);
                    }
                    if (!rs.getBoolean("revocada") && rs.getBoolean("expirada")) {
                        return new SessionEnd(SessionEnd.EXPIRED, null);
                    }
                    return new SessionEnd(SessionEnd.CLOSED, null);
                }, sha256(token))
                .stream().findFirst();
    }

    public static String clientOf(HttpServletRequest request) {
        return CLIENT_MOBILE.equalsIgnoreCase(request.getHeader(CLIENT_HEADER)) ? CLIENT_MOBILE : CLIENT_WEB;
    }

    @org.springframework.transaction.annotation.Transactional
    public AuthenticationResult authenticate(String rawUsername, String rawPassword, String clientIp) {
        return authenticate(rawUsername, rawPassword, clientIp, CLIENT_WEB);
    }

    @org.springframework.transaction.annotation.Transactional
    public AuthenticationResult authenticate(String rawUsername, String rawPassword, String clientIp, String client) {
        String sessionClient = CLIENT_MOBILE.equals(client) ? CLIENT_MOBILE : CLIENT_WEB;
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
                       COALESCE(p.nombre_preferido, p.nombre, u.nombre_usuario) AS display_name,
                """ + ROLE_AND_CLASSROOM_COLUMNS + """
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
                rs.getString("display_name"),
                rs.getString("school_name"),
                rs.getString("rol_codigo"),
                rs.getInt("numero_grado"),
                rs.getString("grado_nombre")
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
            // HU-44: con la clave correcta se dice que está bloqueada, para que pida el desbloqueo a su docente.
            // Una cuenta dada de baja recibe el mismo 401 que una clave equivocada.
            boolean locked = account.deactivatedAt() == null && ("BLOQUEADO".equalsIgnoreCase(account.state())
                    || (account.lockedUntil() != null && account.lockedUntil().toInstant().isAfter(now)));
            return locked ? AuthenticationResult.lockedAccount() : AuthenticationResult.badCredentials();
        }

        // 3b. Roles estrictos (HU-42): una cuenta solo de administración o mesa de ayuda no abre la web ni el
        // celular, que son para estudiantes y docentes. Las cuentas sin ningún rol siguen entrando como estudiante.
        if (isConsoleOnly(account.id())) {
            jdbc.update("""
                    INSERT INTO identidad_intento_acceso (usuario_id, identificador_hash, resultado, origen_hash, ocurrido_en)
                    VALUES (?, ?, 'BLOQUEADO', ?, UTC_TIMESTAMP(6))
                    """, account.id(), idHash, originHash);
            return AuthenticationResult.consoleAccount();
        }

        // 4. Registro de acceso exitoso
        jdbc.update("""
                    INSERT INTO identidad_intento_acceso (usuario_id, identificador_hash, resultado, origen_hash, ocurrido_en)
                    VALUES (?, ?, 'CORRECTO', ?, UTC_TIMESTAMP(6))
                    """, account.id(), idHash, originHash);

        // 5. Sesión única (HU-43): abrir Rupi en la web o en el celular cierra la sesión que estuviera abierta en
        // cualquier otro lugar. La columna generada usuario_activo_id es UNIQUE, así que MySQL también lo garantiza.
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
                VALUES (?, ?, ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) + INTERVAL 30 DAY)
                """, sessionId, account.id(), tokenHash, sessionClient);

        return AuthenticationResult.success(new Student(account.id(), account.displayName(), account.schoolName(), account.role(), account.gradeNumber(), account.gradeName()), token);
    }

    boolean isConsoleOnly(String userId) {
        var counts = jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(r.codigo IN ('ESTUDIANTE', 'DOCENTE')), 0) AS app_roles
                FROM identidad_usuario_rol ur
                JOIN identidad_rol r ON r.id = ur.rol_id
                WHERE ur.usuario_id = ?
                """, userId);
        return ((Number) counts.get("total")).longValue() > 0 && ((Number) counts.get("app_roles")).longValue() == 0;
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
