package pe.rupi.api.learningroute;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.learningroute.LearningRouteDtos.RouteDetailResponse;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteListResponse;

@RestController
@RequestMapping("/api/v1/student/learning-routes")
public class LearningRouteController {
    private final LearningRouteService service;
    private final ActivityService activityService;

    public LearningRouteController(LearningRouteService service, ActivityService activityService) {
        this.service = service;
        this.activityService = activityService;
    }

    @GetMapping
    public RouteListResponse list(Principal principal) {
        UUID studentId = studentIdFrom(principal);
        return new RouteListResponse(service.listForStudent(studentId));
    }

    @GetMapping("/{versionRouteId}")
    public RouteDetailResponse get(@PathVariable UUID versionRouteId, Principal principal) {
        UUID studentId = studentIdFrom(principal);
        return service.getForStudent(studentId, versionRouteId);
    }

    @GetMapping("/{versionRouteId}/diploma")
    public LearningRouteDtos.RouteDiplomaResponse getDiploma(@PathVariable UUID versionRouteId, Principal principal) {
        UUID studentId = studentIdFrom(principal);
        return activityService.getRouteDiploma(studentId, versionRouteId);
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
