package pe.rupi.api.gamification;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.gamification.DailyGoalDtos.*;

/**
 * Servicio de indicador gráfico y meta de progreso diario del estudiante (HU-34).
 *
 * Mantiene el historial en gamificacion_progreso_diario y calcula el porcentaje
 * de avance hacia la meta diaria en tiempo real conforme el alumno completa paradas.
 */
@Service
public class DailyGoalProgressService {
    private static final Logger log = LoggerFactory.getLogger(DailyGoalProgressService.class);
    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final Locale PERU_LOCALE = Locale.of("es", "PE");
    public static final List<Integer> AVAILABLE_TARGET_OPTIONS = DailyGoalTarget.OPTIONS;
    public static final int DEFAULT_TARGET_GOAL = DailyGoalTarget.DEFAULT;

    private final NamedParameterJdbcTemplate jdbc;
    private final StudyStreakService streakService;

    public DailyGoalProgressService(NamedParameterJdbcTemplate jdbc, StudyStreakService streakService) {
        this.jdbc = jdbc;
        this.streakService = streakService;
    }

    @Transactional
    public DailyGoalOverviewResponse getDailyGoalOverview(String studentId) {
        LocalDate todayLocal = LocalDate.now(LIMA_ZONE);
        int targetGoal = resolveStudentTargetGoal(studentId);

        // 1. Contar lecciones completadas hoy en tiempo real
        int completedToday = countLessonsCompletedToday(studentId, todayLocal);

        // 2. Sincronizar / guardar fila de hoy en gamificacion_progreso_diario
        upsertTodayProgress(studentId, todayLocal, targetGoal, completedToday);

        // 3. Construir DTO de hoy
        boolean isGoalMet = completedToday >= targetGoal;
        int pending = Math.max(0, targetGoal - completedToday);
        int percentage = targetGoal > 0 ? (int) Math.round((double) completedToday * 100.0 / targetGoal) : 0;
        String motivationalMessage = buildMotivationalMessage(completedToday, targetGoal, pending, isGoalMet);

        String metAtIso = queryGoalMetAt(studentId, todayLocal);

        DayProgressDto todayDto = new DayProgressDto(
                todayLocal.toString(),
                targetGoal,
                completedToday,
                pending,
                percentage,
                isGoalMet,
                metAtIso,
                motivationalMessage
        );

        // 4. Historial semanal (últimos 7 días)
        List<DayHistoryDto> weeklyHistory = loadWeeklyHistory(studentId, todayLocal, targetGoal, completedToday, isGoalMet);

        // 5. Racha de días de estudio
        int currentStreak = 0;
        try {
            var streak = streakService.current(studentId);
            if (streak != null) {
                currentStreak = streak.days();
            }
        } catch (Exception ex) {
            log.warn("No se pudo obtener racha para {}: {}", studentId, ex.getMessage());
        }

        return new DailyGoalOverviewResponse(
                todayDto,
                weeklyHistory,
                targetGoal,
                AVAILABLE_TARGET_OPTIONS,
                currentStreak
        );
    }

    @Transactional
    public DailyGoalOverviewResponse updateTargetGoal(String studentId, int newTarget) {
        if (!AVAILABLE_TARGET_OPTIONS.contains(newTarget)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La meta diaria debe ser una de las opciones permitidas: de 1 a 5 lecciones.");
        }

        // 1. Guardar la meta única que también usan «Mi ritmo de estudio» y sus avisos
        DailyGoalTarget.save(jdbc, studentId, newTarget, LocalDate.now(LIMA_ZONE));

        // 2. Re-calcular y devolver la vista actualizada
        return getDailyGoalOverview(studentId);
    }

    /**
     * Compara el XP ganado hoy con el de ayer en horario de Lima (HU-36).
     * Solo cuenta movimientos positivos: gastar XP en la tienda no baja el puntaje del día.
     */
    public ScoreComparisonResponse getScoreComparison(String studentId) {
        LocalDate todayLocal = LocalDate.now(LIMA_ZONE);
        LocalDate yesterdayLocal = todayLocal.minusDays(1);

        int todayXp = sumExperienceBetween(studentId, todayLocal, todayLocal.plusDays(1));
        int yesterdayXp = sumExperienceBetween(studentId, yesterdayLocal, todayLocal);
        int todayLessons = countLessonsCompletedBetween(studentId, todayLocal, todayLocal.plusDays(1));
        int yesterdayLessons = countLessonsCompletedBetween(studentId, yesterdayLocal, todayLocal);

        int difference = todayXp - yesterdayXp;
        String trend = difference > 0 ? "SUBE" : difference < 0 ? "BAJA" : "IGUAL";

        return new ScoreComparisonResponse(
                todayLocal.toString(),
                yesterdayLocal.toString(),
                todayXp,
                yesterdayXp,
                difference,
                trend,
                todayLessons,
                yesterdayLessons,
                buildComparisonMessage(todayXp, yesterdayXp, difference)
        );
    }

