package pe.rupi.api.dailychallenge;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
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
import pe.rupi.api.evaluation.EvaluationDtos.AnswerFeedbackResponse;
import pe.rupi.api.evaluation.EvaluationDtos.QuestionDto;

/**
 * Retos diarios de razonamiento lógico y comprensión lectora con contexto peruano (HU-40).
 *
 * Si el grado tiene un reto publicado para hoy, se usa ese. Si no, se rota el banco de retos
 * publicados del grado según el día de Lima, así cada día hay un reto distinto sin cargar uno a mano.
 */
@Service
public class DailyChallengeService {
    private static final String DEFAULT_GRADE_ID = "00000000-0000-4000-8000-000000000002"; // 2.° de primaria
    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    static final int EXPERIENCE_REWARD = 30;

    private final NamedParameterJdbcTemplate jdbc;
    private final EvaluationService evaluationService;

    public DailyChallengeService(NamedParameterJdbcTemplate jdbc, EvaluationService evaluationService) {
        this.jdbc = jdbc;
        this.evaluationService = evaluationService;
    }

    record ChallengeRaw(UUID id, String date, UUID activityVersionId, String title, String instructions, Integer minutes) {}

    /** Índice del banco que corresponde a un día: avanza uno por día y vuelve a empezar al terminar. */
    static int rotationIndex(LocalDate day, int bankSize) {
        return bankSize <= 0 ? 0 : (int) Math.floorMod(day.toEpochDay(), (long) bankSize);
    }

    public DailyChallengeResponse getTodayChallenge(UUID studentId) {
        LocalDate today = LocalDate.now(LIMA_ZONE);
        var challenge = selectChallenge(studentId, today);

        // Lectura del reto (solo los de comprensión lectora tienen bloque TEXTO)
        List<String> readings = jdbc.query("""
                SELECT texto_cuerpo FROM aprendizaje_bloque_contenido
                WHERE version_actividad_id = :actId AND tipo = 'TEXTO' AND texto_cuerpo IS NOT NULL
                ORDER BY numero_secuencia
                """, new MapSqlParameterSource("actId", challenge.activityVersionId().toString()),
                (rs, rowNum) -> rs.getString("texto_cuerpo"));
        String readingText = readings.isEmpty() ? null : String.join("\n\n", readings);

        boolean isCompleted = rewardedToday(studentId, challenge.activityVersionId(), today);
        List<QuestionDto> questions = evaluationService.findQuestionsForActivity(challenge.activityVersionId());

        return new DailyChallengeResponse(
                challenge.id(),
                today.toString(),
                challenge.activityVersionId(),
                challenge.title(),
                challenge.instructions(),
                readingText == null ? DailyChallengeDtos.LOGICAL_REASONING : DailyChallengeDtos.READING_COMPREHENSION,
                readingText,
                challenge.minutes() != null && challenge.minutes() > 0 ? challenge.minutes() : 5,
                isCompleted,
                EXPERIENCE_REWARD,
                questions,
                answeredToday(studentId, challenge.activityVersionId(), today, questions)
        );
    }

