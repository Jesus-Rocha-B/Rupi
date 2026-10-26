package pe.rupi.api.reinforcement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import pe.rupi.api.report.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in shared verification: only SELECT statements, no login or schema changes. */
@EnabledIfEnvironmentVariable(named="RUPI_HU20_SHARED_READ",matches="true")
class AssignmentSharedReadTest {
    @Test void readsActualClassroomsAndEvidenceWithoutWriting() {
        String url=System.getenv("RUPI_DB_URL");
        assertTrue(url.contains(".tidbcloud.com:4000/rupi?")&&url.contains("sslMode=VERIFY_IDENTITY"));
        var source=new DriverManagerDataSource(url,System.getenv("RUPI_DB_USERNAME"),System.getenv("RUPI_DB_PASSWORD"));
        var jdbc=new JdbcTemplate(source);
        var named=new NamedParameterJdbcTemplate(source);
        var repo=new AssignmentRepository(named);var reports=new ResultsReportRepository(named);
        var access=new ResultsReportService(reports);
        var service=new AssignmentService(repo,access,reports,new ReinforcementRepository(named));
        var teachers=jdbc.queryForList("""
            SELECT DISTINCT u.id FROM identidad_cuenta_usuario u
            JOIN identidad_usuario_rol ur ON ur.usuario_id=u.id
            JOIN identidad_rol r ON r.id=ur.rol_id
            WHERE r.codigo='DOCENTE' AND u.estado='ACTIVO'
            """,String.class);
        assertFalse(teachers.isEmpty());
        int boards=0;
        for(var teacher:teachers) for(var room:access.classrooms(teacher)) for(var route:access.routes(teacher,room.id())) {
            var board=service.board(teacher,room.id(),route.id());
            assertNotNull(board.candidates());
            boards++;
        }
        assertTrue(boards>0,"No reportable classrooms/routes in shared data");
        System.out.println("HU20 shared SELECT-only verification: "+boards+" boards; migration available="+repo.available());
    }
}