    @Transactional
    public void recordCompletedActivity(String studentId) {
        try {
            LocalDate todayLocal = LocalDate.now(LIMA_ZONE);
            int targetGoal = resolveStudentTargetGoal(studentId);
            int completedToday = countLessonsCompletedToday(studentId, todayLocal);
            upsertTodayProgress(studentId, todayLocal, targetGoal, completedToday);
        } catch (Exception ex) {
            log.error("Error al registrar progreso diario para {}: {}", studentId, ex.getMessage(), ex);
        }
    }

    private int resolveStudentTargetGoal(String studentId) {
        return DailyGoalTarget.resolve(jdbc, studentId);
    }

    private int countLessonsCompletedToday(String studentId, LocalDate todayLocal) {
        Instant todayStart = todayLocal.atStartOfDay(LIMA_ZONE).toInstant();
        String sql = """
                SELECT COUNT(DISTINCT p.nodo_ruta_id)
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_progreso_nodo p ON p.inscripcion_id = i.id
                WHERE i.estudiante_id = :studentId
                  AND p.estado = 'COMPLETADO'
                  AND p.completado_en >= :todayStart
                """;
        var params = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("todayStart", Timestamp.from(todayStart));
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }

    private int sumExperienceBetween(String studentId, LocalDate fromDay, LocalDate toDay) {
        String sql = """
                SELECT COALESCE(SUM(cantidad), 0)
                FROM gamificacion_movimiento_experiencia
                WHERE usuario_id = :studentId
                  AND cantidad > 0
                  AND ocurrido_en >= :fromInstant
                  AND ocurrido_en < :toInstant
                """;
        Integer total = jdbc.queryForObject(sql, dayRange(studentId, fromDay, toDay), Integer.class);
        return total != null ? total : 0;
    }

    private int countLessonsCompletedBetween(String studentId, LocalDate fromDay, LocalDate toDay) {
        String sql = """
                SELECT COUNT(DISTINCT p.nodo_ruta_id)
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_progreso_nodo p ON p.inscripcion_id = i.id
                WHERE i.estudiante_id = :studentId
                  AND p.estado = 'COMPLETADO'
                  AND p.completado_en >= :fromInstant
                  AND p.completado_en < :toInstant
                """;
        Integer count = jdbc.queryForObject(sql, dayRange(studentId, fromDay, toDay), Integer.class);
        return count != null ? count : 0;
    }

    private MapSqlParameterSource dayRange(String studentId, LocalDate fromDay, LocalDate toDay) {
        return new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("fromInstant", Timestamp.from(fromDay.atStartOfDay(LIMA_ZONE).toInstant()))
                .addValue("toInstant", Timestamp.from(toDay.atStartOfDay(LIMA_ZONE).toInstant()));
    }

    private String buildComparisonMessage(int todayXp, int yesterdayXp, int difference) {
        if (todayXp == 0 && yesterdayXp == 0) {
            return "Hoy es un buen día para empezar. ¡Completa una parada y suma tus primeros XP!";
        }
        if (yesterdayXp == 0) {
            return "¡Ya sumaste " + todayXp + " XP hoy! Ayer no registraste puntaje, así que vas mejor que ayer.";
        }
        if (difference > 0) {
            return "¡Superaste tu ritmo de ayer por " + difference + " XP! Sigue así, explorador.";
        }
        if (difference == 0) {
            return "Vas igual que ayer: " + todayXp + " XP. Una parada más y superas tu ritmo.";
        }
        return "Te faltan " + (-difference) + " XP para igualar lo de ayer. ¡Tú puedes!";
    }

