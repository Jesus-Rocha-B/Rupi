package pe.rupi.api.calendar;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static pe.rupi.api.calendar.CalendarDtos.*;
@RestController
public class CalendarController {
    private final CalendarService service;
    public CalendarController(CalendarService service) {this.service=service;}
    private String user(Principal p) {if(p==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return p.getName();}
    @GetMapping("/api/v1/student/learning-routes/{route}/calendar")
    public Schedule student(@PathVariable UUID route, Principal p) {return service.read(user(p),route.toString(),false);}
    @GetMapping("/api/v1/teacher/learning-routes/{route}/calendar")
    public Schedule teacher(@PathVariable UUID route, Principal p) {return service.read(user(p),route.toString(),true);}
    @PutMapping("/api/v1/teacher/learning-routes/{route}/calendar")
    public Schedule save(@PathVariable UUID route, @RequestBody Dates dates, Principal p) {return service.save(user(p),route.toString(),dates);}
    @DeleteMapping("/api/v1/teacher/learning-routes/{route}/calendar/{event}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID route,@PathVariable UUID event,@RequestParam UUID classroomId,Principal p) {
        service.delete(user(p),route.toString(),classroomId.toString(),event.toString());
    }
}
