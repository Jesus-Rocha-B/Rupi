package pe.rupi.api.learningroute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static pe.rupi.api.learningroute.LearningRouteDtos.*;

class LearningRouteServiceTest {
    final UUID student = UUID.randomUUID(), route = UUID.randomUUID(), enrollment = UUID.randomUUID();
    final LearningRouteRepository repository = mock(LearningRouteRepository.class);
    final LearningRouteService service = new LearningRouteService(repository);
    void enrolled() {
        when(repository.findRouteForStudent(student, route)).thenReturn(Optional.of(
            new LearningRouteRepository.RouteHeader(route, "Ruta", new Grade(2, "Segundo"),
                new Area("MAT", "Matemática"), new Enrollment(enrollment, "ACTIVO", null))));
    }
    RouteNode node(String state) { return new RouteNode(UUID.randomUUID(), 1, "Actividad", "RETO", 8, state, false, null); }
    @Test void unavailableRouteDoesNotReadProgress() {
        when(repository.findRouteForStudent(student, route)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.getForStudent(student, route)).getStatusCode().value());
        verify(repository, never()).findNodes(any(), any());
    }
    @Test void missingProgressIsNotInventedAsLocked() {
        enrolled();
        when(repository.findNodes(enrollment, route)).thenReturn(List.of(node(null)));
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.getForStudent(student, route)).getStatusCode().value());
    }
    @Test void progressIsDerivedFromReturnedNodes() {
        enrolled();
        when(repository.findNodes(enrollment, route)).thenReturn(List.of(node("COMPLETADO"), node("DISPONIBLE"), node("EN_CURSO"), node("BLOQUEADO")));
        assertEquals(new Progress(1, 4), service.getForStudent(student, route).progress());
    }
    @Test void emptyRouteHasZeroProgress() {
        enrolled();
        when(repository.findNodes(enrollment, route)).thenReturn(List.of());
        assertEquals(new Progress(0, 0), service.getForStudent(student, route).progress());
    }
}
