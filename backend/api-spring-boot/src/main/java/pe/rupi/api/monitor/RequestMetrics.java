package pe.rupi.api.monitor;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Tiempos de respuesta de la API en memoria (HU-45).
 *
 * Guarda las últimas peticiones de toda la API y de cada ruta, sin tablas nuevas: para anticipar fallas basta
 * ver cómo responde ahora. Las rutas se agrupan por su patrón (p. ej., {@code /api/v1/student/learning-routes/{id}})
 * para no crear una entrada por cada id. Se reinicia al reiniciar la API.
 */
@Component
public class RequestMetrics {
    static final int GLOBAL_SAMPLES = 4096;
    static final int ROUTE_SAMPLES = 256;
    static final long WINDOW_MS = 15 * 60 * 1000L;
    static final int MAX_ROUTES = 300;

    private final Clock clock;
    private final Instant startedAt;
    private final long[] globalAt = new long[GLOBAL_SAMPLES];
    private final double[] globalMs = new double[GLOBAL_SAMPLES];
    private final int[] globalStatus = new int[GLOBAL_SAMPLES];
    private int globalNext;
    private long globalTotal;
    private final Map<String, RouteStats> routes = new ConcurrentHashMap<>();

    public RequestMetrics() {
        this(Clock.systemUTC());
    }

    RequestMetrics(Clock clock) {
        this.clock = clock;
        this.startedAt = clock.instant();
    }

    public record Window(long requests, double p50Ms, double p95Ms, double maxMs, long serverErrors, double perMinute) {}

    public record RouteSnapshot(String route, long requests, double p50Ms, double p95Ms, double maxMs, long serverErrors,
                                Instant lastAt) {}

    public record Snapshot(Instant startedAt, long totalRequests, Window lastFifteenMinutes, List<RouteSnapshot> slowestRoutes) {}

    public void record(String route, double millis, int status) {
        long now = clock.millis();
        synchronized (this) {
            globalAt[globalNext] = now;
            globalMs[globalNext] = millis;
            globalStatus[globalNext] = status;
            globalNext = (globalNext + 1) % GLOBAL_SAMPLES;
            globalTotal++;
        }
        RouteStats stats = routes.get(route);
        if (stats == null) {
            if (routes.size() >= MAX_ROUTES) {
                route = "(otras rutas)";
            }
            stats = routes.computeIfAbsent(route, key -> new RouteStats());
        }
        stats.add(millis, status, now);
    }

    public Snapshot snapshot(int routeLimit) {
        long now = clock.millis();
        List<Double> recent = new ArrayList<>();
        long errors = 0;
        long total;
        synchronized (this) {
            total = globalTotal;
            int stored = (int) Math.min(globalTotal, GLOBAL_SAMPLES);
            for (int i = 0; i < stored; i++) {
                if (now - globalAt[i] <= WINDOW_MS) {
                    recent.add(globalMs[i]);
                    if (globalStatus[i] >= 500) {
                        errors++;
                    }
                }
            }
        }
        double[] values = recent.stream().mapToDouble(Double::doubleValue).toArray();
        long elapsedMs = Math.max(60_000L, Math.min(WINDOW_MS, now - startedAt.toEpochMilli()));
        Window window = new Window(values.length, percentile(values, 50), percentile(values, 95), max(values), errors,
                round(values.length / (elapsedMs / 60_000.0)));
        List<RouteSnapshot> slowest = routes.entrySet().stream()
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .sorted(Comparator.comparingDouble(RouteSnapshot::p95Ms).reversed())
                .limit(routeLimit)
                .toList();
        return new Snapshot(startedAt, total, window, slowest);
    }

    static double percentile(double[] values, int percent) {
        if (values.length == 0) {
            return 0;
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int index = (int) Math.ceil(percent / 100.0 * sorted.length) - 1;
        return round(sorted[Math.max(0, Math.min(index, sorted.length - 1))]);
    }

    private static double max(double[] values) {
        return round(Arrays.stream(values).max().orElse(0));
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static final class RouteStats {
        private final double[] durations = new double[ROUTE_SAMPLES];
        private int next;
        private long count;
        private long serverErrors;
        private long lastAt;

        synchronized void add(double millis, int status, long at) {
            durations[next] = millis;
            next = (next + 1) % ROUTE_SAMPLES;
            count++;
            if (status >= 500) {
                serverErrors++;
            }
            lastAt = at;
        }

        synchronized RouteSnapshot snapshot(String route) {
            double[] values = Arrays.copyOf(durations, (int) Math.min(count, ROUTE_SAMPLES));
            return new RouteSnapshot(route, count, percentile(values, 50), percentile(values, 95), max(values),
                    serverErrors, Instant.ofEpochMilli(lastAt));
        }
    }
}
