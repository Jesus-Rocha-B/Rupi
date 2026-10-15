package pe.rupi.api.report;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static pe.rupi.api.report.ResultsReportRepository.*;

@Service
@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class ResultsReportService {
    private final ResultsReportRepository repo;
    public ResultsReportService(ResultsReportRepository repo) {this.repo=repo;}
    public record Row(String studentId,String studentName,String enrollmentState,int completedNodes,int totalNodes,
                      int answered,int totalQuestions,int correct,Double accuracy,String status) {}
    public record Unit(String id,String title,List<Competency> competencies,int attentionCount,List<Row> students) {}
    public record Report(Classroom classroom,Route route,String generatedAt,int studentCount,List<Unit> units) {}
    public List<Classroom> classrooms(String teacher) {return repo.classrooms(teacher);}
    private Classroom classroom(String teacher,String room) {
        return repo.classrooms(teacher).stream().filter(c->c.id().equals(room)).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public List<Route> routes(String teacher,String room) {classroom(teacher,room);return repo.routes(room);}
    public List<Member> members(String teacher,String room) {classroom(teacher,room);return repo.members(room);}
    Member authorizeStudent(String room,String student) {
        return repo.members(room).stream().filter(m->m.id().equals(student)).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    record Selection(Classroom classroom, Route route) {}
    Selection authorize(String teacher,String room,String route) {
        var classroom=classroom(teacher,room);
        var selected=repo.routes(room).stream().filter(r->r.id().equals(route)).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new Selection(classroom,selected);
    }
    public Report report(String teacher,String room,String route) {return report(teacher,room,route,null);}
    public Report report(String teacher,String room,String route,String student) {
        var selection=authorize(teacher,room,route);
        var classroom=selection.classroom();
        var selected=selection.route();
        if(student!=null) authorizeStudent(room,student);
        var data=repo.data(room,route,student);
        return new Report(classroom,selected,Instant.now().toString(),data.students().size(),summarize(data));
    }
    static List<Unit> summarize(Data data) {
        Map<String,List<Node>> groups=new LinkedHashMap<>();
        for(var node:data.nodes()) groups.computeIfAbsent(node.unit()==null?"SIN_UNIDAD":node.unit(),k->new ArrayList<>()).add(node);
        var units=new ArrayList<Unit>();
        for(var group:groups.entrySet()) {
            var activities=new HashSet<String>();var nodeIds=new HashSet<String>();
            group.getValue().forEach(n->{activities.add(n.activity());nodeIds.add(n.id());});
            int totalQuestions=(int)data.questions().stream().filter(q->activities.contains(q.activity())).map(Question::id).distinct().count();
            var rows=new ArrayList<Row>();
            for(var student:data.students()) {
                var evidence=new LinkedHashMap<String,Answer>();
                if(student.enrollment()!=null) data.answers().stream()
                    .filter(a->a.student().equals(student.id()) && activities.contains(a.activity()))
                    .forEach(a->evidence.put(a.question(),a));
                int answered=evidence.size(),correct=(int)evidence.values().stream().filter(Answer::correct).count();
                int completed=student.enrollment()==null?0:(int)data.completed().stream()
                    .filter(p->p.student().equals(student.id()) && nodeIds.contains(p.node())).map(Progress::node).distinct().count();
                String status=student.enrollment()==null?"SIN_INSCRIPCION":answered==0?"SIN_RESPUESTAS":answered<2?
                    "EVIDENCIA_INSUFICIENTE":correct*100L<answered*70L?"REQUIERE_ATENCION":"EN_AVANCE";
                rows.add(new Row(student.id(),student.name(),student.enrollmentState(),completed,nodeIds.size(),answered,totalQuestions,
                    correct,answered==0?null:Math.round(correct*1000.0/answered)/10.0,status));
            }
            var competencies=data.competencies().stream().filter(c->c.unit().equals(group.getKey())).toList();
            units.add(new Unit(group.getKey(),group.getValue().getFirst().unitTitle(),competencies,
                (int)rows.stream().filter(r->r.status().equals("REQUIERE_ATENCION")).count(),List.copyOf(rows)));
        }
        return List.copyOf(units);
    }
    static String cell(String value) {
        String text=value==null?"":value;
        // Evitar que Excel interprete nombres o títulos como fórmulas al abrir el CSV.
        String stripped=text.stripLeading();
        if(!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0))>=0 || text.startsWith("\t") || text.startsWith("\r") || text.startsWith("\n")) text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }
    static String csv(Report report) {
        var out=new StringBuilder("\ufeff");
        out.append("Aula,Ruta,Version,Fecha UTC,Estudiante,Unidad,Inscripcion,Paradas completadas,Paradas totales,Respondidas,Preguntas disponibles,Aciertos,Porcentaje,Estado,Competencias asociadas (evidencia indirecta)\r\n");
        for(var unit:report.units()) for(var row:unit.students()) {
            String competencies=unit.competencies().stream().map(c->c.code()+": "+c.name()).collect(java.util.stream.Collectors.joining(" | "));
            var values=List.of(report.classroom().name(),report.route().title(),""+report.route().version(),report.generatedAt(),row.studentName(),unit.title(),
                row.enrollmentState()==null?"SIN_INSCRIPCION":row.enrollmentState(),""+row.completedNodes(),""+row.totalNodes(),""+row.answered(),""+row.totalQuestions(),""+row.correct(),
                row.accuracy()==null?"":row.accuracy().toString(),row.status(),competencies);
            out.append(values.stream().map(ResultsReportService::cell).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
        }
        return out.toString();
    }
}
