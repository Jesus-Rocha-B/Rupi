package pe.rupi.api.monitor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Salud de la API para la consola de administración (HU-45).
 *
 * Implementa el patrón Controller (Presentation Layer) delegando la recolección
 * de telemetría a {@link MonitorService}.
 * Solo responde a quien presente {@code X-Rupi-Monitor-Token} igual a {@code rupi.monitor.token}; sin token
 * configurado el endpoint no existe (404). Así nadie desde la web de estudiantes ve tiempos internos.
 */
@RestController
public class MonitorController {
    static final String PATH = "/api/v1/monitor";
    static final String TOKEN_HEADER = "X-Rupi-Monitor-Token";
    private static final int SLOWEST_ROUTES = 10;

    private final MonitorService monitorService;
    private final byte[] token;

    @org.springframework.beans.factory.annotation.Autowired
    public MonitorController(MonitorService monitorService, @Value("${rupi.monitor.token:}") String token) {
        this.monitorService = monitorService;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    /** Constructor de compatibilidad para pruebas y fábricas manuales. */
    public MonitorController(RequestMetrics metrics, JdbcTemplate jdbc, String token) {
        this(new MonitorService(metrics, jdbc), token);
    }

    public record Database(String state, double latencyMs) {}

    public record Jvm(long usedMemoryMb, long maxMemoryMb, int threads, int processors) {}

    public record SystemResources(
            double cpuPercent,
            double processCpuPercent,
            long usedMemoryMb,
            long totalMemoryMb,
            double memoryPercent) {}

    public record Health(String state, Instant checkedAt, long uptimeSeconds, Database database, Jvm jvm,
                         SystemResources system, RequestMetrics.Snapshot requests) {}

    @GetMapping(PATH + "/health")
    public Health health(@RequestHeader(value = TOKEN_HEADER, required = false) String presented) {
        if (token.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (presented == null || !MessageDigest.isEqual(token, presented.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return monitorService.collectHealth(SLOWEST_ROUTES);
    }
}
