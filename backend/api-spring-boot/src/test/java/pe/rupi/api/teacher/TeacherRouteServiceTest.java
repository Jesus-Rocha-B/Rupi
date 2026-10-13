package pe.rupi.api.teacher;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
}
