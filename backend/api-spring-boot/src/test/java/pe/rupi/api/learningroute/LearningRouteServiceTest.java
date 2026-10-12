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
            new LearningRouteRepository.RouteHeader(route, route, "Ruta", new Grade(2, "Segundo"),
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
    @Test void culturalContextIsIncludedWhenAvailable() {
        enrolled();
        when(repository.findNodes(enrollment, route)).thenReturn(List.of(node("COMPLETADO")));
        var context = new CulturalContext(UUID.randomUUID(), "AYACUCHO_PLAZA", "Ayacucho", "Plaza", "Hecho con raíces",
                "Desc", "No hay prisa", "/img.jpg", "Alt", "Autor", "Fuente", "CC BY-SA 4.0", "http://lic", List.of());
        when(repository.findCulturalContextForRoute(route)).thenReturn(Optional.of(context));
        var result = service.getForStudent(student, route);
        assertNotNull(result.culturalContext());
        assertEquals("Ayacucho", result.culturalContext().city());
    }
    @Test void culturalContextIsNullWhenMissing() {
        enrolled();
        when(repository.findNodes(enrollment, route)).thenReturn(List.of(node("COMPLETADO")));
        when(repository.findCulturalContextForRoute(route)).thenReturn(Optional.empty());
        var result = service.getForStudent(student, route);
        assertNull(result.culturalContext());
    }
    @Test void resumeUsesTheRememberedNodeRatherThanFirstInProgress() {
        var first = node("EN_CURSO"); var last = node("EN_CURSO");
        when(repository.findRouteForStudent(student, route)).thenReturn(Optional.of(
            new LearningRouteRepository.RouteHeader(route, route, "Ruta", new Grade(2, "Segundo"),
                new Area("MAT", "Matemática"), new Enrollment(enrollment, "ACTIVO", last.id()))));
        when(repository.findNodes(enrollment, route)).thenReturn(List.of(first, last));
        assertEquals(last.id(), service.getForStudent(student, route).enrollment().lastVisitedNodeId());
    }
    @Test void blockedRememberedNodeFallsBackToAccessibleNode() {
        var blocked = node("BLOQUEADO"); var available = node("DISPONIBLE");
        when(repository.findRouteForStudent(student, route)).thenReturn(Optional.of(
            new LearningRouteRepository.RouteHeader(route, route, "Ruta", new Grade(2, "Segundo"),
                new Area("MAT", "Matemática"), new Enrollment(enrollment, "ACTIVO", blocked.id()))));
        when(repository.findNodes(enrollment, route)).thenReturn(List.of(blocked, available));
        assertEquals(available.id(), service.getForStudent(student, route).enrollment().lastVisitedNodeId());
    }
    @Test void searchAlternativeRoutesReturnsMatchingResults() {
        var resultRoute = new RouteSearchResult(
                UUID.randomUUID(), "Comunicación: Cuentos del Valle", "Desc",
                2, "2.° de primaria", "COM", "Comunicación", 5, false, "NO_INSCRITO"
        );
        when(repository.findSubjectSuggestions("comun", 6)).thenReturn(List.of("Comunicación"));
        when(repository.searchAlternativeRoutes(student, "comun", List.of(2), List.of(), 20)).thenReturn(List.of(resultRoute));

        var response = service.searchAlternativeRoutes(student, "  comun  ", 2, 20);

        assertEquals("comun", response.query());
        assertEquals(List.of("Comunicación"), response.suggestedSubjects());
        assertEquals(1, response.routes().size());
        assertEquals("Comunicación: Cuentos del Valle", response.routes().getFirst().routeTitle());
        assertFalse(response.routes().getFirst().isEnrolled());
    }
    @Test void searchAlternativeRoutesWithMultipleGradesAndAreas() {
        var result1 = new RouteSearchResult(
                UUID.randomUUID(), "Matemática 3.°: Desafíos Wari", "Desc",
                3, "3.° de primaria", "MAT", "Matemática", 6, true, "ACTIVO"
        );
        var result2 = new RouteSearchResult(
                UUID.randomUUID(), "Matemática 4.°: Fracciones en ferias", "Desc",
                4, "4.° de primaria", "MAT", "Matemática", 6, false, "NO_INSCRITO"
        );
        when(repository.findSubjectSuggestions("", 6)).thenReturn(List.of("Matemática", "Comunicación"));
        when(repository.searchAlternativeRoutes(student, "", List.of(3, 4), List.of("MATEMATICA"), 20))
                .thenReturn(List.of(result1, result2));

        var response = service.searchAlternativeRoutes(student, "", List.of(3, 4), List.of("MATEMATICA"), 20);

        assertEquals(2, response.routes().size());
        assertEquals(List.of(3, 4), response.selectedGrades());
        assertEquals(List.of("MATEMATICA"), response.selectedAreas());
        assertEquals(2, response.totalResults());
        assertTrue(response.routes().getFirst().isEnrolled());
        verify(repository).searchAlternativeRoutes(student, "", List.of(3, 4), List.of("MATEMATICA"), 20);
    }
    @Test void getSubjectSuggestionsSanitizesQuery() {
        when(repository.findSubjectSuggestions("mate", 5)).thenReturn(List.of("Matemática"));
        var suggestions = service.getSubjectSuggestions("  mate  ", 5);
        assertEquals(List.of("Matemática"), suggestions);
        verify(repository).findSubjectSuggestions("mate", 5);
    }

    @Test void joinByCodeSuccessNewEnrollment() {
        var codeId = UUID.randomUUID();
        var routeVersionId = UUID.randomUUID();
        var details = new LearningRouteRepository.AccessCodeDetails(
                codeId, routeVersionId, "Matemática 3.°", new Grade(3, "3.° de primaria"),
                new Area("MAT", "Matemática"), java.time.Instant.now().minusSeconds(3600),
                java.time.Instant.now().plusSeconds(3600), 100, null, true, false, 5
        );
        when(repository.findAccessCode("WARI2026")).thenReturn(Optional.of(details));
        when(repository.isStudentEnrolled(student, routeVersionId)).thenReturn(false);

        var response = service.joinByCode(student, "  wari2026  ");

        assertNotNull(response);
        assertEquals(routeVersionId, response.versionRouteId());
        assertEquals("Matemática 3.°", response.routeTitle());
        assertFalse(response.alreadyEnrolled());
        assertTrue(response.message().contains("exitosamente"));
        verify(repository).enrollStudentWithAccessCode(student, routeVersionId, codeId);
    }

    @Test void joinByCodeSuccessAlreadyEnrolled() {
        var codeId = UUID.randomUUID();
        var routeVersionId = UUID.randomUUID();
        var details = new LearningRouteRepository.AccessCodeDetails(
                codeId, routeVersionId, "Matemática 3.°", new Grade(3, "3.° de primaria"),
                new Area("MAT", "Matemática"), null, null, null, null, true, false, 0
        );
        when(repository.findAccessCode("WARI2026")).thenReturn(Optional.of(details));
        when(repository.isStudentEnrolled(student, routeVersionId)).thenReturn(true);

        var response = service.joinByCode(student, "WARI2026");

        assertNotNull(response);
        assertTrue(response.alreadyEnrolled());
        assertTrue(response.message().contains("Ya formas parte"));
        verify(repository).linkAccessCodeToEnrollment(student, routeVersionId, codeId);
        verify(repository, never()).enrollStudentWithAccessCode(any(), any(), any());
    }

    @Test void joinByCodeInvalidFormatThrowsBadRequest() {
        var ex1 = assertThrows(ResponseStatusException.class, () -> service.joinByCode(student, null));
        assertEquals(400, ex1.getStatusCode().value());

        var ex2 = assertThrows(ResponseStatusException.class, () -> service.joinByCode(student, "ab"));
        assertEquals(400, ex2.getStatusCode().value());

        var ex3 = assertThrows(ResponseStatusException.class, () -> service.joinByCode(student, "codigo!invalido"));
        assertEquals(400, ex3.getStatusCode().value());
    }

    @Test void joinByCodeNotFoundThrowsNotFound() {
        when(repository.findAccessCode("NOEXISTE123")).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class, () -> service.joinByCode(student, "NOEXISTE123"));
        assertEquals(404, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("no existe"));
    }

    @Test void joinByCodeExpiredThrowsBadRequest() {
        var details = new LearningRouteRepository.AccessCodeDetails(
                UUID.randomUUID(), UUID.randomUUID(), "Ruta", new Grade(2, "2.°"),
                new Area("MAT", "Mat"), null, java.time.Instant.now().minusSeconds(3600),
                null, null, true, false, 0
        );
        when(repository.findAccessCode("EXPIRADO")).thenReturn(Optional.of(details));
        var ex = assertThrows(ResponseStatusException.class, () -> service.joinByCode(student, "EXPIRADO"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("ya venció"));
    }

    @Test void joinByCodeRevokedThrowsBadRequest() {
        var details = new LearningRouteRepository.AccessCodeDetails(
                UUID.randomUUID(), UUID.randomUUID(), "Ruta", new Grade(2, "2.°"),
                new Area("MAT", "Mat"), null, null, null, java.time.Instant.now().minusSeconds(60),
                true, false, 0
        );
        when(repository.findAccessCode("REVOCADO")).thenReturn(Optional.of(details));
        var ex = assertThrows(ResponseStatusException.class, () -> service.joinByCode(student, "REVOCADO"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("revocado"));
    }

    @Test void joinByCodeMaxUsesReachedThrowsBadRequest() {
        var details = new LearningRouteRepository.AccessCodeDetails(
                UUID.randomUUID(), UUID.randomUUID(), "Ruta", new Grade(2, "2.°"),
                new Area("MAT", "Mat"), null, null, 10, null, true, false, 10
        );
        when(repository.findAccessCode("LLENO10")).thenReturn(Optional.of(details));
        var ex = assertThrows(ResponseStatusException.class, () -> service.joinByCode(student, "LLENO10"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("límite máximo"));
    }
}

