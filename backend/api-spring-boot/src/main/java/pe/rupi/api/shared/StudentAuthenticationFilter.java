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
        if (user == null) { response.sendError(401); return; }

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
}
