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
<<<<<<< HEAD
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
            List<UUID> nodeIds = ordered.stream().filter(node -> unit.id().equals(unitOfNode.get(node.id())))
                    .map(RouteNode::id).toList();
            List<Competency> competencies = data.competencies().stream()
                    .filter(row -> row.unitId().equals(unit.id()))
                    .sorted(Comparator.comparing(CompetencyRow::code))
                    .map(row -> new Competency(row.code(), row.name())).toList();
            groups.add(new RouteUnit(unit.id(), unit.code(), unit.title(), unit.sequence(), competencies, nodeIds));
        }
        List<UUID> loose = ordered.stream().filter(node -> !unitOfNode.containsKey(node.id())).map(RouteNode::id).toList();
        if (!loose.isEmpty()) {
            groups.add(new RouteUnit(null, null, "Otras paradas", null, List.of(), loose));
        }

        if (!"ACTIVO".equals(data.catalogState())) {
            return new Curriculum(OUTDATED,
                    "El catálogo curricular de esta ruta ya no está vigente. Consulta con tu docente.", groups);
        }
        if (!loose.isEmpty()) {
            return new Curriculum(PARTIAL, "Algunas paradas no tienen unidad y aparecen al final.", groups);
        }
        return new Curriculum(COMPLETE, null, groups);
    }

    private static Curriculum noUnits() {
        return new Curriculum(NO_UNITS, "Esta ruta todavía no está organizada por unidades.", List.of());
    }

    private static boolean coherent(Data data, Set<UUID> usedUnits, Map<UUID, UnitRow> unitsById) {
        for (UUID unitId : usedUnits) {
            UnitRow unit = unitsById.get(unitId);
            if (unit == null || !unit.catalogId().equals(data.catalogId())
                    || !unit.gradeId().equals(data.gradeId()) || !unit.areaId().equals(data.areaId())) {
                return false;
            }
        }
        return data.competencies().stream().filter(row -> usedUnits.contains(row.unitId()))
                .allMatch(row -> row.catalogId().equals(data.catalogId()) && row.areaId().equals(data.areaId()));
=======
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
                competenciesByUnit.computeIfAbsent(c.unitId(), _ -> new ArrayList<>())
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
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
    }
}
