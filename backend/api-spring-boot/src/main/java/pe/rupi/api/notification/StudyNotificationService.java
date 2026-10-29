package pe.rupi.api.notification;
import java.time.*;import java.sql.Timestamp;import java.util.*;
import org.springframework.stereotype.Service;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.reminder.ReminderRepository;
import pe.rupi.api.gamification.DailyGoalTarget;
import pe.rupi.api.gamification.StudyStreakService;
@Service
public class StudyNotificationService {
 public record Preferences(int dailyGoal,String remindAt,boolean goals,boolean evaluations,boolean system,String quietFrom,String quietUntil,long revision){}
 public record Status(Preferences preferences,int completedToday,String localDate){}
 public record Notice(String id,String title,String body,String category,boolean read,String createdAt){}
 public record Inbox(List<Notice> items,boolean quiet,boolean system){}
 private static final ZoneId LIMA=ZoneId.of("America/Lima");
 private final NamedParameterJdbcTemplate db;private final ReminderRepository reminders;private final TransactionTemplate tx;private final Clock clock;private final StudyStreakService streaks;
 @Autowired public StudyNotificationService(NamedParameterJdbcTemplate db,ReminderRepository reminders,PlatformTransactionManager manager){this(db,reminders,manager,Clock.systemUTC());}
 public StudyNotificationService(NamedParameterJdbcTemplate db,ReminderRepository reminders,PlatformTransactionManager manager,Clock clock){this.db=db;this.reminders=reminders;tx=new TransactionTemplate(manager);this.clock=clock;streaks=new StudyStreakService(db,clock);}
 private static ResponseStatusException fail(HttpStatus code,String message){return new ResponseStatusException(code,message);}
 private Map<String,Object> p(String user){return new HashMap<>(Map.of("user",user));}
 private void student(String user){
  int n=db.queryForObject("""
   SELECT COUNT(*) FROM identidad_cuenta_usuario u JOIN identidad_usuario_rol ur ON ur.usuario_id=u.id
   JOIN identidad_rol r ON r.id=ur.rol_id WHERE u.id=:user AND u.estado='ACTIVO' AND u.desactivado_en IS NULL AND r.codigo='ESTUDIANTE'
   """,p(user),Integer.class);
  if(n==0)throw fail(HttpStatus.FORBIDDEN,"Esta función es para estudiantes activos.");
 }
 private boolean category(String user,String category,String channel){
  var params=p(user);params.put("category",category);params.put("channel",channel);
  return db.queryForList("SELECT habilitada FROM notificacion_preferencia_categoria WHERE usuario_id=:user AND categoria=:category AND canal=:channel",params,Boolean.class).stream().findFirst().orElse(false);
 }
 private Preferences preferences(String user){
  var params=p(user);var goal=db.queryForList("SELECT objetivo_actividades,revision FROM gamificacion_preferencia_estudio WHERE usuario_id=:user",params);
  var quiet=db.queryForList("SELECT TIME_FORMAT(no_molestar_desde,'%H:%i') desde,TIME_FORMAT(no_molestar_hasta,'%H:%i') hasta FROM notificacion_preferencia WHERE usuario_id=:user",params);
  var time=db.queryForList("SELECT TIME_FORMAT(hora_local,'%H:%i') FROM notificacion_programacion WHERE usuario_id=:user AND categoria='RECORDATORIO' AND dia_semana=1",params,String.class);
  // Sin fila propia se muestra la misma meta que el anillo de «Mi meta de hoy» (una sola meta diaria).
  return new Preferences(goal.isEmpty()?DailyGoalTarget.resolve(db,user):((Number)goal.getFirst().get("objetivo_actividades")).intValue(),time.isEmpty()?"17:00":time.getFirst(),
   category(user,"RECORDATORIO","INTERNA"),category(user,"TAREA","INTERNA"),
   category(user,"RECORDATORIO","PUSH")||category(user,"TAREA","PUSH"),
   quiet.isEmpty()||quiet.getFirst().get("desde")==null?"21:00":quiet.getFirst().get("desde").toString(),
   quiet.isEmpty()||quiet.getFirst().get("hasta")==null?"08:00":quiet.getFirst().get("hasta").toString(),
   goal.isEmpty()?0:((Number)goal.getFirst().get("revision")).longValue());
 }
 public static boolean quietAt(LocalTime time,LocalTime start,LocalTime end){
  return start.isBefore(end)?!time.isBefore(start)&&time.isBefore(end):!time.isBefore(start)||time.isBefore(end);
 }
 private static LocalTime time(String value){
  try{if(value==null||!value.matches("\\d{2}:\\d{2}"))throw new IllegalArgumentException();return LocalTime.parse(value);}
  catch(Exception e){throw fail(HttpStatus.BAD_REQUEST,"Usa horas válidas en formato HH:mm.");}
 }
 private static void validate(Preferences v){
  if(v==null||v.dailyGoal()<1||v.dailyGoal()>5||v.revision()<0)throw fail(HttpStatus.BAD_REQUEST,"Elige una meta de 1 a 5 actividades.");
  var start=time(v.quietFrom());var end=time(v.quietUntil());var at=time(v.remindAt());
  if(start.equals(end))throw fail(HttpStatus.BAD_REQUEST,"El descanso debe tener horas distintas de inicio y fin.");
  if(v.goals()&&quietAt(at,start,end))throw fail(HttpStatus.BAD_REQUEST,"Elige una hora de recordatorio fuera del descanso.");
 }
 private int completed(String user){
  LocalDate day=LocalDate.now(clock.withZone(LIMA));var params=p(user);
  params.put("start",stamp(day.atStartOfDay(LIMA).toInstant()));params.put("end",stamp(day.plusDays(1).atStartOfDay(LIMA).toInstant()));
  return db.queryForObject("""
   SELECT COUNT(DISTINCT p.nodo_ruta_id) FROM aprendizaje_progreso_nodo p
   JOIN aprendizaje_inscripcion_ruta i ON i.id=p.inscripcion_id
   WHERE i.estudiante_id=:user AND p.estado='COMPLETADO' AND p.completado_en>=:start AND p.completado_en<:end
   """,params,Integer.class);
 }
 public Status status(String user){student(user);return new Status(preferences(user),completed(user),LocalDate.now(clock.withZone(LIMA)).toString());}
 public Status save(String user,Preferences value){
  student(user);validate(value);
  return tx.execute(s->{
   var params=p(user);
   db.update("INSERT INTO gamificacion_preferencia_estudio(usuario_id) VALUES(:user) ON DUPLICATE KEY UPDATE usuario_id=usuario_id",params);
   long revision=db.queryForObject("SELECT revision FROM gamificacion_preferencia_estudio WHERE usuario_id=:user FOR UPDATE",params,Long.class);
   if(value.revision()!=revision)throw fail(HttpStatus.CONFLICT,"Tus preferencias cambiaron en otro dispositivo. Recárgalas antes de guardar.");
   params.put("goal",value.dailyGoal());params.put("from",value.quietFrom());params.put("until",value.quietUntil());params.put("time",value.remindAt());
   db.update("UPDATE gamificacion_preferencia_estudio SET objetivo_actividades=:goal,revision=revision+1,actualizada_en=UTC_TIMESTAMP(6) WHERE usuario_id=:user",params);
   db.update("""
    INSERT INTO notificacion_preferencia(usuario_id,no_molestar_desde,no_molestar_hasta) VALUES(:user,:from,:until)
    ON DUPLICATE KEY UPDATE no_molestar_desde=:from,no_molestar_hasta=:until
    """,params);
   for(String cat:List.of("TAREA","RECORDATORIO")){
    params.put("category",cat);boolean enabled=cat.equals("TAREA")?value.evaluations():value.goals();
    for(String channel:List.of("INTERNA","PUSH")){
     params.put("channel",channel);params.put("enabled",channel.equals("INTERNA")?enabled:enabled&&value.system());
     db.update("""
      INSERT INTO notificacion_preferencia_categoria(usuario_id,categoria,canal,habilitada) VALUES(:user,:category,:channel,:enabled)
      ON DUPLICATE KEY UPDATE habilitada=:enabled
      """,params);
    }
   }
   for(int day=1;day<=7;day++){
    params.put("day",day);params.put("enabled",value.goals());
    db.update("""
     INSERT INTO notificacion_programacion(usuario_id,categoria,dia_semana,hora_local,habilitada) VALUES(:user,'RECORDATORIO',:day,:time,:enabled)
     ON DUPLICATE KEY UPDATE hora_local=:time,habilitada=:enabled
     """,params);
   }
   params.put("date",LocalDate.now(clock.withZone(LIMA)));
   db.update("""
    INSERT INTO gamificacion_meta_diaria(usuario_id,fecha_local,tipo,objetivo) VALUES(:user,:date,'ACTIVIDADES',:goal)
    ON DUPLICATE KEY UPDATE objetivo=:goal
    """,params);
   return status(user);
  });
 }
 private String event(ReminderRepository.Due due){return "EVAL:"+due.route()+":"+due.node()+":"+due.startsAt();}
 private static String streakKey(LocalDate day){return "RACHA:"+day;}
 // Historia 28: novedades del docente de los últimos días (fecha de evaluación nueva o reprogramada, material adjunto a una parada).
 record News(String key,String title,String body,Instant expires){}
 static final Duration NEWS_WINDOW=Duration.ofDays(7);
 private static final java.time.format.DateTimeFormatter DAY_TIME=java.time.format.DateTimeFormatter.ofPattern("d/MM 'a las' HH:mm");
 private List<News> news(String user,Instant now){
  List<News> items=new ArrayList<>();
  for(var due:reminders.scheduledSince(user,now,now.minus(NEWS_WINDOW))){
   String body=due.title()+" · "+DAY_TIME.format(due.startsAt().atZone(LIMA))+". Tu docente ya la puso en el calendario.";
   items.add(new News("FECHA:"+due.route()+":"+due.node()+":"+due.startsAt(),"Nueva fecha de evaluación",body.substring(0,Math.min(body.length(),500)),due.startsAt()));
  }
  var params=p(user);params.put("since",stamp(now.minus(NEWS_WINDOW)));
  for(var row:db.queryForList("""
   SELECT r.id,r.titulo,r.creado_en,ruta.titulo ruta FROM aprendizaje_nodo_recurso r
   JOIN aprendizaje_nodo_ruta n ON n.id=r.nodo_id
   JOIN aprendizaje_version_ruta v ON v.id=n.version_ruta_id AND v.estado='PUBLICADO'
   JOIN aprendizaje_ruta ruta ON ruta.id=v.ruta_id AND ruta.archivado_en IS NULL
   JOIN aprendizaje_inscripcion_ruta i ON i.version_ruta_id=v.id AND i.estudiante_id=:user AND i.estado IN ('ACTIVO','COMPLETADO')
   WHERE r.creado_en>=:since AND r.creado_en>=i.inscrito_en ORDER BY r.creado_en DESC,r.id LIMIT 20
   """,params)){
   var created=((Timestamp)row.get("creado_en")).toLocalDateTime().toInstant(ZoneOffset.UTC);
   String body=row.get("titulo")+" · "+row.get("ruta")+". Ábrelo desde la parada cuando quieras repasar.";
   items.add(new News("MATERIAL:"+row.get("id"),"Nuevo material de tu docente",body.substring(0,Math.min(body.length(),500)),created.plus(NEWS_WINDOW)));
  }
  return items;
 }
 /** Dos horas antes del descanso, pero nunca antes de la hora de recordatorio elegida ni dentro del descanso. */
 static LocalTime streakAt(Preferences s){
  var at=time(s.remindAt());var from=time(s.quietFrom());var risk=from.minusHours(2);
  return risk.isBefore(at)||quietAt(risk,from,time(s.quietUntil()))?at:risk;
 }
 private void message(String user,String event,String category,String title,String body,Instant expires){
  var params=p(user);params.put("key",event);
  if(db.queryForObject("SELECT COUNT(*) FROM notificacion_evento_usuario WHERE usuario_id=:user AND clave_evento=:key",params,Integer.class)>0)return;
  String id=UUID.randomUUID().toString();params.put("id",id);params.put("category",category);params.put("title",title);params.put("body",body);params.put("now",stamp(clock.instant()));params.put("expires",stamp(expires));
  db.update("INSERT INTO notificacion_mensaje(id,usuario_id,categoria,titulo,cuerpo,creada_en,expira_en) VALUES(:id,:user,:category,:title,:body,:now,:expires)",params);
  db.update("INSERT INTO notificacion_evento_usuario(usuario_id,clave_evento,mensaje_id,categoria) VALUES(:user,:key,:id,:category)",params);
  db.update("INSERT INTO notificacion_entrega(id,mensaje_id,canal,estado,programada_en,completada_en) VALUES(:delivery,:id,'INTERNA','ENTREGADA',:now,:now)",new HashMap<>(params){{put("delivery",UUID.randomUUID().toString());}});
 }
 public void generate(String user){
  student(user);
  tx.executeWithoutResult(s->{
   var params=p(user);
   if(db.queryForList("SELECT usuario_id FROM gamificacion_preferencia_estudio WHERE usuario_id=:user FOR UPDATE",params).isEmpty())return;
   var settings=preferences(user);var now=clock.instant();var local=now.atZone(LIMA);
   if(quietAt(local.toLocalTime(),time(settings.quietFrom()),time(settings.quietUntil())))return;
   if(settings.goals()&&!local.toLocalTime().isBefore(time(settings.remindAt()))&&completed(user)<settings.dailyGoal()){
    params.put("goal",settings.dailyGoal());params.put("date",local.toLocalDate());
    db.update("INSERT IGNORE INTO gamificacion_meta_diaria(usuario_id,fecha_local,tipo,objetivo) VALUES(:user,:date,'ACTIVIDADES',:goal)",params);
    message(user,"META:"+local.toLocalDate(),"RECORDATORIO","Un momento para aprender",
     "Tu meta de hoy es completar "+settings.dailyGoal()+" actividad(es). Puedes avanzar con calma y pedir ayuda a tu docente.",
     local.toLocalDate().plusDays(1).atStartOfDay(LIMA).toInstant());
   }
   // Historia 27: la racha sigue viva hasta el fin del día; se avisa una vez, antes del descanso, si hoy aún no hay actividad.
   if(settings.goals()&&!local.toLocalTime().isBefore(streakAt(settings))){
    var streak=streaks.current(user);
    if(streak.atRisk())message(user,streakKey(local.toLocalDate()),"RECORDATORIO","Tu racha te espera",
     "Llevas "+streak.days()+(streak.days()==1?" día":" días")+" seguidos aprendiendo. Si hoy completas una actividad, tu racha sigue creciendo. ¡Tú marcas el ritmo!",
     local.toLocalDate().plusDays(1).atStartOfDay(LIMA).toInstant());
   }
   if(settings.evaluations())for(var due:reminders.due(user,now,now.plus(Duration.ofHours(24)))){
    String title="Evaluación próxima";String body=due.title()+" · "+due.startsAt().atZone(LIMA).toLocalDateTime()+". Revisa tus materiales con calma.";
    message(user,event(due),"TAREA",title,body.substring(0,Math.min(body.length(),500)),due.startsAt());
   }
   if(settings.evaluations())for(var item:news(user,now))message(user,item.key(),"TAREA",item.title(),item.body(),item.expires());
  });
 }
 private List<Notice> visible(String user,Preferences settings){
  var now=clock.instant();var params=p(user);params.put("now",stamp(now));
  Set<String> evaluations=new HashSet<>();if(settings.evaluations()){reminders.due(user,now,now.plus(Duration.ofHours(24))).forEach(d->evaluations.add(event(d)));news(user,now).forEach(n->evaluations.add(n.key()));}
  int done=completed(user);var today=LocalDate.now(clock.withZone(LIMA));String goalKey="META:"+today;String streakKey=streakKey(today);
  return db.queryForList("""
   SELECT m.*,e.clave_evento FROM notificacion_mensaje m JOIN notificacion_evento_usuario e ON e.mensaje_id=m.id
   WHERE m.usuario_id=:user AND (m.expira_en IS NULL OR m.expira_en>:now) ORDER BY m.creada_en DESC,m.id LIMIT 100
   """,params).stream().filter(row->row.get("categoria").equals("RECORDATORIO")?
    settings.goals()&&(done<settings.dailyGoal()&&row.get("clave_evento").equals(goalKey)||done==0&&row.get("clave_evento").equals(streakKey)):settings.evaluations()&&evaluations.contains(row.get("clave_evento").toString()))
   .map(row->new Notice(row.get("id").toString(),row.get("titulo").toString(),row.get("cuerpo").toString(),row.get("categoria").toString(),row.get("leida_en")!=null,row.get("creada_en").toString())).toList();
 }
 public Inbox inbox(String user){generate(user);var settings=preferences(user);return new Inbox(visible(user,settings),quietAt(LocalTime.now(clock.withZone(LIMA)),time(settings.quietFrom()),time(settings.quietUntil())),settings.system());}
 public void read(String user,String id){
  student(user);if(visible(user,preferences(user)).stream().noneMatch(m->m.id().equals(id)))throw fail(HttpStatus.NOT_FOUND,"Aviso no disponible.");
  db.update("UPDATE notificacion_mensaje SET leida_en=COALESCE(leida_en,:now) WHERE id=:id AND usuario_id=:user",Map.of("id",id,"user",user,"now",stamp(clock.instant())));
 }
 public boolean claim(String user,String id){
  student(user);
  return tx.execute(s->{
   var params=p(user);params.put("id",id);params.put("now",stamp(clock.instant()));
   db.queryForList("SELECT usuario_id FROM gamificacion_preferencia_estudio WHERE usuario_id=:user FOR UPDATE",params);
   var settings=preferences(user);
   if(!settings.system()||quietAt(LocalTime.now(clock.withZone(LIMA)),time(settings.quietFrom()),time(settings.quietUntil())))return false;
   if(visible(user,settings).stream().noneMatch(m->m.id().equals(id)&&!m.read()))return false;
   int n=db.update("UPDATE notificacion_evento_usuario SET reclamada_en=:now WHERE usuario_id=:user AND mensaje_id=:id AND reclamada_en IS NULL",params);
   if(n==0)return false;
   params.put("delivery",UUID.randomUUID().toString());
   db.update("INSERT INTO notificacion_entrega(id,mensaje_id,canal,estado,programada_en,completada_en) VALUES(:delivery,:id,'PUSH','ENVIADA',:now,:now)",params);
   return true;
  });
 }
 public List<String> configuredStudents(){return db.queryForList("SELECT p.usuario_id FROM gamificacion_preferencia_estudio p JOIN identidad_cuenta_usuario u ON u.id=p.usuario_id AND u.estado='ACTIVO' AND u.desactivado_en IS NULL",Map.of(),String.class);}
 static Timestamp stamp(Instant now){return Timestamp.valueOf(LocalDateTime.ofInstant(now,ZoneOffset.UTC));}
}
