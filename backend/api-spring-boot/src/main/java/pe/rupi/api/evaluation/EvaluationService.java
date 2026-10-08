package pe.rupi.api.evaluation;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.evaluation.EvaluationDtos.AnswerFeedbackResponse;
import pe.rupi.api.evaluation.EvaluationDtos.EvaluationSummaryDto;
import pe.rupi.api.evaluation.EvaluationDtos.OptionDto;
import pe.rupi.api.evaluation.EvaluationDtos.QuestionDto;
import pe.rupi.api.evaluation.EvaluationDtos.SubmitAnswerRequest;

@Service
public class EvaluationService {
    private final NamedParameterJdbcTemplate jdbc;

    public EvaluationService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<QuestionDto> findQuestionsForActivity(UUID activityVersionId) {
        String sqlQuestions = """
                SELECT id, numero_secuencia, tipo, enunciado, explicacion, puntos
                FROM evaluacion_pregunta
                WHERE version_actividad_id = :actId
                ORDER BY numero_secuencia ASC
                """;
        var params = new MapSqlParameterSource("actId", activityVersionId.toString());

        record QuestionRaw(UUID id, int sequence, String type, String statement, String explanation, BigDecimal points) {}
        List<QuestionRaw> questionsRaw = jdbc.query(sqlQuestions, params, (rs, _) -> new QuestionRaw(
                UUID.fromString(rs.getString("id")),
                rs.getInt("numero_secuencia"),
                rs.getString("tipo"),
                rs.getString("enunciado"),
                rs.getString("explicacion"),
                rs.getBigDecimal("puntos")
        ));

        if (questionsRaw.isEmpty()) {
            return List.of();
        }

        String sqlOptions = """
                SELECT o.id, o.pregunta_id, o.numero_secuencia, o.texto
                FROM evaluacion_opcion_respuesta o
                JOIN evaluacion_pregunta q ON q.id = o.pregunta_id
                WHERE q.version_actividad_id = :actId
                ORDER BY o.numero_secuencia ASC
                """;

        Map<UUID, List<OptionDto>> optionsByQuestion = new HashMap<>();
        jdbc.query(sqlOptions, params, (rs, _) -> {
            UUID qId = UUID.fromString(rs.getString("pregunta_id"));
            OptionDto opt = new OptionDto(
                    UUID.fromString(rs.getString("id")),
                    rs.getInt("numero_secuencia"),
                    rs.getString("texto")
            );
            optionsByQuestion.computeIfAbsent(qId, _ -> new ArrayList<>()).add(opt);
            return null;
        });

        List<QuestionDto> result = new ArrayList<>();
        for (var q : questionsRaw) {
            result.add(new QuestionDto(
                    q.id(),
                    q.sequence(),
                    q.type(),
                    q.statement(),
                    q.explanation(),
                    q.points(),
                    optionsByQuestion.getOrDefault(q.id(), List.of())
            ));
        }
        return result;
    }

    @Transactional
    public AnswerFeedbackResponse checkAndRecordAnswer(
            UUID studentId,
            UUID activityVersionId,
            SubmitAnswerRequest request
    ) {
        if (request.questionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe especificar la pregunta.");
        }

        // 1. Obtener detalles de la pregunta
        String sqlQuestion = """
                SELECT enunciado, explicacion, puntos
                FROM evaluacion_pregunta
                WHERE id = :qId AND version_actividad_id = :actId
                """;
        var qParams = new MapSqlParameterSource()
                .addValue("qId", request.questionId().toString())
                .addValue("actId", activityVersionId.toString());

        record QuestionInfo(String statement, String explanation, BigDecimal points) {}
        var qInfo = jdbc.query(sqlQuestion, qParams, (rs, _) -> new QuestionInfo(
                rs.getString("enunciado"),
                rs.getString("explicacion"),
                rs.getBigDecimal("puntos")
        )).stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Pregunta no encontrada para esta actividad.")
        );

        // 2. Verificar la opción seleccionada si es pregunta de opción
        boolean isCorrect = false;
        UUID correctOptionId = null;

        String sqlCorrectOpt = """
                SELECT id FROM evaluacion_opcion_respuesta
                WHERE pregunta_id = :qId AND es_correcta = TRUE
                LIMIT 1
                """;
        var correctList = jdbc.query(sqlCorrectOpt, new MapSqlParameterSource("qId", request.questionId().toString()),
                (rs, _) -> UUID.fromString(rs.getString("id")));
        if (!correctList.isEmpty()) {
            correctOptionId = correctList.getFirst();
        }

