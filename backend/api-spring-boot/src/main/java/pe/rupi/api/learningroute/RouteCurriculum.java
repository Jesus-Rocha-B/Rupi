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
        if (found.isEmpty() || nodes.isEmpty()) {
            return new Curriculum(NO_UNITS, "Esta ruta aún no tiene unidades curriculares asociadas.", List.of());
        }
        var data = found.get();
        if (!"ACTIVO".equalsIgnoreCase(data.catalogState()) && !"PUBLICADO".equalsIgnoreCase(data.catalogState())) {
            return new Curriculum(OUTDATED, "El catálogo curricular asociado a esta ruta no está vigente.", List.of());
        }

        var nodeMap = new HashMap<UUID, RouteNode>();
        for (var node : nodes) {
            nodeMap.put(node.id(), node);
        }

        var nodeToUnit = new HashMap<UUID, UUID>();
        for (var nu : data.nodeUnits()) {
            nodeToUnit.put(nu.nodeId(), nu.unitId());
        }

        var validUnits = new ArrayList<UnitRow>();
        for (var unit : data.units()) {
            if (data.catalogId().equals(unit.catalogId())
                    && data.gradeId().equals(unit.gradeId())
                    && data.areaId().equals(unit.areaId())) {
                validUnits.add(unit);
            }
        }
        validUnits.sort(Comparator.comparing(UnitRow::sequence));

        var competenciesByUnit = new HashMap<UUID, List<Competency>>();
        for (var c : data.competencies()) {
            if (data.catalogId().equals(c.catalogId()) && data.areaId().equals(c.areaId())) {
                competenciesByUnit.computeIfAbsent(c.unitId(), key -> new ArrayList<>())
                        .add(new Competency(c.code(), c.name()));
            }
        }

        var unitsOut = new ArrayList<RouteUnit>();
        var assignedNodeIds = new HashSet<UUID>();

        for (var unit : validUnits) {
            var unitNodes = new ArrayList<RouteNode>();
            for (var node : nodes) {
                if (unit.id().equals(nodeToUnit.get(node.id()))) {
                    unitNodes.add(node);
                    assignedNodeIds.add(node.id());
                }
            }
            unitNodes.sort(Comparator.comparing(RouteNode::sequence));

            int completed = 0;
            for (var un : unitNodes) {
                if ("COMPLETADO".equalsIgnoreCase(un.state())) {
                    completed++;
                }
            }

            unitsOut.add(new RouteUnit(
                    unit.id(),
                    unit.code(),
                    unit.title(),
                    unit.sequence(),
                    unitNodes.size(),
                    completed,
                    competenciesByUnit.getOrDefault(unit.id(), List.of()),
                    unitNodes
            ));
        }

        var unassignedNodes = new ArrayList<RouteNode>();
        for (var node : nodes) {
            if (!assignedNodeIds.contains(node.id())) {
                unassignedNodes.add(node);
            }
        }
        unassignedNodes.sort(Comparator.comparing(RouteNode::sequence));

        if (!unassignedNodes.isEmpty() && !unitsOut.isEmpty()) {
            int completed = 0;
            for (var un : unassignedNodes) {
                if ("COMPLETADO".equalsIgnoreCase(un.state())) {
                    completed++;
                }
            }
            unitsOut.add(new RouteUnit(
                    null,
                    "OTRAS",
                    "Otras paradas",
                    unitsOut.size() + 1,
                    unassignedNodes.size(),
                    completed,
                    List.of(),
                    unassignedNodes
            ));
        }

        if (unitsOut.isEmpty()) {
            return new Curriculum(NO_UNITS, "No se encontraron unidades vigentes para el grado y área.", List.of());
        }

        String status = unassignedNodes.isEmpty() ? COMPLETE : PARTIAL;
        String message = COMPLETE.equals(status)
                ? "Ruta organizada según las unidades del Currículo Nacional."
                : "Algunas paradas de la ruta no están vinculadas a una unidad curricular oficial.";

        return new Curriculum(status, message, unitsOut);
    }
}
