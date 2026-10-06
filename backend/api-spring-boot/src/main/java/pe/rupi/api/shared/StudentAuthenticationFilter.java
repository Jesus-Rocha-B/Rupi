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
        return !request.getRequestURI().startsWith("/api/v1/student/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        final StudentSessionService.Student student;
        try { student = sessions.resolve(request).orElse(null); }
        catch (DataAccessException exception) { response.sendError(503); return; }
        if (student == null) { response.sendError(401); return; }
        request.setAttribute("student", student);
        Principal principal = () -> student.id();
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public Principal getUserPrincipal() { return principal; }
        }, response);
    }
}
