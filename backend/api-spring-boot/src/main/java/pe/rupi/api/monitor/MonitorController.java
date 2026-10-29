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
        Database database = pingDatabase();
        var snapshot = metrics.snapshot(SLOWEST_ROUTES);
        var runtime = Runtime.getRuntime();
        long runtimeTotal = runtime.totalMemory();
        long runtimeFree = runtime.freeMemory();
        long jvmUsedMb = (runtimeTotal - runtimeFree) / (1024 * 1024);
        long jvmMaxMb = runtime.maxMemory() / (1024 * 1024);
        var jvm = new Jvm(jvmUsedMb, jvmMaxMb,
                ManagementFactory.getThreadMXBean().getThreadCount(), runtime.availableProcessors());

        var osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuPercent = -1.0;
        double processCpuPercent = -1.0;
        long totalSysMemMb = 0;
        long freeSysMemMb = 0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            double sc = sunBean.getCpuLoad();
            double pc = sunBean.getProcessCpuLoad();
            if (sc >= 0) cpuPercent = Math.round(sc * 1000.0) / 10.0;
            if (pc >= 0) processCpuPercent = Math.round(pc * 1000.0) / 10.0;
            totalSysMemMb = sunBean.getTotalMemorySize() / (1024 * 1024);
            freeSysMemMb = sunBean.getFreeMemorySize() / (1024 * 1024);
        }
        long usedSysMemMb = totalSysMemMb > freeSysMemMb ? totalSysMemMb - freeSysMemMb : 0;
        double memPercent = totalSysMemMb > 0 ? Math.round((usedSysMemMb * 1000.0) / totalSysMemMb) / 10.0 : 0.0;
        var systemResources = new SystemResources(cpuPercent, processCpuPercent, usedSysMemMb, totalSysMemMb, memPercent);

        Instant now = Instant.now();
        return new Health("UP".equals(database.state()) ? "UP" : "DEGRADED", now,
                Duration.between(snapshot.startedAt(), now).toSeconds(), database, jvm, systemResources, snapshot);
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
