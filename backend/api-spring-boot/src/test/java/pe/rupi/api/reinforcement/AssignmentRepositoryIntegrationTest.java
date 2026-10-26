package pe.rupi.api.reinforcement;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Refuses every endpoint except the dedicated local test database. No shared TiDB writes. */
@EnabledIfEnvironmentVariable(named="RUPI_HU20_TEST_DB",matches="isolated")
class AssignmentRepositoryIntegrationTest {
    static JdbcTemplate jdbc;
    static AssignmentRepository repo;
    static String student="10000000-0000-4000-8000-000000000001",teacher="10000000-0000-4000-8000-000000000002";
    static String room="20000000-0000-4000-8000-000000000001",route="30000000-0000-4000-8000-000000000001";
    static String node="40000000-0000-4000-8000-000000000001",resource="50000000-0000-4000-8000-000000000001";
    @BeforeAll static void setup() throws Exception {
        String url=System.getenv("RUPI_HU20_TEST_URL");
        assertNotNull(url);
        assertTrue(url.startsWith("jdbc:mysql://127.0.0.1:3307/rupi_hu20_test?"),"Refusing non-isolated database");
        var source=new DriverManagerDataSource(url,System.getenv("RUPI_HU20_TEST_USER"),System.getenv("RUPI_HU20_TEST_PASSWORD"));
        jdbc=new JdbcTemplate(source);repo=new AssignmentRepository(new NamedParameterJdbcTemplate(source));
        String id="id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY";
        String[] tables={
            "identidad_cuenta_usuario ("+id+",estado VARCHAR(20),desactivado_en DATETIME, nombre_usuario VARCHAR(80))",
            "identidad_perfil_usuario (usuario_id CHAR(36),nombre VARCHAR(80),apellido VARCHAR(80))",
            "escuela_aula ("+id+",activa BOOLEAN)",
            "escuela_membresia_aula (aula_id CHAR(36),usuario_id CHAR(36),rol VARCHAR(20),estado VARCHAR(20))",
            "aprendizaje_ruta ("+id+",archivado_en DATETIME)",
            "aprendizaje_version_ruta ("+id+",ruta_id CHAR(36),estado VARCHAR(20))",
            "aprendizaje_actividad ("+id+",archivado_en DATETIME)",
            "aprendizaje_version_actividad ("+id+",actividad_id CHAR(36),estado VARCHAR(20))",
            "aprendizaje_nodo_ruta ("+id+",version_ruta_id CHAR(36),version_actividad_id CHAR(36))",
            "aprendizaje_inscripcion_ruta ("+id+",estudiante_id CHAR(36),version_ruta_id CHAR(36),estado VARCHAR(20))",
            "aprendizaje_progreso_nodo (inscripcion_id CHAR(36),nodo_ruta_id CHAR(36),estado VARCHAR(20))"
        };
        for(String table:tables) jdbc.execute("CREATE TABLE IF NOT EXISTS "+table);
        Path root=Path.of("").toAbsolutePath();
        while(root!=null&&!Files.exists(root.resolve("database/mysql/migrations/V22__refuerzos_diferenciados_hu20.sql"))) root=root.getParent();
        assertNotNull(root);
        String sql=Files.readString(root.resolve("database/mysql/migrations/V22__refuerzos_diferenciados_hu20.sql"));
        jdbc.execute(sql); // Test the actual migration twice (idempotent).
        jdbc.execute(sql);
        jdbc.update("INSERT IGNORE INTO identidad_cuenta_usuario VALUES (?, 'ACTIVO',NULL,'ana'), (?, 'ACTIVO',NULL,'docente')",student,teacher);
        jdbc.update("INSERT IGNORE INTO escuela_aula VALUES (?,TRUE)",room);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM escuela_membresia_aula",Integer.class)==0)
            jdbc.update("INSERT INTO escuela_membresia_aula VALUES (?,?,'ESTUDIANTE','ACTIVO')",room,student);
        jdbc.update("INSERT IGNORE INTO aprendizaje_ruta VALUES (?,NULL)",route);
        jdbc.update("INSERT IGNORE INTO aprendizaje_version_ruta VALUES (?,?,'PUBLICADO')",route,route);
        jdbc.update("INSERT IGNORE INTO aprendizaje_actividad VALUES (?,NULL)",node);
        jdbc.update("INSERT IGNORE INTO aprendizaje_version_actividad VALUES (?,?,'PUBLICADO')",node,node);
        jdbc.update("INSERT IGNORE INTO aprendizaje_nodo_ruta VALUES (?,?,?)",node,route,node);
        jdbc.update("INSERT IGNORE INTO aprendizaje_inscripcion_ruta VALUES (?,?,?,'ACTIVO')",student,student,route);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM aprendizaje_progreso_nodo",Integer.class)==0)
            jdbc.update("INSERT INTO aprendizaje_progreso_nodo VALUES (?,?,'DISPONIBLE')",student,node);
    }
    @BeforeEach void clean() {
        jdbc.update("DELETE FROM aprendizaje_asignacion_refuerzo");
        jdbc.update("UPDATE aprendizaje_version_ruta SET estado='PUBLICADO'");
        jdbc.update("UPDATE escuela_membresia_aula SET estado='ACTIVO'");
    }
    MapSqlParameterSource params(String request,String selectedResource) {
        return new MapSqlParameterSource("id",UUID.randomUUID().toString()).addValue("request",request)
            .addValue("student",student).addValue("teacher",teacher).addValue("room",room).addValue("route",route)
            .addValue("node",node).addValue("resource",selectedResource).addValue("title","Practica de cantidades")
            .addValue("instructions","Resuelve en tu cuaderno").addValue("reason","Dos respuestas incorrectas")
            .addValue("resourceTitle","Cuadernillo MINEDU").addValue("url","https://repositorio.minedu.gob.pe/")
            .addValue("nodeTitle","Cantidades").addValue("answered",2).addValue("correct",0);
    }
    @Test void concurrentRequestsCreateOnlyOneActiveTask() throws Exception {
        var first=params(UUID.randomUUID().toString(),resource);var second=params(UUID.randomUUID().toString(),resource);
        try(var workers=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);
            var a=workers.submit(()->{gate.await();return repo.insert(first);});
            var b=workers.submit(()->{gate.await();return repo.insert(second);});
            gate.countDown();
            assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
        }
        assertEquals(1,repo.student(student).size());
    }
    @Test void differentResourcesAndStudentsRemainIndependent() {
        assertTrue(repo.insert(params(UUID.randomUUID().toString(),resource)));
        assertTrue(repo.insert(params(UUID.randomUUID().toString(),UUID.randomUUID().toString())));
        assertEquals(2,repo.teacher(room,route).size());
        assertTrue(repo.student(teacher).isEmpty());
        assertTrue(repo.teacher(UUID.randomUUID().toString(),route).isEmpty());
        String id=repo.student(student).getFirst().id();
        repo.start(teacher,id);assertNull(repo.student(student).getFirst().startedAt());
        assertEquals(0,repo.complete(teacher,id,"otra persona"));
    }
    @Test void completionPreservesHistoryAndRequestRetryDoesNotReopen() {
        String request=UUID.randomUUID().toString();assertTrue(repo.insert(params(request,resource)));
        var task=repo.student(student).getFirst();
        assertEquals(0,repo.complete(student,task.id(),"Sin iniciar"));
        repo.start(student,task.id());
        assertEquals(1,repo.complete(student,task.id(),"Practique sumas en mi cuaderno"));
        assertEquals(0,repo.complete(student,task.id(),"No debe sobrescribir"));
        assertFalse(repo.insert(params(request,resource)));
        assertTrue(repo.insert(params(UUID.randomUUID().toString(),resource)));
        var history=repo.student(student);assertEquals(2,history.size());
        assertEquals("Practique sumas en mi cuaderno",history.stream().filter(a->a.completedAt()!=null).findFirst().orElseThrow().reflection());
        assertEquals(1,history.stream().filter(a->a.completedAt()==null).count());
    }
    @Test void pausedRouteAndRemovedMembershipHideAssignments() {
        repo.insert(params(UUID.randomUUID().toString(),resource));
        jdbc.update("UPDATE aprendizaje_version_ruta SET estado='BORRADOR'");
        assertTrue(repo.student(student).isEmpty());
        jdbc.update("UPDATE aprendizaje_version_ruta SET estado='PUBLICADO'");
        jdbc.update("UPDATE escuela_membresia_aula SET estado='RETIRADO'");
        assertTrue(repo.student(student).isEmpty());assertTrue(repo.teacher(room,route).isEmpty());
    }
}
