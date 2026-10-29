package pe.rupi.api.shared;

import java.io.IOException;
import java.security.Principal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class StudentAuthenticationFilter extends OncePerRequestFilter {
    private final StudentSessionService sessions;
    public StudentAuthenticationFilter(StudentSessionService sessions) { this.sessions = sessions; }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/v1/student/") && !uri.startsWith("/api/v1/teacher/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        final StudentSessionService.Student user;
        try { user = sessions.resolve(request).orElse(null); }
        catch (DataAccessException exception) { response.sendError(503); return; }
        if (user == null) {
            explainSessionEnd(sessions, request, response);
            response.sendError(401);
            return;
        }

        if (request.getRequestURI().startsWith("/api/v1/teacher/") && !"DOCENTE".equalsIgnoreCase(user.role())) {
            response.sendError(403);
            return;
        }

        request.setAttribute("student", user);
        request.setAttribute("teacher", user);
        request.setAttribute("user", user);
        Principal principal = () -> user.id();
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public Principal getUserPrincipal() { return principal; }
        }, response);
    }

    public static final String SESSION_END_HEADER = "X-Rupi-Sesion-Cerrada";
    public static final String SESSION_NEW_CLIENT_HEADER = "X-Rupi-Sesion-Nueva";

    /** HU-43: un 401 dice si la sesión se cerró porque la cuenta se abrió en otro dispositivo, se cerró o expiró. */
    static void explainSessionEnd(StudentSessionService sessions, HttpServletRequest request, HttpServletResponse response) {
        try {
            sessions.sessionEnd(request).ifPresent(end -> {
                response.setHeader(SESSION_END_HEADER, end.reason());
                if (end.newClient() != null) {
                    response.setHeader(SESSION_NEW_CLIENT_HEADER, end.newClient());
                }
            });
        } catch (DataAccessException ignored) {
            // Sin el motivo, el cliente muestra el ingreso de siempre.
        }
    }
}
