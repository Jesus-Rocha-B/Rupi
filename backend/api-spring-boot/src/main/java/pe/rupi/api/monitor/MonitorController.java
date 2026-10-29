package pe.rupi.api.monitor;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
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
 * Solo responde a quien presente {@code X-Rupi-Monitor-Token} igual a {@code rupi.monitor.token}; sin token
 * configurado el endpoint no existe (404). Así nadie desde la web de estudiantes ve tiempos internos.
 */
@RestController
public class MonitorController {
    static final String PATH = "/api/v1/monitor";
    static final String TOKEN_HEADER = "X-Rupi-Monitor-Token";
    private static final int SLOWEST_ROUTES = 10;

    private final RequestMetrics metrics;
    private final JdbcTemplate jdbc;
    private final byte[] token;

    public MonitorController(RequestMetrics metrics, JdbcTemplate jdbc, @Value("${rupi.monitor.token:}") String token) {
        this.metrics = metrics;
        this.jdbc = jdbc;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    public record Database(String state, double latencyMs) {}

    public record Jvm(long usedMemoryMb, long maxMemoryMb, int threads, int processors) {}

    public record Health(String state, Instant checkedAt, long uptimeSeconds, Database database, Jvm jvm,
                         RequestMetrics.Snapshot requests) {}

    @GetMapping(PATH + "/health")
    public Health health(@RequestHeader(value = TOKEN_HEADER, required = false) String presented) {
        if (token.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (presented == null || !MessageDigest.isEqual(token, presented.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Database database = pingDatabase();
        var snapshot = metrics.snapshot(SLOWEST_ROUTES);
        var runtime = Runtime.getRuntime();
        var jvm = new Jvm((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024), runtime.maxMemory() / (1024 * 1024),
                ManagementFactory.getThreadMXBean().getThreadCount(), runtime.availableProcessors());
        Instant now = Instant.now();
        return new Health("UP".equals(database.state()) ? "UP" : "DEGRADED", now,
                Duration.between(snapshot.startedAt(), now).toSeconds(), database, jvm, snapshot);
    }

    private Database pingDatabase() {
        long start = System.nanoTime();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return new Database("UP", Math.round((System.nanoTime() - start) / 100_000.0) / 10.0);
        } catch (RuntimeException exception) {
            return new Database("DOWN", Math.round((System.nanoTime() - start) / 100_000.0) / 10.0);
        }
    }
}
