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
            long totalNodes
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

    public record RouteDetailResponse(
            UUID versionRouteId,
            String title,
            Grade grade,
            Area area,
            Enrollment enrollment,
            Progress progress,
            List<RouteNode> nodes,
            CulturalContext culturalContext
    ) {
        public RouteDetailResponse(
                UUID versionRouteId,
                String title,
                Grade grade,
                Area area,
                Enrollment enrollment,
                Progress progress,
                List<RouteNode> nodes
        ) {
            this(versionRouteId, title, grade, area, enrollment, progress, nodes, null);
        }
    }


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
}
