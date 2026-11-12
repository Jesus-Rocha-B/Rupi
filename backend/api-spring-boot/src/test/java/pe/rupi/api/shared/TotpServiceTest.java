package pe.rupi.api.shared;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HU-60: verificación TOTP RFC 6238 y exigencia obligatoria de doble factor para docentes en Spring Boot.
 */
class TotpServiceTest {
    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    void matchesRfc6238OfficialTestVectors() {
        assertEquals("287082", TotpService.generateCode(RFC_SECRET, 59));
        assertEquals("081804", TotpService.generateCode(RFC_SECRET, 1111111109));
        assertEquals("005924", TotpService.generateCode(RFC_SECRET, 1234567890));
    }

    @Test
    void acceptsWindowAndRejectsCodeReuse() {
        TotpService service = new TotpService();
        String code = TotpService.generateCode(RFC_SECRET, 1_700_000_000L);

        assertTrue(service.verifyAt("docente-1", RFC_SECRET, code, 1_700_000_030L));
        // No se permite reutilizar el código del mismo paso de tiempo.
        assertFalse(service.verifyAt("docente-1", RFC_SECRET, code, 1_700_000_030L));
        // Otro usuario con el mismo secreto aún no usó ese paso.
        assertTrue(service.verifyAt("docente-2", RFC_SECRET, code, 1_700_000_000L));
    }

    @Test
    void rejectsMalformedOrWrongCode() {
        TotpService service = new TotpService();
        assertFalse(service.verifyAt("docente-1", RFC_SECRET, "1234", 59));
        assertFalse(service.verifyAt("docente-1", RFC_SECRET, "abcdef", 59));
        assertFalse(service.verifyAt("docente-1", RFC_SECRET, "000000", 59));
    }

    @Test
    void controllerReturns428WithCodeWhenTeacherRequiresTwoFactor() {
        var sessions = mock(StudentSessionService.class);
        when(sessions.authenticate(anyString(), anyString(), any(), any()))
                .thenReturn(StudentSessionService.AuthenticationResult.requireTotp());

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("127.0.0.1");
        request.setServerPort(8081);
        request.addHeader("Origin", "http://127.0.0.1:8081");
        var response = new MockHttpServletResponse();

        var ex = assertThrows(SessionController.TwoFactorChallengeException.class, () ->
                new SessionController(sessions, "")
                        .login(new SessionController.LoginRequest("profesor.demo", "123456"), request, response));

        assertEquals(428, ex.status().value());
        assertEquals("TOTP_REQUIRED", ex.code());
        assertNull(response.getHeader("Set-Cookie"));
    }

    @Test
    void controllerReturns428WithSetupCodeWhenTeacherHasNotConfiguredQr() {
        var sessions = mock(StudentSessionService.class);
        when(sessions.authenticate(anyString(), anyString(), any(), any()))
                .thenReturn(StudentSessionService.AuthenticationResult.requireTotpSetup());

        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("127.0.0.1");
        request.setServerPort(8081);
        request.addHeader("Origin", "http://127.0.0.1:8081");
        var response = new MockHttpServletResponse();

        var ex = assertThrows(SessionController.TwoFactorChallengeException.class, () ->
                new SessionController(sessions, "")
                        .login(new SessionController.LoginRequest("profesor.demo", "123456"), request, response));

        assertEquals(428, ex.status().value());
        assertEquals("TOTP_SETUP_REQUIRED", ex.code());
        assertNull(response.getHeader("Set-Cookie"));
    }

    @Test
    void requiresTwoFactorOnlyForPrivilegedOrTeacherRoles() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("doc-1"))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("est-1"))).thenReturn(0);

        var service = new StudentSessionService(jdbc, mock(PasswordService.class));
        assertTrue(service.requiresTwoFactor("doc-1"));
        assertFalse(service.requiresTwoFactor("est-1"));
    }
}
