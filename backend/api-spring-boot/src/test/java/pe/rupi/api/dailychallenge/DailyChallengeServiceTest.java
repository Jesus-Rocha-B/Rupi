package pe.rupi.api.dailychallenge;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.evaluation.EvaluationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DailyChallengeServiceTest {
    private static final UUID STUDENT = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID CHALLENGE = UUID.fromString("e4060000-0000-4000-8000-000000000021");
    private static final UUID ACTIVITY = UUID.fromString("e4020000-0000-4000-8000-000000000001");

    private NamedParameterJdbcTemplate jdbc;
    private DailyChallengeService service;

    @BeforeEach
    void setUp() {
        jdbc = Mockito.mock(NamedParameterJdbcTemplate.class);
        service = new DailyChallengeService(jdbc, Mockito.mock(EvaluationService.class));
    }

    @Test
    void rotationAdvancesOneChallengePerDayAndWraps() {
        LocalDate day = LocalDate.of(2026, 10, 28);
        int first = DailyChallengeService.rotationIndex(day, 4);
        assertEquals((first + 1) % 4, DailyChallengeService.rotationIndex(day.plusDays(1), 4));
        assertEquals(first, DailyChallengeService.rotationIndex(day.plusDays(4), 4));
        assertEquals(0, DailyChallengeService.rotationIndex(day, 0));
    }

    @Test
    void completionMessageReportsScoreAndReward() {
        assertTrue(DailyChallengeService.completionMessage(false, 2, 2).contains("Acertaste las 2 preguntas"));
        assertTrue(DailyChallengeService.completionMessage(false, 1, 2).contains("Acertaste 1 de 2"));
        assertTrue(DailyChallengeService.completionMessage(false, 1, 2).contains("+30 XP"));
        assertTrue(DailyChallengeService.completionMessage(true, 2, 2).contains("Ya habías completado"));
    }

    @Test
    void completingRequiresAnsweringEveryQuestion() {
        stubChallengeLookup();
        when(jdbc.queryForObject(contains("FROM evaluacion_pregunta"), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(2);
        when(jdbc.queryForMap(contains("COUNT(DISTINCT r.pregunta_id)"), any(MapSqlParameterSource.class)))
                .thenReturn(Map.of("answered", 1L, "correct", 1L));

        var error = assertThrows(ResponseStatusException.class, () -> service.completeTodayChallenge(STUDENT, CHALLENGE));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("1 de 2"));
        verify(jdbc, never()).update(contains("INSERT IGNORE INTO gamificacion_movimiento_experiencia"), any(MapSqlParameterSource.class));
    }

    @Test
    void completingAwardsExperienceOncePerDay() {
        stubChallengeLookup();
        when(jdbc.queryForObject(contains("FROM evaluacion_pregunta"), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(2);
        when(jdbc.queryForMap(contains("COUNT(DISTINCT r.pregunta_id)"), any(MapSqlParameterSource.class)))
                .thenReturn(Map.of("answered", 2L, "correct", 1L));
        when(jdbc.queryForObject(contains("motivo = 'RETO'"), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(0);
        when(jdbc.queryForObject(contains("SUM(cantidad)"), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(130);

        var response = service.completeTodayChallenge(STUDENT, CHALLENGE);

        assertEquals(30, response.experienceEarned());
        assertEquals(1, response.correctAnswers());
        assertEquals(2, response.totalQuestions());
        assertEquals(130, response.totalExperience());
        verify(jdbc).update(contains("INSERT IGNORE INTO gamificacion_movimiento_experiencia"), any(MapSqlParameterSource.class));
    }

    @Test
    void onlyTodaysChallengeCanBeCompleted() {
        stubChallengeLookup(UUID.fromString("e4060000-0000-4000-8000-000000000022"));
        var error = assertThrows(ResponseStatusException.class, () -> service.completeTodayChallenge(STUDENT, CHALLENGE));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    private void stubChallengeLookup() {
        stubChallengeLookup(CHALLENGE);
    }

    /** Busca el reto por id (consulta con :challengeId) y arma el banco del grado con el reto de hoy. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubChallengeLookup(UUID todaysChallenge) {
        when(jdbc.query(contains("escuela_membresia_aula"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of("00000000-0000-4000-8000-000000000002"));
        when(jdbc.query(contains("WHERE rd.id = :challengeId"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper mapper = invocation.getArgument(2);
                    var rs = Mockito.mock(java.sql.ResultSet.class);
                    when(rs.getString("id")).thenReturn(CHALLENGE.toString());
                    when(rs.getString("version_actividad_id")).thenReturn(ACTIVITY.toString());
                    when(rs.getString("titulo")).thenReturn("Las alpacas de la abuela Rosa");
                    return List.of(mapper.mapRow(rs, 0));
                });
        when(jdbc.query(contains("rd.fecha <= :today"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn((List) List.of(new DailyChallengeService.ChallengeRaw(
                        todaysChallenge, "2026-10-01", ACTIVITY, "Las alpacas de la abuela Rosa", "Instrucciones", 5)));
    }
}
