package pe.rupi.api.gamification;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudyStreakServiceTest {
    final LocalDate today = LocalDate.parse("2026-10-26");

    @Test
    void withoutActivityThereIsNoStreak() {
        var streak = StudyStreakService.of(List.of(), today);
        assertEquals(0, streak.days());
        assertFalse(streak.activeToday());
        assertFalse(streak.atRisk());
    }

    @Test
    void countsConsecutiveDaysEndingToday() {
        var streak = StudyStreakService.of(List.of(today, today.minusDays(1), today.minusDays(2), today.minusDays(4)), today);
        assertEquals(3, streak.days());
        assertTrue(streak.activeToday());
        assertFalse(streak.atRisk());
    }

    @Test
    void streakFromYesterdayIsAtRiskButNotLost() {
        var streak = StudyStreakService.of(List.of(today.minusDays(1), today.minusDays(2)), today);
        assertEquals(2, streak.days());
        assertFalse(streak.activeToday());
        assertTrue(streak.atRisk());
    }

    @Test
    void aMissedDayResetsTheStreak() {
        var streak = StudyStreakService.of(List.of(today.minusDays(2), today.minusDays(3)), today);
        assertEquals(0, streak.days());
        assertFalse(streak.atRisk());
    }

    @Test
    void severalActivitiesOnTheSameDayCountOnce() {
        var streak = StudyStreakService.of(List.of(today, today, today.minusDays(1)), today);
        assertEquals(2, streak.days());
    }
}
