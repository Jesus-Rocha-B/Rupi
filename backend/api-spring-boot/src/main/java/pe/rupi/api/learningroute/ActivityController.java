package pe.rupi.api.learningroute;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.learningroute.LearningRouteDtos.ActivityCompleteResponse;

@RestController
@RequestMapping("/api/v1/student/learning-routes")
public class ActivityController {
    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @PostMapping("/{routeId}/nodes/{nodeId}/start")
    public ActivityService.Activity start(
            @PathVariable UUID routeId,
            @PathVariable UUID nodeId,
            Principal principal
    ) {
        UUID studentId = studentIdFrom(principal);
        return service.start(studentId, routeId, nodeId);
    }

    @PostMapping("/{routeId}/nodes/{nodeId}/complete")
    public ActivityCompleteResponse complete(
            @PathVariable UUID routeId,
            @PathVariable UUID nodeId,
            Principal principal
    ) {
        UUID studentId = studentIdFrom(principal);
        return service.complete(studentId, routeId, nodeId);
    }

    private static UUID studentIdFrom(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
