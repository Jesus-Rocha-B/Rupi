package pe.rupi.api.reminder;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.learningroute.LearningRouteService;

@Service
public class ReminderService {
    private final ReminderRepository repository;
    private final LearningRouteService routes;
    private final Clock clock;
    @Autowired
    public ReminderService(ReminderRepository repository, LearningRouteService routes) {
        this(repository,routes,Clock.systemUTC());
    }
    ReminderService(ReminderRepository repository, LearningRouteService routes, Clock clock) {
        this.repository=repository;this.routes=routes;this.clock=clock;
    }
    public List<ReminderRepository.Reminder> list(UUID student, UUID route) {
        routes.getForStudent(student,route);
        return repository.visible(student.toString(),route.toString(),clock.instant());
    }
    public void read(UUID student, UUID route, UUID reminder) {
        if (list(student,route).stream().noneMatch(r->r.id().equals(reminder.toString())))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Este recordatorio ya no está disponible.");
        repository.markRead(student.toString(),reminder.toString(),clock.instant());
    }
}
