package pe.rupi.api.shared;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SessionController {
    private final StudentSessionService sessions;
    private final String demoToken;

    public SessionController(StudentSessionService sessions,
            @Value("${rupi.auth.demo-session-token:}") String demoToken) {
        this.sessions = sessions;
        this.demoToken = demoToken;
    }

    public record LoginRequest(String username, String password, String totpCode) {
        public LoginRequest(String username, String password) {
            this(username, password, null);
        }
    }

    public record SessionOptions(boolean demoAvailable) {}

    @GetMapping("/api/v1/student/session")
    public StudentSessionService.Student current(HttpServletRequest request) {
        return (StudentSessionService.Student) request.getAttribute("student");
    }

    @GetMapping("/api/v1/teacher/session")
    public StudentSessionService.Student currentTeacher(HttpServletRequest request) {
        var user = (StudentSessionService.Student) request.getAttribute("teacher");
        if (user == null || !"DOCENTE".equalsIgnoreCase(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return user;
    }

    @GetMapping("/api/v1/auth/session")
    public StudentSessionService.Student currentAuth(HttpServletRequest request, HttpServletResponse response) {
        var user = (StudentSessionService.Student) request.getAttribute("user");
        if (user == null) {
            user = sessions.resolve(request).orElse(null);
        }
        if (user == null) {
            StudentAuthenticationFilter.explainSessionEnd(sessions, request, response);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    @GetMapping("/api/v1/auth/options")
    public SessionOptions options(HttpServletRequest request) {
        return new SessionOptions(!demoToken.isBlank() && isLocal(request));
    }

    static final class TwoFactorChallengeException extends RuntimeException {
        private final HttpStatus status;
        private final String code;

        TwoFactorChallengeException(HttpStatus status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }

        HttpStatus status() { return status; }
        String code() { return code; }
    }

    @ExceptionHandler(TwoFactorChallengeException.class)
    public ResponseEntity<Map<String, String>> handleTwoFactorChallenge(TwoFactorChallengeException ex) {
        return ResponseEntity.status(ex.status()).body(Map.of("code", ex.code(), "message", ex.getMessage()));
    }

    @PostMapping("/api/v1/auth/login")
    public StudentSessionService.Student login(@RequestBody(required = false) LoginRequest body,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        checkOrigin(request);
        if (body == null || body.username() == null || body.username().isBlank()
                || body.password() == null || body.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        String clientIp = resolveClientIp(request);
        String client = StudentSessionService.clientOf(request);
        var result = (body.totpCode() == null || body.totpCode().isBlank())
                ? sessions.authenticate(body.username(), body.password(), clientIp, client)
                : sessions.authenticate(body.username(), body.password(), clientIp, client, body.totpCode());

        // HU-58: los dos frenos dicen cuánto esperar, en el mensaje y en Retry-After para el celular.
        if (result.rateLimited()) {
            response.setHeader("Retry-After", String.valueOf(result.retryMinutes() * 60));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Hubo muchos intentos seguidos desde este equipo. Espera " + minutes(result.retryMinutes())
                            + " y vuelve a probar.");
        }

        if (result.locked() && result.temporary()) {
            response.setHeader("Retry-After", String.valueOf(result.retryMinutes() * 60));
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "Por seguridad, esta cuenta se bloqueó tras varios intentos fallidos. Vuelve a probar en "
                            + minutes(result.retryMinutes()) + " o pide a tu profe que solicite el desbloqueo.");
        }

        if (result.locked()) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "Tu cuenta está bloqueada. Pide a tu profe que solicite el desbloqueo.");
        }

        if (result.consoleOnly()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Esta cuenta es de administración: ingresa desde la consola /admin.");
        }

        // HU-60: cuentas docentes requieren 2FA (configuración QR inicial o token TOTP temporal).
        if (result.totpSetupRequired()) {
            throw new TwoFactorChallengeException(HttpStatus.PRECONDITION_REQUIRED, "TOTP_SETUP_REQUIRED",
                    "Por seguridad debes escanear tu código QR de doble factor (2FA) antes de ingresar.");
        }
        if (result.totpRequired() && !result.totpInvalid()) {
            throw new TwoFactorChallengeException(HttpStatus.PRECONDITION_REQUIRED, "TOTP_REQUIRED",
                    "Ingresa el código temporal de 6 dígitos de tu aplicación autenticadora.");
        }
        if (result.totpInvalid()) {
            throw new TwoFactorChallengeException(HttpStatus.UNAUTHORIZED, "TOTP_INVALID",
                    "El código de doble factor (2FA) es incorrecto o venció.");
        }

        if (!result.authenticated() || result.token() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        response.addHeader("Set-Cookie", ResponseCookie.from(StudentSessionService.COOKIE, result.token())
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Strict")
                .path("/api")
                .maxAge(Duration.ofDays(30))
                .build()
                .toString());

        return result.student();
    }

    private static String minutes(int value) {
        return value == 1 ? "1 minuto" : value + " minutos";
    }

    @PostMapping("/api/v1/auth/demo")
    public StudentSessionService.Student demo(HttpServletRequest request, HttpServletResponse response) {
        if (demoToken.isBlank() || !isLocal(request)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        checkOrigin(request);
        var student = sessions.find(demoToken).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        response.addHeader("Set-Cookie", ResponseCookie.from(StudentSessionService.COOKIE, demoToken)
                .httpOnly(true).secure(request.isSecure()).sameSite("Strict").path("/api").build().toString());
        return student;
    }

    @PostMapping("/api/v1/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        checkOrigin(request);
        sessions.revokeSession(request);
        response.addHeader("Set-Cookie", ResponseCookie.from(StudentSessionService.COOKIE, "")
                .httpOnly(true).secure(request.isSecure()).sameSite("Strict").path("/api").maxAge(0).build().toString());
    }

    private static boolean isLocal(HttpServletRequest request) {
        boolean loopback = "127.0.0.1".equals(request.getRemoteAddr()) || "::1".equals(request.getRemoteAddr())
                || "0:0:0:0:0:0:0:1".equals(request.getRemoteAddr());
        boolean localHost = "localhost".equals(request.getServerName()) || "127.0.0.1".equals(request.getServerName());
        return loopback && localHost;
    }

    public static void checkOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        try {
            URI uri = URI.create(origin);
            int originPort = uri.getPort() >= 0 ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80);
            boolean sameOrigin = uri.getHost().equalsIgnoreCase(request.getServerName())
                    && originPort == request.getServerPort()
                    && uri.getScheme().equalsIgnoreCase(request.getScheme());
            boolean allowedDevOrigin = isLocal(request) && "http".equalsIgnoreCase(uri.getScheme())
                    && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))
                    && (uri.getPort() == 5173 || uri.getPort() == 8081);
            if (!sameOrigin && !allowedDevOrigin) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private static String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
