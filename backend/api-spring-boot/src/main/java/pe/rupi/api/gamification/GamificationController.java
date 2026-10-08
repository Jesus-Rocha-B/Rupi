package pe.rupi.api.gamification;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.learningroute.LearningRouteDtos.BadgeDto;

@RestController
@RequestMapping("/api/v1/student/badges")
public class GamificationController {
    private final NamedParameterJdbcTemplate jdbc;

    public GamificationController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<BadgeDto> listStudentBadges(Principal principal) {
        UUID studentId = studentIdFrom(principal);
        String sql = """
                SELECT i.codigo, i.nombre, i.descripcion
                FROM gamificacion_insignia_usuario u
                JOIN gamificacion_insignia i ON i.id = u.insignia_id
                WHERE u.usuario_id = :userId
                ORDER BY u.obtenida_en DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("userId", studentId.toString()),
                (rs, _) -> new BadgeDto(
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        "medal"
                )
        );
    }

    private static UUID studentIdFrom(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
