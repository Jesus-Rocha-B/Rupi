package pe.rupi.api.shared;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StudentSessionService {
    public static final String COOKIE = "rupi_session";
    private final JdbcTemplate jdbc;
    public StudentSessionService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Student(String id, String name) {}
    public Optional<Student> resolve(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null) {
            if (!authorization.startsWith("Bearer ")) return Optional.empty();
            return find(authorization.substring(7));
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (COOKIE.equals(cookie.getName())) return find(cookie.getValue());
            }
        }
        return Optional.empty();
    }
    public Optional<Student> find(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43,128}")) return Optional.empty();
        byte[] hash;
        try { hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
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
}