    /**
     * Respuestas que el estudiante ya dio hoy a este reto, con la misma retroalimentación que recibió al responder.
     * Antes vivían solo en la memoria de la pantalla: al recargar o reabrir la app había que responder de nuevo.
     */
    private List<AnswerFeedbackResponse> answeredToday(UUID studentId, UUID activityVersionId, LocalDate today,
                                                      List<QuestionDto> questions) {
        var explanations = new java.util.HashMap<String, String>();
        questions.forEach(q -> explanations.put(q.id().toString(), q.explanation()));
        return jdbc.query("""
                SELECT r.pregunta_id, r.es_correcta,
                       (SELECT o.id FROM evaluacion_opcion_respuesta o
                         WHERE o.pregunta_id = r.pregunta_id AND o.es_correcta = TRUE
                         ORDER BY o.numero_secuencia LIMIT 1) AS opcion_correcta
                FROM evaluacion_intento i
                JOIN evaluacion_respuesta r ON r.intento_id = i.id
                WHERE i.estudiante_id = :studentId
                  AND i.version_actividad_id = :actId
                  AND r.respondida_en >= :dayStart
                  AND r.respondida_en = (SELECT MAX(r2.respondida_en) FROM evaluacion_respuesta r2
                                          JOIN evaluacion_intento i2 ON i2.id = r2.intento_id
                                         WHERE i2.estudiante_id = i.estudiante_id
                                           AND i2.version_actividad_id = i.version_actividad_id
                                           AND r2.pregunta_id = r.pregunta_id)
                """, new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("actId", activityVersionId.toString())
                .addValue("dayStart", dayStart(today)), (rs, row) -> {
                    String questionId = rs.getString("pregunta_id");
                    boolean correct = rs.getBoolean("es_correcta");
                    String explanation = explanations.getOrDefault(questionId, "");
                    String correctOption = rs.getString("opcion_correcta");
                    return new AnswerFeedbackResponse(
                            UUID.fromString(questionId),
                            correct,
                            correct ? "¡Excelente, explorador! ¡Descubriste la respuesta correcta!" : explanation,
                            correctOption == null ? null : UUID.fromString(correctOption),
                            explanation
                    );
                });
    }

    /** Reto que le toca hoy al estudiante según su grado. */
    private ChallengeRaw selectChallenge(UUID studentId, LocalDate today) {
        // 1. Obtener el grado del estudiante
        String sqlGrade = """
                SELECT a.grado_id
                FROM escuela_membresia_aula m
                JOIN escuela_aula a ON a.id = m.aula_id
                WHERE m.usuario_id = :studentId AND m.estado = 'ACTIVO'
                ORDER BY a.id
                LIMIT 1
                """;
        List<String> grades = jdbc.query(sqlGrade, new MapSqlParameterSource("studentId", studentId.toString()),
                (rs, rowNum) -> rs.getString("grado_id"));
        String gradeId = grades.isEmpty() ? DEFAULT_GRADE_ID : grades.getFirst();

        // 2. Reto publicado para hoy o, si no hay, el que toca en la rotación del banco del grado
        String sqlChallenges = """
                SELECT rd.id, rd.fecha, rd.version_actividad_id, va.titulo, va.instrucciones, va.minutos_estimados
                FROM aprendizaje_reto_diario rd
                JOIN aprendizaje_version_actividad va ON va.id = rd.version_actividad_id
                WHERE rd.grado_id = :gradeId AND rd.estado = 'PUBLICADO' AND va.estado = 'PUBLICADO'
                  AND rd.fecha <= :today
                ORDER BY rd.fecha ASC, rd.id ASC
                """;
        var params = new MapSqlParameterSource("gradeId", gradeId).addValue("today", Date.valueOf(today));
        List<ChallengeRaw> bank = jdbc.query(sqlChallenges, params, (rs, rowNum) -> new ChallengeRaw(
                UUID.fromString(rs.getString("id")),
                rs.getString("fecha"),
                UUID.fromString(rs.getString("version_actividad_id")),
                rs.getString("titulo"),
                rs.getString("instrucciones"),
                rs.getInt("minutos_estimados")
        ));

        if (bank.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay un reto diario disponible hoy para tu grado.");
        }

        return bank.stream()
                .filter(c -> today.toString().equals(c.date()))
                .findFirst()
                .orElse(bank.get(rotationIndex(today, bank.size())));
    }

