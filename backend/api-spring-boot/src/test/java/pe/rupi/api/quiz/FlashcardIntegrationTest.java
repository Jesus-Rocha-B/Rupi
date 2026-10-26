package pe.rupi.api.quiz;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.flashcard.FlashcardService;
import pe.rupi.api.report.ResultsReportRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@EnabledIfEnvironmentVariable(named="RUPI_HU21_TEST_DB",matches="isolated")
class FlashcardIntegrationTest {
 QuizIntegrationTest f;FlashcardService service;String room,teacher;String card;
 @BeforeEach void setup(){
  f=new QuizIntegrationTest();f.setup();
  room=f.jdbc.queryForObject("SELECT id FROM escuela_aula LIMIT 1",String.class);
  teacher=UUID.randomUUID().toString();
  f.jdbc.update("INSERT INTO escuela_membresia_aula(id,aula_id,usuario_id,rol) VALUES(?,?,?,'ESTUDIANTE')",UUID.randomUUID().toString(),room,f.student);
  when(f.access.routes(teacher,room)).thenReturn(List.of(new ResultsReportRepository.Route(f.route,"Prueba",1,"PUBLICADO")));
  service=new FlashcardService(new NamedParameterJdbcTemplate(f.jdbc),f.repo,f.access);
  create("¿Cuánto es cuatro más tres?");
  card=service.get(f.student,f.route,f.node).current();
 }
 void create(String question){f.tx.executeWithoutResult(s->service.create(teacher,room,f.route,new FlashcardService.Create(UUID.randomUUID(),UUID.fromString(f.node),question,"Siete","Material ficticio de prueba aislada",true)));}
 FlashcardService.Deck cmd(String action,String id){var d=service.get(f.student,f.route,f.node);return f.tx.execute(s->service.command(f.student,f.route,f.node,new FlashcardService.Command(UUID.randomUUID(),d.revision(),action,UUID.fromString(id))));}
 @Test void revealRequiredAndProgressResumesWithoutOfficialAnswers(){
  assertFalse(service.get(f.student,f.route,f.node).revealed());
  assertEquals(409,assertThrows(ResponseStatusException.class,()->cmd("REMEMBER",card)).getStatusCode().value());
  cmd("REVEAL",card);cmd("PRACTICE",card);
  var restored=service.get(f.student,f.route,f.node);assertTrue(restored.revealed());assertEquals("PRACTICAR",restored.cards().getFirst().state());
  assertEquals(0,f.jdbc.queryForObject("SELECT COUNT(*) FROM evaluacion_intento WHERE estudiante_id=?",Integer.class,f.student));
 }
 @Test void duplicateRequestAndStaleRevisionCannotDoubleWrite(){
  var command=new FlashcardService.Command(UUID.randomUUID(),0,"REVEAL",UUID.fromString(card));
  var first=f.tx.execute(s->service.command(f.student,f.route,f.node,command));
  var retry=f.tx.execute(s->service.command(f.student,f.route,f.node,command));
  assertEquals(first,retry);assertEquals(1,retry.revision());
  var stale=new FlashcardService.Command(UUID.randomUUID(),0,"REMEMBER",UUID.fromString(card));
  assertEquals(409,assertThrows(ResponseStatusException.class,()->f.tx.execute(s->service.command(f.student,f.route,f.node,stale))).getStatusCode().value());
 }
 @Test void prioritizeNeedsPracticeAndNavigationDoesNotRate(){
  create("¿Cuál es el resultado de sumar tres y cuatro?");cmd("REVEAL",card);cmd("REMEMBER",card);
  var deck=service.get(f.student,f.route,f.node);assertEquals("NUEVA",deck.cards().getFirst().state());
  String other=deck.cards().getFirst().id();cmd("NAVIGATE",other);
  assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM aprendizaje_repaso_tarjeta WHERE usuario_id=?",Integer.class,f.student));
  cmd("REVEAL",other);cmd("PRACTICE",other);assertEquals(other,cmd("RESTART",other).current());assertFalse(service.get(f.student,f.route,f.node).revealed());
 }
 @Test void anotherStudentForeignCardAndUnavailableNodeAreRejected(){
  assertEquals(404,assertThrows(ResponseStatusException.class,()->service.get(UUID.randomUUID().toString(),f.route,f.node)).getStatusCode().value());
  assertEquals(404,assertThrows(ResponseStatusException.class,()->cmd("NAVIGATE",UUID.randomUUID().toString())).getStatusCode().value());
  f.jdbc.update("UPDATE aprendizaje_nodo_ruta SET disponible_desde=UTC_TIMESTAMP(6)+INTERVAL 1 DAY WHERE id=?",f.node);
  assertThrows(ResponseStatusException.class,()->service.get(f.student,f.route,f.node));
 }
 @Test void retiredCardKeepsHistoryAndNewVersionStartsFresh(){
  cmd("REVEAL",card);cmd("REMEMBER",card);service.retire(teacher,room,f.route,card);
  assertTrue(service.get(f.student,f.route,f.node).cards().isEmpty());
  create("¿Cuánto es cuatro más tres?");var d=service.get(f.student,f.route,f.node);
  assertNotEquals(card,d.current());assertEquals("NUEVA",d.cards().getFirst().state());assertFalse(d.revealed());
  assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM aprendizaje_repaso_tarjeta WHERE tarjeta_id=?",Integer.class,card));
 }
 @Test void teacherMustOwnRoomAndConfirmReview(){
  assertThrows(ResponseStatusException.class,()->service.teacherCards(UUID.randomUUID().toString(),room,f.route));
  assertThrows(ResponseStatusException.class,()->service.create(teacher,room,f.route,new FlashcardService.Create(UUID.randomUUID(),UUID.fromString(f.node),"Pregunta","Respuesta","Fuente",false)));
 }
 @Test void publicationIdempotentAndMembershipRequired(){
  var request=new FlashcardService.Create(UUID.randomUUID(),UUID.fromString(f.node),"Pregunta de prueba","Respuesta","Fuente",true);
  service.create(teacher,room,f.route,request);service.create(teacher,room,f.route,request);
  assertEquals(2,service.get(f.student,f.route,f.node).cards().size());
  f.jdbc.update("UPDATE escuela_membresia_aula SET estado='RETIRADO',retiro_en=UTC_TIMESTAMP(6) WHERE aula_id=? AND usuario_id=?",room,f.student);
  assertTrue(service.get(f.student,f.route,f.node).cards().isEmpty());
 }
}
