package pe.rupi.api.quiz;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.report.ResultsReportService;
import pe.rupi.api.evaluation.EvaluationService;
import static pe.rupi.api.quiz.QuizModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="RUPI_HU21_TEST_DB",matches="isolated")
class QuizIntegrationTest {
 JdbcTemplate jdbc;QuizRepository repo;QuizService service;TransactionTemplate tx;
 FakeProvider provider;String student,route,node;ResultsReportService access;
 static class FakeProvider implements QuizProvider {
  boolean enabled=true,fail=false;AtomicInteger calls=new AtomicInteger();long delay=0;
  public boolean configured(){return enabled;}public String provider(){return "TEST_ONLY";}public String model(){return "fixture";}
  public Draft generate(LearningContext c){
   calls.incrementAndGet();
   try {Thread.sleep(delay);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
   if(fail)throw new IllegalStateException("simulated failure");
   String evidence=c.content().substring(0,Math.min(70,c.content().length()));
   return new Draft(java.util.stream.IntStream.range(0,3).mapToObj(i->new DraftQuestion("Pregunta simulada de prueba numero "+i+"?",List.of("Siete","Ocho","Nueve"),0,"Explicacion simulada solo para pruebas.",evidence)).toList());
  }
 }
 @BeforeEach void setup(){
  String url=System.getenv("RUPI_HU21_TEST_URL");assertNotNull(url);
  assertTrue(url.startsWith("jdbc:mysql://127.0.0.1:3307/rupi_hu21_test?"),"Refusing shared database");
  var source=new DriverManagerDataSource(url,System.getenv("RUPI_HU21_TEST_USER"),System.getenv("RUPI_HU21_TEST_PASSWORD"));
  jdbc=new JdbcTemplate(source);repo=new QuizRepository(new NamedParameterJdbcTemplate(source));
  var manager=new DataSourceTransactionManager(source);tx=new TransactionTemplate(manager);
  provider=new FakeProvider();access=mock(ResultsReportService.class);service=new QuizService(repo,provider,access,manager);
  student=UUID.randomUUID().toString();
  jdbc.update("INSERT INTO identidad_cuenta_usuario(id,nombre_usuario,hash_clave,estado) VALUES(?,?,?,'ACTIVO')",student,"hu21-"+student,"not-a-login");
  var item=jdbc.queryForMap("""
   SELECT v.id route,n.id node FROM aprendizaje_version_ruta v
   JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id=v.id
   JOIN aprendizaje_version_actividad va ON va.id=n.version_actividad_id AND va.estado='PUBLICADO'
   WHERE v.estado='PUBLICADO' ORDER BY n.numero_secuencia LIMIT 1
   """);
  route=(String)item.get("route");node=(String)item.get("node");
  String enrollment=UUID.randomUUID().toString();
  jdbc.update("INSERT INTO aprendizaje_inscripcion_ruta(id,version_ruta_id,estudiante_id) VALUES(?,?,?)",enrollment,route,student);
  jdbc.update("INSERT INTO aprendizaje_progreso_nodo(inscripcion_id,version_ruta_id,nodo_ruta_id,estado,desbloqueado_en) VALUES(?,?,?,'DISPONIBLE',UTC_TIMESTAMP(6))",enrollment,route,node);
  // Isolate each fixture's reviewed fallback by a distinct content snapshot on a new node/activity.
  var sourceNode=jdbc.queryForMap("SELECT version_actividad_id FROM aprendizaje_nodo_ruta WHERE id=?",node);
  String activity=UUID.randomUUID().toString();
  jdbc.update("INSERT INTO aprendizaje_actividad(id,tipo) VALUES(?,'LECCION')",activity);
  jdbc.update("INSERT INTO aprendizaje_version_actividad(id,actividad_id,numero_version,titulo,instrucciones,estado) VALUES(?,?,1,?,?,'PUBLICADO')",
    activity,activity,"Tema de prueba "+student,"Hay cuatro puestos junto a la fuente y tres junto a los arcos. En total hay siete puestos.");
  // Separate route node sequence, avoiding mutations to seed nodes.
  String newNode=UUID.randomUUID().toString();
  int seq=jdbc.queryForObject("SELECT COALESCE(MAX(numero_secuencia),0)+1 FROM aprendizaje_nodo_ruta WHERE version_ruta_id=?",Integer.class,route);
  jdbc.update("INSERT INTO aprendizaje_nodo_ruta(id,version_ruta_id,version_actividad_id,numero_secuencia) VALUES(?,?,?,?)",newNode,route,activity,seq);
  jdbc.update("INSERT INTO aprendizaje_progreso_nodo(inscripcion_id,version_ruta_id,nodo_ruta_id,estado,desbloqueado_en) VALUES(?,?,?,'DISPONIBLE',UTC_TIMESTAMP(6))",enrollment,route,newNode);
  node=newNode;assertNotNull(sourceNode);
 }
 View generate(){return service.generate(student,route,node,UUID.randomUUID());}
 View finish(View v){
  for(var q:v.questions())v=service.answer(student,v.practiceId(),UUID.fromString(q.id()),UUID.fromString(q.options().getFirst().id()));
  return v;
 }
 @Test void generatedPracticePersistsAndHidesSolutionsUntilAnswered(){
  var v=generate();assertEquals("LISTO",v.status());assertEquals(3,v.questions().size());
  assertTrue(v.questions().stream().allMatch(q->q.explanation()==null&&q.correctOption()==null&&q.correct()==null));
  var q=v.questions().getFirst();
  var first=service.answer(student,v.practiceId(),UUID.fromString(q.id()),UUID.fromString(q.options().get(1).id()));
  assertEquals(1,first.answered());assertFalse(first.questions().getFirst().correct());assertNotNull(first.questions().getFirst().explanation());
  var retry=service.answer(student,v.practiceId(),UUID.fromString(q.id()),UUID.fromString(q.options().getFirst().id()));
  assertEquals(q.options().get(1).id(),retry.questions().getFirst().selectedOption());
  assertEquals("COMPLETADO",finish(retry).status());
  assertEquals(3,service.get(student,v.practiceId()).answered());
 }
 @Test void concurrentGenerationReusesOnePracticeAndAttempt()throws Exception{
  provider.delay=250;
  try(var pool=Executors.newFixedThreadPool(2)){
   var gate=new CountDownLatch(1);
   var a=pool.submit(()->{gate.await();return generate();});
   var b=pool.submit(()->{gate.await();return generate();});
   gate.countDown();var x=a.get(20,TimeUnit.SECONDS);var y=b.get(20,TimeUnit.SECONDS);
   assertEquals(x.practiceId(),y.practiceId());assertEquals(1,provider.calls.get());
  }
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM evaluacion_intento WHERE estudiante_id=?",Integer.class,student));
 }
 @Test void requestRetryAfterCompletionDoesNotCreateNewAttempt(){
  UUID request=UUID.randomUUID();var v=service.generate(student,route,node,request);finish(v);
  var again=service.generate(student,route,node,request);
  assertEquals(v.practiceId(),again.practiceId());assertEquals(1,provider.calls.get());
 }
 @Test void unavailableProviderAndInvalidOutputDoNotAffectLesson(){
  provider.enabled=false;
  assertEquals(503,assertThrows(ResponseStatusException.class,this::generate).getStatusCode().value());
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM evaluacion_practica_ia WHERE estudiante_id=?",Integer.class,student));
  provider.enabled=true;provider.fail=true;
  assertEquals("FALLIDO",generate().status());
  assertNotNull(repo.context(student,route,node));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM evaluacion_intento WHERE estudiante_id=?",Integer.class,student));
 }
 @Test void durableQuotaLimitsFailuresAndStopsRepeatedGeneration(){
  provider.fail=true;
  for(int i=0;i<5;i++)assertEquals("FALLIDO",generate().status());
  assertEquals(429,assertThrows(ResponseStatusException.class,this::generate).getStatusCode().value());assertEquals(5,provider.calls.get());
 }
 @Test void differentStudentAndBlockedNodeCannotAccessPractice(){
  var v=generate();
  assertEquals(404,assertThrows(ResponseStatusException.class,()->service.get(UUID.randomUUID().toString(),v.practiceId())).getStatusCode().value());
  jdbc.update("UPDATE aprendizaje_nodo_ruta SET disponible_desde=UTC_TIMESTAMP(6)+INTERVAL 1 DAY WHERE id=?",node);
  assertEquals(404,assertThrows(ResponseStatusException.class,()->service.get(student,v.practiceId())).getStatusCode().value());
 }
 @Test void reportQuarantinesAllFurtherUseAndPreservesAnswers(){
  var v=generate();var q=v.questions().getFirst();
  service.answer(student,v.practiceId(),UUID.fromString(q.id()),UUID.fromString(q.options().getFirst().id()));
  service.report(student,v.practiceId(),UUID.fromString(q.id()),"Esta pregunta parece ambigua");
  assertEquals("RETIRADO",service.get(student,v.practiceId()).status());assertTrue(service.get(student,v.practiceId()).questions().isEmpty());
  assertEquals(409,assertThrows(ResponseStatusException.class,()->service.answer(student,v.practiceId(),UUID.fromString(q.id()),UUID.fromString(q.options().getFirst().id()))).getStatusCode().value());
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM evaluacion_respuesta r JOIN evaluacion_intento i ON i.id=r.intento_id WHERE i.estudiante_id=?",Integer.class,student));
 }
 @Test void onlyTeacherReviewedSameContextIsReusedOnOutage(){
  var v=finish(generate());var p=repo.own(student,v.practiceId(),false);
  tx.executeWithoutResult(s->repo.review(p.quiz(),student,true,"Contenido verificado en prueba aislada"));
  provider.enabled=false;
  var reused=generate();assertEquals("REUTILIZADO",reused.source());assertEquals(0,reused.answered());
  assertNotEquals(v.practiceId(),reused.practiceId());assertEquals(p.quiz(),repo.own(student,reused.practiceId(),false).quiz());
 }
 @Test void generatedVersionsCannotLeakThroughLegacyApiOrEnterRoadmapGrades(){
  var v=generate();var p=repo.own(student,v.practiceId(),false);var quiz=repo.quiz(p.quiz(),false);
  var evaluation=new EvaluationService(new NamedParameterJdbcTemplate(jdbc));
  assertEquals(404,assertThrows(ResponseStatusException.class,()->evaluation.findQuestionsForActivity(UUID.fromString(quiz.activity()))).getStatusCode().value());
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM aprendizaje_nodo_ruta WHERE version_actividad_id=?",Integer.class,quiz.activity()));
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM evaluacion_respuesta WHERE intento_id=?",Integer.class,p.attempt()));
 }
 @Test void foreignOptionsAreRejectedAndQuizVersionIsImmutable(){
  var v=generate();var q=v.questions().getFirst();
  assertEquals(400,assertThrows(ResponseStatusException.class,()->service.answer(student,v.practiceId(),UUID.fromString(q.id()),UUID.randomUUID())).getStatusCode().value());
  assertEquals(v.questions(),service.get(student,v.practiceId()).questions());
 }
 @Test void staleGenerationCanBeRecovered(){
  var c=repo.context(student,route,node);
  var reservation=tx.execute(s->repo.reserve(student,c,UUID.randomUUID().toString()));
  jdbc.update("UPDATE evaluacion_practica_ia SET creado_en=UTC_TIMESTAMP(6)-INTERVAL 3 MINUTE WHERE id=?",reservation.practice().id());
  assertEquals("FALLIDO",service.get(student,reservation.practice().id()).status());
  assertEquals("LISTO",generate().status());
 }
}
