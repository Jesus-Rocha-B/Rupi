package pe.rupi.api.tutor;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * HU-15: el docente decide si el tutor Rupi de su aula guía con pistas (método socrático, el comportamiento
 * anterior) o explica directamente el procedimiento. El estudiante recibe el modo del aula de su ruta.
 */
@Service
public class TutorSupportService {
    public static final String GUIDED="PISTAS_GUIADAS";
    public static final String DIRECT="EXPLICACION_DIRECTA";
    static final Set<String> MODES=Set.of(GUIDED,DIRECT);

    public record ClassroomSupport(String classroomId,String classroomName,int grade,String mode,
                                   boolean configured,Instant updatedAt,String updatedBy) {}
    /** source: AULA cuando hay un aula, PREDETERMINADO cuando el estudiante no tiene aula del grado de la ruta. */
    public record StudentSupport(String mode,String classroomName,String source) {}

    private final TutorSupportRepository repo;
    public TutorSupportService(TutorSupportRepository repo) { this.repo=repo; }

    private TutorSupportRepository.Classroom own(String teacher,String room) {
        return repo.teacherClassroom(teacher,room)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No encontramos esa aula entre las tuyas."));
    }

    private ClassroomSupport view(TutorSupportRepository.Classroom room) {
        var setting=repo.setting(room.id());
        return new ClassroomSupport(room.id(),room.name(),room.grade(),
            setting.map(TutorSupportRepository.Setting::mode).orElse(GUIDED),setting.isPresent(),
            setting.map(TutorSupportRepository.Setting::updatedAt).orElse(null),
            setting.map(TutorSupportRepository.Setting::updatedBy).orElse(null));
    }

    public ClassroomSupport classroom(String teacher,String room) { return view(own(teacher,room)); }

    public ClassroomSupport save(String teacher,String room,String mode) {
        String normalized=mode==null?"":mode.trim().toUpperCase(Locale.ROOT);
        if(!MODES.contains(normalized))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Elige «Pistas guiadas» o «Explicación directa».");
        var classroom=own(teacher,room);
        repo.save(classroom.id(),normalized,teacher);
        return view(classroom);
    }

    /**
     * Si el estudiante está en varias aulas del mismo grado y alguna pide pistas guiadas, se usan pistas:
     * ante la duda se conserva la ayuda que menos resuelve por el niño.
     */
    public StudentSupport student(String user,String route) {
        var rooms=repo.routeClassrooms(user,route);
        if(rooms.isEmpty()) return new StudentSupport(GUIDED,null,"PREDETERMINADO");
        TutorSupportRepository.Classroom chosen=null;String mode=null;
        for(var room:rooms) {
            String current=repo.setting(room.id()).map(TutorSupportRepository.Setting::mode).orElse(GUIDED);
            if(chosen==null||(GUIDED.equals(current)&&!GUIDED.equals(mode))) { chosen=room;mode=current; }
        }
        return new StudentSupport(mode,chosen.name(),"AULA");
    }
}
