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

@EnabledIfEnvironmentVariable(named="RUPI_HU17_TEST_DB",matches="isolated")
class ResultsReportRepositoryIntegrationTest {
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
            VALUES (?, ?, ?, 'OPCION_UNICA','Pregunta reporte HU17',1)
            """,id,activity,sequence);return id;
    }
    void answer(String q,boolean correct,String date,String enrollmentId) {
        String attempt=UUID.randomUUID().toString();jdbc.update("""
            INSERT INTO evaluacion_intento(id,estudiante_id,version_actividad_id,cliente_evento_id,inscripcion_id)
            VALUES (?,?,?,?,?)
            """,attempt,STUDENT,activity,UUID.randomUUID().toString(),enrollmentId);
        jdbc.update("""
            INSERT INTO evaluacion_respuesta(id,intento_id,pregunta_id,es_correcta,respondida_en)
            VALUES (?,?,?,?,?)
            """,UUID.randomUUID().toString(),attempt,q,correct,date);
    }
    @Test void returnsOnlyOwnedClassroomsAndAssociatedRoutes() {
        assertTrue(repo.classrooms(teacher).stream().anyMatch(c->c.id().equals(room)));
        assertTrue(repo.classrooms(UUID.randomUUID().toString()).isEmpty());
        assertTrue(repo.routes(room).stream().anyMatch(r->r.id().equals(route)));
        assertFalse(service.report(teacher,room,route).units().isEmpty());
        assertThrows(ResponseStatusException.class,()->service.report(UUID.randomUUID().toString(),room,route));
    }
    @Test void dataDoesNotIncludeMembersFromAnotherClassroom() {rollback(()->{
        String other=UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO escuela_aula(id,institucion_id,anio_escolar_id,grado_id,codigo,nombre,docente_responsable_id)
            SELECT ?,institucion_id,anio_escolar_id,grado_id,?,'Aula HU17',docente_responsable_id FROM escuela_aula WHERE id=?
            """,other,"HU17-"+other.substring(0,8),room);
        assertTrue(repo.data(other,route).students().isEmpty());assertTrue(repo.data(other,route).answers().isEmpty());assertTrue(repo.routes(other).isEmpty());
    });}
    @Test void withdrawnStudentsDisappearFromRowsAndEvidence() {rollback(()->{
        jdbc.update("UPDATE escuela_membresia_aula SET estado='RETIRADO',retiro_en=UTC_TIMESTAMP(6) WHERE aula_id=? AND usuario_id=? AND rol='ESTUDIANTE'",room,STUDENT);
        var data=repo.data(room,route);assertTrue(data.students().stream().noneMatch(s->s.id().equals(STUDENT)));
        assertTrue(data.answers().stream().noneMatch(a->a.student().equals(STUDENT)));
    });}
    @Test void latestAnswerWinsWithoutCountingAttemptsTwice() {rollback(()->{
        String q=question(601);answer(q,false,"2026-10-01 10:00:00",enrollment);answer(q,true,"2026-10-02 10:00:00",enrollment);
        var results=repo.data(room,route).answers().stream().filter(a->a.question().equals(q)).toList();
        assertEquals(1,results.size());assertTrue(results.getFirst().correct());
    });}
    @Test void ambiguousUnscopedAnswersAreExcludedButScopedEvidenceRemains() {rollback(()->{
        String otherNode=jdbc.queryForObject("SELECT id FROM aprendizaje_nodo_ruta WHERE version_ruta_id<>? LIMIT 1",String.class,route);
        jdbc.update("UPDATE aprendizaje_nodo_ruta SET version_actividad_id=? WHERE id=?",activity,otherNode);
        String q=question(602);answer(q,false,"2026-10-01 10:00:00",null);
        assertTrue(repo.data(room,route).answers().stream().noneMatch(a->a.question().equals(q)));
        answer(q,true,"2026-10-02 10:00:00",enrollment);
        assertTrue(repo.data(room,route).answers().stream().anyMatch(a->a.question().equals(q)&&a.correct()));
    });}
    @Test void incoherentCurriculumCannotClaimCompetencies() {rollback(()->{
        String wrong=jdbc.queryForObject("""
            SELECT u.id FROM curriculo_unidad u JOIN aprendizaje_version_ruta v ON v.id=?
            WHERE u.grado_id<>v.grado_id LIMIT 1
            """,String.class,route);
        jdbc.update("UPDATE aprendizaje_nodo_ruta SET unidad_id=? WHERE version_ruta_id=?",wrong,route);
        var data=repo.data(room,route);assertTrue(data.nodes().stream().allMatch(n->n.unit()==null));assertTrue(data.competencies().isEmpty());
        assertEquals("SIN_UNIDAD",ResultsReportService.summarize(data).getFirst().id());
    });}
    @Test void pausedRoutesKeepHistoricalResults() {rollback(()->{
        jdbc.update("UPDATE aprendizaje_version_ruta SET estado='BORRADOR' WHERE id=?",route);
        assertEquals("BORRADOR",service.report(teacher,room,route).route().state());
    });}
    @Test void missingEnrollmentIsVisibleWithoutReadingForeignResults() {rollback(()->{
        String other=UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO escuela_membresia_aula(id,aula_id,usuario_id,rol,estado)
            VALUES (?,?,?,'ESTUDIANTE','ACTIVO')
            """,other,room,teacher);
        var data=repo.data(room,route);var member=data.students().stream().filter(s->s.id().equals(teacher)).findFirst().orElseThrow();
        assertNull(member.enrollment());
        assertEquals("SIN_INSCRIPCION",ResultsReportService.summarize(data).getFirst().students().stream().filter(s->s.studentId().equals(teacher)).findFirst().orElseThrow().status());
    });}
}
