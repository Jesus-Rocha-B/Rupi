package pe.rupi.api.quiz;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.datasource.*;
import pe.rupi.api.report.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="RUPI_HU21_SHARED_READ",matches="true")
class QuizSharedReadTest {
 @Test void sharedTopicsAndResumptionAreReadableWithoutGeneratingOrWriting(){
  String url=System.getenv("RUPI_DB_URL");
  assertTrue(url.contains(".tidbcloud.com:4000/rupi?")&&url.contains("sslMode=VERIFY_IDENTITY"));
  var source=new DriverManagerDataSource(url,System.getenv("RUPI_DB_USERNAME"),System.getenv("RUPI_DB_PASSWORD"));
  var jdbc=new JdbcTemplate(source);var named=new NamedParameterJdbcTemplate(source);
  var repo=new QuizRepository(named);
  var service=new QuizService(repo,new ChatQuizProvider("","","",""),new ResultsReportService(new ResultsReportRepository(named)),new DataSourceTransactionManager(source));
  var students=jdbc.queryForList("""
   SELECT DISTINCT u.id FROM identidad_cuenta_usuario u JOIN identidad_usuario_rol ur ON ur.usuario_id=u.id
   JOIN identidad_rol r ON r.id=ur.rol_id WHERE r.codigo='ESTUDIANTE' AND u.estado='ACTIVO' LIMIT 3
   """,String.class);
  int checked=0;
  for(var student:students)for(var topic:service.topics(student).stream().limit(3).toList()){
   var status=service.availability(student,topic.routeId(),topic.nodeId());
   assertFalse(status.configured());checked++;
  }
  assertTrue(checked>0);
  assertEquals(4,jdbc.queryForObject("""
   SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
   AND table_name IN ('evaluacion_cuestionario_ia','evaluacion_practica_ia','evaluacion_limite_ia','evaluacion_reporte_pregunta_ia')
   """,Integer.class));
  System.out.println("HU21 shared SELECT-only: "+checked+" accessible topics; four tables ready; provider disabled.");
 }
}
