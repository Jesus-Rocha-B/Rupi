package pe.rupi.api.dashboard;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import pe.rupi.api.dashboard.DashboardDtos.*;
import pe.rupi.api.gamification.RupiAccessoriesService;
import pe.rupi.api.gamification.StudyStreakService;
import pe.rupi.api.shared.StudentSessionService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class DashboardSummaryServiceTest {

    private NamedParameterJdbcTemplate jdbc;
    private StudyStreakService streakService;
    private RupiAccessoriesService accessoriesService;
    private DashboardSummaryService service;

    private final StudentSessionService.Student demoStudent = new StudentSessionService.Student(
            "11111111-1111-4111-8111-111111111111",
            "Mateo Quispe",
            "I.E. 38001 Mariscal Sucre",
            "ESTUDIANTE",
            3,
            "3.° de primaria"
    );

    @BeforeEach
    void setUp() {
        jdbc = Mockito.mock(NamedParameterJdbcTemplate.class);
        streakService = Mockito.mock(StudyStreakService.class);
        accessoriesService = Mockito.mock(RupiAccessoriesService.class);
        service = new DashboardSummaryService(jdbc, streakService, accessoriesService);
    }

    @Test
    void studentWithoutEnrollmentsReturnsZeroMetrics() {
        when(accessoriesService.getEquippedMap(anyString()))
                .thenReturn(Collections.emptyMap());
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(0L);
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(0);
        when(streakService.current(anyString()))
                .thenReturn(new StudyStreakService.Streak(0, false, false, "2026-10-27"));

        DashboardSummaryResponse response = service.getSummary(demoStudent);

        assertNotNull(response);
        assertEquals("Mateo Quispe", response.student().studentName());
        assertEquals(3, response.student().gradeNumber());
        assertTrue(response.student().equippedAccessories().isEmpty());

        assertEquals(0, response.metrics().totalCompletedLessons());
        assertEquals(0, response.metrics().totalPendingLessons());
        assertEquals(0, response.metrics().totalEnrolledLessons());
        assertEquals(0, response.metrics().overallCompletionPercentage());
        assertEquals(0, response.metrics().lessonsCompletedToday());
        assertEquals(0, response.metrics().currentStreakDays());
        assertEquals(0, response.metrics().totalXp());
        assertEquals(0, response.metrics().activeRoutesCount());

        assertTrue(response.subjects().isEmpty());
        assertNull(response.nextLesson());
        assertNotNull(response.sync().dataHash());
        assertNotNull(response.sync().syncedAt());
    }

    @Test
    void studentWithEnrollmentsComputesExactLessonCountsAndProgress() {
        UUID route1 = UUID.randomUUID();
        UUID route2 = UUID.randomUUID();
        UUID node1 = UUID.randomUUID();

        var subj1 = new SubjectProgress(route1, "Matemática en Ayacucho", "MAT", "Matemática", 3, 4, 6, 10, 40, "2026-10-27T00:00:00Z");
        var subj2 = new SubjectProgress(route2, "Cuentos Andinos", "COM", "Comunicación", 3, 2, 4, 6, 33, "2026-10-26T18:00:00Z");
        var nextRec = new NextLessonRecommendation(route1, "Matemática en Ayacucho", "MAT", "Matemática", node1, "La suma", 5, "RETO", 10, "EN_CURSO");

        when(accessoriesService.getEquippedMap(anyString()))
                .thenReturn(Map.of("CABEZA", "CHULLO_ANDINO"));
        
        // Mock query calls
        when(jdbc.query(contains("FROM aprendizaje_inscripcion_ruta i"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(subj1, subj2));
        when(jdbc.query(contains("HAVING estado_progreso IN"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(nextRec));

        // Mock today's count
        when(jdbc.queryForObject(contains("completado_en >= :todayStart"), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(2L);
        // Mock XP sum
        when(jdbc.queryForObject(contains("gamificacion_movimiento_experiencia"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(850);
        when(streakService.current(anyString()))
                .thenReturn(new StudyStreakService.Streak(5, true, false, "2026-10-27"));

        DashboardSummaryResponse response = service.getSummary(demoStudent);

        assertNotNull(response);
        assertEquals(List.of("CHULLO_ANDINO"), response.student().equippedAccessories());

        // Check exact lesson counters
        // Completed: 4 + 2 = 6
        assertEquals(6, response.metrics().totalCompletedLessons());
        // Pending: 6 + 4 = 10
        assertEquals(10, response.metrics().totalPendingLessons());
        // Total: 16
        assertEquals(16, response.metrics().totalEnrolledLessons());
        // Percentage: round(6 / 16 * 100) = 38%
        assertEquals(38, response.metrics().overallCompletionPercentage());
        assertEquals(2, response.metrics().lessonsCompletedToday());
        assertEquals(5, response.metrics().currentStreakDays());
        assertEquals(850, response.metrics().totalXp());
        assertEquals(2, response.metrics().activeRoutesCount());

        // Subjects list
        assertEquals(2, response.subjects().size());
        assertEquals("Matemática en Ayacucho", response.subjects().get(0).routeTitle());

        // Next lesson recommendation
        assertNotNull(response.nextLesson());
        assertEquals("La suma", response.nextLesson().nodeTitle());
        assertEquals("EN_CURSO", response.nextLesson().nodeState());

        // Sync hash
        assertNotNull(response.sync().dataHash());
    }

    @Test
    void syncHashChangesWhenMetricsChange() {
        when(accessoriesService.getEquippedMap(anyString()))
                .thenReturn(Collections.emptyMap());
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(0L);
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(100);
        when(streakService.current(anyString()))
                .thenReturn(new StudyStreakService.Streak(1, true, false, "2026-10-27"));

        var res1 = service.getSummary(demoStudent);

        when(jdbc.queryForObject(contains("gamificacion_movimiento_experiencia"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(200);

        var res2 = service.getSummary(demoStudent);

        assertNotEquals(res1.sync().dataHash(), res2.sync().dataHash());
    }
}
