package pe.rupi.api.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static pe.rupi.api.report.ResultsReportRepository.*;
import static pe.rupi.api.report.GradeCalculator.*;

@Service
@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class GradesService {
    private final ResultsReportService access;
    private final ResultsReportRepository reports;
    private final GradesRepository grades;
    public GradesService(ResultsReportService access,ResultsReportRepository reports,GradesRepository grades) {
        this.access=access;this.reports=reports;this.grades=grades;
    }
    public record Unit(String id,String title) {}
    public record Detail(String attemptId,String activityId,String title,String date,BigDecimal earned,BigDecimal possible,BigDecimal grade) {}
    public record Cell(String unitId,Average average,int totalActivities,List<Detail> details) {}
    public record Row(String studentId,String studentName,List<Cell> cells) {}
    public record Report(Classroom classroom,Route route,String generatedAt,List<Unit> units,List<Row> students,int unassignedNodes) {}
    public Report report(String teacher,String room,String route) {return report(teacher,room,route,null);}
    public Report report(String teacher,String room,String route,String student) {
        var selection=access.authorize(teacher,room,route);
        if(student!=null) access.authorizeStudent(room,student);
        return summarize(selection.classroom(),selection.route(),reports.data(room,route,student),grades.attempts(room,route,student));
    }
    static String csv(Report report) {
        var out=new StringBuilder("\ufeffAula,Ruta,Version,Fecha UTC,Estudiante,Unidad,Promedio sobre 20,Letra orientativa Rupi,Actividades calificadas,Actividades totales\r\n");
        for(var student:report.students()) for(var cell:student.cells()) {
            var unit=report.units().stream().filter(u->u.id().equals(cell.unitId())).findFirst().orElseThrow();
            var values=List.of(report.classroom().name(),report.route().title(),""+report.route().version(),report.generatedAt(),
                student.studentName(),unit.title(),cell.average()==null?"":cell.average().value().toPlainString(),
                cell.average()==null?"":cell.average().letter(),""+cell.details().size(),""+cell.totalActivities());
            out.append(values.stream().map(ResultsReportService::cell).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
        }
        return out.toString();
    }
    static Report summarize(Classroom classroom,Route route,Data data,List<GradesRepository.Attempt> attempts) {
        Map<String,Unit> units=new LinkedHashMap<>();
        Map<String,Set<String>> activities=new LinkedHashMap<>();
        for(var node:data.nodes()) if(node.unit()!=null) {
            units.putIfAbsent(node.unit(),new Unit(node.unit(),node.unitTitle()));
            activities.computeIfAbsent(node.unit(),k->new LinkedHashSet<>()).add(node.activity());
        }
        // El repositorio ordena por última respuesta, inicio e ID; conservar el primer intento completo válido.
        Map<String,Map<String,GradesRepository.Attempt>> latest=new HashMap<>();
        for(var attempt:attempts) if(attempt.eligible()) latest.computeIfAbsent(attempt.student(),k->new HashMap<>())
            .putIfAbsent(attempt.activity(),attempt);
        var rows=new ArrayList<Row>();
        for(var student:data.students()) {
            var cells=new ArrayList<Cell>();
            for(var unit:units.values()) {
                var details=new ArrayList<Detail>();var scores=new ArrayList<Score>();
                for(var activity:activities.get(unit.id())) {
                    var attempt=student.enrollment()==null?null:latest.getOrDefault(student.id(),Map.of()).get(activity);
                    if(attempt==null) continue;
                    var score=new Score(attempt.earned(),attempt.possible());scores.add(score);
                    details.add(new Detail(attempt.id(),activity,attempt.title(),attempt.date(),attempt.earned(),attempt.possible(),average(List.of(score)).value()));
                }
                cells.add(new Cell(unit.id(),average(scores),activities.get(unit.id()).size(),List.copyOf(details)));
            }
            rows.add(new Row(student.id(),student.name(),List.copyOf(cells)));
        }
        return new Report(classroom,route,Instant.now().toString(),List.copyOf(units.values()),List.copyOf(rows),
            (int)data.nodes().stream().filter(n->n.unit()==null).count());
    }
}
