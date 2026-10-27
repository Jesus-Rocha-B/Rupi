package pe.rupi.api.gamification;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Historia 29: XP acumulado del estudiante, sumado del libro de movimientos idempotente.
 * Incluye actividades, retos, bonos de insignia, ajustes y reversos; no se guarda un total aparte.
 */
@RestController
@RequestMapping("/api/v1/student/experience")
public class ExperienceController {
    public record Experience(int totalXp, int badges) {}

    private final NamedParameterJdbcTemplate jdbc;

    public ExperienceController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Experience current(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String user;
        try {
            user = UUID.fromString(principal.getName()).toString();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var params = new MapSqlParameterSource("userId", user);
        Integer totalXp = jdbc.queryForObject("""
                SELECT COALESCE(SUM(cantidad), 0) FROM gamificacion_movimiento_experiencia WHERE usuario_id = :userId
                """, params, Integer.class);
        Integer badges = jdbc.queryForObject("""
                SELECT COUNT(*) FROM gamificacion_insignia_usuario WHERE usuario_id = :userId
                """, params, Integer.class);
        return new Experience(Math.max(0, totalXp == null ? 0 : totalXp), badges == null ? 0 : badges);
    }
}
