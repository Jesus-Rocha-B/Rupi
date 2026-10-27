package pe.rupi.api.dashboard;

import java.util.List;
import java.util.UUID;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record StudentOverview(
            String studentId,
            String studentName,
            String schoolName,
            String role,
            Integer gradeNumber,
            String gradeName,
            List<String> equippedAccessories
    ) {}

    public record MetricSummary(
            long totalCompletedLessons,
            long totalPendingLessons,
            long totalEnrolledLessons,
            int overallCompletionPercentage,
            long lessonsCompletedToday,
            int currentStreakDays,
            int totalXp,
            int activeRoutesCount
    ) {}

    public record SubjectProgress(
            UUID versionRouteId,
            String routeTitle,
            String areaCode,
            String areaName,
            int gradeNumber,
            long completedLessons,
            long pendingLessons,
            long totalLessons,
            int completionPercentage,
            String lastVisitedAt
    ) {}

    public record NextLessonRecommendation(
            UUID versionRouteId,
            String routeTitle,
            String areaCode,
            String areaName,
            UUID nodeId,
            String nodeTitle,
            int sequence,
            String activityType,
            Integer estimatedMinutes,
            String nodeState
    ) {}

    public record SyncMeta(
            String syncedAt,
            String dataHash
    ) {}

    public record DashboardSummaryResponse(
            StudentOverview student,
            MetricSummary metrics,
            List<SubjectProgress> subjects,
            NextLessonRecommendation nextLesson,
            SyncMeta sync
    ) {}
}
