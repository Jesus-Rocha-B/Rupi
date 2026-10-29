package pe.rupi.api.monitor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** HU-45: tiempos de respuesta en memoria y acceso al monitor solo con token. */
class RequestMetricsTest {

    /** Reloj que avanza a mano para probar la ventana de 15 minutos. */
    private static final class ManualClock extends Clock {
        final AtomicLong millis = new AtomicLong(Instant.parse("2026-10-29T10:00:00Z").toEpochMilli());
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
    }

    @Test
    void percentilesUseTheNearestRank() {
        double[] values = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        assertEquals(50, RequestMetrics.percentile(values, 50));
        assertEquals(100, RequestMetrics.percentile(values, 95));
        assertEquals(0, RequestMetrics.percentile(new double[0], 95));
    }

    @Test
    void slowestRoutesComeFirstAndServerErrorsAreCounted() {
        var metrics = new RequestMetrics(new ManualClock());
        for (int i = 0; i < 20; i++) {
            metrics.record("GET /api/v1/student/session", 5, 200);
            metrics.record("GET /api/v1/teacher/reports", 900 + i, i == 0 ? 500 : 200);
        }
        var snapshot = metrics.snapshot(5);
        assertEquals(40, snapshot.totalRequests());
        assertEquals("GET /api/v1/teacher/reports", snapshot.slowestRoutes().get(0).route());
        assertEquals(1, snapshot.slowestRoutes().get(0).serverErrors());
        assertEquals(1, snapshot.lastFifteenMinutes().serverErrors());
        assertEquals(40, snapshot.lastFifteenMinutes().requests());
    }

    @Test
    void oldRequestsLeaveTheFifteenMinuteWindow() {
        var clock = new ManualClock();
        var metrics = new RequestMetrics(clock);
        metrics.record("GET /api/v1/student/session", 30, 200);
        clock.millis.addAndGet(16 * 60 * 1000L);
        metrics.record("GET /api/v1/student/session", 10, 200);
        var window = metrics.snapshot(5).lastFifteenMinutes();
        assertEquals(1, window.requests());
        assertEquals(10, window.maxMs());
    }

    @Test
    void monitorIsHiddenWithoutTokenAndClosedWithAWrongOne() {
        var jdbc = mock(JdbcTemplate.class);
        var hidden = new MonitorController(new RequestMetrics(), jdbc, "");
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> hidden.health("x")).getStatusCode().value());
        var closed = new MonitorController(new RequestMetrics(), jdbc, "secreto-de-prueba");
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> closed.health("otro")).getStatusCode().value());
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> closed.health(null)).getStatusCode().value());
    }

    @Test
    void monitorReportsDatabaseDownAsDegraded() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("caída"));
        var health = new MonitorController(new RequestMetrics(), jdbc, "secreto-de-prueba").health("secreto-de-prueba");
        assertEquals("DEGRADED", health.state());
        assertEquals("DOWN", health.database().state());
    }
}
