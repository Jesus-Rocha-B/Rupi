package pe.rupi.api.monitor;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** Mide cada petición a /api completa, incluida la autenticación (HU-45). El propio monitor no se mide. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestMetricsFilter extends OncePerRequestFilter {
    static final String UNMATCHED = "(sin sesión o ruta inexistente)";

    private final RequestMetrics metrics;

    public RequestMetricsFilter(RequestMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/") || uri.startsWith(MonitorController.PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            // Las peticiones que el filtro de sesión rechaza (401) no llegan a un controlador ni tienen patrón.
            String route = request.getMethod() + " " + (pattern != null ? pattern : UNMATCHED);
            metrics.record(route, (System.nanoTime() - start) / 1_000_000.0, response.getStatus());
        }
    }
}