    private void upsertTodayProgress(String studentId, LocalDate todayLocal, int targetGoal, int completedToday) {
        boolean isGoalMet = completedToday >= targetGoal;
        String sql = """
                INSERT INTO gamificacion_progreso_diario
                (id, usuario_id, fecha_local, meta_actividades, actividades_completadas, meta_cumplida, cumplida_en)
                VALUES
                (:id, :studentId, :todayDate, :targetGoal, :completed, :isGoalMet, :metAt)
                ON DUPLICATE KEY UPDATE
                  meta_actividades = VALUES(meta_actividades),
                  actividades_completadas = VALUES(actividades_completadas),
                  meta_cumplida = VALUES(meta_cumplida),
                  cumplida_en = CASE
                    WHEN VALUES(meta_cumplida) = 1 AND cumplida_en IS NULL THEN UTC_TIMESTAMP(6)
                    WHEN VALUES(meta_cumplida) = 0 THEN NULL
                    ELSE cumplida_en
                  END
                """;
        var params = new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID().toString())
                .addValue("studentId", studentId)
                .addValue("todayDate", Date.valueOf(todayLocal))
                .addValue("targetGoal", targetGoal)
                .addValue("completed", completedToday)
                .addValue("isGoalMet", isGoalMet ? 1 : 0)
                .addValue("metAt", isGoalMet ? Timestamp.from(Instant.now()) : null);

        jdbc.update(sql, params);
    }

    private String queryGoalMetAt(String studentId, LocalDate todayLocal) {
        String sql = """
                SELECT cumplida_en
                FROM gamificacion_progreso_diario
                WHERE usuario_id = :studentId AND fecha_local = :todayDate
                """;
        var params = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("todayDate", Date.valueOf(todayLocal));
        try {
            Timestamp ts = jdbc.queryForObject(sql, params, Timestamp.class);
            return ts != null ? ts.toInstant().toString() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private List<DayHistoryDto> loadWeeklyHistory(
            String studentId,
            LocalDate todayLocal,
            int todayTarget,
            int todayCompleted,
            boolean todayMet
    ) {
        LocalDate startDate = todayLocal.minusDays(6);
        String sql = """
                SELECT fecha_local, meta_actividades, actividades_completadas, meta_cumplida
                FROM gamificacion_progreso_diario
                WHERE usuario_id = :studentId
                  AND fecha_local BETWEEN :startDate AND :endDate
                ORDER BY fecha_local ASC
                """;
        var params = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("startDate", Date.valueOf(startDate))
                .addValue("endDate", Date.valueOf(todayLocal));

        record HistoryRow(LocalDate date, int target, int completed, boolean met) {}
        Map<LocalDate, HistoryRow> historyMap = new HashMap<>();

        jdbc.query(sql, params, rs -> {
            LocalDate d = rs.getDate("fecha_local").toLocalDate();
            int target = rs.getInt("meta_actividades");
            int completed = rs.getInt("actividades_completadas");
            boolean met = rs.getBoolean("meta_cumplida");
            historyMap.put(d, new HistoryRow(d, target, completed, met));
        });

        // Aseguramos que el día de hoy esté actualizado con los datos en tiempo real
        historyMap.put(todayLocal, new HistoryRow(todayLocal, todayTarget, todayCompleted, todayMet));

        List<DayHistoryDto> result = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate day = todayLocal.minusDays(i);
            HistoryRow row = historyMap.get(day);
            int target = row != null ? row.target() : todayTarget;
            int completed = row != null ? row.completed() : 0;
            boolean met = row != null ? row.met() : false;
            int pct = target > 0 ? (int) Math.round((double) completed * 100.0 / target) : 0;
            boolean isToday = day.equals(todayLocal);

            String dayName = formatDayOfWeek(day.getDayOfWeek());

            result.add(new DayHistoryDto(
                    day.toString(),
                    dayName,
                    target,
                    completed,
                    pct,
                    met,
                    isToday
            ));
        }

        return result;
    }

    private String formatDayOfWeek(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "LUN";
            case TUESDAY -> "MAR";
            case WEDNESDAY -> "MIÉ";
            case THURSDAY -> "JUE";
            case FRIDAY -> "VIE";
            case SATURDAY -> "SÁB";
            case SUNDAY -> "DOM";
        };
    }

    private String buildMotivationalMessage(int completed, int target, int pending, boolean isGoalMet) {
        if (isGoalMet) {
            return "¡Felicitaciones! Has alcanzado tu meta diaria escolar. ¡Eres imparable! 🎉";
        }
        if (completed == 0) {
            return "¡Inicia el día con tu primera parada escolar para avanzar en tu meta!";
        }
        if (pending == 1) {
            return "¡Solo te falta 1 lección para alcanzar tu meta de hoy! ¡Tú puedes!";
        }
        return "Llevas " + completed + " de " + target + " lecciones hoy. ¡Sigue avanzando paso a paso!";
    }
}
