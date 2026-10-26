package pe.rupi.api.notification;
import java.security.Principal;import java.util.*;import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.shared.SessionController;
@RestController @RequestMapping("/api/v1/student/notifications")
public class StudyNotificationController {
 private final StudyNotificationService service;
 public StudyNotificationController(StudyNotificationService service){this.service=service;}
 private String user(Principal p){if(p==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return UUID.fromString(p.getName()).toString();}
 @GetMapping("/preferences") public StudyNotificationService.Status get(Principal p){return service.status(user(p));}
 @PutMapping("/preferences") public StudyNotificationService.Status save(@RequestBody StudyNotificationService.Preferences value,Principal p,HttpServletRequest req){SessionController.checkOrigin(req);return service.save(user(p),value);}
 @GetMapping public StudyNotificationService.Inbox inbox(Principal p){return service.inbox(user(p));}
 @PutMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) public void read(@PathVariable UUID id,Principal p,HttpServletRequest req){SessionController.checkOrigin(req);service.read(user(p),id.toString());}
 @PostMapping("/{id}/claim") public Map<String,Boolean> claim(@PathVariable UUID id,Principal p,HttpServletRequest req){SessionController.checkOrigin(req);return Map.of("claimed",service.claim(user(p),id.toString()));}
}
