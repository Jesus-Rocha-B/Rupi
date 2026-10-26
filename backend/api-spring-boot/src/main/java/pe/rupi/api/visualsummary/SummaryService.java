package pe.rupi.api.visualsummary;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.quiz.*;
import pe.rupi.api.report.ResultsReportService;
@Service
public class SummaryService {
 public record View(String id,String state,String version,String topic,int grade,String area,String source,String provider,String model,SummaryProvider.Draft content){}
 public record Availability(boolean configured,View resource){}
 private record Reservation(String id,boolean generate){}
 private final NamedParameterJdbcTemplate db;private final QuizRepository access;private final SummaryProvider ai;private final ResultsReportService teachers;private final TransactionTemplate tx;
 public SummaryService(NamedParameterJdbcTemplate db,QuizRepository access,SummaryProvider ai,ResultsReportService teachers,PlatformTransactionManager manager){
  this.db=db;this.access=access;this.ai=ai;this.teachers=teachers;tx=new TransactionTemplate(manager);
 }
 static ResponseStatusException fail(HttpStatus status,String message){return new ResponseStatusException(status,message);}
 static String json(Object o){return SummaryProvider.JSON.writeValueAsString(o);}
 static String hash(QuizModels.Context c){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json(QuizRepository.learning(c)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private Map<String,Object> parameters(QuizModels.Context c){return new HashMap<>(Map.of("node",c.node(),"route",c.route(),"activity",c.activity(),"hash",hash(c),"context",json(QuizRepository.learning(c))));}
 private static java.time.Instant instant(Object value){return value instanceof java.sql.Timestamp t?t.toInstant():((java.time.LocalDateTime)value).toInstant(java.time.ZoneOffset.UTC);}
 private View view(Map<String,Object> row){
  var context=SummaryProvider.JSON.readValue(row.get("contexto_json").toString(),QuizModels.LearningContext.class);
  String state=row.get("estado").toString();
  if(state.equals("GENERANDO")&&instant(row.get("actualizado_en")).isBefore(java.time.Instant.now().minusSeconds(120)))state="FALLIDO";
  var content=row.get("contenido_json");
  return new View(row.get("id").toString(),state,row.get("contexto_hash").toString(),context.topic(),context.grade(),context.area(),
   "Contenido de la actividad · resumen asistido por IA",row.get("proveedor").toString(),row.get("modelo").toString(),
   Set.of("VALIDADO_AUTOMATICO","REVISADO_DOCENTE").contains(state)&&content!=null?SummaryProvider.JSON.readValue(content.toString(),SummaryProvider.Draft.class):null);
 }
 private View byId(String id){return view(db.queryForMap("SELECT * FROM aprendizaje_resumen_visual WHERE id=:id",Map.of("id",id)));}
 public Availability get(String student,String route,String node){
  var c=access.context(student,route,node);
  var rows=db.queryForList("SELECT * FROM aprendizaje_resumen_visual WHERE nodo_id=:node AND contexto_hash=:hash",parameters(c));
  return new Availability(ai.configured(),rows.isEmpty()?null:view(rows.getFirst()));
 }
 public View check(String student,String route,String node,String id){
  var c=access.context(student,route,node);
  var rows=db.queryForList("SELECT * FROM aprendizaje_resumen_visual WHERE id=:id AND nodo_id=:node AND ruta_id=:route",Map.of("id",id,"node",node,"route",route));
  if(rows.isEmpty())throw fail(HttpStatus.NOT_FOUND,"Resumen no disponible.");
  var old=view(rows.getFirst());
  if(!old.version().equals(hash(c)))return new View(old.id(),"DESACTUALIZADO",old.version(),old.topic(),old.grade(),old.area(),old.source(),old.provider(),old.model(),null);
  return old;
 }
 private Reservation reserve(String student,QuizModels.Context c,UUID request){
  var p=parameters(c);p.put("student",student);p.put("request",request.toString());
  db.update("INSERT INTO evaluacion_limite_ia(estudiante_id) VALUES(:student) ON DUPLICATE KEY UPDATE estudiante_id=estudiante_id",p);
  db.queryForObject("SELECT estudiante_id FROM evaluacion_limite_ia WHERE estudiante_id=:student FOR UPDATE",p,String.class);
  var prior=db.queryForList("SELECT resumen_id FROM aprendizaje_resumen_solicitud WHERE estudiante_id=:student AND solicitud_id=:request",p,String.class);
  if(!prior.isEmpty()){check(student,c.route(),c.node(),prior.getFirst());return new Reservation(prior.getFirst(),false);}
  if(db.queryForObject("SELECT COUNT(*) FROM aprendizaje_resumen_solicitud WHERE estudiante_id=:student AND creado_en>UTC_TIMESTAMP(6)-INTERVAL 1 HOUR",p,Integer.class)>=5)throw fail(HttpStatus.TOO_MANY_REQUESTS,"Ya preparaste varios resúmenes. Continúa con tu lección y vuelve más tarde.");
  p.put("id",UUID.randomUUID().toString());p.put("provider",ai.provider());p.put("model",ai.model());p.put("prompt",SummaryProvider.VERSION);
  int inserted=db.update("""
   INSERT IGNORE INTO aprendizaje_resumen_visual(id,nodo_id,ruta_id,version_actividad_id,contexto_hash,contexto_json,proveedor,modelo,version_prompt,estado)
   VALUES(:id,:node,:route,:activity,:hash,:context,:provider,:model,:prompt,'GENERANDO')
   """,p);
  var row=db.queryForMap("SELECT * FROM aprendizaje_resumen_visual WHERE nodo_id=:node AND contexto_hash=:hash FOR UPDATE",p);
  String id=row.get("id").toString();p.put("id",id);
  boolean generate=inserted==1;
  if(!generate&&"FALLIDO".equals(view(row).state())&&instant(row.get("actualizado_en")).isBefore(java.time.Instant.now().minusSeconds(60))){
   db.update("UPDATE aprendizaje_resumen_visual SET estado='GENERANDO',actualizado_en=UTC_TIMESTAMP(6) WHERE id=:id",p);generate=true;
  }
  db.update("INSERT INTO aprendizaje_resumen_solicitud(estudiante_id,solicitud_id,resumen_id) VALUES(:student,:request,:id)",p);
  return new Reservation(id,generate);
 }
 public View generate(String student,String route,String node,UUID request){
  if(request==null)throw fail(HttpStatus.BAD_REQUEST,"Falta identificar la solicitud.");
  var c=access.context(student,route,node);var current=get(student,route,node).resource();
  if(current!=null&&!"FALLIDO".equals(current.state()))return current;
  if(!ai.configured())throw fail(HttpStatus.SERVICE_UNAVAILABLE,"La IA todavía no está configurada. Puedes continuar tu lección.");
  var reserved=tx.execute(s->reserve(student,c,request));if(!reserved.generate())return byId(reserved.id());
  try{
   var draft=ai.generate(QuizRepository.learning(c));
   SummaryProvider.validate(draft,QuizRepository.learning(c));
   if(!hash(c).equals(hash(access.context(student,route,node))))throw new IllegalArgumentException("El contenido cambió");
   db.update("UPDATE aprendizaje_resumen_visual SET contenido_json=:content,estado='VALIDADO_AUTOMATICO',actualizado_en=UTC_TIMESTAMP(6) WHERE id=:id AND estado='GENERANDO'",
    Map.of("id",reserved.id(),"content",json(draft)));
  }catch(Exception e){db.update("UPDATE aprendizaje_resumen_visual SET estado='FALLIDO',actualizado_en=UTC_TIMESTAMP(6) WHERE id=:id AND estado='GENERANDO'",Map.of("id",reserved.id()));}
  return byId(reserved.id());
 }
 public void report(String student,String route,String node,String id,String reason){
  if(reason==null||reason.isBlank()||reason.length()>500)throw fail(HttpStatus.BAD_REQUEST,"Explica qué debemos revisar (máximo 500 caracteres).");
  tx.executeWithoutResult(s->{
   check(student,route,node,id);
   var p=Map.of("id",id,"student",student,"reason",reason.strip());
   db.queryForObject("SELECT id FROM aprendizaje_resumen_visual WHERE id=:id FOR UPDATE",p,String.class);
   db.update("INSERT INTO aprendizaje_resumen_reporte(resumen_id,estudiante_id,motivo) VALUES(:id,:student,:reason) ON DUPLICATE KEY UPDATE motivo=:reason",p);
   db.update("UPDATE aprendizaje_resumen_visual SET estado='REPORTADO',actualizado_en=UTC_TIMESTAMP(6) WHERE id=:id",p);
  });
 }
 private void teacher(String user,String room,String route){if(teachers.routes(user,room).stream().noneMatch(r->r.id().equals(route)))throw fail(HttpStatus.NOT_FOUND,"Ruta fuera de tu aula.");}
 public List<View> reviews(String user,String room,String route){
  teacher(user,room,route);
  return db.queryForList("SELECT * FROM aprendizaje_resumen_visual WHERE ruta_id=:route AND contenido_json IS NOT NULL ORDER BY actualizado_en DESC LIMIT 100",Map.of("route",route)).stream().map(row->{
   var v=view(row);return new View(v.id(),v.state(),v.version(),v.topic(),v.grade(),v.area(),v.source(),v.provider(),v.model(),SummaryProvider.JSON.readValue(row.get("contenido_json").toString(),SummaryProvider.Draft.class));
  }).toList();
 }
 public void review(String user,String room,String route,String id,boolean approve,String note){
  teacher(user,room,route);
  if(note==null||note.isBlank()||note.length()>1000)throw fail(HttpStatus.BAD_REQUEST,"Describe tu revisión.");
  int n=db.update("""
   UPDATE aprendizaje_resumen_visual SET estado=:state,revisado_por=:user,nota_revision=:note,actualizado_en=UTC_TIMESTAMP(6)
   WHERE id=:id AND ruta_id=:route AND contenido_json IS NOT NULL
   """,Map.of("id",id,"route",route,"state",approve?"REVISADO_DOCENTE":"RECHAZADO","user",user,"note",note.strip()));
  if(n==0)throw fail(HttpStatus.NOT_FOUND,"Resumen fuera de la ruta.");
 }
 public View downloadable(String student,String route,String node,String id){
  var v=check(student,route,node,id);if(v.content()==null)throw fail(HttpStatus.CONFLICT,"Este resumen no está disponible para descargar.");return v;
 }
}
