package pe.rupi.api.monitor;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link MonitorService} aislando la capa de servicio de la capa HTTP.
 */
class MonitorServiceTest {

    @Test
    void pingDatabaseReturnsUpWhenQuerySucceeds() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        var service = new MonitorService(new RequestMetrics(), jdbc);

        var db = service.pingDatabase();

        assertEquals("UP", db.state());
        assertTrue(db.latencyMs() >= 0);
    }

    @Test
    void pingDatabaseReturnsDownWhenQueryFails() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("Conexión perdida"));
        var service = new MonitorService(new RequestMetrics(), jdbc);

        var db = service.pingDatabase();

        assertEquals("DOWN", db.state());
    }

    @Test
    void collectHealthAggregatesAllSubsystems() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        var service = new MonitorService(new RequestMetrics(), jdbc);

        var health = service.collectHealth(5);

        assertNotNull(health);
        assertEquals("UP", health.state());
        assertNotNull(health.jvm());
        assertNotNull(health.system());
        assertNotNull(health.requests());
        assertTrue(health.jvm().processors() > 0);
    }
}
