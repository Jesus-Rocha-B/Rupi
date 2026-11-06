package pe.rupi.api.monitor;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Servicio de recopilación de telemetría y salud de infraestructura (HU-45).
 *
 * Implementa el patrón Service Layer (Separation of Concerns), desacoplando las consultas
 * a base de datos, telemetría de la JVM y métricas del sistema operativo del controlador HTTP.
 */
@Service
public class MonitorService {

    private final RequestMetrics metrics;
    private final JdbcTemplate jdbc;

    public MonitorService(RequestMetrics metrics, JdbcTemplate jdbc) {
        this.metrics = metrics;
        this.jdbc = jdbc;
    }

    public MonitorController.Database pingDatabase() {
        long start = System.nanoTime();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return new MonitorController.Database("UP", Math.round((System.nanoTime() - start) / 100_000.0) / 10.0);
        } catch (RuntimeException exception) {
            return new MonitorController.Database("DOWN", Math.round((System.nanoTime() - start) / 100_000.0) / 10.0);
        }
    }

    public MonitorController.Jvm collectJvmMetrics() {
        var runtime = Runtime.getRuntime();
        long runtimeTotal = runtime.totalMemory();
        long runtimeFree = runtime.freeMemory();
        long jvmUsedMb = (runtimeTotal - runtimeFree) / (1024 * 1024);
        long jvmMaxMb = runtime.maxMemory() / (1024 * 1024);
        return new MonitorController.Jvm(
                jvmUsedMb,
                jvmMaxMb,
                ManagementFactory.getThreadMXBean().getThreadCount(),
                runtime.availableProcessors());
    }

    public MonitorController.SystemResources collectSystemResources() {
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
        return new MonitorController.SystemResources(cpuPercent, processCpuPercent, usedSysMemMb, totalSysMemMb, memPercent);
    }

    public MonitorController.Health collectHealth(int slowestRoutesLimit) {
        var database = pingDatabase();
        var snapshot = metrics.snapshot(slowestRoutesLimit);
        var jvm = collectJvmMetrics();
        var systemResources = collectSystemResources();

        Instant now = Instant.now();
        return new MonitorController.Health(
                "UP".equals(database.state()) ? "UP" : "DEGRADED",
                now,
                Duration.between(snapshot.startedAt(), now).toSeconds(),
                database,
                jvm,
                systemResources,
                snapshot);
    }
}
