package pe.rupi.api.learningroute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import pe.rupi.api.learningroute.LearningRouteDtos.Competency;
import pe.rupi.api.learningroute.LearningRouteDtos.Curriculum;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteNode;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteUnit;

/**
 * Agrupa los nodos de una ruta por unidad curricular y comprueba que las unidades pertenezcan al
 * mismo catálogo, grado y área de la ruta. El orden sale de {@code numero_secuencia}, no de React.
 */
final class RouteCurriculum {
    static final String COMPLETE = "COMPLETA";
    static final String PARTIAL = "PARCIAL";
    static final String NO_UNITS = "SIN_UNIDADES";
    static final String OUTDATED = "NO_VIGENTE";
    static final String INCONSISTENT = "INCOHERENTE";

    private RouteCurriculum() {}

    record NodeUnit(UUID nodeId, UUID unitId) {}

    record UnitRow(UUID id, String code, String title, Integer sequence, UUID catalogId, UUID gradeId, UUID areaId) {}

    record CompetencyRow(UUID unitId, String code, String name, UUID catalogId, UUID areaId) {}

    record Data(UUID catalogId, String catalogState, UUID gradeId, UUID areaId,
                List<NodeUnit> nodeUnits, List<UnitRow> units, List<CompetencyRow> competencies) {}

    static Curriculum build(Optional<Data> found, List<RouteNode> nodes) {
        if (found.isEmpty()) return noUnits();
        Data data = found.get();
        Map<UUID, UUID> unitOfNode = new HashMap<>();
        for (NodeUnit link : data.nodeUnits()) {
            if (link.unitId() != null) unitOfNode.put(link.nodeId(), link.unitId());
        }
        List<RouteNode> ordered = nodes.stream().sorted(Comparator.comparingInt(RouteNode::sequence)).toList();
        if (ordered.stream().noneMatch(node -> unitOfNode.containsKey(node.id()))) return noUnits();

        Map<UUID, UnitRow> unitsById = new HashMap<>();
        data.units().forEach(unit -> unitsById.put(unit.id(), unit));
        Set<UUID> usedUnits = new HashSet<>();
        for (RouteNode node : ordered) {
            UUID unitId = unitOfNode.get(node.id());
            if (unitId != null) usedUnits.add(unitId);
        }
        if (!coherent(data, usedUnits, unitsById)) {
            return new Curriculum(INCONSISTENT,
                    "Las unidades de esta ruta no coinciden con su grado, área o catálogo. Se muestra sin agrupar.",
                    List.of());
        }

        List<UnitRow> sortedUnits = usedUnits.stream().map(unitsById::get)
                .sorted(Comparator.comparing(UnitRow::sequence, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(UnitRow::code).thenComparing(unit -> unit.id().toString()))
                .toList();
        List<RouteUnit> groups = new ArrayList<>();
        for (UnitRow unit : sortedUnits) {
            List<RouteNode> unitNodes = ordered.stream()
                    .filter(node -> unit.id().equals(unitOfNode.get(node.id()))).toList();
            List<Competency> competencies = data.competencies().stream()
                    .filter(row -> row.unitId().equals(unit.id()))
                    .sorted(Comparator.comparing(CompetencyRow::code))
                    .map(row -> new Competency(row.code(), row.name())).toList();
            groups.add(group(unit.id(), unit.code(), unit.title(), unit.sequence(), competencies, unitNodes));
        }
        List<RouteNode> loose = ordered.stream().filter(node -> !unitOfNode.containsKey(node.id())).toList();
        if (!loose.isEmpty()) {
            groups.add(group(null, "OTRAS", "Otras paradas", null, List.of(), loose));
        }

        if (!"ACTIVO".equals(data.catalogState())) {
            return new Curriculum(OUTDATED,
                    "El catálogo curricular de esta ruta ya no está vigente. Consulta con tu docente.", groups);
        }
        if (!loose.isEmpty()) {
            return new Curriculum(PARTIAL, null, groups);
        }
        return new Curriculum(COMPLETE, null, groups);
    }

    private static RouteUnit group(UUID id, String code, String title, Integer sequence,
                                   List<Competency> competencies, List<RouteNode> unitNodes) {
        int completed = (int) unitNodes.stream().filter(node -> "COMPLETADO".equalsIgnoreCase(node.state())).count();
        return new RouteUnit(id, code, title, sequence, unitNodes.size(), completed, competencies, unitNodes);
    }

    private static Curriculum noUnits() {
        return new Curriculum(NO_UNITS, "Esta ruta todavía no está organizada por unidades.", List.of());
    }

    private static boolean coherent(Data data, Set<UUID> usedUnits, Map<UUID, UnitRow> unitsById) {
        for (UUID id : usedUnits) {
            UnitRow unit = unitsById.get(id);
            if (unit == null || !unit.catalogId().equals(data.catalogId())
                    || !unit.gradeId().equals(data.gradeId()) || !unit.areaId().equals(data.areaId())) {
                return false;
            }
        }
        return data.competencies().stream().filter(row -> usedUnits.contains(row.unitId()))
                .allMatch(row -> row.catalogId().equals(data.catalogId()) && row.areaId().equals(data.areaId()));
    }
}
