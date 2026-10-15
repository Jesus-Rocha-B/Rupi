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

@EnabledIfEnvironmentVariable(named="RUPI_HU19_TEST_DB",matches="isolated")
class StudentReportRepositoryIntegrationTest {
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
            VALUES (?, ?, ?, 'OPCION_UNICA','Pregunta reporte HU19',1)
            """,id,activity,sequence);return id;
    }
    GradesRepository grades() {return new GradesRepository(new NamedParameterJdbcTemplate(jdbc));}
    String q1,q2;
    void isolatedActivity() {
        String previous=activity;activity=UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO aprendizaje_version_actividad(id,actividad_id,numero_version,titulo,estado)
            SELECT ?,actividad_id,9001,'Actividad aislada HU19','PUBLICADO' FROM aprendizaje_version_actividad WHERE id=?
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
    StudentReportService overviewService() {return new StudentReportService(service,repo,grades(),new StudentReportRepository(new NamedParameterJdbcTemplate(jdbc)));}
    void extraMember() {jdbc.update("INSERT INTO escuela_membresia_aula(id,aula_id,usuario_id,rol,estado) VALUES (?,?,?,'ESTUDIANTE','ACTIVO')",UUID.randomUUID().toString(),room,teacher);}
    @Test void filterReturnsOnlySelectedStudentAndAllRestoresRoster() {rollback(()->{
        extraMember();isolatedActivity();graded("1","0","2026-10-15 10:00:00",enrollment,"COMPLETADO");
        var all=service.report(teacher,room,route);assertTrue(all.studentCount()>=2);
        var one=service.report(teacher,room,route,STUDENT);assertEquals(1,one.studentCount());
        assertTrue(one.units().stream().flatMap(u->u.students().stream()).allMatch(s->s.studentId().equals(STUDENT)));
        var data=repo.data(room,route,teacher);assertTrue(data.answers().isEmpty());assertTrue(data.completed().isEmpty());
        assertTrue(grades().attempts(room,route,teacher).isEmpty());
    });}
    @Test void selectedMemberWithoutEnrollmentHasNoGradeAndNoExportableData() {rollback(()->{
        extraMember();var overview=overviewService().overview(teacher,room,route,teacher);
        assertNull(overview.enrollmentState());assertEquals(0,overview.answered());assertEquals(0,overview.completedNodes());
        assertTrue(overview.units().stream().allMatch(u->u.average()==null && u.status().equals("SIN_INSCRIPCION")));
        var report=new GradesService(service,repo,grades()).report(teacher,room,route,teacher);
        assertEquals(1,report.students().size());assertTrue(report.students().getFirst().cells().stream().allMatch(c->c.average()==null));
    });}
    @Test void retiredMemberIsRejectedAcrossAllReports() {rollback(()->{
        extraMember();jdbc.update("UPDATE escuela_membresia_aula SET estado='RETIRADO',retiro_en=UTC_TIMESTAMP(6) WHERE aula_id=? AND usuario_id=? AND rol='ESTUDIANTE'",room,teacher);
        assertTrue(service.members(teacher,room).stream().noneMatch(m->m.id().equals(teacher)));
        assertThrows(ResponseStatusException.class,()->service.report(teacher,room,route,teacher));
        assertThrows(ResponseStatusException.class,()->new GradesService(service,repo,grades()).report(teacher,room,route,teacher));
        assertThrows(ResponseStatusException.class,()->overviewService().overview(teacher,room,route,teacher));
    });}
    @Test void foreignStudentOrRoomOrRouteIsRejected() {
        String foreign=UUID.randomUUID().toString();
        assertThrows(ResponseStatusException.class,()->service.report(teacher,room,route,foreign));
        assertThrows(ResponseStatusException.class,()->overviewService().overview(teacher,foreign,route,STUDENT));
        assertThrows(ResponseStatusException.class,()->overviewService().overview(teacher,room,foreign,STUDENT));
    }
    @Test void summaryAndBothExportsContainOnlySelectedEvidence() {rollback(()->{
        extraMember();isolatedActivity();graded("1","0","2026-10-15 10:00:00",enrollment,"COMPLETADO");
        var summary=overviewService().overview(teacher,room,route,STUDENT);
        var report=service.report(teacher,room,route,STUDENT);
        var g=new GradesService(service,repo,grades()).report(teacher,room,route,STUDENT);
        assertEquals(summary.student().id(),g.students().getFirst().studentId());
        var csv=ResultsReportService.csv(report);var gradeCsv=GradesService.csv(g);
        String otherName=repo.members(room).stream().filter(m->m.id().equals(teacher)).findFirst().orElseThrow().name();
        assertFalse(csv.contains(ResultsReportService.cell(otherName)));assertFalse(gradeCsv.contains(ResultsReportService.cell(otherName)));
        assertTrue(summary.activities().stream().anyMatch(a->a.id().equals(activity)));
        assertTrue(gradeCsv.contains("\"5.0\",\"C\""));
    });}
    @Test void reinforcementsRemainReadOnlyScopedAndKeepCompletion() {rollback(()->{
        extraMember();String id=UUID.randomUUID().toString();String resource=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO aprendizaje_refuerzo_nodo(id,estudiante_id,nodo_id,recurso_id,realizado_en) VALUES (?,?,?,?,UTC_TIMESTAMP(6))",id,STUDENT,node,resource);
        var links=new StudentReportRepository(new NamedParameterJdbcTemplate(jdbc));
        assertTrue(links.reinforcements(room,route,STUDENT).stream().anyMatch(l->l.id().equals(id)&&l.completedAt()!=null));
        assertTrue(links.reinforcements(room,route,teacher).isEmpty());
        assertTrue(links.reinforcements(room,UUID.randomUUID().toString(),STUDENT).isEmpty());
        int count=jdbc.queryForObject("SELECT COUNT(*) FROM aprendizaje_refuerzo_nodo",Integer.class);
        overviewService().overview(teacher,room,route,STUDENT);
        assertEquals(count,jdbc.queryForObject("SELECT COUNT(*) FROM aprendizaje_refuerzo_nodo",Integer.class));
    });}
}
