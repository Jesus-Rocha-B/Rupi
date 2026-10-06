package pe.rupi.api.shared;

import java.net.URI;
import java.time.Duration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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

    public record LoginRequest(String username, String password) {}

    public record SessionOptions(boolean demoAvailable) {}

    @GetMapping("/api/v1/student/session")
    public StudentSessionService.Student current(HttpServletRequest request) {
        return (StudentSessionService.Student) request.getAttribute("student");
    }

    @GetMapping("/api/v1/auth/options")
    public SessionOptions options(HttpServletRequest request) {
        return new SessionOptions(!demoToken.isBlank() && isLocal(request));
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
        var result = sessions.authenticate(body.username(), body.password(), clientIp);

        if (result.rateLimited()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
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

    private static void checkOrigin(HttpServletRequest request) {
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
            boolean allowedDevOrigin = isLocal(request)
                    && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))
                    && (uri.getPort() == 5173 || uri.getPort() == 8081 || uri.getPort() == -1);
            if (!sameOrigin && !allowedDevOrigin) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            int comma = xForwardedFor.indexOf(',');
            return (comma >= 0 ? xForwardedFor.substring(0, comma) : xForwardedFor).trim();
        }
        return request.getRemoteAddr();
    }
}
