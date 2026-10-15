package pe.rupi.api.reinforcement;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/student/learning-routes/{route}/nodes/{node}/reinforcements")
public class ReinforcementController {
    private final ReinforcementService service;
    public ReinforcementController(ReinforcementService service) { this.service=service; }
    private UUID student(Principal principal) {
        if(principal==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {return UUID.fromString(principal.getName());}
        catch(IllegalArgumentException error) {throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
    }
    // POST explícito: actualiza el vínculo personal según las respuestas guardadas.
    @PostMapping("/refresh")
    public ReinforcementService.View refresh(@PathVariable UUID route,@PathVariable UUID node,Principal principal) {
        return service.refresh(student(principal),route,node);
    }
    @PutMapping("/{id}/complete") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable UUID route,@PathVariable UUID node,@PathVariable UUID id,Principal principal) {
        service.complete(student(principal),route,node,id);
    }
}
