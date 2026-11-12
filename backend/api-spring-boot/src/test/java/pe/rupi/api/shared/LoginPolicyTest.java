package pe.rupi.api.shared;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** HU-58: bloqueo automático calculado con los intentos fallidos y mensajes con el tiempo de espera. */
class LoginPolicyTest {
    private static final Instant NOW = Instant.parse("2026-11-11T15:00:00Z");
    /** 10 fallos de la cuenta en 5 minutos la bloquean 15 minutos. */
    private final LoginPolicy policy = LoginPolicy.DEFAULT;

    private static List<Instant> failures(Instant first, int count, int secondsApart) {
        List<Instant> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(first.plus((long) i * secondsApart, SECONDS));
        return list;
    }

    @Test
    void defaultsKeepTheLimitsUsedBeforeHu58() {
        assertEquals(5, policy.windowMinutes());
        assertEquals(5, policy.perDeviceAccount());
        assertEquals(20, policy.perDevice());
        assertTrue(policy.autoLock());
    }

    @Test
    void fewerFailuresThanTheLimitDoNotLock() {
        assertTrue(policy.lockedUntil(failures(NOW.minus(2, MINUTES), 9, 10), NOW).isEmpty());
    }

    @Test
    void reachingTheLimitInsideTheWindowLocksFromTheLastFailure() {
        var list = failures(NOW.minus(3, MINUTES), 10, 10);
        var until = policy.lockedUntil(list, NOW);
        assertEquals(list.get(9).plus(15, MINUTES), until.orElseThrow());
    }

    @Test
    void failuresSpreadBeyondTheWindowDoNotLock() {
        // 10 fallos, uno cada 40 segundos: entre el primero y el décimo pasan 6 minutos.
        assertTrue(policy.lockedUntil(failures(NOW.minus(7, MINUTES), 10, 40), NOW).isEmpty());
    }

    @Test
    void theLockEndsByItself() {
        var list = failures(NOW.minus(20, MINUTES), 10, 5);
        assertTrue(policy.lockedUntil(list, NOW).isEmpty());
    }

    @Test
    void failuresThatAlreadyCausedALockDoNotCountAgain() {
        // Ventana larga y bloqueo corto: tras cumplir el bloqueo, un solo fallo más no vuelve a bloquear.
        var custom = new LoginPolicy(60, 5, 20, true, 5, 5);
        var list = failures(NOW.minus(20, MINUTES), 5, 10);
        list.add(NOW.minus(1, MINUTES));
        assertTrue(custom.lockedUntil(list, NOW).isEmpty());
    }

    @Test
    void disabledAutoLockNeverLocks() {
        var off = new LoginPolicy(5, 5, 20, false, 10, 15);
        assertTrue(off.lockedUntil(failures(NOW.minus(1, MINUTES), 30, 1), NOW).isEmpty());
    }

    @Test
    void minutesLeftRoundUpAndNeverZero() {
        assertEquals(15, LoginPolicy.minutesLeft(NOW.plus(14, MINUTES).plus(1, SECONDS), NOW));
        assertEquals(1, LoginPolicy.minutesLeft(NOW.plus(3, SECONDS), NOW));
        assertEquals(1, LoginPolicy.minutesLeft(NOW.minus(3, SECONDS), NOW));
    }

    private static ResponseStatusException login(StudentSessionService.AuthenticationResult result,
                                                 MockHttpServletResponse response) {
        var sessions = mock(StudentSessionService.class);
        when(sessions.authenticate(anyString(), anyString(), any(), any())).thenReturn(result);
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("127.0.0.1");
        request.setServerPort(8081);
        request.addHeader("Origin", "http://127.0.0.1:8081");
        return assertThrows(ResponseStatusException.class, () -> new SessionController(sessions, "")
                .login(new SessionController.LoginRequest("lucia.huaman", "123456"), request, response));
    }

    @Test
    void autoLockedAccountAnswers423WithMinutes() {
        var response = new MockHttpServletResponse();
        var error = login(StudentSessionService.AuthenticationResult.autoLocked(12), response);
        assertEquals(423, error.getStatusCode().value());
        assertTrue(error.getReason().contains("12 minutos"));
        assertTrue(error.getReason().contains("desbloqueo"));
        assertEquals("720", response.getHeader("Retry-After"));
        assertNull(response.getHeader("Set-Cookie"));
    }

    @Test
    void throttledDeviceAnswers429WithMinutes() {
        var response = new MockHttpServletResponse();
        var error = login(StudentSessionService.AuthenticationResult.throttled(1), response);
        assertEquals(429, error.getStatusCode().value());
        assertTrue(error.getReason().contains("1 minuto "));
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void consoleLockKeepsItsMessageWithoutWaitingTime() {
        var response = new MockHttpServletResponse();
        var error = login(StudentSessionService.AuthenticationResult.lockedAccount(), response);
        assertEquals(423, error.getStatusCode().value());
        assertFalse(error.getReason().contains("minuto"));
        assertNull(response.getHeader("Retry-After"));
    }
}
