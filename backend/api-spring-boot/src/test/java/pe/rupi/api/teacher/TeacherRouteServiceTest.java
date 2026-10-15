package pe.rupi.api.teacher;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TeacherRouteServiceTest {

    private TeacherRouteRepository repository;
    private TeacherRouteService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(TeacherRouteRepository.class);
        service = new TeacherRouteService(repository);
    }

    @Test
    @DisplayName("Crea un roadmap exitosamente con título, grado y área válidos")
    void createRouteSuccess() {
        var req = new TeacherDtos.CreateRouteRequest("Lecturas de Huanta", "Ruta de comprensión lectora", 3, "COM");
        var expectedResponse = new TeacherDtos.CreateRouteResponse("r-1", "vr-1", "Lecturas de Huanta", "BORRADOR", "OK");

        when(repository.createRoute(eq("profesor-1"), any())).thenReturn(expectedResponse);

        var response = service.createRoute("profesor-1", req);

        assertNotNull(response);
        assertEquals("r-1", response.routeId());
        assertEquals("Lecturas de Huanta", response.title());
        verify(repository, times(1)).createRoute("profesor-1", req);
    }

    @Test
    @DisplayName("Rechaza creación de roadmap con título vacío o demasiado corto")
    void createRouteRejectsShortTitle() {
        var req = new TeacherDtos.CreateRouteRequest("ab", "Descripción", 3, "COM");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.createRoute("profesor-1", req)
        );

        assertTrue(ex.getReason().contains("al menos 3 caracteres"));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Rechaza creación con grado fuera del rango 1 a 6 de primaria")
    void createRouteRejectsInvalidGrade() {
        var req = new TeacherDtos.CreateRouteRequest("Ruta Secundaria", "Descripción", 7, "COM");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.createRoute("profesor-1", req)
        );

        assertTrue(ex.getReason().contains("1.° y 6.° de primaria"));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Rechaza creación sin área curricular seleccionada")
    void createRouteRejectsBlankArea() {
        var req = new TeacherDtos.CreateRouteRequest("Ruta Válida", "Descripción", 2, "   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.createRoute("profesor-1", req)
        );

        assertTrue(ex.getReason().contains("área curricular"));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Agrega parada pedagógica a la ruta exitosamente")
    void addNodeSuccess() {
        var req = new TeacherDtos.CreateNodeRequest("Parada 1: Cuento Andino", "Lee con atención", "LECCION", 15, 1, 50.0, 20.0);
        when(repository.addNodeToRoute(eq("profesor-1"), eq("r-1"), eq(req))).thenReturn("node-123");

        String nodeId = service.addNode("profesor-1", "r-1", req);

        assertEquals("node-123", nodeId);
        verify(repository, times(1)).addNodeToRoute("profesor-1", "r-1", req);
    }

    @Test
    @DisplayName("Adjunta material educativo a una parada exitosamente")
    void attachResourceSuccess() {
        var req = new TeacherDtos.AttachResourceRequest("DOCUMENTO", "Ficha de lectura PDF", "/api/v1/materials/mat-1/download", "mat-1");
        when(repository.attachResourceToNode(eq("profesor-1"), eq("node-1"), eq(req))).thenReturn("res-999");

        String resourceId = service.attachResource("profesor-1", "node-1", req);

        assertEquals("res-999", resourceId);
        verify(repository, times(1)).attachResourceToNode("profesor-1", "node-1", req);
    }

    @Test
    @DisplayName("Rechaza un tipo de actividad fuera del catálogo con 400, sin tocar la base")
    void addNodeRejectsUnknownActivityType() {
        var req = new TeacherDtos.CreateNodeRequest("Parada válida", null, "VIDEOJUEGO", 10, null, null, null);

        var ex = assertThrows(ResponseStatusException.class, () -> service.addNode("profesor-1", "r-1", req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Rechaza enlaces javascript:, data: o relativos que el alumno abriría")
    void attachResourceRejectsUnsafeUrls() {
        for (String url : List.of("javascript:alert(1)", "data:text/html,hola", "/otra/ruta", "ftp://servidor/x.pdf", "https://")) {
            var req = new TeacherDtos.AttachResourceRequest("ENLACE", "Enlace", url, null);
            var ex = assertThrows(ResponseStatusException.class, () -> service.attachResource("profesor-1", "node-1", req), url);
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode(), url);
        }
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Acepta enlaces web y descargas de materiales de RUPI")
    void attachResourceAcceptsWebAndRupiMaterialUrls() {
        assertTrue(TeacherRouteService.isSafeResourceUrl("https://www.perueduca.pe/recurso"));
        assertTrue(TeacherRouteService.isSafeResourceUrl("http://localhost:8081/doc.pdf"));
        assertTrue(TeacherRouteService.isSafeResourceUrl("/api/v1/materials/d5785bf3-50df-4ec2-a9bc-4874c52e04f7/download"));
    }

    @Test
    @DisplayName("Rechaza un tipo de material fuera del catálogo con 400")
    void attachResourceRejectsUnknownType() {
        var req = new TeacherDtos.AttachResourceRequest("VIDEO", "Video", "https://www.youtube.com/watch?v=1", null);

        var ex = assertThrows(ResponseStatusException.class, () -> service.attachResource("profesor-1", "node-1", req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Historia 8: pausa un roadmap publicado")
    void pauseRouteSuccess() {
        var expected = new TeacherDtos.PublishRouteResponse("r-1", "vr-1", "PAUSADO", "2026-10-13T12:00:00Z", "En pausa");
        when(repository.pauseRoute("profesor-1", "r-1")).thenReturn(expected);

        assertEquals("PAUSADO", service.pauseRoute("profesor-1", "r-1").state());
    }

    @Test
    @DisplayName("Historia 8: pausar una ruta que no está publicada responde 409")
    void pauseRouteNotPublishedIsConflict() {
        when(repository.pauseRoute("profesor-1", "r-1")).thenThrow(new IllegalStateException("Solo se puede pausar un roadmap publicado."));

        var ex = assertThrows(ResponseStatusException.class, () -> service.pauseRoute("profesor-1", "r-1"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("Historia 8: pausar una ruta ajena responde 404")
    void pauseForeignRouteIsNotFound() {
        when(repository.pauseRoute("profesor-1", "ajena")).thenThrow(new IllegalArgumentException("Ruta no encontrada o no pertenece al docente."));

        var ex = assertThrows(ResponseStatusException.class, () -> service.pauseRoute("profesor-1", "ajena"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Publica roadmap exitosamente")
    void publishRouteSuccess() {
        var expectedResponse = new TeacherDtos.PublishRouteResponse("r-1", "vr-1", "PUBLICADO", "2026-10-13T12:00:00Z", "Publicado");
        when(repository.publishRoute("profesor-1", "r-1")).thenReturn(expectedResponse);

        var response = service.publishRoute("profesor-1", "r-1");

        assertNotNull(response);
        assertEquals("PUBLICADO", response.state());
        verify(repository, times(1)).publishRoute("profesor-1", "r-1");
    }

    @Test
    @DisplayName("Lanza 404 si la ruta solicitada no existe o no pertenece al docente")
    void getRouteDetailNotFound() {
        when(repository.findTeacherRouteDetail("profesor-1", "r-inexistente")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                service.getRouteDetail("profesor-1", "r-inexistente")
        );
    }

    @Test
    @DisplayName("Reordena paradas del roadmap exitosamente y retorna el detalle actualizado")
    void reorderNodesSuccess() {
        var nodeIds = List.of("node-2", "node-1", "node-3");
        var req = new TeacherDtos.ReorderNodesRequest(nodeIds);

        var mockDetail = new TeacherDtos.TeacherRouteDetail(
                "r-1", "vr-1", "Ruta Reordenada", "Desc",
                new TeacherDtos.GradeInfo(2, "2.° de primaria"),
                new TeacherDtos.AreaInfo("MAT", "Matemática"),
                "BORRADOR",
                List.of(
                        new TeacherDtos.TeacherNodeDto("node-2", 1, "Parada B", "Inst", "LECCION", 10, 50.0, 15.0, List.of()),
                        new TeacherDtos.TeacherNodeDto("node-1", 2, "Parada A", "Inst", "LECCION", 15, 75.0, 30.0, List.of()),
                        new TeacherDtos.TeacherNodeDto("node-3", 3, "Parada C", "Inst", "LECCION", 20, 25.0, 45.0, List.of())
                ),
                "2026-10-13T10:00:00Z", null
        );

        when(repository.findTeacherRouteDetail("profesor-1", "r-1")).thenReturn(Optional.of(mockDetail));

        var result = service.reorderNodes("profesor-1", "r-1", req);

        assertNotNull(result);
        assertEquals(3, result.nodes().size());
        assertEquals("node-2", result.nodes().get(0).id());
        assertEquals(1, result.nodes().get(0).sequence());
        assertEquals("node-1", result.nodes().get(1).id());
        assertEquals(2, result.nodes().get(1).sequence());
        verify(repository, times(1)).reorderNodes("profesor-1", "r-1", nodeIds);
    }

    @Test
    @DisplayName("Rechaza reordenamiento si la lista de paradas es nula o vacía")
    void reorderNodesEmptyThrowsBadRequest() {
        assertThrows(ResponseStatusException.class, () ->
                service.reorderNodes("profesor-1", "r-1", new TeacherDtos.ReorderNodesRequest(List.of()))
        );
        assertThrows(ResponseStatusException.class, () ->
                service.reorderNodes("profesor-1", "r-1", null)
        );
    }

    @Test
    @DisplayName("Mapea IllegalArgumentException del repository a BAD_REQUEST al reordenar")
    void reorderNodesRepositoryErrorThrowsBadRequest() {
        var req = new TeacherDtos.ReorderNodesRequest(List.of("node-999"));
        doThrow(new IllegalArgumentException("El nodo no pertenece a este roadmap"))
                .when(repository).reorderNodes("profesor-1", "r-1", req.nodeIds());

        var ex = assertThrows(ResponseStatusException.class, () ->
                service.reorderNodes("profesor-1", "r-1", req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
