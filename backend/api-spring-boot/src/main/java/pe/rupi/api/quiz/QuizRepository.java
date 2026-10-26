package pe.rupi.api.quiz;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static pe.rupi.api.quiz.QuizModels.*;

@Repository
public class QuizRepository {
 private final NamedParameterJdbcTemplate jdbc;
 public QuizRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}
 static MapSqlParameterSource p(String student,String route,String node) {
  return new MapSqlParameterSource("student",student).addValue("route",route).addValue("node",node);
 }
 static ResponseStatusException error(HttpStatus code,String text){return new ResponseStatusException(code,text);}
 private static final String ACCESS="""
  FROM aprendizaje_inscripcion_ruta i
  JOIN identidad_cuenta_usuario account ON account.id=i.estudiante_id AND account.estado='ACTIVO' AND account.desactivado_en IS NULL
  JOIN aprendizaje_version_ruta v ON v.id=i.version_ruta_id AND v.estado='PUBLICADO'
  JOIN aprendizaje_ruta r ON r.id=v.ruta_id AND r.archivado_en IS NULL
  JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id=v.id
  JOIN aprendizaje_version_actividad va ON va.id=n.version_actividad_id AND va.estado='PUBLICADO'
  JOIN aprendizaje_actividad a ON a.id=va.actividad_id AND a.archivado_en IS NULL
  JOIN curriculo_grado g ON g.id=v.grado_id JOIN curriculo_area area ON area.id=v.area_id
  JOIN aprendizaje_progreso_nodo progress ON progress.inscripcion_id=i.id AND progress.nodo_ruta_id=n.id
  WHERE i.estudiante_id=:student AND i.estado IN ('ACTIVO','COMPLETADO')
    AND progress.estado IN ('DISPONIBLE','EN_CURSO','COMPLETADO')
    AND (n.disponible_desde IS NULL OR n.disponible_desde<=UTC_TIMESTAMP(6))
    AND (n.bloqueado_desde IS NULL OR n.bloqueado_desde>UTC_TIMESTAMP(6))
  """;
 public record Topic(String routeId,String nodeId,String routeTitle,String title,int grade) {}
 public List<Topic> topics(String student) {
  return jdbc.query("SELECT v.id route_id,n.id node_id,r.titulo route_title,va.titulo,g.numero_grado "+ACCESS+
   " ORDER BY i.inscrito_en DESC,n.numero_secuencia LIMIT 100",Map.of("student",student),
   (r,n)->new Topic(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getInt(5)));
 }
 public Context context(String student,String route,String node) {
  var result=jdbc.query("SELECT i.id,va.id activity,n.id node_id,v.id route_id,g.numero_grado,area.codigo,va.titulo,va.instrucciones "+
   ACCESS+" AND v.id=:route AND n.id=:node",p(student,route,node),
   (r,n)->new Context(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getInt(5),r.getString(6),r.getString(7),r.getString(8)))
   .stream().findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"Esta actividad no está disponible."));
  var blocks=jdbc.query("""
   SELECT texto_cuerpo FROM aprendizaje_bloque_contenido WHERE version_actividad_id=:activity
   AND tipo IN ('TEXTO','DESTACADO') ORDER BY numero_secuencia LIMIT 20
   """,Map.of("activity",result.activity()),(r,n)->r.getString(1));
  return new Context(result.enrollment(),result.activity(),node,route,result.grade(),result.area(),
   QuizValidation.clean(result.topic(),180),QuizValidation.clean(Objects.toString(result.content(),"")+"\n"+
    String.join("\n",blocks.stream().filter(Objects::nonNull).toList()),6000));
 }
 public static LearningContext learning(Context c){return new LearningContext(c.grade(),c.area(),c.topic(),c.content());}
 static String json(Object obj){try{return QuizValidation.JSON.writeValueAsString(obj);}catch(Exception e){throw new IllegalStateException(e);}}
 static String hash(Context c){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json(learning(c)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private Practice map(java.sql.ResultSet r) throws java.sql.SQLException {
  return new Practice(r.getString("id"),r.getString("estudiante_id"),r.getString("nodo_id"),r.getString("ruta_id"),
   r.getString("cuestionario_id"),r.getString("intento_id"),("GENERANDO".equals(r.getString("estado")) && r.getTimestamp("creado_en").toInstant().isBefore(java.time.Instant.now().minusSeconds(120)))?"FALLIDO":r.getString("estado"),r.getString("origen"));
 }
 public Optional<Practice> current(String student,String route,String node) {
  return jdbc.query("SELECT * FROM evaluacion_practica_ia WHERE estudiante_id=:student AND ruta_id=:route AND nodo_id=:node ORDER BY creado_en DESC,id DESC LIMIT 1",
   p(student,route,node),(r,n)->map(r)).stream().findFirst();
 }
 public Practice own(String student,String id,boolean lock) {
  return jdbc.query("SELECT * FROM evaluacion_practica_ia WHERE id=:id AND estudiante_id=:student"+(lock?" FOR UPDATE":""),
   Map.of("id",id,"student",student),(r,n)->map(r)).stream().findFirst()
   .orElseThrow(()->error(HttpStatus.NOT_FOUND,"Práctica no disponible."));
 }
 public Quiz quiz(String id,boolean lock) {
  return jdbc.query("SELECT id,version_actividad_id,estado,proveedor,modelo,version_prompt FROM evaluacion_cuestionario_ia WHERE id=:id"+(lock?" FOR UPDATE":""),
   Map.of("id",id),(r,n)->new Quiz(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6)))
   .stream().findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"Cuestionario no disponible."));
 }
 public Optional<String> reviewed(Context context) {
  return jdbc.query("""
   SELECT id FROM evaluacion_cuestionario_ia WHERE nodo_id=:node AND contexto_hash=:hash
    AND estado='REVISADO_DOCENTE' ORDER BY revisado_en DESC,id LIMIT 1
   """,Map.of("node",context.node(),"hash",hash(context)),(r,n)->r.getString(1)).stream().findFirst();
 }
 public record Reservation(Practice practice,boolean created) {}
 /** Must run inside a transaction. Lock a quota row, never a shared identity table. */
 public Reservation reserve(String student,Context c,String request) {
  var params=p(student,c.route(),c.node()).addValue("request",request);
  jdbc.update("INSERT INTO evaluacion_limite_ia(estudiante_id) VALUES(:student) ON DUPLICATE KEY UPDATE estudiante_id=estudiante_id",params);
  jdbc.queryForObject("SELECT estudiante_id FROM evaluacion_limite_ia WHERE estudiante_id=:student FOR UPDATE",params,String.class);
  var previous=jdbc.query("SELECT * FROM evaluacion_practica_ia WHERE estudiante_id=:student AND solicitud_id=:request",params,(r,n)->map(r));
  if(!previous.isEmpty()) {
   if(!previous.getFirst().node().equals(c.node())||!previous.getFirst().route().equals(c.route())) throw error(HttpStatus.CONFLICT,"La solicitud ya pertenece a otra actividad.");
   return new Reservation(previous.getFirst(),false);
  }
  jdbc.update("""
   UPDATE evaluacion_practica_ia SET estado='FALLIDO' WHERE estudiante_id=:student AND estado='GENERANDO'
    AND creado_en<UTC_TIMESTAMP(6)-INTERVAL 2 MINUTE
   """,params);
  // A reported quiz must not keep a permanent active slot.
  jdbc.update("""
   UPDATE evaluacion_practica_ia p JOIN evaluacion_cuestionario_ia q ON q.id=p.cuestionario_id
    SET p.estado='RETIRADO' WHERE p.estudiante_id=:student AND p.estado='LISTO' AND q.estado IN ('REPORTADO','RECHAZADO')
   """,params);
  var active=jdbc.query("SELECT * FROM evaluacion_practica_ia WHERE estudiante_id=:student AND nodo_id=:node AND estado IN ('GENERANDO','LISTO')",params,(r,n)->map(r));
  if(!active.isEmpty())return new Reservation(active.getFirst(),false);
  int count=jdbc.queryForObject("SELECT COUNT(*) FROM evaluacion_practica_ia WHERE estudiante_id=:student AND creado_en>UTC_TIMESTAMP(6)-INTERVAL 1 HOUR",params,Integer.class);
  if(count>=5)throw error(HttpStatus.TOO_MANY_REQUESTS,"Ya realizaste varias prácticas. Continúa con tu lección y vuelve más tarde.");
  String id=UUID.randomUUID().toString();params.addValue("id",id);
  jdbc.update("""
   INSERT INTO evaluacion_practica_ia(id,estudiante_id,solicitud_id,nodo_id,ruta_id,estado)
   VALUES(:id,:student,:request,:node,:route,'GENERANDO')
   """,params);
  return new Reservation(own(student,id,false),true);
 }
 public void failed(Practice p) {
  jdbc.update("UPDATE evaluacion_practica_ia SET estado='FALLIDO' WHERE id=:id AND estado='GENERANDO'",Map.of("id",p.id()));
 }
 public void persist(Practice practice,Context c,Draft draft,String provider,String model,String fallback) {
  var current=own(practice.student(),practice.id(),true);
  if(!current.state().equals("GENERANDO"))return;
  String quizId,activity;
  if(fallback!=null) {
   var q=quiz(fallback,true);
   if(!q.state().equals("REVISADO_DOCENTE"))throw error(HttpStatus.CONFLICT,"El cuestionario está en revisión.");
   quizId=q.id();activity=q.activity();
  } else {
   quizId=UUID.randomUUID().toString();activity=UUID.randomUUID().toString();
   var args=new MapSqlParameterSource("id",activity).addValue("title",c.topic());
   jdbc.update("INSERT INTO aprendizaje_actividad(id,tipo) VALUES(:id,'CUESTIONARIO')",args);
   jdbc.update("""
    INSERT INTO aprendizaje_version_actividad(id,actividad_id,numero_version,titulo,instrucciones,estado)
    VALUES(:id,:id,1,:title,'Práctica formativa IA separada de la ruta','BORRADOR')
    """,args);
   jdbc.update("""
    INSERT INTO evaluacion_cuestionario_ia(id,version_actividad_id,nodo_id,ruta_id,contexto_json,contexto_hash,proveedor,modelo,version_prompt)
    VALUES(:quiz,:id,:node,:route,:context,:hash,:provider,:model,:prompt)
    """,args.addValue("quiz",quizId).addValue("node",c.node()).addValue("route",c.route()).addValue("context",json(learning(c)))
     .addValue("hash",hash(c)).addValue("provider",provider).addValue("model",model).addValue("prompt",ChatQuizProvider.PROMPT_VERSION));
   int seq=0;
   for(var question:draft.questions()) {
    String qid=UUID.randomUUID().toString();
    jdbc.update("""
     INSERT INTO evaluacion_pregunta(id,version_actividad_id,numero_secuencia,tipo,enunciado,explicacion,puntos)
     VALUES(:id,:activity,:seq,'OPCION_UNICA',:text,:explanation,1)
     """,new MapSqlParameterSource("id",qid).addValue("activity",activity).addValue("seq",++seq).addValue("text",question.text()).addValue("explanation",question.explanation()));
    for(int i=0;i<question.options().size();i++)jdbc.update("""
     INSERT INTO evaluacion_opcion_respuesta(id,pregunta_id,numero_secuencia,texto,es_correcta)
     VALUES(:id,:q,:seq,:text,:correct)
     """,new MapSqlParameterSource("id",UUID.randomUUID().toString()).addValue("q",qid).addValue("seq",i+1).addValue("text",question.options().get(i)).addValue("correct",i==question.correctIndex()));
   }
  }
  String attempt=UUID.randomUUID().toString();
  var params=new MapSqlParameterSource("id",practice.id()).addValue("attempt",attempt).addValue("student",practice.student())
   .addValue("activity",activity).addValue("enrollment",c.enrollment()).addValue("quiz",quizId).addValue("origin",fallback==null?"IA":"REUTILIZADO");
  jdbc.update("""
   INSERT INTO evaluacion_intento(id,estudiante_id,version_actividad_id,inscripcion_id,cliente_evento_id)
   VALUES(:attempt,:student,:activity,:enrollment,:id)
   """,params);
  jdbc.update("UPDATE evaluacion_practica_ia SET estado='LISTO',cuestionario_id=:quiz,intento_id=:attempt,origen=:origin WHERE id=:id",params);
 }
 public List<Question> questions(Quiz quiz,String attempt,boolean solutions) {
  var params=new MapSqlParameterSource("activity",quiz.activity()).addValue("attempt",attempt);
  record Row(String id,String text,String explanation,Boolean correct,String selected){}
  var rows=jdbc.query("""
   SELECT q.id,q.enunciado,q.explicacion,r.es_correcta,
    (SELECT ro.opcion_id FROM evaluacion_respuesta_opcion ro WHERE ro.respuesta_id=r.id LIMIT 1) selected
   FROM evaluacion_pregunta q LEFT JOIN evaluacion_respuesta r ON r.pregunta_id=q.id AND r.intento_id=:attempt
   WHERE q.version_actividad_id=:activity ORDER BY q.numero_secuencia
   """,params,(r,n)->new Row(r.getString(1),r.getString(2),r.getString(3),r.getObject(4)==null?null:r.getBoolean(4),r.getString(5)));
  List<Question> result=new ArrayList<>();
  for(var row:rows) {
   record Choice(String id,String text,boolean correct){}
   var choices=jdbc.query("SELECT id,texto,es_correcta FROM evaluacion_opcion_respuesta WHERE pregunta_id=:q ORDER BY numero_secuencia",Map.of("q",row.id()),
    (r,n)->new Choice(r.getString(1),r.getString(2),r.getBoolean(3)));
   boolean reveal=solutions||row.selected()!=null;
   result.add(new Question(row.id(),row.text(),choices.stream().map(o->new Option(o.id(),o.text())).toList(),row.selected(),row.correct(),
    reveal?row.explanation():null,reveal?choices.stream().filter(Choice::correct).map(Choice::id).findFirst().orElse(null):null));
  }
  return List.copyOf(result);
 }
 public void answer(Practice p,Quiz q,String question,String option) {
  var params=new MapSqlParameterSource("q",question).addValue("option",option).addValue("activity",q.activity()).addValue("attempt",p.attempt());
  var valid=jdbc.query("""
   SELECT o.es_correcta FROM evaluacion_opcion_respuesta o JOIN evaluacion_pregunta q ON q.id=o.pregunta_id
   WHERE o.id=:option AND q.id=:q AND q.version_actividad_id=:activity
   """,params,(r,n)->r.getBoolean(1));
  if(valid.isEmpty())throw error(HttpStatus.BAD_REQUEST,"Elige una alternativa de esta pregunta.");
  if(jdbc.queryForObject("SELECT COUNT(*) FROM evaluacion_respuesta WHERE intento_id=:attempt AND pregunta_id=:q",params,Integer.class)>0)return;
  String response=UUID.randomUUID().toString();params.addValue("response",response).addValue("correct",valid.getFirst());
  jdbc.update("""
   INSERT INTO evaluacion_respuesta(id,intento_id,pregunta_id,es_correcta,puntaje_obtenido)
   VALUES(:response,:attempt,:q,:correct,IF(:correct,1,0))
   """,params);
  jdbc.update("INSERT INTO evaluacion_respuesta_opcion(respuesta_id,pregunta_id,opcion_id) VALUES(:response,:q,:option)",params);
  var questions=questions(q,p.attempt(),false);
  if(questions.stream().allMatch(item->item.selectedOption()!=null)) {
   jdbc.update("UPDATE evaluacion_intento SET estado='COMPLETADO',finalizado_en=UTC_TIMESTAMP(6) WHERE id=:attempt",params);
   jdbc.update("UPDATE evaluacion_practica_ia SET estado='COMPLETADO' WHERE id=:id",Map.of("id",p.id()));
  }
 }
 public void report(Practice p,String question,String reason) {
  var q=quiz(p.quiz(),true);
  if(questions(q,p.attempt(),false).stream().noneMatch(item->item.id().equals(question)))throw error(HttpStatus.NOT_FOUND,"Pregunta no disponible.");
  var params=new MapSqlParameterSource("id",UUID.randomUUID().toString()).addValue("student",p.student()).addValue("quiz",p.quiz()).addValue("q",question).addValue("reason",reason);
  jdbc.update("""
   INSERT INTO evaluacion_reporte_pregunta_ia(id,cuestionario_id,pregunta_id,estudiante_id,motivo)
   VALUES(:id,:quiz,:q,:student,:reason) ON DUPLICATE KEY UPDATE id=evaluacion_reporte_pregunta_ia.id
   """,params);
  int pending=jdbc.queryForObject("SELECT COUNT(*) FROM evaluacion_reporte_pregunta_ia WHERE cuestionario_id=:quiz AND resuelto_en IS NULL",params,Integer.class);
  if(pending>0)jdbc.update("UPDATE evaluacion_cuestionario_ia SET estado='REPORTADO' WHERE id=:quiz",params);
 }
 public List<String> reviewIds(String room,String route) {
  return jdbc.query("""
   SELECT DISTINCT q.id FROM evaluacion_cuestionario_ia q JOIN evaluacion_practica_ia p ON p.cuestionario_id=q.id
   JOIN escuela_membresia_aula m ON m.usuario_id=p.estudiante_id AND m.aula_id=:room AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO'
   WHERE q.ruta_id=:route ORDER BY q.id LIMIT 50
   """,Map.of("room",room,"route",route),(r,n)->r.getString(1));
 }
 public Review review(String id) {
  var q=quiz(id,false);
  var context=jdbc.queryForObject("SELECT contexto_json FROM evaluacion_cuestionario_ia WHERE id=:id",Map.of("id",id),String.class);
  var reports=jdbc.query("SELECT motivo FROM evaluacion_reporte_pregunta_ia WHERE cuestionario_id=:id AND resuelto_en IS NULL ORDER BY creado_en",Map.of("id",id),(r,n)->r.getString(1));
  return new Review(id,context,q.state(),q.provider(),q.model(),questions(q,null,true),reports);
 }
 public void review(String id,String teacher,boolean approve,String note) {
  quiz(id,true);
  var params=new MapSqlParameterSource("id",id).addValue("teacher",teacher).addValue("note",note).addValue("state",approve?"REVISADO_DOCENTE":"RECHAZADO");
  jdbc.update("UPDATE evaluacion_cuestionario_ia SET estado=:state,revisado_por=:teacher,revisado_en=UTC_TIMESTAMP(6),nota_revision=:note WHERE id=:id",params);
  jdbc.update("UPDATE evaluacion_reporte_pregunta_ia SET resuelto_en=UTC_TIMESTAMP(6) WHERE cuestionario_id=:id AND resuelto_en IS NULL",params);
 }
}
