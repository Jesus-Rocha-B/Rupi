package pe.rupi.api.quiz;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;import java.util.*;import java.util.concurrent.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.notification.StudyNotificationService;
import pe.rupi.api.notification.StudyNotificationService.Preferences;
import pe.rupi.api.reminder.ReminderRepository;
@EnabledIfEnvironmentVariable(named="RUPI_HU21_TEST_DB",matches="isolated")
class StudyNotificationIntegrationTest {
 QuizIntegrationTest f;StudyNotificationService service;
 @BeforeEach void setup(){
  f=new QuizIntegrationTest();f.setup();
  f.jdbc.update("INSERT IGNORE INTO identidad_rol(id,codigo,nombre) VALUES(UUID(),'ESTUDIANTE','Estudiante de prueba')");
  f.jdbc.update("INSERT INTO identidad_usuario_rol(usuario_id,rol_id) SELECT ?,id FROM identidad_rol WHERE codigo='ESTUDIANTE'",f.student);
  service=at("2026-10-26T22:30:00Z");
 }
 StudyNotificationService at(String instant){return new StudyNotificationService(new NamedParameterJdbcTemplate(f.jdbc),new ReminderRepository(f.jdbc),new DataSourceTransactionManager(f.jdbc.getDataSource()),Clock.fixed(Instant.parse(instant),ZoneOffset.UTC));}
 Preferences prefs(boolean goals,boolean eval,boolean system,long revision){return new Preferences(2,"17:00",goals,eval,system,"21:00","08:00",revision);}
 @Test void startsDisabledAndPersistsAcrossInstances(){
  assertFalse(service.status(f.student).preferences().goals());assertTrue(service.inbox(f.student).items().isEmpty());
  service.save(f.student,prefs(true,false,true,0));
  assertEquals(2,at("2026-10-26T22:30:00Z").status(f.student).preferences().dailyGoal());
  assertEquals(1,service.status(f.student).preferences().revision());
 }
 @Test void concurrentWorkersAndDevicesNeverDuplicate()throws Exception{
  service.save(f.student,prefs(true,false,true,0));
  try(var pool=Executors.newFixedThreadPool(2)){
   var a=pool.submit(()->service.inbox(f.student));var b=pool.submit(()->service.inbox(f.student));a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
  }
  var item=service.inbox(f.student).items().getFirst();
  assertEquals(1,service.inbox(f.student).items().size());
  try(var pool=Executors.newFixedThreadPool(2)){
   var a=pool.submit(()->service.claim(f.student,item.id()));var b=pool.submit(()->service.claim(f.student,item.id()));
   assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
  }
  assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM notificacion_entrega WHERE mensaje_id=? AND canal='PUSH'",Integer.class,item.id()));
 }
 @Test void quietHoursAndPreferenceChangesSuppressDelivery(){
  service.save(f.student,prefs(true,false,true,0));
  var item=service.inbox(f.student).items().getFirst();
  assertFalse(at("2026-10-27T02:00:00Z").claim(f.student,item.id()));
  service.save(f.student,prefs(false,false,false,1));
  assertTrue(service.inbox(f.student).items().isEmpty());assertFalse(service.claim(f.student,item.id()));
 }
 @Test void quietCrossesMidnightAndResumesAtBoundary(){
  assertTrue(StudyNotificationService.quietAt(LocalTime.of(23,0),LocalTime.of(21,0),LocalTime.of(8,0)));
  assertTrue(StudyNotificationService.quietAt(LocalTime.of(7,59),LocalTime.of(21,0),LocalTime.of(8,0)));
  assertFalse(StudyNotificationService.quietAt(LocalTime.of(8,0),LocalTime.of(21,0),LocalTime.of(8,0)));
  service.save(f.student,prefs(true,false,true,0));
  assertTrue(at("2026-10-26T21:59:00Z").inbox(f.student).items().isEmpty());
  assertTrue(at("2026-10-27T02:00:00Z").inbox(f.student).items().isEmpty());
 }
 @Test void rejectsInvalidTimeGoalAndStaleEdits(){
  assertEquals(400,assertThrows(ResponseStatusException.class,()->service.save(f.student,new Preferences(0,"17:00",true,false,false,"21:00","08:00",0))).getStatusCode().value());
  assertEquals(400,assertThrows(ResponseStatusException.class,()->service.save(f.student,new Preferences(2,"27:00",true,false,false,"21:00","08:00",0))).getStatusCode().value());
  assertEquals(400,assertThrows(ResponseStatusException.class,()->service.save(f.student,new Preferences(2,"22:00",true,false,false,"21:00","08:00",0))).getStatusCode().value());
  service.save(f.student,prefs(true,false,false,0));
  assertEquals(409,assertThrows(ResponseStatusException.class,()->service.save(f.student,prefs(false,false,false,0))).getStatusCode().value());
 }
 @Test void readIsIdempotentAndForeignAccessDenied(){
  service.save(f.student,prefs(true,false,true,0));var id=service.inbox(f.student).items().getFirst().id();
  service.read(f.student,id);service.read(f.student,id);assertTrue(service.inbox(f.student).items().getFirst().read());assertFalse(service.claim(f.student,id));
  assertEquals(403,assertThrows(ResponseStatusException.class,()->service.read(UUID.randomUUID().toString(),id)).getStatusCode().value());
 }
 @Test void completingGoalSuppressesPendingReminder(){
  service.save(f.student,new Preferences(1,"17:00",true,false,true,"21:00","08:00",0));
  var id=service.inbox(f.student).items().getFirst().id();
  f.jdbc.update("UPDATE aprendizaje_progreso_nodo p JOIN aprendizaje_inscripcion_ruta i ON i.id=p.inscripcion_id SET p.estado='COMPLETADO',p.completado_en='2026-10-26 22:15:00' WHERE i.estudiante_id=? AND p.nodo_ruta_id=?",f.student,f.node);
  assertEquals(1,service.status(f.student).completedToday());assertTrue(service.inbox(f.student).items().isEmpty());assertFalse(service.claim(f.student,id));
 }
 @Test void nextDayProducesNewEventAndExpiredOnesDisappear(){
  service.save(f.student,prefs(true,false,false,0));var id=service.inbox(f.student).items().getFirst().id();
  var next=at("2026-10-27T22:30:00Z").inbox(f.student);assertEquals(1,next.items().size());assertNotEquals(id,next.items().getFirst().id());
 }