        if (request.selectedOptionId() != null) {
            String sqlCheck = """
                    SELECT es_correcta FROM evaluacion_opcion_respuesta
                    WHERE id = :optId AND pregunta_id = :qId
                    """;
            var checkParams = new MapSqlParameterSource()
                    .addValue("optId", request.selectedOptionId().toString())
                    .addValue("qId", request.questionId().toString());

            var selectedCorrect = jdbc.query(sqlCheck, checkParams, (rs, _) -> rs.getBoolean("es_correcta"));
            if (!selectedCorrect.isEmpty() && Boolean.TRUE.equals(selectedCorrect.getFirst())) {
                isCorrect = true;
            }
        }

        // 3. Crear o reusar evaluacion_intento
        String sqlFindAttempt = """
                SELECT id FROM evaluacion_intento
                WHERE estudiante_id = :studentId AND version_actividad_id = :actId AND estado = 'EN_CURSO'
                ORDER BY iniciado_en DESC LIMIT 1
                """;
        var attParams = new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("actId", activityVersionId.toString());

        List<String> attempts = jdbc.query(sqlFindAttempt, attParams, (rs, _) -> rs.getString("id"));
        String attemptId;
        if (attempts.isEmpty()) {
            attemptId = UUID.randomUUID().toString();
            String clientEventId = UUID.randomUUID().toString();
            jdbc.update("""
                    INSERT INTO evaluacion_intento
                    (id, estudiante_id, version_actividad_id, numero_intento, estado, cliente_evento_id, iniciado_en)
                    VALUES (:id, :studentId, :actId, 1, 'EN_CURSO', :clientEventId, CURRENT_TIMESTAMP(6))
                    """, new MapSqlParameterSource()
                    .addValue("id", attemptId)
                    .addValue("studentId", studentId.toString())
                    .addValue("actId", activityVersionId.toString())
                    .addValue("clientEventId", clientEventId));
        } else {
            attemptId = attempts.getFirst();
        }

        // 4. Guardar o actualizar la respuesta
        String responseId = UUID.randomUUID().toString();
        BigDecimal scoreEarned = isCorrect ? qInfo.points() : BigDecimal.ZERO;

        jdbc.update("""
                INSERT INTO evaluacion_respuesta
                (id, intento_id, pregunta_id, texto_respuesta, es_correcta, puntaje_obtenido, respondida_en)
                VALUES (:id, :intentoId, :qId, :textAns, :isCorrect, :score, CURRENT_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE
                    texto_respuesta = VALUES(texto_respuesta),
                    es_correcta = VALUES(es_correcta),
                    puntaje_obtenido = VALUES(puntaje_obtenido),
                    respondida_en = CURRENT_TIMESTAMP(6)
                """, new MapSqlParameterSource()
                .addValue("id", responseId)
                .addValue("intentoId", attemptId)
                .addValue("qId", request.questionId().toString())
                .addValue("textAns", request.textAnswer())
                .addValue("isCorrect", isCorrect)
                .addValue("score", scoreEarned));

        // Obtener el ID de la respuesta persistida para la relación N:M
        String finalResponseId = jdbc.queryForObject("""
                SELECT id FROM evaluacion_respuesta
                WHERE intento_id = :intentoId AND pregunta_id = :qId
                """, new MapSqlParameterSource()
                .addValue("intentoId", attemptId)
                .addValue("qId", request.questionId().toString()), String.class);

        if (request.selectedOptionId() != null && finalResponseId != null) {
            jdbc.update("""
                    INSERT IGNORE INTO evaluacion_respuesta_opcion (respuesta_id, pregunta_id, opcion_id)
                    VALUES (:respId, :qId, :optId)
                    """, new MapSqlParameterSource()
                    .addValue("respId", finalResponseId)
                    .addValue("qId", request.questionId().toString())
                    .addValue("optId", request.selectedOptionId().toString()));
        }

        // 5. Mensaje pedagógico empático de Rupi
        String feedbackMessage;
        if (isCorrect) {
            feedbackMessage = "¡Excelente, explorador! ¡Descubriste la respuesta correcta!";
        } else {
            feedbackMessage = qInfo.explanation() != null && !qInfo.explanation().isBlank()
                    ? "¡Buen intento! Recuerda: " + qInfo.explanation() + ". ¡Inténtalo de nuevo!"
                    : "¡Buen intento! Revisa los datos de la lección e inténtalo una vez más con calma.";
        }

        return new AnswerFeedbackResponse(
                request.questionId(),
                isCorrect,
                feedbackMessage,
                correctOptionId,
                qInfo.explanation()
        );
    }
}
