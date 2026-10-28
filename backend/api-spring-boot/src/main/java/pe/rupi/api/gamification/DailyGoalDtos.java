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

    /** Comparación breve entre el puntaje (XP ganado) de hoy y el de ayer (HU-36). */
    public record ScoreComparisonResponse(
            String today,
            String yesterday,
            int todayExperience,
            int yesterdayExperience,
            int difference,
            String trend,
            int todayLessons,
            int yesterdayLessons,
            String message
    ) {}

    public record UpdateTargetGoalRequest(
            int targetLessons
    ) {}
}
