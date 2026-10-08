package pe.rupi.api.evaluation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.evaluation.EvaluationDtos.SubmitAnswerRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EvaluationServiceTest {

    final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    final EvaluationService service = new EvaluationService(jdbc);

    final UUID studentId = UUID.randomUUID();
    final UUID activityVersionId = UUID.randomUUID();
    final UUID questionId = UUID.randomUUID();
    final UUID correctOptionId = UUID.randomUUID();
    final UUID incorrectOptionId = UUID.randomUUID();

    @Test
    void submitAnswerRequiresQuestionId() {
        var request = new SubmitAnswerRequest(null, correctOptionId, null);
        var ex = assertThrows(ResponseStatusException.class, () ->
                service.checkAndRecordAnswer(studentId, activityVersionId, request));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void checkAndRecordAnswerWithCorrectOptionReturnsPositiveFeedback() {
        var request = new SubmitAnswerRequest(questionId, correctOptionId, null);

        // Mock 1: Pregunta existe
        doAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("FROM evaluacion_pregunta WHERE id = :qId")) {
                var rs = mock(java.sql.ResultSet.class);
                when(rs.getString("enunciado")).thenReturn("¿Cuánto es 5 + 4?");
                when(rs.getString("explicacion")).thenReturn("5 + 4 = 9");
                when(rs.getBigDecimal("puntos")).thenReturn(BigDecimal.ONE);
                RowMapper<?> mapper = call.getArgument(2);
                return List.of(mapper.mapRow(rs, 0));
            }
            if (sql.contains("WHERE pregunta_id = :qId AND es_correcta = TRUE")) {
                var rs = mock(java.sql.ResultSet.class);
                when(rs.getString("id")).thenReturn(correctOptionId.toString());
                RowMapper<?> mapper = call.getArgument(2);
                return List.of(mapper.mapRow(rs, 0));
            }
            if (sql.contains("WHERE id = :optId AND pregunta_id = :qId")) {
                var rs = mock(java.sql.ResultSet.class);
                when(rs.getBoolean("es_correcta")).thenReturn(true);
                RowMapper<?> mapper = call.getArgument(2);
                return List.of(mapper.mapRow(rs, 0));
            }
            if (sql.contains("FROM evaluacion_intento")) {
                return List.of(UUID.randomUUID().toString());
            }
            return List.of();
        }).when(jdbc).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));

        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(String.class)))
                .thenReturn(UUID.randomUUID().toString());

        var feedback = service.checkAndRecordAnswer(studentId, activityVersionId, request);

        assertNotNull(feedback);
        assertTrue(feedback.isCorrect());
        assertEquals(questionId, feedback.questionId());
        assertEquals(correctOptionId, feedback.correctOptionId());
        assertTrue(feedback.rupiFeedback().contains("Excelente"));
    }

    @Test
    void checkAndRecordAnswerWithIncorrectOptionReturnsFriendlyExplanation() {
        var request = new SubmitAnswerRequest(questionId, incorrectOptionId, null);

        doAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("FROM evaluacion_pregunta WHERE id = :qId")) {
                var rs = mock(java.sql.ResultSet.class);
                when(rs.getString("enunciado")).thenReturn("¿Cuánto es 10 - 3?");
                when(rs.getString("explicacion")).thenReturn("Restamos 10 - 3 = 7");
                when(rs.getBigDecimal("puntos")).thenReturn(BigDecimal.ONE);
                RowMapper<?> mapper = call.getArgument(2);
                return List.of(mapper.mapRow(rs, 0));
            }
            if (sql.contains("WHERE pregunta_id = :qId AND es_correcta = TRUE")) {
                var rs = mock(java.sql.ResultSet.class);
                when(rs.getString("id")).thenReturn(correctOptionId.toString());
                RowMapper<?> mapper = call.getArgument(2);
                return List.of(mapper.mapRow(rs, 0));
            }
            if (sql.contains("WHERE id = :optId AND pregunta_id = :qId")) {
                var rs = mock(java.sql.ResultSet.class);
                when(rs.getBoolean("es_correcta")).thenReturn(false);
                RowMapper<?> mapper = call.getArgument(2);
                return List.of(mapper.mapRow(rs, 0));
            }
            if (sql.contains("FROM evaluacion_intento")) {
                return List.of(UUID.randomUUID().toString());
            }
            return List.of();
        }).when(jdbc).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));

        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(String.class)))
                .thenReturn(UUID.randomUUID().toString());

        var feedback = service.checkAndRecordAnswer(studentId, activityVersionId, request);

        assertNotNull(feedback);
        assertFalse(feedback.isCorrect());
        assertEquals(correctOptionId, feedback.correctOptionId());
        assertTrue(feedback.rupiFeedback().contains("Buen intento"));
        assertTrue(feedback.rupiFeedback().contains("Restamos 10 - 3 = 7"));
    }
}
