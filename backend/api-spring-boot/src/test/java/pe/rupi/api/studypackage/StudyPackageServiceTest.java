package pe.rupi.api.studypackage;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.learningroute.LearningRouteDtos.Area;
import pe.rupi.api.learningroute.LearningRouteDtos.Competency;
import pe.rupi.api.learningroute.LearningRouteDtos.Curriculum;
import pe.rupi.api.learningroute.LearningRouteDtos.Enrollment;
import pe.rupi.api.learningroute.LearningRouteDtos.Grade;
import pe.rupi.api.learningroute.LearningRouteDtos.Progress;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteDetailResponse;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteNode;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteUnit;
import pe.rupi.api.learningroute.LearningRouteService;
import pe.rupi.api.material.MaterialStorageService;
import pe.rupi.api.studypackage.StudyPackageModel.Block;
import pe.rupi.api.studypackage.StudyPackageModel.Stop;
import pe.rupi.api.studypackage.StudyPackageModel.Unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class StudyPackageServiceTest {
    private static final UUID STUDENT = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ROUTE = UUID.fromString("a1000000-0000-4000-8000-000000000001");
    private static final UUID UNIT = UUID.fromString("c1000000-0000-4000-8000-000000000001");
    private static final UUID NODE_1 = UUID.fromString("d1000000-0000-4000-8000-000000000001");
    private static final UUID NODE_2 = UUID.fromString("d1000000-0000-4000-8000-000000000002");

    @TempDir
    Path dir;

    private LearningRouteService routes;
    private StudyPackageRepository repository;
    private StudyPackageService service;

    @BeforeEach
    void setUp() {
        routes = Mockito.mock(LearningRouteService.class);
        repository = Mockito.mock(StudyPackageRepository.class);
        var materials = Mockito.mock(MaterialStorageService.class);
        var clock = Clock.fixed(Instant.parse("2026-10-13T15:00:00Z"), ZoneOffset.UTC);
        var cache = new StudyPackageCache(dir, Duration.ofHours(24), clock);
        service = new StudyPackageService(routes, repository, materials, cache, new StudyPackageBuilder(), clock);
        when(routes.getForStudent(STUDENT, ROUTE)).thenReturn(route());
        when(repository.findFiles(anyCollection())).thenReturn(List.of());
    }

    private static RouteNode node(UUID id, int sequence) {
        return new RouteNode(id, sequence, "Parada " + sequence, "LECCION", 10, "DISPONIBLE", false, null);
    }

    private static RouteDetailResponse route() {
        var unit = new RouteUnit(UNIT, "MAT-3-U1", "Números en la feria", 1, 1, 0,
                List.of(new Competency("C1", "Resuelve problemas de cantidad")), List.of(node(NODE_1, 1)));
        var loose = new RouteUnit(null, "OTRAS", "Otras paradas", null, 1, 0, List.of(), List.of(node(NODE_2, 2)));
        return new RouteDetailResponse(ROUTE, "Matemática en Ayacucho", new Grade(3, "3.° grado"),
                new Area("MAT", "Matemática"), new Enrollment(UUID.randomUUID(), "ACTIVO", null),
                new Progress(0, 2), List.of(node(NODE_1, 1), node(NODE_2, 2)), null,
                new Curriculum("PARCIAL", null, List.of(unit, loose)));
    }

    private static Stop stop(int sequence, String text) {
        return new Stop(sequence, "Parada " + sequence, false, 10, null,
                List.of(new Block("TEXTO", text, null, null)), List.of(), List.of());
    }

    @Test
    void resourcesRespectRouteAuthorizationAndGrade() {
        var entries = service.resourcesForRoute(STUDENT, ROUTE);
        assertEquals(3, entries.getFirst().grade());
        assertEquals("MATEMATICA", entries.getFirst().areaCode());
        when(routes.getForStudent(STUDENT, ROUTE)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThrows(ResponseStatusException.class, () -> service.resourcesForRoute(STUDENT, ROUTE));
    }

    @Test
    @DisplayName("Arma el paquete de la unidad pedida solo con sus paradas")
    void buildsPackageForUnit() {
        when(repository.findStops(ROUTE, List.of(NODE_1))).thenReturn(List.of(stop(1, "Contamos retablos.")));

        var pack = service.packageForUnit(STUDENT, ROUTE, UNIT.toString());

        assertEquals("rupi-matematica-en-ayacucho-mat-3-u1.zip", pack.fileName());
        assertFalse(pack.fromCache());
        assertTrue(pack.sizeBytes() > 0);
        assertEquals(64, pack.sha256().length());
    }

    @Test
    @DisplayName("La segunda descarga de la misma unidad sale de la caché")
    void repeatedDownloadIsCached() {
        when(repository.findStops(ROUTE, List.of(NODE_1))).thenReturn(List.of(stop(1, "Contamos retablos.")));

        var first = service.packageForUnit(STUDENT, ROUTE, UNIT.toString());
        var second = service.packageForUnit(STUDENT, ROUTE, UNIT.toString());

        assertTrue(second.fromCache());
        assertEquals(first.sha256(), second.sha256());
    }

    @Test
    @DisplayName("Si el docente cambia el contenido, cambia la versión del paquete")
    void contentChangeInvalidatesCache() {
        when(repository.findStops(ROUTE, List.of(NODE_1))).thenReturn(List.of(stop(1, "Contamos retablos.")));
        var before = service.packageForUnit(STUDENT, ROUTE, UNIT.toString());

        when(repository.findStops(ROUTE, List.of(NODE_1))).thenReturn(List.of(stop(1, "Contamos mantas.")));
        var after = service.packageForUnit(STUDENT, ROUTE, UNIT.toString());

        assertNotEquals(before.contentKey(), after.contentKey());
        assertFalse(after.fromCache());
    }

    @Test
    @DisplayName("La clave depende del orden de las paradas")
    void keyDependsOnOrder() {
        Unit ordered = new Unit("R", "3.°", "Mat", "U1", "Unidad", List.of(), List.of(stop(1, "a"), stop(2, "b")));
        Unit swapped = new Unit("R", "3.°", "Mat", "U1", "Unidad", List.of(), List.of(stop(2, "b"), stop(1, "a")));

        assertEquals(StudyPackageService.contentKey(ordered, List.of()), StudyPackageService.contentKey(ordered, List.of()));
        assertNotEquals(StudyPackageService.contentKey(ordered, List.of()), StudyPackageService.contentKey(swapped, List.of()));
    }

    @Test
    @DisplayName("Las paradas sin unidad se descargan con la clave «otras»")
    void looseStopsUseOtrasKey() {
        when(repository.findStops(ROUTE, List.of(NODE_2))).thenReturn(List.of(stop(2, "Repaso final.")));

        var pack = service.packageForUnit(STUDENT, ROUTE, "otras");

        assertEquals("rupi-matematica-en-ayacucho-otras-paradas.zip", pack.fileName());
    }

    @Test
    @DisplayName("Una unidad que no es de la ruta responde 404")
    void unknownUnitIsNotFound() {
        var error = assertThrows(ResponseStatusException.class,
                () -> service.packageForUnit(STUDENT, ROUTE, UUID.randomUUID().toString()));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());

        var invalid = assertThrows(ResponseStatusException.class,
                () -> service.packageForUnit(STUDENT, ROUTE, "no-es-uuid"));
        assertEquals(HttpStatus.NOT_FOUND, invalid.getStatusCode());
    }

    @Test
    @DisplayName("Sin inscripción no hay paquete: se respeta el 404 de la ruta")
    void notEnrolledIsNotFound() {
        UUID other = UUID.randomUUID();
        when(routes.getForStudent(eq(other), any())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        var error = assertThrows(ResponseStatusException.class, () -> service.packageForUnit(other, ROUTE, UNIT.toString()));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    @DisplayName("Una unidad sin lecciones publicadas responde 409")
    void unitWithoutPublishedStopsIsConflict() {
        when(repository.findStops(ROUTE, List.of(NODE_1))).thenReturn(List.of());

        var error = assertThrows(ResponseStatusException.class, () -> service.packageForUnit(STUDENT, ROUTE, UNIT.toString()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}
