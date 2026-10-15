package pe.rupi.api.teacher;

import java.util.List;

public class TeacherDtos {

    public record CreateRouteRequest(
            String title,
            String description,
            Integer gradeNumber,
            String areaCode
    ) {}

    public record CreateNodeRequest(
            String title,
            String instructions,
            String activityType,
            Integer estimatedMinutes,
            Integer sequence,
            Double mapX,
            Double mapY
    ) {}

    public record AttachResourceRequest(
            String type,
            String title,
            String url,
            String multimediaId
    ) {}

    public record ReorderNodesRequest(
            List<String> nodeIds
    ) {}

    public record GradeInfo(int number, String name) {}

    public record AreaInfo(String code, String name) {}

    public record TeacherResourceDto(
            String id,
            String type,
            String title,
            String url,
            String multimediaId,
            String createdAt
    ) {}

    public record TeacherOptionDto(
            String id,
            Integer sequence,
            String text,
            Boolean isCorrect
    ) {}

    public record TeacherQuestionDto(
            String id,
            Integer sequence,
            String type,
            String statement,
            String explanation,
            Double points,
            List<TeacherOptionDto> options
    ) {}

    public record TeacherNodeDto(
            String id,
            int sequence,
            String title,
            String instructions,
            String activityType,
            Integer estimatedMinutes,
            Double mapX,
            Double mapY,
            String availableFrom,
            String lockedFrom,
            String videoUrl,
            List<TeacherResourceDto> resources
    ) {
        public TeacherNodeDto(
                String id,
                int sequence,
                String title,
                String instructions,
                String activityType,
                Integer estimatedMinutes,
                Double mapX,
                Double mapY,
                List<TeacherResourceDto> resources
        ) {
            this(id, sequence, title, instructions, activityType, estimatedMinutes, mapX, mapY, null, null, null, resources);
        }
    }

    public record TeacherNodeDetailDto(
            String id,
            int sequence,
            String title,
            String instructions,
            String activityType,
            Integer estimatedMinutes,
            Double mapX,
            Double mapY,
            String availableFrom,
            String lockedFrom,
            String videoUrl,
            List<TeacherQuestionDto> questions,
            List<TeacherResourceDto> resources
    ) {}

    public record UpdateNodeRequest(
            String title,
            String instructions,
            String activityType,
            Integer estimatedMinutes,
            String availableFrom,
            String lockedFrom,
            String videoUrl,
            List<TeacherQuestionDto> questions
    ) {}

    public record TeacherRouteSummary(
            String routeId,
            String versionRouteId,
            String code,
            String title,
            String description,
            int gradeNumber,
            String gradeName,
            String areaCode,
            String areaName,
            String state,
            int totalNodes,
            String createdAt,
            String publishedAt
    ) {
        public TeacherRouteSummary(
                String routeId,
                String versionRouteId,
                String title,
                String description,
                int gradeNumber,
                String gradeName,
                String areaCode,
                String areaName,
                String state,
                int totalNodes,
                String createdAt,
                String publishedAt
        ) {
            this(routeId, versionRouteId, null, title, description, gradeNumber, gradeName, areaCode, areaName, state, totalNodes, createdAt, publishedAt);
        }
    }

    public record TeacherRouteDetail(
            String routeId,
            String versionRouteId,
            String code,
            String title,
            String description,
            GradeInfo grade,
            AreaInfo area,
            String state,
            List<TeacherNodeDto> nodes,
            String createdAt,
            String publishedAt
    ) {
        public TeacherRouteDetail(
                String routeId,
                String versionRouteId,
                String title,
                String description,
                GradeInfo grade,
                AreaInfo area,
                String state,
                List<TeacherNodeDto> nodes,
                String createdAt,
                String publishedAt
        ) {
            this(routeId, versionRouteId, null, title, description, grade, area, state, nodes, createdAt, publishedAt);
        }
    }

    public record CreateRouteResponse(
            String routeId,
            String versionRouteId,
            String code,
            String title,
            String state,
            String message
    ) {
        public CreateRouteResponse(
                String routeId,
                String versionRouteId,
                String title,
                String state,
                String message
        ) {
            this(routeId, versionRouteId, null, title, state, message);
        }
    }

    public record PublishRouteResponse(
            String routeId,
            String versionRouteId,
            String state,
            String publishedAt,
            String message
    ) {}
}


