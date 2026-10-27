package pe.rupi.api.gamification;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.gamification.DailyGoalDtos.DailyGoalOverviewResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class DailyGoalProgressServiceTest {

    private NamedParameterJdbcTemplate jdbc;
    private StudyStreakService streakService;
    private DailyGoalProgressService service;

    @BeforeEach
    void setUp() {
        jdbc = Mockito.mock(NamedParameterJdbcTemplate.class);
        streakService = Mockito.mock(StudyStreakService.class);
        service = new DailyGoalProgressService(jdbc, streakService);
    }

    @Test
    void getDailyGoalOverviewComputesProgressAndMotivationalMessage() {
        // Target = 3
        when(jdbc.queryForObject(contains("meta_diaria_actividades"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(3);
        // Completed today = 2
        when(jdbc.queryForObject(contains("completado_en >= :todayStart"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(2);
        when(streakService.current(anyString()))
                .thenReturn(new StudyStreakService.Streak(4, true, false, "2026-10-27"));

        DailyGoalOverviewResponse response = service.getDailyGoalOverview("user-1");

        assertNotNull(response);
        assertEquals(3, response.today().targetLessons());
        assertEquals(2, response.today().completedLessons());
        assertEquals(1, response.today().pendingLessons());
        assertEquals(67, response.today().percentage());
        assertFalse(response.today().isGoalMet());
        assertTrue(response.today().motivationalMessage().contains("1 lección"));
        assertEquals(4, response.currentStreakDays());

        // Weekly history must contain 7 days
        assertEquals(7, response.weeklyHistory().size());
        assertTrue(response.weeklyHistory().get(6).isToday());
    }

    @Test
    void getDailyGoalOverviewWhenGoalIsMet() {
        when(jdbc.queryForObject(contains("meta_diaria_actividades"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(3);
        // Completed today = 4 (superó la meta)
        when(jdbc.queryForObject(contains("completado_en >= :todayStart"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(4);
        when(streakService.current(anyString()))
                .thenReturn(new StudyStreakService.Streak(5, true, false, "2026-10-27"));

        DailyGoalOverviewResponse response = service.getDailyGoalOverview("user-1");

        assertNotNull(response);
        assertEquals(3, response.today().targetLessons());
        assertEquals(4, response.today().completedLessons());
        assertEquals(0, response.today().pendingLessons());
        assertEquals(133, response.today().percentage());
        assertTrue(response.today().isGoalMet());
        assertTrue(response.today().motivationalMessage().contains("Felicitaciones"));
    }

    @Test
    void updateTargetGoalRejectsInvalidTarget() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.updateTargetGoal("user-1", 4) // Solo 1, 2, 3 o 5
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("opciones permitidas"));
    }

    @Test
    void updateTargetGoalAcceptsValidTarget() {
        when(jdbc.queryForObject(contains("meta_diaria_actividades"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(5);
        when(jdbc.queryForObject(contains("completado_en >= :todayStart"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(1);
        when(streakService.current(anyString()))
                .thenReturn(new StudyStreakService.Streak(2, true, false, "2026-10-27"));

        DailyGoalOverviewResponse response = service.updateTargetGoal("user-1", 5);

        assertNotNull(response);
        assertEquals(5, response.today().targetLessons());
        assertEquals(1, response.today().completedLessons());
        assertEquals(4, response.today().pendingLessons());
        assertEquals(20, response.today().percentage());
    }
}
