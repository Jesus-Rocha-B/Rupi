package pe.rupi.api.dailychallenge;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.dailychallenge.DailyChallengeDtos.DailyChallengeCompleteResponse;
import pe.rupi.api.dailychallenge.DailyChallengeDtos.DailyChallengeResponse;
import pe.rupi.api.evaluation.EvaluationService;
import pe.rupi.api.evaluation.EvaluationDtos.QuestionDto;

@Service
public class DailyChallengeService {
    private static final String DEFAULT_GRADE_ID = "00000000-0000-4000-8000-000000000002"; // 2.° de primaria

    private final NamedParameterJdbcTemplate jdbc;
    private final EvaluationService evaluationService;

    public DailyChallengeService(NamedParameterJdbcTemplate jdbc, EvaluationService evaluationService) {
        this.jdbc = jdbc;
        this.evaluationService = evaluationService;
    }

    public DailyChallengeResponse getTodayChallenge(UUID studentId) {
        // 1. Obtener el grado del estudiante
        String sqlGrade = """
                SELECT a.grado_id
                FROM escuela_membresia_aula m
                JOIN escuela_aula a ON a.id = m.aula_id
                WHERE m.usuario_id = :studentId AND m.estado = 'ACTIVO'
                LIMIT 1
                """;
        List<String> grades = jdbc.query(sqlGrade, new MapSqlParameterSource("studentId", studentId.toString()),
                (rs, _) -> rs.getString("grado_id"));
        String gradeId = grades.isEmpty() ? DEFAULT_GRADE_ID : grades.getFirst();

        // 2. Buscar reto diario vigente para ese grado
        String sqlChallenge = """
                SELECT rd.id, rd.fecha, rd.version_actividad_id, va.titulo, va.instrucciones, va.minutos_estimados
                FROM aprendizaje_reto_diario rd
                JOIN aprendizaje_version_actividad va ON va.id = rd.version_actividad_id
                WHERE rd.grado_id = :gradeId AND rd.estado = 'PUBLICADO'
                ORDER BY rd.fecha DESC
                LIMIT 1
                """;
        var params = new MapSqlParameterSource("gradeId", gradeId);

        record ChallengeRaw(UUID id, String date, UUID activityVersionId, String title, String instructions, Integer minutes) {}
        var challengeOpt = jdbc.query(sqlChallenge, params, (rs, _) -> new ChallengeRaw(
                UUID.fromString(rs.getString("id")),
                rs.getString("fecha"),
                UUID.fromString(rs.getString("version_actividad_id")),
                rs.getString("titulo"),
                rs.getString("instrucciones"),
                rs.getInt("minutos_estimados")
        )).stream().findFirst();

        if (challengeOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay un reto diario disponible hoy para tu grado.");
        }

        var challenge = challengeOpt.get();

        // 3. Comprobar si ya se completó hoy
        String sqlCompleted = """
                SELECT COUNT(*)
                FROM gamificacion_movimiento_experiencia
                WHERE usuario_id = :studentId
                  AND motivo = 'RETO'
                  AND referencia_id = :actId
                  AND DATE(ocurrido_en) = CURRENT_DATE
                """;
        Integer completedCount = jdbc.queryForObject(sqlCompleted, new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("actId", challenge.activityVersionId().toString()), Integer.class);

        boolean isCompleted = completedCount != null && completedCount > 0;

        // 4. Obtener preguntas interactivas asociadas
        List<QuestionDto> questions = evaluationService.findQuestionsForActivity(challenge.activityVersionId());

        return new DailyChallengeResponse(
                challenge.id(),
                challenge.date(),
                challenge.activityVersionId(),
                challenge.title(),
                challenge.instructions(),
                challenge.minutes() != null && challenge.minutes() > 0 ? challenge.minutes() : 5,
                isCompleted,
                30, // 30 XP de recompensa
                questions
        );
    }

    @Transactional
    public DailyChallengeCompleteResponse completeTodayChallenge(UUID studentId, UUID challengeId) {
        String sqlChallenge = """
                SELECT rd.id, rd.version_actividad_id, va.titulo
                FROM aprendizaje_reto_diario rd
                JOIN aprendizaje_version_actividad va ON va.id = rd.version_actividad_id
                WHERE rd.id = :challengeId
                """;
        record ChallengeInfo(UUID id, UUID activityVersionId, String title) {}
        var challenge = jdbc.query(sqlChallenge, new MapSqlParameterSource("challengeId", challengeId.toString()),
                (rs, _) -> new ChallengeInfo(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("version_actividad_id")),
                        rs.getString("titulo")
                )).stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Reto diario no encontrado.")
        );

        String today = LocalDate.now().toString();
        String idempotencyKey = UUID.nameUUIDFromBytes(
                (studentId + ":" + challengeId + ":" + today).getBytes(StandardCharsets.UTF_8)
        ).toString();

        String sqlCheckToday = """
                SELECT COUNT(*) FROM gamificacion_movimiento_experiencia
                WHERE usuario_id = :studentId
                  AND motivo = 'RETO'
                  AND referencia_id = :actId
                  AND DATE(ocurrido_en) = CURRENT_DATE
                """;
        Integer completedToday = jdbc.queryForObject(sqlCheckToday, new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("actId", challenge.activityVersionId().toString()), Integer.class);

        boolean wasAlreadyCompleted = completedToday != null && completedToday > 0;
        int experienceEarned = wasAlreadyCompleted ? 0 : 30;

        if (!wasAlreadyCompleted) {
            jdbc.update("""
                    INSERT IGNORE INTO gamificacion_movimiento_experiencia
                    (id, usuario_id, cantidad, motivo, referencia_tipo, referencia_id, evento_idempotencia, ocurrido_en)
                    VALUES (:id, :studentId, :amount, 'RETO', 'RETO', :actId, :idempotency, CURRENT_TIMESTAMP(6))
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID().toString())
                    .addValue("studentId", studentId.toString())
                    .addValue("amount", experienceEarned)
                    .addValue("actId", challenge.activityVersionId().toString())
                    .addValue("idempotency", idempotencyKey));
        }

        Integer totalXp = jdbc.queryForObject("""
                SELECT COALESCE(SUM(cantidad), 0)
                FROM gamificacion_movimiento_experiencia
                WHERE usuario_id = :studentId
                """, new MapSqlParameterSource("studentId", studentId.toString()), Integer.class);

        String rupiMessage = wasAlreadyCompleted
                ? "¡Ya habías completado el reto de hoy! Gracias por seguir practicando con entusiasmo."
                : "¡Felicitaciones! Has completado el reto diario de hoy y sumaste +30 XP a tu cuenta.";

        return new DailyChallengeCompleteResponse(
                challengeId,
                true,
                experienceEarned,
                totalXp != null ? totalXp : 0,
                rupiMessage
        );
    }
}