    @Transactional
    public DailyChallengeCompleteResponse completeTodayChallenge(UUID studentId, UUID challengeId) {
        String sqlChallenge = """
                SELECT rd.id, rd.version_actividad_id, va.titulo
                FROM aprendizaje_reto_diario rd
                JOIN aprendizaje_version_actividad va ON va.id = rd.version_actividad_id
                WHERE rd.id = :challengeId AND rd.estado = 'PUBLICADO'
                """;
        record ChallengeInfo(UUID id, UUID activityVersionId, String title) {}
        var challenge = jdbc.query(sqlChallenge, new MapSqlParameterSource("challengeId", challengeId.toString()),
                (rs, rowNum) -> new ChallengeInfo(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("version_actividad_id")),
                        rs.getString("titulo")
                )).stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Reto diario no encontrado.")
        );

        LocalDate today = LocalDate.now(LIMA_ZONE);
        if (!selectChallenge(studentId, today).id().equals(challenge.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este no es tu reto de hoy. Vuelve a abrir el reto del día.");
        }

        // Para terminar el reto hay que responder todas sus preguntas hoy (en cualquier orden).
        int totalQuestions = countQuestions(challenge.activityVersionId());
        var answers = jdbc.queryForMap("""
                SELECT COUNT(DISTINCT r.pregunta_id) answered,
                       COUNT(DISTINCT CASE WHEN r.es_correcta THEN r.pregunta_id END) correct
                FROM evaluacion_intento i
                JOIN evaluacion_respuesta r ON r.intento_id = i.id
                JOIN evaluacion_pregunta q ON q.id = r.pregunta_id AND q.version_actividad_id = i.version_actividad_id
                WHERE i.estudiante_id = :studentId
                  AND i.version_actividad_id = :actId
                  AND r.respondida_en >= :dayStart
                """, new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("actId", challenge.activityVersionId().toString())
                .addValue("dayStart", dayStart(today)));
        int answered = ((Number) answers.get("answered")).intValue();
        int correct = ((Number) answers.get("correct")).intValue();
        if (totalQuestions == 0 || answered < totalQuestions) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Responde todas las preguntas del reto antes de terminarlo (" + answered + " de " + totalQuestions + ").");
        }

        String idempotencyKey = UUID.nameUUIDFromBytes(
                (studentId + ":" + challengeId + ":" + today).getBytes(StandardCharsets.UTF_8)
        ).toString();

        boolean wasAlreadyCompleted = rewardedToday(studentId, challenge.activityVersionId(), today);
        int experienceEarned = wasAlreadyCompleted ? 0 : EXPERIENCE_REWARD;

        if (!wasAlreadyCompleted) {
            jdbc.update("""
                    INSERT IGNORE INTO gamificacion_movimiento_experiencia
                    (id, usuario_id, cantidad, motivo, referencia_tipo, referencia_id, evento_idempotencia, ocurrido_en)
                    VALUES (:id, :studentId, :amount, 'RETO', 'RETO', :actId, :idempotency, UTC_TIMESTAMP(6))
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

        return new DailyChallengeCompleteResponse(
                challengeId,
                true,
                experienceEarned,
                totalXp != null ? totalXp : 0,
                correct,
                totalQuestions,
                completionMessage(wasAlreadyCompleted, correct, totalQuestions)
        );
    }

    static String completionMessage(boolean wasAlreadyCompleted, int correct, int total) {
        if (wasAlreadyCompleted) {
            return "¡Ya habías completado el reto de hoy! Gracias por seguir practicando con entusiasmo.";
        }
        String result = correct == total
                ? "¡Acertaste las " + total + " preguntas!"
                : "Acertaste " + correct + " de " + total + ". Revisa las explicaciones para aprender de cada pregunta.";
        return result + " Completaste el reto diario y sumaste +" + EXPERIENCE_REWARD + " XP. ¡Mañana te espera uno nuevo!";
    }

    private boolean rewardedToday(UUID studentId, UUID activityVersionId, LocalDate today) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM gamificacion_movimiento_experiencia
                WHERE usuario_id = :studentId
                  AND motivo = 'RETO'
                  AND referencia_id = :actId
                  AND ocurrido_en >= :dayStart
                """, new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("actId", activityVersionId.toString())
                .addValue("dayStart", dayStart(today)), Integer.class);
        return count != null && count > 0;
    }

    private int countQuestions(UUID activityVersionId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM evaluacion_pregunta WHERE version_actividad_id = :actId",
                new MapSqlParameterSource("actId", activityVersionId.toString()), Integer.class);
        return count != null ? count : 0;
    }

    private static Timestamp dayStart(LocalDate day) {
        return Timestamp.from(day.atStartOfDay(LIMA_ZONE).toInstant());
    }
}
