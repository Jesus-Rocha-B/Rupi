package pe.rupi.api.learningroute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
<<<<<<< HEAD
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static pe.rupi.api.learningroute.LearningRouteDtos.*;
import static pe.rupi.api.learningroute.RouteCurriculum.*;

class RouteCurriculumTest {
    final UUID catalog = UUID.randomUUID(), grade = UUID.randomUUID(), area = UUID.randomUUID();
    final UUID unit1 = UUID.randomUUID(), unit2 = UUID.randomUUID();

    RouteNode node(int sequence) {
        return new RouteNode(UUID.randomUUID(), sequence, "Actividad " + sequence, "LECCION", 8, "DISPONIBLE", false, null);
    }
    UnitRow unit(UUID id, String code, Integer sequence) { return new UnitRow(id, code, "Unidad " + code, sequence, catalog, grade, area); }
    Data data(String state, List<NodeUnit> links, List<UnitRow> units, List<CompetencyRow> competencies) {
        return new Data(catalog, state, grade, area, links, units, competencies);
    }

    @Test void groupsNodesByUnitInCurricularOrderNotInInsertionOrder() {
        var a = node(1); var b = node(2); var c = node(3);
        var result = build(Optional.of(data("ACTIVO",
                List.of(new NodeUnit(a.id(), unit2), new NodeUnit(b.id(), unit1), new NodeUnit(c.id(), unit1)),
                List.of(unit(unit2, "U2", 2), unit(unit1, "U1", 1)), List.of())), List.of(c, a, b));
        assertEquals(COMPLETE, result.status());
        assertEquals(List.of("U1", "U2"), result.units().stream().map(RouteUnit::code).toList());
        assertEquals(List.of(b.id(), c.id()), result.units().get(0).nodeIds());
        assertEquals(List.of(a.id()), result.units().get(1).nodeIds());
    }
    @Test void nodesWithoutUnitGoToAnExplicitFinalGroup() {
        var a = node(1); var b = node(2);
        var result = build(Optional.of(data("ACTIVO", List.of(new NodeUnit(a.id(), unit1), new NodeUnit(b.id(), null)),
                List.of(unit(unit1, "U1", 1)), List.of())), List.of(a, b));
        assertEquals(PARTIAL, result.status());
        var last = result.units().get(1);
        assertNull(last.id());
        assertEquals(List.of(b.id()), last.nodeIds());
    }
    @Test void routeWithoutAnyUnitIsReportedAndNotGrouped() {
        var a = node(1);
        var result = build(Optional.of(data("ACTIVO", List.of(new NodeUnit(a.id(), null)), List.of(), List.of())), List.of(a));
        assertEquals(NO_UNITS, result.status());
        assertTrue(result.units().isEmpty());
        assertEquals(NO_UNITS, build(Optional.empty(), List.of(a)).status());
    }
    @Test void unitFromAnotherGradeAreaOrCatalogIsNeverMixed() {
        var a = node(1);
        var links = List.of(new NodeUnit(a.id(), unit1));
        var otherGrade = new UnitRow(unit1, "U1", "t", 1, catalog, UUID.randomUUID(), area);
        var otherArea = new UnitRow(unit1, "U1", "t", 1, catalog, grade, UUID.randomUUID());
        var otherCatalog = new UnitRow(unit1, "U1", "t", 1, UUID.randomUUID(), grade, area);
        for (var wrong : List.of(otherGrade, otherArea, otherCatalog)) {
            var result = build(Optional.of(data("ACTIVO", links, List.of(wrong), List.of())), List.of(a));
            assertEquals(INCONSISTENT, result.status());
            assertTrue(result.units().isEmpty());
        }
    }
    @Test void competencyFromAnotherCatalogMakesTheCurriculumInconsistent() {
        var a = node(1);
        var foreign = new CompetencyRow(unit1, "C1", "Competencia", UUID.randomUUID(), area);
        var result = build(Optional.of(data("ACTIVO", List.of(new NodeUnit(a.id(), unit1)),
                List.of(unit(unit1, "U1", 1)), List.of(foreign))), List.of(a));
        assertEquals(INCONSISTENT, result.status());
    }
    @Test void retiredCatalogIsFlaggedButKeepsItsOwnUnits() {
        var a = node(1);
        var result = build(Optional.of(data("RETIRADO", List.of(new NodeUnit(a.id(), unit1)),
                List.of(unit(unit1, "U1", 1)), List.of())), List.of(a));
        assertEquals(OUTDATED, result.status());
        assertEquals(1, result.units().size());
        assertNotNull(result.message());
    }
    @Test void competenciesAreListedPerUnitSortedByCode() {
        var a = node(1);
        var result = build(Optional.of(data("ACTIVO", List.of(new NodeUnit(a.id(), unit1)), List.of(unit(unit1, "U1", 1)),
                List.of(new CompetencyRow(unit1, "MAT-C2", "Segunda", catalog, area),
                        new CompetencyRow(unit1, "MAT-C1", "Primera", catalog, area)))), List.of(a));
        assertEquals(List.of("MAT-C1", "MAT-C2"), result.units().get(0).competencies().stream().map(Competency::code).toList());
    }
    @Test void unitsWithoutSequenceGoAfterNumberedOnes() {
        var a = node(1); var b = node(2);
        var result = build(Optional.of(data("ACTIVO", List.of(new NodeUnit(a.id(), unit1), new NodeUnit(b.id(), unit2)),
                List.of(unit(unit1, "A", null), unit(unit2, "B", 5)), List.of())), List.of(a, b));
        assertEquals(List.of("B", "A"), result.units().stream().map(RouteUnit::code).toList());
=======

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import pe.rupi.api.learningroute.LearningRouteDtos.RouteNode;

class RouteCurriculumTest {

    @Test
    void emptyDataReturnsNoUnits() {
        var result = RouteCurriculum.build(Optional.empty(), List.of());
        assertEquals("SIN_UNIDADES", result.status());
        assertTrue(result.units().isEmpty());
    }

    @Test
    void nonActiveCatalogReturnsOutdated() {
        UUID catalogId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        var data = new RouteCurriculum.Data(catalogId, "BORRADOR", gradeId, areaId, List.of(), List.of(), List.of());
        var node = new RouteNode(UUID.randomUUID(), 1, "Parada 1", "LECCION", 10, "DISPONIBLE", false, null);

        var result = RouteCurriculum.build(Optional.of(data), List.of(node));
        assertEquals("NO_VIGENTE", result.status());
    }

    @Test
    void activeCatalogWithUnitsBuildsCompleteCurriculum() {
        UUID catalogId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID nodeId1 = UUID.randomUUID();
        UUID nodeId2 = UUID.randomUUID();

        var node1 = new RouteNode(nodeId1, 1, "Parada 1", "LECCION", 10, "COMPLETADO", false, null);
        var node2 = new RouteNode(nodeId2, 2, "Parada 2", "RETO", 8, "EN_CURSO", false, null);

        var nodeUnits = List.of(
                new RouteCurriculum.NodeUnit(nodeId1, unitId),
                new RouteCurriculum.NodeUnit(nodeId2, unitId)
        );
        var units = List.of(
                new RouteCurriculum.UnitRow(unitId, "U1", "Unidad 1", 1, catalogId, gradeId, areaId)
        );
        var competencies = List.of(
                new RouteCurriculum.CompetencyRow(unitId, "C1", "Resuelve problemas de cantidad", catalogId, areaId)
        );

        var data = new RouteCurriculum.Data(catalogId, "ACTIVO", gradeId, areaId, nodeUnits, units, competencies);
        var result = RouteCurriculum.build(Optional.of(data), List.of(node1, node2));

        assertEquals("COMPLETA", result.status());
        assertEquals(1, result.units().size());
        var unit = result.units().getFirst();
        assertEquals("Unidad 1", unit.title());
        assertEquals(2, unit.totalNodes());
        assertEquals(1, unit.completedNodes());
        assertEquals(1, unit.competencies().size());
        assertEquals("Resuelve problemas de cantidad", unit.competencies().getFirst().name());
    }

    @Test
    void unassignedNodesMarkCurriculumAsPartial() {
        UUID catalogId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID nodeId1 = UUID.randomUUID();
        UUID nodeId2 = UUID.randomUUID();

        var node1 = new RouteNode(nodeId1, 1, "Parada 1", "LECCION", 10, "COMPLETADO", false, null);
        var node2 = new RouteNode(nodeId2, 2, "Parada Libre", "RETO", 8, "DISPONIBLE", false, null);

        var nodeUnits = List.of(new RouteCurriculum.NodeUnit(nodeId1, unitId));
        var units = List.of(new RouteCurriculum.UnitRow(unitId, "U1", "Unidad 1", 1, catalogId, gradeId, areaId));

        var data = new RouteCurriculum.Data(catalogId, "ACTIVO", gradeId, areaId, nodeUnits, units, List.of());
        var result = RouteCurriculum.build(Optional.of(data), List.of(node1, node2));

        assertEquals("PARCIAL", result.status());
        assertEquals(2, result.units().size());
        assertEquals("Otras paradas", result.units().get(1).title());
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
    }
}
