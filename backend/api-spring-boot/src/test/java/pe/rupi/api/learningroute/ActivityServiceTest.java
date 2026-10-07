package pe.rupi.api.learningroute;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityServiceTest {
    final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    final ActivityService service = new ActivityService(jdbc);
    final UUID student = UUID.randomUUID(), route = UUID.randomUUID(), node = UUID.randomUUID();
    void fixture(String state, String instructions, List<ActivityService.Block> blocks) {
        doAnswer(call -> {
            String sql = call.getArgument(0);
            return sql.contains("FOR UPDATE") ? List.of(new ActivityService.Access("enrollment", "activity", "Sumas", instructions, state)) : blocks;
        }).when(jdbc).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
    }
    @Test void unavailableEnrollmentDoesNotWrite() {
        doReturn(List.of()).when(jdbc).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.start(student, route, node)).getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(SqlParameterSource.class));
    }
    @Test void blockedActivityDoesNotWriteOrReturnContent() {
        fixture("BLOQUEADO", "Contenido", List.of());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.start(student, route, node)).getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(SqlParameterSource.class));
        verify(jdbc, times(1)).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
    }
    @Test void emptyActivityDoesNotPretendToStart() {
        fixture("DISPONIBLE", null, List.of());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.start(student, route, node)).getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(SqlParameterSource.class));
    }
    @Test void availableActivityStartsAndReturnsItsContent() {
        var blocks = List.of(new ActivityService.Block("block", "TEXTO", "Dos más dos", null, null));
        fixture("DISPONIBLE", null, blocks);
        var result = service.start(student, route, node);
        assertEquals("EN_CURSO", result.state()); assertEquals(blocks, result.blocks());
        assertEquals(node.toString(), result.nodeId());
        verify(jdbc, times(2)).update(anyString(), any(SqlParameterSource.class));
    }
    @Test void reopeningCompletedActivityPreservesCompletion() {
        fixture("COMPLETADO", "Repaso", List.of());
        assertEquals("COMPLETADO", service.start(student, route, node).state());
    }
    @Test void reopeningActivityPreservesInProgressState() {
        fixture("EN_CURSO", "Repaso", List.of());
        assertEquals("EN_CURSO", service.start(student, route, node).state());
    }
}
