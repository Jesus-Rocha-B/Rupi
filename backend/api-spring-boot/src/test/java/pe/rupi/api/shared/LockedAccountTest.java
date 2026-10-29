package pe.rupi.api.shared;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** HU-44: una cuenta bloqueada a pedido recibe 423 con la indicación de pedir el desbloqueo. */
class LockedAccountTest {

    @Test
    void lockedAccountAnswers423WithoutCookie() {
        var sessions = mock(StudentSessionService.class);
        when(sessions.authenticate(anyString(), anyString(), any(), any()))
                .thenReturn(StudentSessionService.AuthenticationResult.lockedAccount());
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("127.0.0.1");
        request.setServerPort(8081);
        request.addHeader("Origin", "http://127.0.0.1:8081");
        var response = new MockHttpServletResponse();
        var error = assertThrows(ResponseStatusException.class, () -> new SessionController(sessions, "")
                .login(new SessionController.LoginRequest("lucia.huaman", "123456"), request, response));
        assertEquals(423, error.getStatusCode().value());
        assertTrue(error.getReason().contains("desbloqueo"));
        assertNull(response.getHeader("Set-Cookie"));
    }

    @Test
    void lockedIsNotAuthenticated() {
        var result = StudentSessionService.AuthenticationResult.lockedAccount();
        assertFalse(result.authenticated());
        assertNull(result.token());
    }
}
