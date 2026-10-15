package pe.rupi.api.reinforcement;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Exclusivamente MySQL aislado 3308; todas las fixtures se revierten. */
@EnabledIfEnvironmentVariable(named="RUPI_HU16_TEST_DB",matches="isolated")
class ReinforcementRepositoryIntegrationTest {
    static final UUID STUDENT=UUID.fromString("11111111-1111-4111-8111-111111111111");
    JdbcTemplate jdbc; ReinforcementRepository repo; ReinforcementService service; TransactionTemplate tx;
    UUID route,node;String activity;
    @BeforeEach void setup() {
        String url=System.getenv("RUPI_DB_URL");assertTrue(url.startsWith("jdbc:mysql://127.0.0.1:3308/rupi?"));
        var source=new DriverManagerDataSource(url,System.getenv("RUPI_DB_USERNAME"),System.getenv("RUPI_DB_PASSWORD"));
        jdbc=new JdbcTemplate(source);repo=new ReinforcementRepository(new NamedParameterJdbcTemplate(source));
        service=new ReinforcementService(repo);tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        var fixture=jdbc.queryForMap("""
            SELECT v.id route,n.id node,n.version_actividad_id activity
            FROM aprendizaje_inscripcion_ruta i JOIN aprendizaje_version_ruta v ON v.id=i.version_ruta_id
            JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id=v.id
            JOIN aprendizaje_progreso_nodo p ON p.inscripcion_id=i.id AND p.nodo_ruta_id=n.id
            WHERE i.estudiante_id=? AND v.estado='PUBLICADO' AND p.estado IN ('EN_CURSO','COMPLETADO','DISPONIBLE')
            ORDER BY n.numero_secuencia,v.id LIMIT 1
            """,STUDENT.toString());
        route=UUID.fromString((String)fixture.get("route"));node=UUID.fromString((String)fixture.get("node"));activity=(String)fixture.get("activity");
    }
    void rollback(Runnable body) {tx.executeWithoutResult(s->{try {body.run();}finally{s.setRollbackOnly();}});}
    String question(int sequence) {
        String id=UUID.randomUUID().toString();jdbc.update("""
            INSERT INTO evaluacion_pregunta(id,version_actividad_id,numero_secuencia,tipo,enunciado,puntos)
            VALUES (?, ?, ?, 'OPCION_UNICA','Pregunta HU16',1)
            """,id,activity,sequence);return id;
    }
    void answer(String question,boolean correct,String time) {
        String attempt=UUID.randomUUID().toString();jdbc.update("""
            INSERT INTO evaluacion_intento(id,estudiante_id,version_actividad_id,cliente_evento_id)
            VALUES (?,?,?,?)
            """,attempt,STUDENT.toString(),activity,UUID.randomUUID().toString());
        jdbc.update("""
            INSERT INTO evaluacion_respuesta(id,intento_id,pregunta_id,es_correcta,respondida_en)
            VALUES (?,?,?,?,?)
            """,UUID.randomUUID().toString(),attempt,question,correct,time);
    }
    @Test void latestResponsePerQuestionWinsAcrossAttempts() {rollback(()->{
        String q=question(500);answer(q,false,"2026-10-01 10:00:00");answer(q,true,"2026-10-02 10:00:00");
        var evidence=repo.evidence(STUDENT,activity).stream().filter(e->e.question().equals("Pregunta HU16")).toList();
        assertEquals(1,evidence.size());assertTrue(evidence.getFirst().correct());
    });}
    @Test void dedupSurvivesNewRepositoryAndDoesNotResetCompletion() {rollback(()->{
        String key=UUID.randomUUID().toString();repo.link(STUDENT,node,key);repo.link(STUDENT,node,key);
        var first=repo.links(STUDENT,node).stream().filter(l->l.resourceId().equals(key)).findFirst().orElseThrow();
        repo.complete(STUDENT,node,UUID.fromString(first.id()));
        new ReinforcementRepository(new NamedParameterJdbcTemplate(jdbc)).link(STUDENT,node,key);
        var links=repo.links(STUDENT,node).stream().filter(l->l.resourceId().equals(key)).toList();
        assertEquals(1,links.size());assertTrue(links.getFirst().completed());assertEquals(first.id(),links.getFirst().id());
    });}
    @Test void anotherStudentCannotReadOrCompleteLink() {rollback(()->{
        String key=UUID.randomUUID().toString();repo.link(STUDENT,node,key);
        var link=repo.links(STUDENT,node).stream().filter(l->l.resourceId().equals(key)).findFirst().orElseThrow();
        UUID other=UUID.randomUUID();assertTrue(repo.context(other,route,node).isEmpty());assertTrue(repo.links(other,node).isEmpty());
        repo.complete(other,node,UUID.fromString(link.id()));assertFalse(repo.links(STUDENT,node).stream().filter(l->l.id().equals(link.id())).findFirst().orElseThrow().completed());
    });}
    @Test void pausedRouteRevokesAccess() {rollback(()->{
        assertTrue(repo.context(STUDENT,route,node).isPresent());
        jdbc.update("UPDATE aprendizaje_version_ruta SET estado='BORRADOR' WHERE id=?",route.toString());
        assertTrue(repo.context(STUDENT,route,node).isEmpty());
    });}
    @Test void blockedNodeAndSuspendedEnrollmentAreRejected() {rollback(()->{
        jdbc.update("UPDATE aprendizaje_progreso_nodo SET estado='BLOQUEADO',desbloqueado_en=NULL,completado_en=NULL WHERE nodo_ruta_id=?",node.toString());
        assertTrue(repo.context(STUDENT,route,node).isEmpty());
        jdbc.update("UPDATE aprendizaje_progreso_nodo SET estado='EN_CURSO',desbloqueado_en=UTC_TIMESTAMP(6) WHERE nodo_ruta_id=?",node.toString());
        jdbc.update("UPDATE aprendizaje_inscripcion_ruta SET estado='SUSPENDIDO' WHERE estudiante_id=? AND version_ruta_id=?",STUDENT.toString(),route.toString());
        assertTrue(repo.context(STUDENT,route,node).isEmpty());
    });}
    @Test void foreignRouteCannotBeCombinedWithAnAccessibleNode() {assertTrue(repo.context(STUDENT,UUID.randomUUID(),node).isEmpty());}
    @Test void lowPerformanceLinksResourceAndImprovementKeepsHistory() {rollback(()->{
        jdbc.update("""
            DELETE o FROM evaluacion_respuesta_opcion o JOIN evaluacion_respuesta r ON r.id=o.respuesta_id
            JOIN evaluacion_intento i ON i.id=r.intento_id WHERE i.estudiante_id=? AND i.version_actividad_id=?
            """,STUDENT.toString(),activity);
        jdbc.update("""
            DELETE r FROM evaluacion_respuesta r JOIN evaluacion_intento i ON i.id=r.intento_id
            WHERE i.estudiante_id=? AND i.version_actividad_id=?
            """,STUDENT.toString(),activity);
        String a=question(501),b=question(502);answer(a,false,"2026-10-01 10:00:00");answer(b,true,"2026-10-01 10:00:00");
        var first=service.refresh(STUDENT,route,node);assertEquals("REFUERZO",first.status());assertEquals(1,first.resources().size());
        var id=UUID.fromString(first.resources().getFirst().id());service.complete(STUDENT,route,node,id);
        service.complete(STUDENT,route,node,id);answer(a,true,"2026-10-02 10:00:00");
        var improved=service.refresh(STUDENT,route,node);assertEquals("AVANCE",improved.status());assertEquals(2,improved.correct());
        assertTrue(improved.resources().getFirst().completed());assertEquals(id.toString(),improved.resources().getFirst().id());
    });}
    @Test void concurrentRefreshesCreateOneLink() throws Exception {
        String key=UUID.randomUUID().toString();
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var gate=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Void> task=()->{gate.await();repo.link(STUDENT,node,key);return null;};
            var a=pool.submit(task);var b=pool.submit(task);gate.countDown();
            a.get(10,java.util.concurrent.TimeUnit.SECONDS);b.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(1,repo.links(STUDENT,node).stream().filter(l->l.resourceId().equals(key)).count());
        } finally {jdbc.update("DELETE FROM aprendizaje_refuerzo_nodo WHERE estudiante_id=? AND nodo_id=? AND recurso_id=?",STUDENT.toString(),node.toString(),key);}
    }
}
