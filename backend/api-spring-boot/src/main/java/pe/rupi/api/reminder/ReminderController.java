package pe.rupi.api.reminder;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/student/learning-routes/{route}/reminders")
public class ReminderController {
    private final ReminderService service;
    public ReminderController(ReminderService service) { this.service=service; }
    private UUID user(Principal principal) {
        if(principal==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {return UUID.fromString(principal.getName());}
        catch(IllegalArgumentException error) {throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
    }
    @GetMapping
    public List<ReminderRepository.Reminder> list(@PathVariable UUID route, Principal principal) {
        return service.list(user(principal),route);
    }
    @PutMapping("/{reminder}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable UUID route,@PathVariable UUID reminder,Principal principal) {
        service.read(user(principal),route,reminder);
    }
}
