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
    public ResultsReportController(ResultsReportService service,GradesService grades) {this.service=service;this.grades=grades;}
    private String teacher(Principal principal) {
        if(principal==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {return UUID.fromString(principal.getName()).toString();}
        catch(IllegalArgumentException ex) {throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
    }
    @GetMapping("/classrooms")
    public List<ResultsReportRepository.Classroom> classrooms(Principal principal) {return service.classrooms(teacher(principal));}
    @GetMapping("/classrooms/{room}/routes")
    public List<ResultsReportRepository.Route> routes(@PathVariable UUID room,Principal principal) {return service.routes(teacher(principal),room.toString());}
    @GetMapping
    public ResultsReportService.Report report(@RequestParam UUID classroomId,@RequestParam UUID routeId,Principal principal) {
        return service.report(teacher(principal),classroomId.toString(),routeId.toString());
    }
    @GetMapping("/grades")
    public GradesService.Report grades(@RequestParam UUID classroomId,@RequestParam UUID routeId,Principal principal) {
        return grades.report(teacher(principal),classroomId.toString(),routeId.toString());
    }
    @GetMapping(value="/export",produces="text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(@RequestParam UUID classroomId,@RequestParam UUID routeId,Principal principal) {
        var report=service.report(teacher(principal),classroomId.toString(),routeId.toString());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"rupi-resultados.csv\"")
            .body(ResultsReportService.csv(report).getBytes(StandardCharsets.UTF_8));
    }
}
