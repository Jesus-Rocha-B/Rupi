package pe.rupi.api.gamification;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Racha de días seguidos con al menos una parada completada (historias 27 y 30).
 * Se calcula en cada consulta desde aprendizaje_progreso_nodo; no se guarda un contador aparte.
 */
@Service
public class StudyStreakService {
    public record Streak(int days, boolean activeToday, boolean atRisk, String localDate) {}

    static final ZoneId LIMA = ZoneId.of("America/Lima");
    /** Un año alcanza para cualquier racha escolar y acota la consulta. */
    private static final Duration WINDOW = Duration.ofDays(400);

    private final NamedParameterJdbcTemplate db;
    private final Clock clock;

    @Autowired
    public StudyStreakService(NamedParameterJdbcTemplate db) {
        this(db, Clock.systemUTC());
    }

    public StudyStreakService(NamedParameterJdbcTemplate db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    public Streak current(String user) {
        var since = Timestamp.valueOf(LocalDateTime.ofInstant(clock.instant().minus(WINDOW), ZoneOffset.UTC));
        var params = new MapSqlParameterSource("user", user).addValue("since", since);
        var completedAt = db.queryForList("""
                SELECT p.completado_en FROM aprendizaje_progreso_nodo p
                JOIN aprendizaje_inscripcion_ruta i ON i.id = p.inscripcion_id
                WHERE i.estudiante_id = :user AND p.estado = 'COMPLETADO'
                  AND p.completado_en IS NOT NULL AND p.completado_en >= :since
                """, params, Timestamp.class);
        Set<LocalDate> days = new HashSet<>();
        for (Timestamp stamp : completedAt) {
            // completado_en se guarda en UTC; el día escolar se cuenta en hora de Perú.
            days.add(stamp.toLocalDateTime().toInstant(ZoneOffset.UTC).atZone(LIMA).toLocalDate());
        }
        return of(days, LocalDate.now(clock.withZone(LIMA)));
    }

    /**
     * Cuenta los días seguidos que terminan hoy o, si hoy aún no hay actividad, ayer.
     * Así la racha no se pierde durante el día: solo queda en riesgo hasta completar algo.
     */
    static Streak of(Collection<LocalDate> completedDays, LocalDate today) {
        Set<LocalDate> days = new HashSet<>(completedDays);
        boolean activeToday = days.contains(today);
        LocalDate cursor = activeToday ? today : today.minusDays(1);
        int count = 0;
        while (days.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return new Streak(count, activeToday, !activeToday && count > 0, today.toString());
    }
}
