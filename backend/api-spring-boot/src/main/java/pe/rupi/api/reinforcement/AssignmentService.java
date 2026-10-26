package pe.rupi.api.reinforcement;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import pe.rupi.api.report.ResultsReportRepository;
import pe.rupi.api.report.ResultsReportService;
import pe.rupi.api.studypackage.PrimaryStudyCatalog;
import static pe.rupi.api.reinforcement.AssignmentRepository.*;

@Service
public class AssignmentService {
    private final AssignmentRepository repo;
    private final ResultsReportService access;
    private final ResultsReportRepository reports;
    private final ReinforcementRepository reinforcement;
    public AssignmentService(AssignmentRepository repo,ResultsReportService access,ResultsReportRepository reports,ReinforcementRepository reinforcement) {
        this.repo=repo;this.access=access;this.reports=reports;this.reinforcement=reinforcement;
    }
    public record Resource(String id,String title,String url) {}
    public record Candidate(String studentId,String studentName,String nodeId,String nodeTitle,String unitTitle,
        String status,int answered,int correct,List<Resource> resources) {}
    public record Board(boolean available,List<Candidate> candidates,List<Assignment> assignments) {}
    public record StudentBoard(boolean available,List<Assignment> assignments) {}
    public record Request(UUID requestId,UUID nodeId,UUID resourceId,List<UUID> students,String title,String instructions,String reason) {}
    public record Assigned(int created,int existing) {}
    private static ResponseStatusException failure(HttpStatus status,String text) { return new ResponseStatusException(status,text); }
    private void authorize(String teacher,String room,String route) {
        if(access.routes(teacher,room).stream().noneMatch(r->r.id().equals(route)))
            throw failure(HttpStatus.NOT_FOUND,"Ruta no disponible para esta aula.");
    }
    public Board board(String teacher,String room,String route) {
        authorize(teacher,room,route);
        var data=reports.data(room,route);
        var nodes=repo.nodes(route);
        List<Candidate> candidates=new ArrayList<>();
        for(var s:data.students()) for(var n:data.nodes()) {
            var node=nodes.stream().filter(x->x.id().equals(n.id())).findFirst();
            if(node.isEmpty()) continue;
            var evidence=data.answers().stream().filter(a->a.student().equals(s.id())&&a.activity().equals(n.activity()))
                .map(a->new ReinforcementRepository.Evidence(a.question(),a.correct(),null)).toList();
            boolean enrolled=s.enrollment()!=null && Set.of("ACTIVO","COMPLETADO").contains(s.enrollmentState());
            String status=!enrolled?"SIN_INSCRIPCION":evidence.isEmpty()?"SIN_RESPUESTAS":evidence.size()<2?"EVIDENCIA_INSUFICIENTE":
                ReinforcementService.needsPractice(evidence)?"REQUIERE_ATENCION":"EN_AVANCE";
            var resources=PrimaryStudyCatalog.forGradeAndArea(node.get().grade(),node.get().area()).stream()
                .map(e->new Resource(ReinforcementService.resourceId(e),e.title(),e.sourceUrl())).toList();
            candidates.add(new Candidate(s.id(),s.name(),n.id(),node.get().title(),n.unitTitle(),status,
                enrolled?evidence.size():0,enrolled?(int)evidence.stream().filter(ReinforcementRepository.Evidence::correct).count():0,resources));
        }
        boolean available=repo.available();
        return new Board(available,List.copyOf(candidates),available?repo.teacher(room,route):List.of());
    }
    static String required(String text,int max,String field) {
        if(text==null||text.isBlank()||text.strip().length()>max) throw failure(HttpStatus.BAD_REQUEST,"Revisa "+field+".");
        return text.strip();
    }
    @Transactional
    public Assigned assign(String teacher,String room,String route,Request request) {
        if(request==null||request.requestId()==null||request.nodeId()==null||request.resourceId()==null
            ||request.students()==null||request.students().isEmpty()||request.students().size()>50||request.students().stream().anyMatch(Objects::isNull))
            throw failure(HttpStatus.BAD_REQUEST,"Selecciona entre 1 y 50 estudiantes y una actividad.");
        String title=required(request.title(),180,"el título"),instructions=required(request.instructions(),2000,"las instrucciones"),
            reason=required(request.reason(),1000,"el motivo");
        var board=board(teacher,room,route);
        if(!board.available()) throw failure(HttpStatus.SERVICE_UNAVAILABLE,"Refuerzos pendientes de habilitación por el administrador.");
        var members=access.members(teacher,room);
        var selected=new LinkedHashSet<>(request.students());
        var candidates=new ArrayList<Candidate>();
        // Validate the entire batch before any insert; the transaction rolls back on failure.
        for(var student:selected) {
            if(members.stream().noneMatch(m->m.id().equals(student.toString()))) throw failure(HttpStatus.NOT_FOUND,"Estudiante fuera del aula.");
            var candidate=board.candidates().stream().filter(c->c.studentId().equals(student.toString())&&c.nodeId().equals(request.nodeId().toString()))
                .findFirst().orElseThrow(()->failure(HttpStatus.NOT_FOUND,"Parada no disponible."));
            if(!candidate.status().equals("REQUIERE_ATENCION")) throw failure(HttpStatus.CONFLICT,"Actualiza el reporte: falta evidencia de dificultad para algún estudiante.");
            if(candidate.resources().stream().noneMatch(r->r.id().equals(request.resourceId().toString())))
                throw failure(HttpStatus.BAD_REQUEST,"El recurso no corresponde al grado y área.");
            if(reinforcement.context(student,UUID.fromString(route),request.nodeId()).isEmpty())
                throw failure(HttpStatus.CONFLICT,"La parada no está disponible para algún estudiante.");
            candidates.add(candidate);
        }
        int created=0;
        for(var c:candidates) {
            var resource=c.resources().stream().filter(r->r.id().equals(request.resourceId().toString())).findFirst().orElseThrow();
            var p=new MapSqlParameterSource("id",UUID.randomUUID().toString()).addValue("request",request.requestId().toString())
                .addValue("room",room).addValue("teacher",teacher).addValue("student",c.studentId()).addValue("route",route)
                .addValue("node",c.nodeId()).addValue("resource",resource.id()).addValue("title",title).addValue("instructions",instructions)
                .addValue("reason",reason).addValue("resourceTitle",resource.title()).addValue("url",resource.url())
                .addValue("nodeTitle",c.nodeTitle()).addValue("answered",c.answered()).addValue("correct",c.correct());
            if(repo.insert(p)) created++;
        }
        return new Assigned(created,candidates.size()-created);
    }
    public StudentBoard student(String student) {
        boolean available=repo.available();
        return new StudentBoard(available,available?repo.student(student):List.of());
    }
    private Assignment own(String student,String id) {
        return student(student).assignments().stream().filter(a->a.id().equals(id)).findFirst()
            .orElseThrow(()->failure(HttpStatus.NOT_FOUND,"Refuerzo no disponible."));
    }
    @Transactional
    public void start(String student,String id) { own(student,id);repo.start(student,id); }
    @Transactional
    public void complete(String student,String id,String reflection) {
        var task=own(student,id);
        if(task.completedAt()!=null) return;
        if(task.startedAt()==null) throw failure(HttpStatus.CONFLICT,"Primero comienza tu práctica.");
        String text=required(reflection,1000,"lo que practicaste");
        if(text.length()<10) throw failure(HttpStatus.BAD_REQUEST,"Cuéntanos un poco más sobre lo que practicaste (10 caracteres como mínimo).");
        repo.complete(student,id,text);
    }
}
