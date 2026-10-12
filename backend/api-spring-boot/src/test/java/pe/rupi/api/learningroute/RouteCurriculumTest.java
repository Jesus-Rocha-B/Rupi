package pe.rupi.api.learningroute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    }
}
