package pe.rupi.api.teacher;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.shared.StudentSessionService;

@RestController
@RequestMapping("/api/v1/teacher/learning-routes")
public class TeacherRouteController {

    private final TeacherRouteService service;

    public TeacherRouteController(TeacherRouteService service) {
        this.service = service;
    }

    private StudentSessionService.Student requireTeacher(HttpServletRequest request) {
        var user = (StudentSessionService.Student) request.getAttribute("teacher");
        if (user == null || !"DOCENTE".equalsIgnoreCase(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso restringido para profesores.");
        }
        return user;
    }

    @GetMapping
    public List<TeacherDtos.TeacherRouteSummary> listRoutes(HttpServletRequest request, Principal principal) {
        var teacher = requireTeacher(request);
        String teacherId = principal != null ? principal.getName() : teacher.id();
        return service.listRoutes(teacherId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherDtos.CreateRouteResponse createRoute(
            @RequestBody TeacherDtos.CreateRouteRequest body,
            HttpServletRequest request,
            Principal principal
    ) {
        var teacher = requireTeacher(request);
        String teacherId = principal != null ? principal.getName() : teacher.id();
        return service.createRoute(teacherId, body);
    }

    @GetMapping("/{routeId}")
    public TeacherDtos.TeacherRouteDetail getRouteDetail(
            @PathVariable("routeId") String routeId,
            HttpServletRequest request,
            Principal principal
    ) {
        var teacher = requireTeacher(request);
        String teacherId = principal != null ? principal.getName() : teacher.id();
        return service.getRouteDetail(teacherId, routeId);
    }

    @PostMapping("/{routeId}/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> addNode(
            @PathVariable("routeId") String routeId,
            @RequestBody TeacherDtos.CreateNodeRequest body,
            HttpServletRequest request,
            Principal principal
    ) {
        var teacher = requireTeacher(request);
        String teacherId = principal != null ? principal.getName() : teacher.id();
        String nodeId = service.addNode(teacherId, routeId, body);
        return Map.of("nodeId", nodeId, "message", "Parada pedagógica agregada exitosamente.");
    }

    @PostMapping("/{routeId}/nodes/{nodeId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> attachResource(
            @PathVariable("routeId") String routeId,
            @PathVariable("nodeId") String nodeId,
            @RequestBody TeacherDtos.AttachResourceRequest body,
            HttpServletRequest request,
            Principal principal
    ) {
        var teacher = requireTeacher(request);
        String teacherId = principal != null ? principal.getName() : teacher.id();
        String resourceId = service.attachResource(teacherId, nodeId, body);
        return Map.of("resourceId", resourceId, "message", "Material educativo enlazado exitosamente al nodo.");
    }

    @PostMapping("/{routeId}/publish")
    public TeacherDtos.PublishRouteResponse publishRoute(
            @PathVariable("routeId") String routeId,
            HttpServletRequest request,
            Principal principal
    ) {
        var teacher = requireTeacher(request);
        String teacherId = principal != null ? principal.getName() : teacher.id();
        return service.publishRoute(teacherId, routeId);
    }

    /** Historia 8: pausa la visibilidad; los alumnos dejan de ver y abrir la ruta hasta que se vuelva a publicar. */
    @PostMapping("/{routeId}/pause")
    public TeacherDtos.PublishRouteResponse pauseRoute(
            @PathVariable("routeId") String routeId,
            HttpServletRequest request,
            Principal principal
    ) {
        var teacher = requireTeacher(request);
        String teacherId = principal != null ? principal.getName() : teacher.id();
        return service.pauseRoute(teacherId, routeId);
    }

    @PutMapping("/{routeId}/nodes/reorder")
    public TeacherDtos.TeacherRouteDetail reorderNodes(
            @PathVariable("routeId") String routeId,
            @RequestBody TeacherDtos.ReorderNodesRequest body,
            HttpServletRequest request,
            Principal principal
    ) {
        var teacher = requireTeacher(request);
        String teacherId = principal != null ? principal.getName() : teacher.id();
        return service.reorderNodes(teacherId, routeId, body);
    }
}
