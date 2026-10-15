package pe.rupi.api.report;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import pe.rupi.api.studypackage.PrimaryStudyCatalog;
import static pe.rupi.api.report.ResultsReportRepository.*;

@Service
@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class StudentReportService {
    private final ResultsReportService access;
    private final ResultsReportRepository reports;
    private final GradesRepository grades;
    private final StudentReportRepository links;
    public StudentReportService(ResultsReportService access,ResultsReportRepository reports,GradesRepository grades,StudentReportRepository links) {
        this.access=access;this.reports=reports;this.grades=grades;this.links=links;
    }
    public record Resource(String id,String nodeTitle,String title,String url,String linkedAt,String completedAt) {}
    public record Activity(String id,String title,int answered,int correct) {}
    public record Unit(String id,String title,String status,GradeCalculator.Average average) {}
    /** Parada del roadmap con su nota del último intento válido (escala 0-20, orientativa). */
    public record Stop(String id,int sequence,String title,String unitId,String unitTitle,boolean optional,String state,String completedAt,
        GradeCalculator.Average grade,String gradedAt) {}
    public record Overview(Member student,Classroom classroom,Route route,String enrollmentState,int completedNodes,int totalNodes,
        int answered,int totalQuestions,int correct,List<Unit> units,List<Activity> activities,List<Resource> reinforcements,
        List<Stop> roadmap,GradeCalculator.Average overallAverage) {}
    public Overview overview(String teacher,String room,String route,String student) {
        var scope=access.authorize(teacher,room,route);
        var member=access.authorizeStudent(room,student);
        var data=reports.data(room,route,student);
        var attempts=grades.attempts(room,route,student);
        var results=ResultsReportService.summarize(data);
        var gradeReport=GradesService.summarize(scope.classroom(),scope.route(),data,attempts);
        var gradeCells=gradeReport.students().stream().flatMap(s->s.cells().stream()).toList();
        var units=results.stream().map(u->new Unit(u.id(),u.title(),u.students().getFirst().status(),
            gradeCells.stream().filter(c->c.unitId().equals(u.id())).map(GradesService.Cell::average).filter(Objects::nonNull).findFirst().orElse(null))).toList();
        Map<String,Answer> evidence=new LinkedHashMap<>();data.answers().forEach(a->evidence.put(a.question(),a));
        var activities=new ArrayList<Activity>();
        for(var activity:evidence.values().stream().map(Answer::activity).distinct().toList()) {
            var answers=evidence.values().stream().filter(a->a.activity().equals(activity)).toList();
            String title=attempts.stream().filter(a->a.activity().equals(activity)).map(GradesRepository.Attempt::title).findFirst().orElse("Actividad con respuestas");
            activities.add(new Activity(activity,title,answers.size(),(int)answers.stream().filter(Answer::correct).count()));
        }
        var resources=links.reinforcements(room,route,student).stream().map(link->{
            var entry=PrimaryStudyCatalog.forGradeAndArea(link.grade(),link.area()).stream()
                .filter(e->UUID.nameUUIDFromBytes(e.sourceUrl().getBytes(StandardCharsets.UTF_8)).toString().equals(link.resource())).findFirst();
            return new Resource(link.id(),link.title(),entry.map(PrimaryStudyCatalog.Entry::title).orElse("Recurso fuera del catálogo actual"),
                entry.map(PrimaryStudyCatalog.Entry::sourceUrl).orElse(null),link.linkedAt(),link.completedAt());
        }).toList();
        // El último intento válido de cada actividad (los intentos vienen del más reciente al más antiguo).
        Map<String,GradesRepository.Attempt> latest=new HashMap<>();
        for(var attempt:attempts) if(attempt.eligible()) latest.putIfAbsent(attempt.activity(),attempt);
        boolean enrolled=data.students().getFirst().enrollment()!=null;
        var roadmap=links.roadmap(route,student).stream().map(stop->{
            var attempt=enrolled?latest.get(stop.activity()):null;
            var grade=attempt==null?null:GradeCalculator.average(List.of(new GradeCalculator.Score(attempt.earned(),attempt.possible())));
            return new Stop(stop.id(),stop.sequence(),stop.title(),stop.unitId(),stop.unitTitle(),stop.optional(),stop.state(),stop.completedAt(),
                grade,attempt==null?null:attempt.date());
        }).toList();
        // Promedio de todo el avance: una nota por actividad calificada, aunque se repita en varias paradas.
        var overall=enrolled?GradeCalculator.average(latest.values().stream().map(a->new GradeCalculator.Score(a.earned(),a.possible())).toList()):null;
        return new Overview(member,scope.classroom(),scope.route(),data.students().getFirst().enrollmentState(),
            (int)data.completed().stream().map(Progress::node).distinct().count(),data.nodes().size(),evidence.size(),
            (int)data.questions().stream().map(Question::id).distinct().count(),(int)evidence.values().stream().filter(Answer::correct).count(),
            units,List.copyOf(activities),resources,roadmap,overall);
    }
}
