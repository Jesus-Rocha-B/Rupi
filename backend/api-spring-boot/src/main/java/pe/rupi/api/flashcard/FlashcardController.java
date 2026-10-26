package pe.rupi.api.flashcard;
import java.security.Principal;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pe.rupi.api.shared.SessionController;
@RestController
public class FlashcardController {
 private final FlashcardService service;
 public FlashcardController(FlashcardService service){this.service=service;}
 private String user(Principal p){if(p==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return UUID.fromString(p.getName()).toString();}
 @GetMapping("/api/v1/student/learning-routes/{route}/nodes/{node}/flashcards")
 public FlashcardService.Deck get(@PathVariable UUID route,@PathVariable UUID node,Principal p){return service.get(user(p),route.toString(),node.toString());}
 @PostMapping("/api/v1/student/learning-routes/{route}/nodes/{node}/flashcards")
 public FlashcardService.Deck command(@PathVariable UUID route,@PathVariable UUID node,@RequestBody FlashcardService.Command body,Principal p,HttpServletRequest req){SessionController.checkOrigin(req);return service.command(user(p),route.toString(),node.toString(),body);}
 @GetMapping("/api/v1/teacher/flashcards")
 public Map<String,Object> list(@RequestParam UUID classroomId,@RequestParam UUID routeId,Principal p){
  String u=user(p),room=classroomId.toString(),route=routeId.toString();
  return Map.of("nodes",service.nodes(u,room,route),"cards",service.teacherCards(u,room,route));
 }
 @PostMapping("/api/v1/teacher/flashcards") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void create(@RequestParam UUID classroomId,@RequestParam UUID routeId,@RequestBody FlashcardService.Create body,Principal p,HttpServletRequest req){
  SessionController.checkOrigin(req);service.create(user(p),classroomId.toString(),routeId.toString(),body);
 }
 @DeleteMapping("/api/v1/teacher/flashcards/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void retire(@PathVariable UUID id,@RequestParam UUID classroomId,@RequestParam UUID routeId,Principal p,HttpServletRequest req){
  SessionController.checkOrigin(req);service.retire(user(p),classroomId.toString(),routeId.toString(),id.toString());
 }
}
