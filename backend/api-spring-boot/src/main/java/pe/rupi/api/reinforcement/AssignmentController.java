package pe.rupi.api.reinforcement;

import java.security.Principal;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.shared.SessionController;

@RestController
public class AssignmentController {
    private final AssignmentService service;
    public AssignmentController(AssignmentService service) {this.service=service;}
    private String user(Principal p) {
        if(p==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {return UUID.fromString(p.getName()).toString();}
        catch(IllegalArgumentException e) {throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
    }
    @GetMapping("/api/v1/teacher/reinforcement-assignments")
    public AssignmentService.Board board(@RequestParam UUID classroomId,@RequestParam UUID routeId,Principal p) {
        return service.board(user(p),classroomId.toString(),routeId.toString());
    }
    @PostMapping("/api/v1/teacher/reinforcement-assignments")
    public AssignmentService.Assigned assign(@RequestParam UUID classroomId,@RequestParam UUID routeId,
            @RequestBody AssignmentService.Request body,Principal p,HttpServletRequest request) {
        SessionController.checkOrigin(request);
        return service.assign(user(p),classroomId.toString(),routeId.toString(),body);
    }
    @GetMapping("/api/v1/student/reinforcement-assignments")
    public AssignmentService.StudentBoard student(Principal p) {return service.student(user(p));}
    @PutMapping("/api/v1/student/reinforcement-assignments/{id}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void start(@PathVariable UUID id,Principal p,HttpServletRequest request) {
        SessionController.checkOrigin(request);service.start(user(p),id.toString());
    }
    public record Completion(String reflection) {}
    @PutMapping("/api/v1/student/reinforcement-assignments/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable UUID id,@RequestBody Completion body,Principal p,HttpServletRequest request) {
        SessionController.checkOrigin(request);service.complete(user(p),id.toString(),body.reflection());
    }
}
