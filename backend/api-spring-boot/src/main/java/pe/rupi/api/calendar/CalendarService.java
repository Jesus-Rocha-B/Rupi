package pe.rupi.api.calendar;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.learningroute.LearningRouteService;
import static pe.rupi.api.calendar.CalendarDtos.*;
@Service
public class CalendarService {
    private final CalendarRepository repository;
    private final LearningRouteService routes;
    public CalendarService(CalendarRepository repository, LearningRouteService routes) {this.repository=repository;this.routes=routes;}
    public Schedule read(String user, String route, boolean teacher) {
        if (teacher) requireManager(user,route); else routes.getForStudent(UUID.fromString(user),UUID.fromString(route));
        var rooms=repository.classrooms(user,route,teacher);
        return new Schedule(rooms,teacher ? repository.targets(route) : java.util.List.of(),repository.events(route,rooms));
    }
    @Transactional
    public Schedule save(String user, String route, Dates dates) {
        requireManager(user,route);
        validate(dates);
        requireClassroom(user,route,dates.classroomId());
        var target=repository.targets(route).stream().filter(t->t.id().equals(dates.targetId()) && t.type().equals(dates.type()))
            .findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona una unidad o evaluación de esta ruta."));
        repository.save(user,route,dates,target);
        return read(user,route,true);
    }
    @Transactional
    public void delete(String user,String route,String classroom,String event) {
        requireManager(user,route);requireClassroom(user,route,classroom);
        if (repository.delete(route,classroom,event)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"La fecha ya no existe en esta aula.");
    }
    /**
     * HU-13 habla del «roadmap institucional»: además del autor, puede calendarizar el docente con un aula activa del
     * grado de la ruta, si la ruta está publicada (p. ej., las rutas oficiales de RUPI). Las fechas siguen siendo
     * por aula: cada docente solo guarda o retira eventos de sus propias aulas (requireClassroom).
     */
    private void requireManager(String user,String route) {
        boolean manages = repository.owns(user,route)
                || (repository.isPublished(route) && !repository.classrooms(user,route,true).isEmpty());
        if(!manages) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No puedes gestionar el calendario de esta ruta.");
    }
    private void requireClassroom(String user,String route,String room) {
        if(repository.classrooms(user,route,true).stream().noneMatch(a->a.id().equals(room)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Selecciona un aula de tu responsabilidad y del grado de la ruta.");
    }
    static void validate(Dates d) {
        if(d==null || d.classroomId()==null || d.targetId()==null || !("UNIDAD".equals(d.type()) || "EVALUACION".equals(d.type()))
           || d.start()==null || d.end()==null || d.end().isBefore(d.start())
           || d.start().getYear()<2000 || d.end().getYear()>2100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Completa las fechas; el final debe ser igual o posterior al inicio (años 2000 a 2100).");
    }
}
