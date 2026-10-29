package pe.rupi.api.shared;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** HU-43: una sola sesión por cuenta entre la web y el celular, con el motivo del cierre en el 401. */
class SingleSessionTest {

    @Test
    void mobileIdentifiesItselfAndAnythingElseIsWeb() {
        var request = new MockHttpServletRequest();
        assertEquals("WEB_USUARIO", StudentSessionService.clientOf(request));
        request.addHeader("X-Rupi-Cliente", "movil");
        assertEquals("MOVIL", StudentSessionService.clientOf(request));
        var forged = new MockHttpServletRequest();
        forged.addHeader("X-Rupi-Cliente", "WEB_ADMIN");
        assertEquals("WEB_USUARIO", StudentSessionService.clientOf(forged));
    }

    @Test
    void sessionReplacedOnAnotherDeviceIsExplained() throws Exception {
        var sessions = mock(StudentSessionService.class);
        when(sessions.resolve(any())).thenReturn(Optional.empty());
        when(sessions.sessionEnd(any())).thenReturn(Optional.of(
                new StudentSessionService.SessionEnd(StudentSessionService.SessionEnd.OTHER_DEVICE, "MOVIL")));
        var response = new MockHttpServletResponse();
        new StudentAuthenticationFilter(sessions).doFilter(
                new MockHttpServletRequest("GET", "/api/v1/student/learning-routes"), response, (req, res) -> fail());
        assertEquals(401, response.getStatus());
        assertEquals("OTRO_DISPOSITIVO", response.getHeader("X-Rupi-Sesion-Cerrada"));
        assertEquals("MOVIL", response.getHeader("X-Rupi-Sesion-Nueva"));
    }

    @Test
    void unknownTokenGetsAPlain401() throws Exception {
        var sessions = mock(StudentSessionService.class);
        when(sessions.resolve(any())).thenReturn(Optional.empty());
        when(sessions.sessionEnd(any())).thenReturn(Optional.empty());
        var response = new MockHttpServletResponse();
        new StudentAuthenticationFilter(sessions).doFilter(
                new MockHttpServletRequest("GET", "/api/v1/student/session"), response, (req, res) -> fail());
        assertEquals(401, response.getStatus());
        assertNull(response.getHeader("X-Rupi-Sesion-Cerrada"));
    }

    @Test
    void loginPassesTheClientToTheSession() {
        var sessions = mock(StudentSessionService.class);
        when(sessions.authenticate(anyString(), anyString(), any(), any()))
                .thenReturn(StudentSessionService.AuthenticationResult.success(
                        new StudentSessionService.Student("u-1", "Lucía"), "t".repeat(43)));
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("127.0.0.1");
        request.setServerPort(8081);
        request.addHeader("Origin", "http://127.0.0.1:8081");
        request.addHeader("X-Rupi-Cliente", "MOVIL");
        new SessionController(sessions, "").login(new SessionController.LoginRequest("lucia", "123456"), request,
                new MockHttpServletResponse());
        verify(sessions).authenticate(eq("lucia"), eq("123456"), any(), eq("MOVIL"));
    }
}
