package pe.rupi.api.reminder;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Solo la instancia aislada de desarrollo en 3308; nunca modifica la base local habitual. */
@EnabledIfEnvironmentVariable(named="RUPI_HU14_TEST_DB",matches="isolated")
class ReminderRepositoryIntegrationTest {
    static JdbcTemplate jdbc; ReminderRepository repo;
    static final String STUDENT="11111111-1111-4111-8111-111111111111";
    static final Instant NOW=Instant.parse("2026-10-14T14:00:00Z");
    String room,route,node,teacher;
    @BeforeAll static void connect() {
        String url=System.getenv("RUPI_DB_URL");
        assertTrue(url.startsWith("jdbc:mysql://127.0.0.1:3308/rupi?"),"Usa solo MySQL temporal 3308");
        jdbc=new JdbcTemplate(new DriverManagerDataSource(url,System.getenv("RUPI_DB_USERNAME"),System.getenv("RUPI_DB_PASSWORD")));
    }
    @BeforeEach void setup() {
        repo=new ReminderRepository(jdbc);
        var fixture=jdbc.queryForMap("""
            SELECT m.aula_id,v.id route_id,n.id node_id,COALESCE(r.propietario_id,m.usuario_id) teacher
            FROM escuela_membresia_aula m JOIN escuela_aula aula ON aula.id=m.aula_id
            JOIN aprendizaje_inscripcion_ruta i ON i.estudiante_id=m.usuario_id
            JOIN aprendizaje_version_ruta v ON v.id=i.version_ruta_id AND v.grado_id=aula.grado_id AND v.estado='PUBLICADO'
            JOIN aprendizaje_ruta r ON r.id=v.ruta_id
            JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id=v.id
            JOIN aprendizaje_version_actividad av ON av.id=n.version_actividad_id AND av.estado='PUBLICADO'
            JOIN aprendizaje_actividad act ON act.id=av.actividad_id AND act.tipo IN ('RETO','CUESTIONARIO')
            WHERE m.usuario_id=? AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO' ORDER BY v.id,n.numero_secuencia LIMIT 1
            """,STUDENT);
        room=(String)fixture.get("aula_id");route=(String)fixture.get("route_id");node=(String)fixture.get("node_id");teacher=(String)fixture.get("teacher");
        jdbc.update("DELETE FROM escuela_recordatorio_evaluacion");
        jdbc.update("DELETE FROM escuela_planificacion_ruta");
    }
    void plan(Instant date) {
        jdbc.update("""
            INSERT INTO escuela_planificacion_ruta(id,aula_id,version_ruta_id,tipo,referencia_id,titulo,inicia_en,termina_en,actualizado_por)
            VALUES (?,?,?,'EVALUACION',?,'Evaluación HU14',?,?,?) ON DUPLICATE KEY UPDATE inicia_en=VALUES(inicia_en),termina_en=VALUES(termina_en)
            """,UUID.randomUUID().toString(),room,route,node,ReminderRepository.timestamp(date),ReminderRepository.timestamp(date.plusSeconds(3600)),teacher);
    }
    void generate() {repo.generate(NOW,NOW.plus(Duration.ofHours(24)));}
    int count() {return jdbc.queryForObject("SELECT COUNT(*) FROM escuela_recordatorio_evaluacion",Integer.class);}
    @Test void automaticallyGeneratesOnlyInsideWindowIncludingBoundary() {
        plan(NOW.plus(Duration.ofHours(24)).plusSeconds(1));generate();assertEquals(0,count());
        plan(NOW.plus(Duration.ofHours(24)));generate();assertEquals(1,count());
        assertEquals(1,repo.visible(STUDENT,route,NOW).size());
    }
    @Test void doesNotSendExpiredEvaluationsOrUnits() {
        plan(NOW);generate();assertEquals(0,count());
        plan(NOW.minusSeconds(60));generate();assertEquals(0,count());
        plan(NOW.plusSeconds(3600));jdbc.update("UPDATE escuela_planificacion_ruta SET tipo='UNIDAD'");generate();assertEquals(0,count());
    }
    @Test void repeatedCyclesAndNewWorkerDoNotDuplicateOrResetRead() {
        plan(NOW.plusSeconds(3600));generate();var reminder=repo.visible(STUDENT,route,NOW).getFirst();
        repo.markRead(STUDENT,reminder.id(),NOW);generate();new ReminderRepository(jdbc).generate(NOW,NOW.plusSeconds(86400));
        assertEquals(1,count());assertTrue(repo.visible(STUDENT,route,NOW).getFirst().read());
        repo.markRead(STUDENT,reminder.id(),NOW.plusSeconds(100));assertEquals(1,count());
    }
    @Test void concurrentWorkersCreateExactlyOneReminder() throws Exception {
        plan(NOW.plusSeconds(3600));
        try(var pool=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);
            Callable<Void> task=()->{gate.await();generate();return null;};
            var a=pool.submit(task);var b=pool.submit(task);gate.countDown();a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
        }
        assertEquals(1,count());
    }
    @Test void reschedulingAndDeletingHidesOldNoticeAndRecreatingSameDateDoesNotDuplicate() {
        var original=NOW.plusSeconds(3600);plan(original);generate();String first=repo.visible(STUDENT,route,NOW).getFirst().id();
        plan(NOW.plusSeconds(7200));assertTrue(repo.visible(STUDENT,route,NOW).isEmpty());generate();assertEquals(2,count());
        assertNotEquals(first,repo.visible(STUDENT,route,NOW).getFirst().id());
        jdbc.update("DELETE FROM escuela_planificacion_ruta");assertTrue(repo.visible(STUDENT,route,NOW).isEmpty());
        plan(original);generate();assertEquals(2,count());assertEquals(first,repo.visible(STUDENT,route,NOW).getFirst().id());
    }
    @Test void disabledClassroomCannotReceiveOrViewReminders() {
        plan(NOW.plusSeconds(3600));generate();
        try {jdbc.update("UPDATE escuela_aula SET activa=FALSE WHERE id=?",room);generate();assertTrue(repo.visible(STUDENT,route,NOW).isEmpty());}
        finally {jdbc.update("UPDATE escuela_aula SET activa=TRUE WHERE id=?",room);}
        assertEquals(1,count());
    }
    @Test void anotherUserCannotReadOrMarkTheNotice() {
        plan(NOW.plusSeconds(3600));generate();String id=repo.visible(STUDENT,route,NOW).getFirst().id();
        String other=UUID.randomUUID().toString();assertTrue(repo.visible(other,route,NOW).isEmpty());repo.markRead(other,id,NOW);assertFalse(repo.visible(STUDENT,route,NOW).getFirst().read());
    }
    @Test void unpublishedRouteDoesNotSendReminders() {
        plan(NOW.plusSeconds(3600));
        try {jdbc.update("UPDATE aprendizaje_version_ruta SET estado='BORRADOR' WHERE id=?",route);generate();assertEquals(0,count());}
        finally {jdbc.update("UPDATE aprendizaje_version_ruta SET estado='PUBLICADO' WHERE id=?",route);}
    }
    @Test void suspensionRevokesVisibilityAndNewDelivery() {
        plan(NOW.plusSeconds(3600));generate();
        try {jdbc.update("UPDATE aprendizaje_inscripcion_ruta SET estado='SUSPENDIDO' WHERE estudiante_id=? AND version_ruta_id=?",STUDENT,route);
            assertTrue(repo.visible(STUDENT,route,NOW).isEmpty());plan(NOW.plusSeconds(7200));generate();assertEquals(1,count());}
        finally {jdbc.update("UPDATE aprendizaje_inscripcion_ruta SET estado='ACTIVO' WHERE estudiante_id=? AND version_ruta_id=?",STUDENT,route);}
    }
}