 @Test void evaluationPreferencesRespectMembershipAndRescheduling(){
  String room=f.jdbc.queryForObject("SELECT a.id FROM escuela_aula a JOIN aprendizaje_version_ruta v ON v.grado_id=a.grado_id WHERE v.id=? LIMIT 1",String.class,f.route);
  f.jdbc.update("INSERT INTO escuela_membresia_aula(id,aula_id,usuario_id,rol,estado) VALUES(UUID(),?,?,'ESTUDIANTE','ACTIVO')",room,f.student);
  f.jdbc.update("UPDATE aprendizaje_actividad a JOIN aprendizaje_version_actividad v ON v.actividad_id=a.id JOIN aprendizaje_nodo_ruta n ON n.version_actividad_id=v.id SET a.tipo='CUESTIONARIO' WHERE n.id=?",f.node);
  String plan=UUID.randomUUID().toString();
  f.jdbc.update("INSERT INTO escuela_planificacion_ruta(id,aula_id,version_ruta_id,tipo,referencia_id,titulo,inicia_en,termina_en,actualizado_por) VALUES(?,?,?,'EVALUACION',?,'Prueba aislada','2026-10-27 14:00:00','2026-10-27 15:00:00',?)",plan,room,f.route,f.node,f.student);
  service.save(f.student,prefs(false,true,true,0));
  service.inbox(f.student);var id=f.jdbc.queryForObject("SELECT mensaje_id FROM notificacion_evento_usuario WHERE usuario_id=? AND clave_evento LIKE ?",String.class,f.student,"%"+f.node+"%");
  service.save(f.student,prefs(false,false,false,1));assertTrue(service.inbox(f.student).items().isEmpty());
  var repo=new ReminderRepository(f.jdbc);
  assertTrue(repo.due(f.student,Instant.parse("2026-10-26T22:30:00Z"),Instant.parse("2026-10-27T22:30:00Z")).isEmpty());
  service.save(f.student,prefs(false,true,true,2));
  f.jdbc.update("UPDATE escuela_planificacion_ruta SET inicia_en='2026-10-27 15:00:00',termina_en='2026-10-27 16:00:00' WHERE id=?",plan);
  var newer=service.inbox(f.student).items();assertFalse(newer.isEmpty());assertTrue(newer.stream().noneMatch(n->n.id().equals(id)));
  f.jdbc.update("UPDATE escuela_membresia_aula SET estado='RETIRADO',retiro_en=UTC_TIMESTAMP(6) WHERE usuario_id=?",f.student);
  assertTrue(service.inbox(f.student).items().isEmpty());assertFalse(service.claim(f.student,newer.getFirst().id()));
 }
}
