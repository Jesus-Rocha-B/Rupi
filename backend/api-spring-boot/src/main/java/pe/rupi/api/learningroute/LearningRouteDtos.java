package pe.rupi.api.learningroute;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class LearningRouteDtos {
    private LearningRouteDtos() {}

    public record RouteListResponse(List<RouteSummary> items) {}

    public record RouteSummary(
            UUID versionRouteId,
            String title,
            Grade grade,
            Area area,
            String enrollmentState,
            long completedNodes,
            long totalNodes,
            String lastVisitedAt
    ) {}

    public record CulturalFact(
            UUID id,
            Integer nodeSequence,
            int order,
            String title,
            String content,
            String icon,
            String source
    ) {}

    public record CulturalContext(
            UUID id,
            String code,
            String city,
            String place,
            String title,
            String description,
            String motivationalMessage,
            String imageUrl,
            String imageAlt,
            String author,
            String source,
            String license,
            String licenseUrl,
            List<CulturalFact> facts
    ) {}

    public record Competency(String code, String name) {}

    public record RouteUnit(
            UUID id,
            String code,
            String title,
            Integer sequence,
            int totalNodes,
            int completedNodes,
            List<Competency> competencies,
            List<RouteNode> nodes
    ) {}

    public record Curriculum(String status, String message, List<RouteUnit> units) {}

    public record RouteDetailResponse(
            UUID versionRouteId,
            String title,
            Grade grade,
            Area area,
            Enrollment enrollment,
            Progress progress,
            List<RouteNode> nodes,
            CulturalContext culturalContext,
            Curriculum curriculum
    ) {
        public RouteDetailResponse(
                UUID versionRouteId,
                String title,
                Grade grade,
                Area area,
                Enrollment enrollment,
                Progress progress,
                List<RouteNode> nodes,
                CulturalContext culturalContext
        ) {
            this(versionRouteId, title, grade, area, enrollment, progress, nodes, culturalContext, null);
        }

        public RouteDetailResponse(
                UUID versionRouteId,
                String title,
                Grade grade,
                Area area,
                Enrollment enrollment,
                Progress progress,
                List<RouteNode> nodes
        ) {
            this(versionRouteId, title, grade, area, enrollment, progress, nodes, null, null);
<<<<<<< HEAD
        }

        public RouteDetailResponse(
                UUID versionRouteId,
                String title,
                Grade grade,
                Area area,
                Enrollment enrollment,
                Progress progress,
                List<RouteNode> nodes,
                CulturalContext culturalContext
        ) {
            this(versionRouteId, title, grade, area, enrollment, progress, nodes, culturalContext, null);
        }
    }

    public record Competency(String code, String name) {}

    /** Grupo de paradas. Si {@code id} es null es el grupo final de paradas sin unidad. */
    public record RouteUnit(
            UUID id,
            String code,
            String title,
            Integer sequence,
            List<Competency> competencies,
            List<UUID> nodeIds
    ) {}

    /** Estado curricular: COMPLETA, PARCIAL, SIN_UNIDADES, NO_VIGENTE o INCOHERENTE. */
    public record Curriculum(String status, String message, List<RouteUnit> units) {}


=======
        }
    }

>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
    public record Grade(int number, String name) {}

    public record Area(String code, String name) {}

    public record Enrollment(UUID id, String state, UUID lastVisitedNodeId) {}

    public record Progress(long completedNodes, long totalNodes) {}

    public record Position(BigDecimal x, BigDecimal y) {}

    public record RouteNode(
            UUID id,
            int sequence,
            String title,
            String activityType,
            Integer estimatedMinutes,
            String state,
            boolean optional,
            Position position
    ) {}

    public record ContentBlock(
            String id,
            String type,
            String text,
            String url,
            String accessibleText
    ) {}

    public record ActivityContent(
            UUID nodeId,
            String title,
            String instructions,
            String state,
            List<ContentBlock> blocks
    ) {}

    public record NextUnlockedNode(
            UUID id,
            int sequence,
            String title
    ) {}

    public record ActivityCompleteResponse(
            UUID nodeId,
            String state,
            int experienceEarned,
            int totalExperience,
            NextUnlockedNode nextUnlockedNode,
            String rupiMessage
    ) {}
}
