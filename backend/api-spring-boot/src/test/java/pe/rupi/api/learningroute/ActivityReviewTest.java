package pe.rupi.api.learningroute;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.evaluation.EvaluationService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityReviewTest {
    final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    final EvaluationService evaluation = mock(EvaluationService.class);
    final ActivityService service = new ActivityService(jdbc, evaluation);

    @Test void inaccessibleReviewDoesNotExposeAnswersOrWriteProgress() {
        doReturn(List.of()).when(jdbc).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())).getStatusCode().value());
        verifyNoInteractions(evaluation);
        verify(jdbc, never()).update(anyString(), any(SqlParameterSource.class));
    }

    @Test void reviewUsesStudentScopeAndDoesNotStartOrCompleteActivity() {
        var student = UUID.randomUUID(); var activity = UUID.randomUUID();
        doAnswer(call -> {
            String sql = call.getArgument(0);
            SqlParameterSource params = call.getArgument(1);
            if (sql.contains("evaluacion_banco_repaso")) {
                assertTrue(sql.contains("g.numero_grado=b.grado"));
                assertTrue(sql.contains("b.curso=CASE"));
                assertTrue(sql.contains("b.activo=TRUE"));
                return List.of();
            }
            assertTrue(sql.contains("p.estado='COMPLETADO'"));
            assertTrue(sql.contains("i.estudiante_id=:student"));
            assertTrue(sql.contains("vr.estado='PUBLICADO'"));
            assertEquals(student.toString(), params.getValue("student"));
            return List.of(activity.toString());
        }).when(jdbc).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
        when(evaluation.findQuestionsForActivity(activity)).thenReturn(List.of());
        assertTrue(service.review(student, UUID.randomUUID(), UUID.randomUUID()).questions().isEmpty());
        verifyNoInteractions(evaluation);
        verify(jdbc, never()).update(anyString(), any(SqlParameterSource.class));
    }
}
