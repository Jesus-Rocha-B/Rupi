package pe.rupi.api.report;

import java.security.Principal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/teacher/results-reports")
public class ResultsReportController {
    private final ResultsReportService service;
    private final GradesService grades;
    private final StudentReportService students;
    public ResultsReportController(ResultsReportService service,GradesService grades,StudentReportService students) {this.service=service;this.grades=grades;this.students=students;}
    private String teacher(Principal principal) {
        if(principal==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {return UUID.fromString(principal.getName()).toString();}
        catch(IllegalArgumentException ex) {throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
    }
    @GetMapping("/classrooms")
    public List<ResultsReportRepository.Classroom> classrooms(Principal principal) {return service.classrooms(teacher(principal));}
    @GetMapping("/classrooms/{room}/routes")
    public List<ResultsReportRepository.Route> routes(@PathVariable UUID room,Principal principal) {return service.routes(teacher(principal),room.toString());}
    @GetMapping("/classrooms/{room}/students")
    public List<ResultsReportRepository.Member> students(@PathVariable UUID room,Principal principal) {return service.members(teacher(principal),room.toString());}
    @GetMapping("/student-overview")
    public StudentReportService.Overview overview(@RequestParam UUID classroomId,@RequestParam UUID routeId,@RequestParam UUID studentId,Principal principal) {
        return students.overview(teacher(principal),classroomId.toString(),routeId.toString(),studentId.toString());
    }
    @GetMapping
    public ResultsReportService.Report report(@RequestParam UUID classroomId,@RequestParam UUID routeId,@RequestParam(required=false) UUID studentId,Principal principal) {
        return service.report(teacher(principal),classroomId.toString(),routeId.toString(),studentId==null?null:studentId.toString());
    }
    @GetMapping("/grades")
    public GradesService.Report grades(@RequestParam UUID classroomId,@RequestParam UUID routeId,@RequestParam(required=false) UUID studentId,Principal principal) {
        return grades.report(teacher(principal),classroomId.toString(),routeId.toString(),studentId==null?null:studentId.toString());
    }
    @GetMapping(value="/export",produces="text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(@RequestParam UUID classroomId,@RequestParam UUID routeId,@RequestParam(required=false) UUID studentId,Principal principal) {
        var report=service.report(teacher(principal),classroomId.toString(),routeId.toString(),studentId==null?null:studentId.toString());
        if(report.units().stream().flatMap(u->u.students().stream()).noneMatch(r->r.answered()>0||r.completedNodes()>0)) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"rupi-resultados.csv\"")
            .body(ResultsReportService.csv(report).getBytes(StandardCharsets.UTF_8));
    }
    @GetMapping(value="/grades/export",produces="text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportGrades(@RequestParam UUID classroomId,@RequestParam UUID routeId,@RequestParam(required=false) UUID studentId,Principal principal) {
        var report=grades.report(teacher(principal),classroomId.toString(),routeId.toString(),studentId==null?null:studentId.toString());
        if(report.students().stream().flatMap(s->s.cells().stream()).noneMatch(c->c.average()!=null)) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"rupi-calificaciones.csv\"")
            .body(GradesService.csv(report).getBytes(StandardCharsets.UTF_8));
    }

}
