package pe.rupi.api.shared;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** HU-42: las cuentas solo de consola (administración o mesa de ayuda) no abren la web ni el celular. */
class ConsoleOnlyAccountTest {

    private static StudentSessionService serviceWithRoles(long total, long appRoles) {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForMap(anyString(), eq("u-1"))).thenReturn(Map.of("total", total, "app_roles", appRoles));
        return new StudentSessionService(jdbc, mock(PasswordService.class));
    }

    @Test
    void accountWithOnlyConsoleRolesIsConsoleOnly() {
        assertTrue(serviceWithRoles(1, 0).isConsoleOnly("u-1"));
    }

    @Test
    void teacherWhoAlsoAdministratesKeepsTheApp() {
        assertFalse(serviceWithRoles(2, 1).isConsoleOnly("u-1"));
    }

    @Test
    void accountWithoutRolesKeepsEnteringAsStudent() {
        assertFalse(serviceWithRoles(0, 0).isConsoleOnly("u-1"));
    }

    @Test
    void loginAnswers403WithAHintForConsoleAccounts() {
        var sessions = mock(StudentSessionService.class);
        when(sessions.authenticate(anyString(), anyString(), any()))
                .thenReturn(StudentSessionService.AuthenticationResult.consoleAccount());
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("127.0.0.1");
        request.setServerPort(8081);
        request.addHeader("Origin", "http://127.0.0.1:8081");
        var response = new MockHttpServletResponse();
        var error = assertThrows(ResponseStatusException.class, () -> new SessionController(sessions, "")
                .login(new SessionController.LoginRequest("admin.rupi", "clave-larga"), request, response));
        assertEquals(403, error.getStatusCode().value());
        assertTrue(error.getReason().contains("/admin"));
        assertNull(response.getHeader("Set-Cookie"));
    }
}
