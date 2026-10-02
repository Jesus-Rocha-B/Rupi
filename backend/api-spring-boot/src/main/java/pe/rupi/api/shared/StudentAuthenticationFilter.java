package pe.rupi.api.shared;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StudentAuthenticationFilter extends OncePerRequestFilter {

    private final String defaultStudentId;

    public StudentAuthenticationFilter(
            @Value("${rupi.auth.default-student-id:}") String defaultStudentId
    ) {
        this.defaultStudentId = defaultStudentId == null ? "" : defaultStudentId.trim();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String studentHeader = request.getHeader("X-Student-Id");

        String rawId = null;
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            rawId = authHeader.substring(7).trim();
        } else if (studentHeader != null && !studentHeader.isBlank()) {
            rawId = studentHeader.trim();
        } else if (!defaultStudentId.isBlank()) {
            rawId = defaultStudentId;
        }

        if (rawId != null && !rawId.isBlank()) {
            try {
                UUID studentUuid = UUID.fromString(rawId);
                Principal principal = studentUuid::toString;
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public Principal getUserPrincipal() {
                        return principal;
                    }
                };
                filterChain.doFilter(wrappedRequest, response);
                return;
            } catch (IllegalArgumentException ignored) {
                // If invalid UUID format, continue without principal -> triggers 401 in controller
            }
        }

        filterChain.doFilter(request, response);
    }
}
