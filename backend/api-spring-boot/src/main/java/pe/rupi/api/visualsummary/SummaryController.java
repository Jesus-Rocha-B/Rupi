package pe.rupi.api.visualsummary;
import java.security.Principal;import java.util.*;import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.shared.SessionController;import pe.rupi.api.studypackage.StudyPackageBuilder;
@RestController
public class SummaryController {
 private final SummaryService service;
 public SummaryController(SummaryService service){this.service=service;}
 private String user(Principal p){if(p==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return UUID.fromString(p.getName()).toString();}
 public record Generate(UUID requestId){}
 public record Report(String reason){}
 public record Review(boolean approve,String note){}
 private static final String BASE="/api/v1/student/learning-routes/{route}/nodes/{node}/visual-summary";
 @GetMapping(BASE) public SummaryService.Availability get(@PathVariable UUID route,@PathVariable UUID node,Principal p){return service.get(user(p),route.toString(),node.toString());}
 @PostMapping(BASE) public SummaryService.View generate(@PathVariable UUID route,@PathVariable UUID node,@RequestBody Generate body,Principal p,HttpServletRequest req){SessionController.checkOrigin(req);return service.generate(user(p),route.toString(),node.toString(),body.requestId());}
 @GetMapping(BASE+"/{id}") public SummaryService.View check(@PathVariable UUID route,@PathVariable UUID node,@PathVariable UUID id,Principal p){return service.check(user(p),route.toString(),node.toString(),id.toString());}
 @PostMapping(BASE+"/{id}/report") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void report(@PathVariable UUID route,@PathVariable UUID node,@PathVariable UUID id,@RequestBody Report body,Principal p,HttpServletRequest req){SessionController.checkOrigin(req);service.report(user(p),route.toString(),node.toString(),id.toString(),body.reason());}
 @GetMapping(BASE+"/{id}/package") public ResponseEntity<byte[]> download(@PathVariable UUID route,@PathVariable UUID node,@PathVariable UUID id,Principal p)throws Exception{
  var view=service.downloadable(user(p),route.toString(),node.toString(),id.toString());
  byte[] bytes=StudyPackageBuilder.visualSupplement(SummaryDocument.html(view),Map.of("routeId",route,"nodeId",node,"resource",view));
  return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
   .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename("rupi-resumen-"+id+".zip").build().toString())
   .header("X-Rupi-Paquete-Sha256",SummaryDocument.sha(bytes)).header("X-Rupi-Paquete-Version",view.version()).body(bytes);
 }
 @GetMapping("/api/v1/teacher/visual-summaries") public List<SummaryService.View> reviews(@RequestParam UUID classroomId,@RequestParam UUID routeId,Principal p){return service.reviews(user(p),classroomId.toString(),routeId.toString());}
 @PostMapping("/api/v1/teacher/visual-summaries/{id}/review") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void review(@PathVariable UUID id,@RequestParam UUID classroomId,@RequestParam UUID routeId,@RequestBody Review body,Principal p,HttpServletRequest req){SessionController.checkOrigin(req);service.review(user(p),classroomId.toString(),routeId.toString(),id.toString(),body.approve(),body.note());}
}
