package pe.rupi.api.shared;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentAuthenticationFilterTest {
    @Test void demoRejectsNonLocalHostEvenFromLoopback() {
        var controller = new SessionController(mock(StudentSessionService.class), "a".repeat(64));
        var request = new MockHttpServletRequest();
        request.setServerName("attacker.example");
        assertFalse(controller.options(request).demoAvailable());
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.demo(request, new MockHttpServletResponse())).getStatusCode().value());
    }
    @Test void missingSessionDoesNotReachController() throws Exception {
        var sessions = mock(StudentSessionService.class);
        when(sessions.resolve(any())).thenReturn(Optional.empty());
        var request = new MockHttpServletRequest("GET", "/api/v1/student/learning-routes");
        var response = new MockHttpServletResponse();
        new StudentAuthenticationFilter(sessions).doFilter(request, response, (req, res) -> fail("Unauthorized request reached controller"));
        assertEquals(401, response.getStatus());
    }
    @Test void databaseFailureIs503() throws Exception {
        var sessions = mock(StudentSessionService.class);
        when(sessions.resolve(any())).thenThrow(new DataAccessResourceFailureException("offline"));
        var response = new MockHttpServletResponse();
        new StudentAuthenticationFilter(sessions).doFilter(new MockHttpServletRequest("GET", "/api/v1/student/session"), response, (req, res) -> fail());
        assertEquals(503, response.getStatus());
    }
    @Test void browserUuidIsNotASessionToken() {
        var jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var sessions = new StudentSessionService(jdbc);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Student-Id", "11111111-1111-4111-8111-111111111111");
        assertTrue(sessions.resolve(request).isEmpty());
        request.addHeader("Authorization", "Bearer 11111111-1111-4111-8111-111111111111");
        assertTrue(sessions.resolve(request).isEmpty());
        verifyNoInteractions(jdbc);
    }
    @Test void demoIsDisabledByDefault() {
        var controller = new SessionController(mock(StudentSessionService.class), "");
        var request = new MockHttpServletRequest();
        assertFalse(controller.options(request).demoAvailable());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.demo(request, new MockHttpServletResponse()));
    }
    @Test void crossOriginDemoIsRejected() {
        var sessions = mock(StudentSessionService.class);
        var controller = new SessionController(sessions, "a".repeat(64));
        var request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://other.example");
        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.demo(request, new MockHttpServletResponse()));
        assertEquals(403, exception.getStatusCode().value());
        verifyNoInteractions(sessions);
    }
}
