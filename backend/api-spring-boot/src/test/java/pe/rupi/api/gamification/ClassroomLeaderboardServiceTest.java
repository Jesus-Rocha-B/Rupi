package pe.rupi.api.gamification;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ClassroomLeaderboardServiceTest {

    private NamedParameterJdbcTemplate db;
    private StudyStreakService streakService;
    private ClassroomLeaderboardService service;

    @BeforeEach
    void setUp() {
        db = Mockito.mock(NamedParameterJdbcTemplate.class);
        streakService = Mockito.mock(StudyStreakService.class);
        service = new ClassroomLeaderboardService(db, streakService);
    }

    @Test
    void studentWithoutClassroomReturnsFallbackSoloBoard() {
        when(db.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of()); // Sin aula
        when(streakService.current(anyString()))
                .thenReturn(new StudyStreakService.Streak(4, true, false, "2026-10-27"));
        when(db.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(250);

        var board = service.getLeaderboard("user-1", "XP");

        assertNotNull(board);
        assertEquals(1, board.myRank());
        assertEquals(1, board.entries().size());
        assertEquals(250, board.entries().get(0).totalXp());
        assertEquals(4, board.entries().get(0).streakDays());
        assertTrue(board.entries().get(0).esUsuarioActual());
    }

    @Test
    void updatePrivacyRejectsInvalidAliasPattern() {
        var invalidUpdate = new ClassroomLeaderboardService.PrivacySettings(true, "al!", "ALIAS");

        var ex = assertThrows(ResponseStatusException.class, () ->
                service.updatePrivacy("user-1", invalidUpdate)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("alfanuméricos"));
    }

    @Test
    void updatePrivacyRejectsTooShortAlias() {
        var tooShort = new ClassroomLeaderboardService.PrivacySettings(true, "ab", "ALIAS");

        var ex = assertThrows(ResponseStatusException.class, () ->
                service.updatePrivacy("user-1", tooShort)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updatePrivacyRejectsTakenAlias() {
        when(db.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(1); // Ya existe otro usuario con ese alias

        var takenUpdate = new ClassroomLeaderboardService.PrivacySettings(true, "super_rupi", "ALIAS");

        var ex = assertThrows(ResponseStatusException.class, () ->
                service.updatePrivacy("user-1", takenUpdate)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("ya está siendo usado"));
    }

    @Test
    void updatePrivacySucceedsForValidAlias() {
        when(db.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(0); // Libre
        when(db.update(anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(1);

        var valid = new ClassroomLeaderboardService.PrivacySettings(true, "chaski_veloz", "ALIAS");
        var res = service.updatePrivacy("user-1", valid);

        assertEquals("chaski_veloz", res.aliasPublico());
        assertTrue(res.mostrarEnClasificacion());
        assertEquals("ALIAS", res.mostrarComo());
    }
}
