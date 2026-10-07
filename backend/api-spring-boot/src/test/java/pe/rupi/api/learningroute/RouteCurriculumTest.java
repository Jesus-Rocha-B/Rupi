package pe.rupi.api.learningroute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    }
}
