package pe.rupi.api.report;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUPI_HU18_TEST_DB",matches="isolated")
class GradesRepositoryIntegrationTest {
    static final String STUDENT="11111111-1111-4111-8111-111111111111";
    JdbcTemplate jdbc;ResultsReportRepository repo;ResultsReportService service;TransactionTemplate tx;
    String room,route,node,activity,enrollment,teacher;
    @BeforeEach void setup() {
        String url=System.getenv("RUPI_DB_URL");assertTrue(url.startsWith("jdbc:mysql://127.0.0.1:3308/rupi?"));
        var source=new DriverManagerDataSource(url,System.getenv("RUPI_DB_USERNAME"),System.getenv("RUPI_DB_PASSWORD"));
        jdbc=new JdbcTemplate(source);repo=new ResultsReportRepository(new NamedParameterJdbcTemplate(source));
        service=new ResultsReportService(repo);tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        var f=jdbc.queryForMap("""
            SELECT a.id room,a.docente_responsable_id teacher,v.id route,n.id node,n.version_actividad_id activity,i.id enrollment
            FROM escuela_membresia_aula m JOIN escuela_aula a ON a.id=m.aula_id AND a.activa=TRUE
            JOIN aprendizaje_inscripcion_ruta i ON i.estudiante_id=m.usuario_id
            JOIN aprendizaje_version_ruta v ON v.id=i.version_ruta_id AND v.grado_id=a.grado_id
            JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id=v.id
            WHERE m.usuario_id=? AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO' AND a.docente_responsable_id IS NOT NULL
            ORDER BY v.id,n.numero_secuencia LIMIT 1
            """,STUDENT);
        room=(String)f.get("room");teacher=(String)f.get("teacher");route=(String)f.get("route");
        node=(String)f.get("node");activity=(String)f.get("activity");enrollment=(String)f.get("enrollment");
    }
    void rollback(Runnable body) {tx.executeWithoutResult(s->{try {body.run();}finally{s.setRollbackOnly();}});}
    String question(int sequence) {
        String id=UUID.randomUUID().toString();jdbc.update("""
            INSERT INTO evaluacion_pregunta(id,version_actividad_id,numero_secuencia,tipo,enunciado,puntos)
            VALUES (?, ?, ?, 'OPCION_UNICA','Pregunta reporte HU18',1)
            """,id,activity,sequence);return id;
    }
    GradesRepository grades() {return new GradesRepository(new NamedParameterJdbcTemplate(jdbc));}
    String q1,q2;
    void isolatedActivity() {
        String previous=activity;activity=UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO aprendizaje_version_actividad(id,actividad_id,numero_version,titulo,estado)
            SELECT ?,actividad_id,9001,'Actividad aislada HU18','PUBLICADO' FROM aprendizaje_version_actividad WHERE id=?
            """,activity,previous);
        jdbc.update("UPDATE aprendizaje_nodo_ruta SET version_actividad_id=? WHERE id=?",activity,node);
        q1=question(701);q2=question(702);
        jdbc.update("UPDATE evaluacion_pregunta SET puntos=3 WHERE id=?",q2);
    }
    String graded(String earned1,String earned2,String date,String enrollmentId,String state) {
        String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO evaluacion_intento(id,estudiante_id,version_actividad_id,cliente_evento_id,inscripcion_id,estado) VALUES (?,?,?,?,?,?)",
            id,STUDENT,activity,UUID.randomUUID().toString(),enrollmentId,state);
        if(earned1!=null) jdbc.update("INSERT INTO evaluacion_respuesta(id,intento_id,pregunta_id,es_correcta,puntaje_obtenido,respondida_en) VALUES (?,?,?,TRUE,?,?)",UUID.randomUUID().toString(),id,q1,earned1,date);
        if(earned2!=null) jdbc.update("INSERT INTO evaluacion_respuesta(id,intento_id,pregunta_id,es_correcta,puntaje_obtenido,respondida_en) VALUES (?,?,?,FALSE,?,?)",UUID.randomUUID().toString(),id,q2,earned2,date);
        return id;
    }
    GradesService.Cell cell() {
        return new GradesService(service,repo,grades()).report(teacher,room,route).students().stream().filter(s->s.studentId().equals(STUDENT)).findFirst().orElseThrow()
            .cells().stream().filter(c->c.details().stream().anyMatch(d->d.activityId().equals(activity))).findFirst().orElseThrow();
    }
    @Test void calculatesWeightedPointsAndIncludesFullyAnsweredOpenAttempts() {rollback(()->{
        isolatedActivity();String id=graded("1","0","2026-10-01 10:00:00",enrollment,"EN_CURSO");
        var a=grades().attempts(room,route).stream().filter(x->x.id().equals(id)).findFirst().orElseThrow();
        assertTrue(a.eligible());assertEquals(0,a.possible().compareTo(new java.math.BigDecimal("4")));
        assertEquals("2026-10-01T10:00:00.000000Z",a.date());
        var detail=cell().details().stream().filter(d->d.activityId().equals(activity)).findFirst().orElseThrow();
        assertEquals(new java.math.BigDecimal("5.0"),detail.grade());
    });}
    @Test void latestCompleteWinsAndDoesNotMixQuestionsAcrossAttempts() {rollback(()->{
        isolatedActivity();graded("1","3","2026-10-01 10:00:00",enrollment,"COMPLETADO");
        String latest=graded("0","0","2026-10-02 10:00:00",enrollment,"EN_CURSO");
        graded("1",null,"2026-10-03 10:00:00",enrollment,"EN_CURSO");
        assertEquals(latest,cell().details().stream().filter(d->d.activityId().equals(activity)).findFirst().orElseThrow().attemptId());
    });}
    @Test void invalidPartialAbandonedAndUnsupportedAreExcluded() {rollback(()->{
        isolatedActivity();String bad=graded("2","0","2026-10-01 10:00:00",enrollment,"COMPLETADO");
        String partial=graded("1",null,"2026-10-02 10:00:00",enrollment,"COMPLETADO");
        String abandoned=graded("1","3","2026-10-03 10:00:00",enrollment,"ABANDONADO");
        var attempts=grades().attempts(room,route);
        assertFalse(attempts.stream().filter(a->a.id().equals(bad)||a.id().equals(partial)).anyMatch(GradesRepository.Attempt::eligible));
        assertTrue(attempts.stream().noneMatch(a->a.id().equals(abandoned)));
        String unsupported=graded("1","3","2026-10-04 10:00:00",enrollment,"COMPLETADO");
        jdbc.update("UPDATE evaluacion_pregunta SET tipo='TEXTO_CORTO' WHERE id=?",q2);
        assertFalse(grades().attempts(room,route).stream().filter(a->a.id().equals(unsupported)).findFirst().orElseThrow().eligible());
    });}
    @Test void ambiguousLegacyAttemptsDoNotLeakAcrossRoutes() {rollback(()->{
        isolatedActivity();String legacy=graded("1","3","2026-10-01 10:00:00",null,"EN_CURSO");
        assertTrue(grades().attempts(room,route).stream().anyMatch(a->a.id().equals(legacy)));
        String otherNode=jdbc.queryForObject("SELECT id FROM aprendizaje_nodo_ruta WHERE version_ruta_id<>? LIMIT 1",String.class,route);
        jdbc.update("UPDATE aprendizaje_nodo_ruta SET version_actividad_id=? WHERE id=?",activity,otherNode);
        String scoped=graded("1","0","2026-10-02 10:00:00",enrollment,"COMPLETADO");
        var attempts=grades().attempts(room,route);assertTrue(attempts.stream().noneMatch(a->a.id().equals(legacy)));
        assertTrue(attempts.stream().anyMatch(a->a.id().equals(scoped)));
    });}
    @Test void withdrawnMembersAndOtherClassroomsCannotContribute() {rollback(()->{
        isolatedActivity();String id=graded("1","3","2026-10-01 10:00:00",enrollment,"COMPLETADO");
        assertTrue(grades().attempts(UUID.randomUUID().toString(),route).isEmpty());
        jdbc.update("UPDATE escuela_membresia_aula SET estado='RETIRADO',retiro_en=UTC_TIMESTAMP(6) WHERE aula_id=? AND usuario_id=?",room,STUDENT);
        assertTrue(grades().attempts(room,route).stream().noneMatch(a->a.id().equals(id)));
    });}
    @Test void duplicateNodesDoNotMultiplyActivityPoints() {rollback(()->{
        isolatedActivity();String id=graded("1","3","2026-10-01 10:00:00",enrollment,"COMPLETADO");
        jdbc.update("UPDATE aprendizaje_nodo_ruta SET version_actividad_id=? WHERE version_ruta_id=?",activity,route);
        var attempts=grades().attempts(room,route).stream().filter(a->a.id().equals(id)).toList();assertEquals(1,attempts.size());
        assertEquals(0,attempts.getFirst().possible().compareTo(new java.math.BigDecimal("4")));
    });}
}
