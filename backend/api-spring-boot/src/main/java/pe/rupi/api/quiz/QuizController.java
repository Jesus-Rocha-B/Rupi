package pe.rupi.api.quiz;
import java.security.Principal;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import pe.rupi.api.shared.SessionController;
import static pe.rupi.api.quiz.QuizRepository.error;
import static pe.rupi.api.quiz.QuizModels.*;

@RestController
public class QuizController {
 private final QuizService service;private final QuizHintService hints;
 public QuizController(QuizService service,QuizHintService hints){this.service=service;this.hints=hints;}
 private String user(Principal p) {
  if(p==null)throw error(HttpStatus.UNAUTHORIZED,"Inicia sesión.");
  try{return UUID.fromString(p.getName()).toString();}catch(IllegalArgumentException e){throw error(HttpStatus.UNAUTHORIZED,"Inicia sesión.");}
 }
 public record Generation(UUID requestId) {}
 public record Answer(UUID questionId,UUID optionId) {}
 public record Report(UUID questionId,String reason) {}
 public record ReviewRequest(boolean approve,String note) {}
 @GetMapping("/api/v1/student/ai-quizzes/topics")
 public List<QuizRepository.Topic> topics(Principal p){return service.topics(user(p));}
 @GetMapping("/api/v1/student/learning-routes/{route}/nodes/{node}/ai-quiz")
 public Availability available(@PathVariable UUID route,@PathVariable UUID node,Principal p){return service.availability(user(p),route.toString(),node.toString());}
 @PostMapping("/api/v1/student/learning-routes/{route}/nodes/{node}/ai-quiz")
 public View generate(@PathVariable UUID route,@PathVariable UUID node,@RequestBody Generation body,Principal p,HttpServletRequest request) {
  SessionController.checkOrigin(request);return service.generate(user(p),route.toString(),node.toString(),body.requestId());
 }
 @GetMapping("/api/v1/student/ai-quizzes/{id}")
 public View get(@PathVariable UUID id,Principal p){return service.get(user(p),id.toString());}
 @PostMapping("/api/v1/student/ai-quizzes/{id}/answers")
 public View answer(@PathVariable UUID id,@RequestBody Answer body,Principal p,HttpServletRequest request) {
  SessionController.checkOrigin(request);return service.answer(user(p),id.toString(),body.questionId(),body.optionId());
 }
 /** HU-37: pista adaptada al nivel para entender una respuesta equivocada. */
 @PostMapping("/api/v1/student/ai-quizzes/{id}/questions/{question}/hint")
 public QuizHintService.Hint hint(@PathVariable UUID id,@PathVariable UUID question,Principal p,HttpServletRequest request) {
  SessionController.checkOrigin(request);return hints.hint(user(p),id.toString(),question.toString());
 }
 @PostMapping("/api/v1/student/ai-quizzes/{id}/reports") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void report(@PathVariable UUID id,@RequestBody Report body,Principal p,HttpServletRequest request) {
  SessionController.checkOrigin(request);service.report(user(p),id.toString(),body.questionId(),body.reason());
 }
 @GetMapping("/api/v1/teacher/ai-quizzes")
 public List<Review> reviews(@RequestParam UUID classroomId,@RequestParam UUID routeId,Principal p) {
  return service.reviews(user(p),classroomId.toString(),routeId.toString());
 }
 @PostMapping("/api/v1/teacher/ai-quizzes/{id}/review") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void review(@PathVariable UUID id,@RequestParam UUID classroomId,@RequestParam UUID routeId,@RequestBody ReviewRequest body,Principal p,HttpServletRequest request) {
  SessionController.checkOrigin(request);service.review(user(p),classroomId.toString(),routeId.toString(),id.toString(),body.approve(),body.note());
 }
}
