package pe.rupi.api.quiz;
import java.util.*;import java.util.concurrent.*;import java.io.*;import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.visualsummary.*;import pe.rupi.api.report.ResultsReportRepository;import pe.rupi.api.studypackage.StudyPackageBuilder;
import static org.mockito.Mockito.*;import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="RUPI_HU21_TEST_DB",matches="isolated")
class SummaryIntegrationTest {
 QuizIntegrationTest f;SummaryService service;SummaryProvider ai;
 @BeforeEach void setup(){
  f=new QuizIntegrationTest();f.setup();ai=mock(SummaryProvider.class);
  when(ai.configured()).thenReturn(true);when(ai.provider()).thenReturn("TEST_ONLY");when(ai.model()).thenReturn("fixture");
  when(ai.generate(any())).thenAnswer(call->draft(call.getArgument(0)));
  service=new SummaryService(new NamedParameterJdbcTemplate(f.jdbc),f.repo,ai,f.access,new DataSourceTransactionManager(f.jdbc.getDataSource()));
 }
 static SummaryProvider.Draft draft(QuizModels.LearningContext c){
  String evidence=c.content().substring(0,Math.min(65,c.content().length()));
  return new SummaryProvider.Draft("Sumamos cantidades",List.of(new SummaryProvider.Section("Sumar","Juntamos cuatro y tres.",evidence),new SummaryProvider.Section("Resultado","En total hay siete.",evidence)),List.of("Cuenta cuatro.","Agrega tres."),"Cuatro más tres es siete.","Sumar reúne cantidades.");
 }
 SummaryService.View generate(){return service.generate(f.student,f.route,f.node,UUID.randomUUID());}
 @Test void persistAndReuseWithoutChangingGrades(){
  var a=generate();var b=generate();assertEquals(a.id(),b.id());assertNotNull(service.get(f.student,f.route,f.node).resource().content());
  verify(ai,times(1)).generate(any());assertEquals(0,f.jdbc.queryForObject("SELECT COUNT(*) FROM evaluacion_intento WHERE estudiante_id=?",Integer.class,f.student));
 }
 @Test void concurrentCallsAndRetryCreateOnlyOneVersion()throws Exception{
  doAnswer(call->{Thread.sleep(250);return draft(call.getArgument(0));}).when(ai).generate(any());
  try(var pool=Executors.newFixedThreadPool(2)){
   var gate=new CountDownLatch(1);
   var a=pool.submit(()->{gate.await();return generate();});var b=pool.submit(()->{gate.await();return generate();});gate.countDown();
   assertEquals(a.get(20,TimeUnit.SECONDS).id(),b.get(20,TimeUnit.SECONDS).id());verify(ai,times(1)).generate(any());
  }
 }
 @Test void unavailableProviderAndInvalidOutputDoNotInventContent(){
  when(ai.configured()).thenReturn(false);assertEquals(503,assertThrows(ResponseStatusException.class,this::generate).getStatusCode().value());
  assertNull(service.get(f.student,f.route,f.node).resource());
  when(ai.configured()).thenReturn(true);doReturn(new SummaryProvider.Draft(null,List.of(),List.of(),"","")).when(ai).generate(any());
  var view=generate();assertEquals("FALLIDO",view.state());assertNull(view.content());
 }
 @Test void foreignStudentAndUnavailableNodeAreRejected(){
  var view=generate();assertThrows(ResponseStatusException.class,()->service.check(UUID.randomUUID().toString(),f.route,f.node,view.id()));
  f.jdbc.update("UPDATE aprendizaje_nodo_ruta SET disponible_desde=UTC_TIMESTAMP(6)+INTERVAL 1 DAY WHERE id=?",f.node);
  assertThrows(ResponseStatusException.class,()->service.downloadable(f.student,f.route,f.node,view.id()));
 }
 @Test void reportWithdrawsDownloadAndTeacherCanReview(){
  var v=generate();service.report(f.student,f.route,f.node,v.id(),"El ejemplo necesita revisión");
  assertEquals("REPORTADO",service.get(f.student,f.route,f.node).resource().state());assertNull(service.get(f.student,f.route,f.node).resource().content());
  assertThrows(ResponseStatusException.class,()->service.downloadable(f.student,f.route,f.node,v.id()));
  assertThrows(ResponseStatusException.class,()->service.review("other","room",f.route,v.id(),true,"Revisado"));
  when(f.access.routes("teacher","room")).thenReturn(List.of(new ResultsReportRepository.Route(f.route,"Ruta",1,"PUBLICADO")));
  assertNotNull(service.reviews("teacher","room",f.route).getFirst().content());
  service.review("teacher","room",f.route,v.id(),true,"Operaciones y lenguaje revisados");assertEquals("REVISADO_DOCENTE",service.downloadable(f.student,f.route,f.node,v.id()).state());
 }
 @Test void changedLessonInvalidatesPreviousDownloadedVersion(){
  var v=generate();f.jdbc.update("UPDATE aprendizaje_version_actividad SET instrucciones=CONCAT(instrucciones,' Otro ejemplo para repasar.') WHERE id=(SELECT version_actividad_id FROM aprendizaje_nodo_ruta WHERE id=?)",f.node);
  assertEquals("DESACTUALIZADO",service.check(f.student,f.route,f.node,v.id()).state());assertNull(service.get(f.student,f.route,f.node).resource());
  assertNotEquals(v.id(),generate().id());
 }
 @Test void zipHasSelfContainedHtmlAndChecksumManifest()throws Exception{
  var v=generate();String html=SummaryDocument.html(v);
  assertFalse(html.contains("<script"));assertFalse(html.contains("src="));assertTrue(html.contains("Copia descargada"));
  byte[] zip=StudyPackageBuilder.visualSupplement(html,Map.of("resource",v));
  java.nio.file.Files.write(java.nio.file.Path.of("target","hu23-test-package.zip"),zip);
  java.nio.file.Files.writeString(java.nio.file.Path.of("target","hu23-offline-fixture.html"),html);
  var names=new HashSet<String>();
  try(var input=new ZipInputStream(new ByteArrayInputStream(zip))){ZipEntry e;while((e=input.getNextEntry())!=null){names.add(e.getName());assertTrue(input.readAllBytes().length>0);}}
  assertEquals(Set.of("ABRIR.html","contenido.json","manifest.json","LEEME.txt"),names);
 }
 @Test void durableRateLimitCountsFailedGenerations(){
  doThrow(new IllegalStateException("simulated outage")).when(ai).generate(any());
  for(int i=0;i<5;i++){generate();f.jdbc.update("UPDATE aprendizaje_resumen_visual SET actualizado_en=UTC_TIMESTAMP(6)-INTERVAL 3 MINUTE WHERE nodo_id=?",f.node);}
  assertEquals(429,assertThrows(ResponseStatusException.class,this::generate).getStatusCode().value());
 }
}
