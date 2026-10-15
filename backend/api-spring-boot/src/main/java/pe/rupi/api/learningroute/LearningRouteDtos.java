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
            String lastVisitedAt,
            String category,
            String authorName
    ) {
        public RouteSummary(
                UUID versionRouteId,
                String title,
                Grade grade,
                Area area,
                String enrollmentState,
                long completedNodes,
                long totalNodes,
                String lastVisitedAt
        ) {
            this(versionRouteId, title, grade, area, enrollmentState, completedNodes, totalNodes, lastVisitedAt, "OFICIAL", "Rupi Oficial · CNEB");
        }
    }

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
            String code,
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
                CulturalContext culturalContext,
                Curriculum curriculum
        ) {
            this(versionRouteId, null, title, grade, area, enrollment, progress, nodes, culturalContext, curriculum);
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
            this(versionRouteId, null, title, grade, area, enrollment, progress, nodes, culturalContext, null);
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
            this(versionRouteId, null, title, grade, area, enrollment, progress, nodes, null, null);
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

    public record BadgeDto(
            String code,
            String name,
            String description,
            String icon
    ) {}

    public record RouteDiplomaResponse(
            UUID versionRouteId,
            String routeTitle,
            String studentName,
            String schoolName,
            int gradeNumber,
            String gradeName,
            String areaName,
            int totalCompletedNodes,
            int totalNodes,
            int totalExperienceEarned,
            String completedAt,
            BadgeDto graduationBadge,
            List<Competency> competencies,
            String rupiHonorMessage
    ) {}

    public record ActivityCompleteResponse(
            UUID nodeId,
            String state,
            int experienceEarned,
            int totalExperience,
            NextUnlockedNode nextUnlockedNode,
            String rupiMessage,
            BadgeDto badgeEarned,
            boolean isRouteCompleted,
            int routeBonusExperience
    ) {
        public ActivityCompleteResponse(
                UUID nodeId,
                String state,
                int experienceEarned,
                int totalExperience,
                NextUnlockedNode nextUnlockedNode,
                String rupiMessage,
                BadgeDto badgeEarned
        ) {
            this(nodeId, state, experienceEarned, totalExperience, nextUnlockedNode, rupiMessage, badgeEarned, false, 0);
        }

        public ActivityCompleteResponse(
                UUID nodeId,
                String state,
                int experienceEarned,
                int totalExperience,
                NextUnlockedNode nextUnlockedNode,
                String rupiMessage
        ) {
            this(nodeId, state, experienceEarned, totalExperience, nextUnlockedNode, rupiMessage, null, false, 0);
        }
    }

    public record RouteSearchResult(
            UUID versionRouteId,
            String routeTitle,
            String description,
            int gradeNumber,
            String gradeName,
            String areaCode,
            String areaName,
            long totalNodes,
            boolean isEnrolled,
            String enrollmentState,
            String category,
            String authorName
    ) {
        public RouteSearchResult(
                UUID versionRouteId,
                String routeTitle,
                String description,
                int gradeNumber,
                String gradeName,
                String areaCode,
                String areaName,
                long totalNodes,
                boolean isEnrolled,
                String enrollmentState
        ) {
            this(versionRouteId, routeTitle, description, gradeNumber, gradeName, areaCode, areaName, totalNodes, isEnrolled, enrollmentState,
                    "OFICIAL", "Rupi Oficial · CNEB");
        }
    }

    public record SearchAutocompleteResponse(
            String query,
            List<String> suggestedSubjects,
            List<RouteSearchResult> routes,
            List<Integer> selectedGrades,
            List<String> selectedAreas,
            long totalResults
    ) {
        public SearchAutocompleteResponse(String query, List<String> suggestedSubjects, List<RouteSearchResult> routes) {
            this(query, suggestedSubjects, routes, List.of(), List.of(), routes != null ? routes.size() : 0);
        }
    }

    public record JoinByCodeRequest(
            String code
    ) {}

    public record JoinByCodeResponse(
            UUID versionRouteId,
            String routeTitle,
            Grade grade,
            Area area,
            boolean alreadyEnrolled,
            String message
    ) {}
}
