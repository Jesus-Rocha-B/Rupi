package pe.rupi.api.gamification;

import java.util.List;

public final class DailyGoalDtos {
    private DailyGoalDtos() {}

    public record DayProgressDto(
            String date,
            int targetLessons,
            int completedLessons,
            int pendingLessons,
            int percentage,
            boolean isGoalMet,
            String metAt,
            String motivationalMessage
    ) {}

    public record DayHistoryDto(
            String date,
            String dayOfWeek,
            int targetLessons,
            int completedLessons,
            int percentage,
            boolean isGoalMet,
            boolean isToday
    ) {}

    public record DailyGoalOverviewResponse(
            DayProgressDto today,
            List<DayHistoryDto> weeklyHistory,
            int currentDailyTargetSetting,
            List<Integer> availableTargetOptions,
            int currentStreakDays
    ) {}

    public record UpdateTargetGoalRequest(
            int targetLessons
    ) {}
}
