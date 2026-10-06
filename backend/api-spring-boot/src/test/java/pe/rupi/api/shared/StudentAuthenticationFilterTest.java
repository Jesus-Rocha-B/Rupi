package pe.rupi.api.shared;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentAuthenticationFilterTest {

    @Test
    void demoRejectsNonLocalHostEvenFromLoopback() {
        var controller = new SessionController(mock(StudentSessionService.class), "a".repeat(64));
        var request = new MockHttpServletRequest();
        request.setServerName("attacker.example");
        assertFalse(controller.options(request).demoAvailable());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> controller.demo(request, new MockHttpServletResponse())).getStatusCode().value());
    }

    @Test
    void missingSessionDoesNotReachController() throws Exception {
        var sessions = mock(StudentSessionService.class);
        when(sessions.resolve(any())).thenReturn(Optional.empty());
        var request = new MockHttpServletRequest("GET", "/api/v1/student/learning-routes");
        var response = new MockHttpServletResponse();
        new StudentAuthenticationFilter(sessions).doFilter(request, response, (req, res) -> fail("Unauthorized request reached controller"));
        assertEquals(401, response.getStatus());
    }

    @Test
    void databaseFailureIs503() throws Exception {
        var sessions = mock(StudentSessionService.class);
        when(sessions.resolve(any())).thenThrow(new DataAccessResourceFailureException("offline"));
        var response = new MockHttpServletResponse();
        new StudentAuthenticationFilter(sessions).doFilter(new MockHttpServletRequest("GET", "/api/v1/student/session"), response, (req, res) -> fail());
        assertEquals(503, response.getStatus());
    }

    @Test
    void browserUuidIsNotASessionToken() {
        var jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var passwords = mock(PasswordService.class);
        var sessions = new StudentSessionService(jdbc, passwords);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Student-Id", "11111111-1111-4111-8111-111111111111");
        assertTrue(sessions.resolve(request).isEmpty());
        request.addHeader("Authorization", "Bearer 11111111-1111-4111-8111-111111111111");
        assertTrue(sessions.resolve(request).isEmpty());
        verifyNoInteractions(jdbc);
    }

    @Test
    void demoIsDisabledByDefault() {
        var controller = new SessionController(mock(StudentSessionService.class), "");
        var request = new MockHttpServletRequest();
        assertFalse(controller.options(request).demoAvailable());
        assertThrows(ResponseStatusException.class,
                () -> controller.demo(request, new MockHttpServletResponse()));
    }

    @Test
    void crossOriginDemoIsRejected() {
        var sessions = mock(StudentSessionService.class);
        var controller = new SessionController(sessions, "a".repeat(64));
        var request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://other.example");
        var exception = assertThrows(ResponseStatusException.class,
                () -> controller.demo(request, new MockHttpServletResponse()));
        assertEquals(403, exception.getStatusCode().value());
        verifyNoInteractions(sessions);
    }

    @Test
    void loginWithValidCredentialsReturnsStudentAndSetsCookie() {
        var sessions = mock(StudentSessionService.class);
        var student = new StudentSessionService.Student("11111111-1111-4111-8111-111111111111", "Mateo");
        when(sessions.authenticate("mateo", "clave123", "127.0.0.1"))
                .thenReturn(StudentSessionService.AuthenticationResult.success(student, "token-seguro-123456789012345678901234567890123"));

        var controller = new SessionController(sessions, "");
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("localhost");
        request.setServerPort(5173);
        request.setScheme("http");
        request.addHeader("Origin", "http://localhost:5173");
        request.setRemoteAddr("127.0.0.1");

        var response = new MockHttpServletResponse();
        var result = controller.login(new SessionController.LoginRequest("mateo", "clave123"), request, response);

        assertEquals("Mateo", result.name());
        assertEquals("11111111-1111-4111-8111-111111111111", result.id());
        String cookie = response.getHeader("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("rupi_session="));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Strict"));
    }

    @Test
    void loginWithInvalidCredentialsReturns401() {
        var sessions = mock(StudentSessionService.class);
        when(sessions.authenticate(anyString(), anyString(), anyString()))
                .thenReturn(StudentSessionService.AuthenticationResult.badCredentials());

        var controller = new SessionController(sessions, "");
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("localhost");
        request.setServerPort(5173);
        request.setScheme("http");
        request.addHeader("Origin", "http://localhost:5173");
        request.setRemoteAddr("127.0.0.1");

        var response = new MockHttpServletResponse();
        var ex = assertThrows(ResponseStatusException.class,
                () -> controller.login(new SessionController.LoginRequest("usuario", "erronea"), request, response));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void loginWithRateLimitingReturns429() {
        var sessions = mock(StudentSessionService.class);
        when(sessions.authenticate(anyString(), anyString(), anyString()))
                .thenReturn(StudentSessionService.AuthenticationResult.rateLimited());

        var controller = new SessionController(sessions, "");
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("localhost");
        request.setServerPort(5173);
        request.setScheme("http");
        request.addHeader("Origin", "http://localhost:5173");
        request.setRemoteAddr("127.0.0.1");

        var response = new MockHttpServletResponse();
        var ex = assertThrows(ResponseStatusException.class,
                () -> controller.login(new SessionController.LoginRequest("usuario", "pass"), request, response));
        assertEquals(429, ex.getStatusCode().value());
    }

    @Test
    void loginWithBlankFieldsReturns400() {
        var sessions = mock(StudentSessionService.class);
        var controller = new SessionController(sessions, "");
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("localhost");
        request.setServerPort(5173);
        request.setScheme("http");
        request.addHeader("Origin", "http://localhost:5173");

        var response = new MockHttpServletResponse();
        var ex = assertThrows(ResponseStatusException.class,
                () -> controller.login(new SessionController.LoginRequest("", "pass"), request, response));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void logoutRevokesSessionAndClearsCookie() {
        var sessions = mock(StudentSessionService.class);
        var controller = new SessionController(sessions, "");
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        request.setServerName("localhost");
        request.setServerPort(5173);
        request.setScheme("http");
        request.addHeader("Origin", "http://localhost:5173");

        var response = new MockHttpServletResponse();
        controller.logout(request, response);

        verify(sessions).revokeSession(request);
        String cookie = response.getHeader("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("Max-Age=0") || cookie.contains("max-age=0"));
    }
}
