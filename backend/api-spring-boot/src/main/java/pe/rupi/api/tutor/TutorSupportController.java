package pe.rupi.api.tutor;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.shared.SessionController;

/** HU-15: nivel de asistencia del tutor IA por aula. */
@RestController
public class TutorSupportController {
    private final TutorSupportService service;
    public TutorSupportController(TutorSupportService service) { this.service=service; }

    private static String user(Principal principal) {
        if(principal==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try { return UUID.fromString(principal.getName()).toString(); }
        catch(IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED); }
    }

    public record Change(String mode) {}

    @GetMapping("/api/v1/teacher/tutor-support/classrooms/{room}")
    public TutorSupportService.ClassroomSupport classroom(@PathVariable UUID room,Principal principal) {
        return service.classroom(user(principal),room.toString());
    }

    @PutMapping("/api/v1/teacher/tutor-support/classrooms/{room}")
    public TutorSupportService.ClassroomSupport save(@PathVariable UUID room,@RequestBody Change body,
                                                     Principal principal,HttpServletRequest request) {
        SessionController.checkOrigin(request);
        return service.save(user(principal),room.toString(),body==null?null:body.mode());
    }

    @GetMapping("/api/v1/student/tutor-support")
    public TutorSupportService.StudentSupport student(@RequestParam UUID routeId,Principal principal) {
        return service.student(user(principal),routeId.toString());
    }
}
