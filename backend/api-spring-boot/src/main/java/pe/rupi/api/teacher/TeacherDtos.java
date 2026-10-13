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

    public record TeacherNodeDto(
            String id,
            int sequence,
            String title,
            String instructions,
            String activityType,
            Integer estimatedMinutes,
            Double mapX,
            Double mapY,
            List<TeacherResourceDto> resources
    ) {}

    public record TeacherRouteSummary(
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
    ) {}

    public record TeacherRouteDetail(
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
    ) {}

    public record CreateRouteResponse(
            String routeId,
            String versionRouteId,
            String title,
            String state,
            String message
    ) {}

    public record PublishRouteResponse(
            String routeId,
            String versionRouteId,
            String state,
            String publishedAt,
            String message
    ) {}
}
