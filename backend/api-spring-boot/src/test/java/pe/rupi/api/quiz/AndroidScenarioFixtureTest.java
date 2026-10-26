package pe.rupi.api.quiz;
import java.util.*;import java.nio.file.*;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import pe.rupi.api.shared.PasswordService;import pe.rupi.api.flashcard.FlashcardService;import pe.rupi.api.visualsummary.*;import pe.rupi.api.report.ResultsReportRepository;
import static org.mockito.Mockito.*;
/** Opt-in fixture. QuizIntegrationTest.setup refuses any non-local test database. */
@EnabledIfEnvironmentVariable(named="RUPI_ANDROID_FIXTURE",matches="isolated")
class AndroidScenarioFixtureTest {
 @Test void createOnlyIsolatedEmulatorScenario() throws Exception {
  var f=new QuizIntegrationTest();f.setup();String username="android-rupi-"+f.student.substring(0,8);
  f.jdbc.update("UPDATE identidad_cuenta_usuario SET nombre_usuario=?,hash_clave=? WHERE id=?",username,new PasswordService().encode("RupiTest2026!"),f.student);
  f.jdbc.update("INSERT INTO identidad_usuario_rol(usuario_id,rol_id) SELECT ?,id FROM identidad_rol WHERE codigo='ESTUDIANTE'",f.student);
  String room=f.jdbc.queryForObject("SELECT id FROM escuela_aula LIMIT 1",String.class),teacher=UUID.randomUUID().toString();
  f.jdbc.update("INSERT INTO escuela_membresia_aula(id,aula_id,usuario_id,rol) VALUES(?,?,?,'ESTUDIANTE')",UUID.randomUUID().toString(),room,f.student);
  when(f.access.routes(teacher,room)).thenReturn(List.of(new ResultsReportRepository.Route(f.route,"Ruta de prueba",1,"PUBLICADO")));
  var named=new NamedParameterJdbcTemplate(f.jdbc);var cards=new FlashcardService(named,f.repo,f.access);
  for(int i=0;i<2;i++){int n=i;f.tx.executeWithoutResult(s->cards.create(teacher,room,f.route,new FlashcardService.Create(UUID.randomUUID(),UUID.fromString(f.node),"PRUEBA AISLADA "+n+": ¿Cuánto es cuatro más tres?","Siete.","Fixture aislado, no material oficial.",true)));}
  f.generate();
  var ai=mock(SummaryProvider.class);when(ai.configured()).thenReturn(true);when(ai.provider()).thenReturn("TEST_ONLY");when(ai.model()).thenReturn("fixture");
  when(ai.generate(any())).thenAnswer(c->SummaryIntegrationTest.draft(c.getArgument(0)));
  var summaries=new SummaryService(named,f.repo,ai,f.access,new DataSourceTransactionManager(f.jdbc.getDataSource()));
  var summary=summaries.generate(f.student,f.route,f.node,UUID.randomUUID());
  Files.writeString(Path.of("../../.local/android-fixture.json"),SummaryProvider.JSON.writeValueAsString(Map.of("username",username,"student",f.student,"route",f.route,"node",f.node,"summary",summary.id())));
 }
}
