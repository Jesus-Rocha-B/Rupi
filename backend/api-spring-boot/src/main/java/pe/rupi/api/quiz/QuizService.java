package pe.rupi.api.quiz;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.http.HttpStatus;
import pe.rupi.api.report.ResultsReportService;
import static pe.rupi.api.quiz.QuizModels.*;
import static pe.rupi.api.quiz.QuizRepository.error;

@Service
public class QuizService {
 private final QuizRepository repo;private final QuizProvider provider;private final ResultsReportService access;
 private final TransactionTemplate tx;
 public QuizService(QuizRepository repo,QuizProvider provider,ResultsReportService access,PlatformTransactionManager manager) {
  this.repo=repo;this.provider=provider;this.access=access;this.tx=new TransactionTemplate(manager);
 }
 public List<QuizRepository.Topic> topics(String student){return repo.topics(student);}
 public Availability availability(String student,String route,String node) {
  var context=repo.context(student,route,node);
  return new Availability(provider.configured(),repo.reviewed(context).isPresent(),repo.current(student,route,node).map(this::view).orElse(null));
 }
 public View get(String student,String id) {
  var p=repo.own(student,id,false);repo.context(student,p.route(),p.node());return view(p);
 }
 public View generate(String student,String route,String node,UUID request) {
  if(request==null)throw error(HttpStatus.BAD_REQUEST,"Falta el identificador de solicitud.");
  var context=repo.context(student,route,node);
  if(context.content().length()<40)throw error(HttpStatus.CONFLICT,"Esta lección todavía no tiene suficiente contenido para preparar un cuestionario.");
  var fallback=repo.reviewed(context);
  // Reuse even after a restart. Disabled generation does not overwrite previous attempts.
  var previous=repo.current(student,route,node);
  if(previous.isPresent()&&Set.of("GENERANDO","LISTO").contains(previous.get().state())) {
   var current=view(previous.get());if(!current.status().equals("RETIRADO")&&!current.status().equals("FALLIDO"))return current;
  }
  if(!provider.configured()&&fallback.isEmpty())
   throw error(HttpStatus.SERVICE_UNAVAILABLE,"La IA aún no está configurada y no hay cuestionarios revisados para este tema. Puedes continuar tu lección.");
  var reservation=tx.execute(status->repo.reserve(student,context,request.toString()));
  if(reservation==null)throw new IllegalStateException();
  if(!reservation.created())return view(reservation.practice());
  var practice=reservation.practice();
  Draft generated=null;String reused=null;
  if(provider.configured()) {
   try {generated=QuizValidation.validate(provider.generate(QuizRepository.learning(context)),QuizRepository.learning(context));}
   catch(RuntimeException ignored) {reused=fallback.orElse(null);}
  } else {reused=fallback.orElse(null);}
  if(generated==null&&reused==null) {
   tx.executeWithoutResult(status->repo.failed(practice));return view(repo.own(student,practice.id(),false));
  }
  final Draft draft=generated;final String reviewed=reused;
  try {
   tx.executeWithoutResult(status->{
    var fresh=repo.context(student,route,node);
    if(!QuizRepository.hash(context).equals(QuizRepository.hash(fresh)))throw error(HttpStatus.CONFLICT,"El contenido cambió. Vuelve a abrir la lección.");
    repo.persist(practice,context,draft,provider.provider(),provider.model(),reviewed);
   });
  } catch(RuntimeException e) {tx.executeWithoutResult(status->repo.failed(practice));throw e;}
  return view(repo.own(student,practice.id(),false));
 }
 private View view(Practice p) {
  if(p.quiz()==null) return new View(p.state(),p.id(),p.origin(),null,0,0,List.of());
  var q=repo.quiz(p.quiz(),false);
  if(!Set.of("VALIDADO_AUTOMATICO","REVISADO_DOCENTE").contains(q.state()))
   return new View("RETIRADO",p.id(),p.origin(),q.state(),0,0,List.of());
  var questions=repo.questions(q,p.attempt(),false);
  return new View(p.state(),p.id(),p.origin(),q.state(),(int)questions.stream().filter(a->a.selectedOption()!=null).count(),
   (int)questions.stream().filter(a->Boolean.TRUE.equals(a.correct())).count(),questions);
 }
 public View answer(String student,String id,UUID question,UUID option) {
  if(question==null||option==null)throw error(HttpStatus.BAD_REQUEST,"Selecciona una alternativa.");
  return tx.execute(status->{
   var p=repo.own(student,id,true);repo.context(student,p.route(),p.node());
   if(p.quiz()==null)throw error(HttpStatus.CONFLICT,"La práctica todavía no está lista.");
   var q=repo.quiz(p.quiz(),true);
   if(!Set.of("VALIDADO_AUTOMATICO","REVISADO_DOCENTE").contains(q.state()))throw error(HttpStatus.CONFLICT,"Este cuestionario está en revisión.");
   if(!Set.of("LISTO","COMPLETADO").contains(p.state()))throw error(HttpStatus.CONFLICT,"Esta práctica ya no admite respuestas.");
   repo.answer(p,q,question.toString(),option.toString());return view(repo.own(student,id,false));
  });
 }
 public void report(String student,String id,UUID question,String reason) {
  if(question==null||reason==null||reason.isBlank()||reason.length()>500)throw error(HttpStatus.BAD_REQUEST,"Explica brevemente qué debemos revisar (máximo 500 caracteres).");
  tx.executeWithoutResult(status->{
   var p=repo.own(student,id,true);repo.context(student,p.route(),p.node());
   if(p.quiz()==null)throw error(HttpStatus.CONFLICT,"La práctica todavía no está lista.");
   repo.report(p,question.toString(),reason.strip());
  });
 }
 private List<String> reviewIds(String teacher,String room,String route) {
  if(access.routes(teacher,room).stream().noneMatch(r->r.id().equals(route)))throw error(HttpStatus.NOT_FOUND,"Ruta no disponible para esta aula.");
  return repo.reviewIds(room,route);
 }
 public List<Review> reviews(String teacher,String room,String route){return reviewIds(teacher,room,route).stream().map(repo::review).toList();}
 public void review(String teacher,String room,String route,String id,boolean approve,String note) {
  if(note==null||note.isBlank()||note.length()>1000)throw error(HttpStatus.BAD_REQUEST,"Escribe el resultado de tu revisión (máximo 1000 caracteres).");
  tx.executeWithoutResult(status->{
   if(!reviewIds(teacher,room,route).contains(id))throw error(HttpStatus.NOT_FOUND,"Cuestionario fuera de tu aula.");
   repo.review(id,teacher,approve,note.strip());
  });
 }
}
