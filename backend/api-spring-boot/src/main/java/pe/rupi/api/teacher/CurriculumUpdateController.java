package pe.rupi.api.teacher;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.shared.SessionController;

/** HU-51: el docente revisa y aplica a su ruta la correspondencia de la resolución ministerial publicada. */
@RestController
@RequestMapping("/api/v1/teacher/learning-routes/{routeId}/curriculum-update")
public class CurriculumUpdateController {
    private final CurriculumUpdateService service;
    public CurriculumUpdateController(CurriculumUpdateService service) { this.service=service; }

    private static String teacher(Principal principal) {
        if(principal==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try { return UUID.fromString(principal.getName()).toString(); }
        catch(IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED); }
    }

    @GetMapping
    public CurriculumUpdateService.Plan plan(@PathVariable UUID routeId,Principal principal) {
        return service.plan(teacher(principal),routeId.toString());
    }

    @PostMapping
    public CurriculumUpdateService.Plan apply(@PathVariable UUID routeId,Principal principal,HttpServletRequest request) {
        SessionController.checkOrigin(request);
        return service.apply(teacher(principal),routeId.toString());
    }
}
