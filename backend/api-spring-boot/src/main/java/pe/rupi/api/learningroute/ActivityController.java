package pe.rupi.api.learningroute;

import java.security.Principal;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import pe.rupi.api.shared.SessionController;

@RestController
@RequestMapping("/api/v1/student/learning-routes")
public class ActivityController {
    private final ActivityService service;
    public ActivityController(ActivityService service) { this.service = service; }
    @PostMapping("/{routeId}/nodes/{nodeId}/start")
    public ActivityService.Activity start(@PathVariable UUID routeId, @PathVariable UUID nodeId,
            Principal principal, HttpServletRequest request) {
        SessionController.checkOrigin(request);
        return service.start(UUID.fromString(principal.getName()), routeId, nodeId);
    }
}
