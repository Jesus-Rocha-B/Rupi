package pe.rupi.api.flashcard;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pe.rupi.api.quiz.QuizRepository;
import pe.rupi.api.report.ResultsReportService;

@Service
public class FlashcardService {
 private final NamedParameterJdbcTemplate db; private final QuizRepository access; private final ResultsReportService teachers;
 public FlashcardService(NamedParameterJdbcTemplate db,QuizRepository access,ResultsReportService teachers){this.db=db;this.access=access;this.teachers=teachers;}
 public record Card(String id,String question,String answer,String source,String state){}
 public record Deck(List<Card> cards,String current,boolean revealed,long revision){}
 public record Command(UUID requestId,long revision,String action,UUID cardId){}
 public record Create(UUID requestId,UUID nodeId,String question,String answer,String source,boolean reviewed){}
 public record Node(String id,String title){}
 private static ResponseStatusException fail(HttpStatus status,String message){return new ResponseStatusException(status,message);}
 private Map<String,Object> params(String student,String route,String node){return new HashMap<>(Map.of("student",student,"route",route,"node",node));}
 private List<Card> cards(String student,String route,String node) {
  var context=access.context(student,route,node);
  var p=params(student,route,node);p.put("activity",context.activity());
  return db.query("""
   SELECT t.id,c.frente,c.reverso,t.fuente,CASE WHEN p.tarjeta_id IS NULL THEN 'NUEVA' WHEN p.intervalo_dias=0 THEN 'PRACTICAR' ELSE 'RECUERDO' END estado
   FROM aprendizaje_tarjeta_revision t JOIN aprendizaje_tarjeta_memoria c ON c.id=t.id
   LEFT JOIN aprendizaje_repaso_tarjeta p ON p.tarjeta_id=t.id AND p.usuario_id=:student
   WHERE t.ruta_id=:route AND t.nodo_id=:node AND c.version_actividad_id=:activity AND t.retirado_en IS NULL
   AND EXISTS(SELECT 1 FROM escuela_membresia_aula m WHERE m.usuario_id=:student AND m.rol='ESTUDIANTE' AND m.aula_id=t.aula_id AND m.estado='ACTIVO')
   ORDER BY CASE WHEN p.intervalo_dias=0 THEN 0 WHEN p.tarjeta_id IS NULL THEN 1 ELSE 2 END,t.revisado_en,t.id
   """,p,(r,n)->new Card(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)));
 }
 public Deck get(String student,String route,String node){
  var list=cards(student,route,node);
  var rows=db.queryForList("SELECT * FROM aprendizaje_tarjeta_sesion WHERE estudiante_id=:student AND ruta_id=:route AND nodo_id=:node",params(student,route,node));
  if(rows.isEmpty())return new Deck(list,list.isEmpty()?null:list.getFirst().id(),false,0);
  var row=rows.getFirst();String current=(String)row.get("tarjeta_id");
  boolean valid=list.stream().anyMatch(c->c.id().equals(current));
  return new Deck(list,valid?current:list.isEmpty()?null:list.getFirst().id(),valid&&Boolean.TRUE.equals(row.get("revelada")),((Number)row.get("revision")).longValue());
 }
 @Transactional
 public Deck command(String student,String route,String node,Command cmd){
  if(cmd==null||cmd.requestId()==null||cmd.action()==null)throw fail(HttpStatus.BAD_REQUEST,"Falta identificar la acción.");
  access.context(student,route,node);
  var p=params(student,route,node);
  db.update("INSERT INTO aprendizaje_tarjeta_sesion(estudiante_id,ruta_id,nodo_id) VALUES(:student,:route,:node) ON DUPLICATE KEY UPDATE estudiante_id=estudiante_id",p);
  var row=db.queryForMap("SELECT * FROM aprendizaje_tarjeta_sesion WHERE estudiante_id=:student AND ruta_id=:route AND nodo_id=:node FOR UPDATE",p);
  if(cmd.requestId().toString().equals(row.get("ultima_solicitud")))return get(student,route,node);
  if(((Number)row.get("revision")).longValue()!=cmd.revision())throw fail(HttpStatus.CONFLICT,"Tu repaso cambió. Actualiza las tarjetas antes de continuar.");
  Deck deck=get(student,route,node);
  String target=cmd.cardId()==null?deck.current():cmd.cardId().toString();
  if(!deck.cards().stream().map(Card::id).toList().contains(target))throw fail(HttpStatus.NOT_FOUND,"Esta tarjeta ya no está disponible.");
  boolean reveal=false;
  switch(cmd.action()){
   case "REVEAL" -> {if(!Objects.equals(target,deck.current()))throw fail(HttpStatus.CONFLICT,"Primero abre la tarjeta.");reveal=true;}
   case "NAVIGATE" -> {}
   case "RESTART" -> target=deck.cards().getFirst().id();
   case "REMEMBER","PRACTICE" -> {
    if(!deck.revealed()||!Objects.equals(target,deck.current()))throw fail(HttpStatus.CONFLICT,"Revisa primero la respuesta.");
    p.put("card",target);p.put("interval",cmd.action().equals("REMEMBER")?1:0);
    db.update("""
     INSERT INTO aprendizaje_repaso_tarjeta(usuario_id,tarjeta_id,intervalo_dias,proximo_repaso_en,repasada_en) VALUES(:student,:card,:interval,TIMESTAMPADD(DAY,:interval,UTC_TIMESTAMP(6)),UTC_TIMESTAMP(6))
     ON DUPLICATE KEY UPDATE intervalo_dias=:interval,proximo_repaso_en=TIMESTAMPADD(DAY,:interval,UTC_TIMESTAMP(6)),repasada_en=UTC_TIMESTAMP(6)
     """,p);reveal=true;
   }
   default -> throw fail(HttpStatus.BAD_REQUEST,"Acción no válida.");
  }
  p.put("card",target);p.put("revealed",reveal);p.put("request",cmd.requestId().toString());
  db.update("""
   UPDATE aprendizaje_tarjeta_sesion SET tarjeta_id=:card,revelada=:revealed,revision=revision+1,
   ultima_solicitud=:request,actualizado_en=UTC_TIMESTAMP(6)
   WHERE estudiante_id=:student AND ruta_id=:route AND nodo_id=:node
   """,p);
  return get(student,route,node);
 }
 private void teacher(String user,String room,String route){if(teachers.routes(user,room).stream().noneMatch(r->r.id().equals(route)))throw fail(HttpStatus.NOT_FOUND,"Ruta fuera de tu aula.");}
 public List<Node> nodes(String user,String room,String route){
  teacher(user,room,route);return db.query("""
   SELECT n.id,a.titulo FROM aprendizaje_nodo_ruta n JOIN aprendizaje_version_actividad a ON a.id=n.version_actividad_id
   WHERE n.version_ruta_id=:route AND a.estado='PUBLICADO' ORDER BY n.numero_secuencia
   """,Map.of("route",route),(r,n)->new Node(r.getString(1),r.getString(2)));
 }
 public List<Card> teacherCards(String user,String room,String route){
  teacher(user,room,route);return db.query("SELECT t.id,c.frente,c.reverso,t.fuente FROM aprendizaje_tarjeta_revision t JOIN aprendizaje_tarjeta_memoria c ON c.id=t.id WHERE aula_id=:room AND ruta_id=:route AND retirado_en IS NULL ORDER BY revisado_en DESC",Map.of("room",room,"route",route),(r,n)->new Card(r.getString(1),r.getString(2),r.getString(3),r.getString(4),"REVISADA"));
 }
 private String text(String value,int limit){if(value==null||value.isBlank()||value.length()>limit)throw fail(HttpStatus.BAD_REQUEST,"Completa los textos dentro de los límites indicados.");return value.strip();}
 @Transactional
 public void create(String user,String room,String route,Create body){
  teacher(user,room,route);
  if(body.requestId()==null||body.nodeId()==null||!body.reviewed())throw fail(HttpStatus.BAD_REQUEST,"Confirma la revisión del contenido y elige una actividad.");
  var p=new HashMap<String,Object>(Map.of("user",user,"room",room,"route",route,"node",body.nodeId().toString(),"request",body.requestId().toString(),
   "question",text(body.question(),500),"answer",text(body.answer(),1500),"source",text(body.source(),1000)));
  var activities=db.queryForList("SELECT version_actividad_id FROM aprendizaje_nodo_ruta WHERE id=:node AND version_ruta_id=:route",p,String.class);
  if(activities.isEmpty())throw fail(HttpStatus.NOT_FOUND,"Actividad fuera de la ruta.");
  p.put("activity",activities.getFirst());p.put("id",UUID.randomUUID().toString());
  db.queryForObject("SELECT id FROM aprendizaje_version_actividad WHERE id=:activity FOR UPDATE",p,String.class);
  if(!db.queryForList("SELECT id FROM aprendizaje_tarjeta_revision WHERE docente_id=:user AND solicitud_id=:request",p).isEmpty())return;
  p.put("sequence",db.queryForObject("SELECT COALESCE(MAX(numero_secuencia),0)+1 FROM aprendizaje_tarjeta_memoria WHERE version_actividad_id=:activity",p,Integer.class));
  db.update("INSERT INTO aprendizaje_tarjeta_memoria(id,version_actividad_id,numero_secuencia,frente,reverso) VALUES(:id,:activity,:sequence,:question,:answer)",p);
  db.update("""
   INSERT INTO aprendizaje_tarjeta_revision(id,solicitud_id,docente_id,aula_id,ruta_id,nodo_id,fuente)
   VALUES(:id,:request,:user,:room,:route,:node,:source)

   """,p);
 }
 public void retire(String user,String room,String route,String id){
  teacher(user,room,route);db.update("UPDATE aprendizaje_tarjeta_revision SET retirado_en=UTC_TIMESTAMP(6) WHERE id=:id AND aula_id=:room AND ruta_id=:route",Map.of("id",id,"room",room,"route",route));
 }
}
