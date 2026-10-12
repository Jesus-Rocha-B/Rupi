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

    @org.springframework.web.bind.annotation.PostMapping("/{versionRouteId}/enroll")
    public RouteDetailResponse enroll(@PathVariable UUID versionRouteId, Principal principal) {
        UUID studentId = studentIdFrom(principal);
        return service.enroll(studentId, versionRouteId);
    }

    @GetMapping("/search")
    public LearningRouteDtos.SearchAutocompleteResponse search(
            @org.springframework.web.bind.annotation.RequestParam(value = "q", required = false, defaultValue = "") String query,
            @org.springframework.web.bind.annotation.RequestParam(value = "grade", required = false) Integer grade,
            @org.springframework.web.bind.annotation.RequestParam(value = "grades", required = false) List<Integer> grades,
            @org.springframework.web.bind.annotation.RequestParam(value = "area", required = false) String area,
            @org.springframework.web.bind.annotation.RequestParam(value = "areas", required = false) List<String> areas,
            @org.springframework.web.bind.annotation.RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            Principal principal
    ) {
        UUID studentId = studentIdFrom(principal);
        List<Integer> combinedGrades = new java.util.ArrayList<>();
        if (grades != null) {
            combinedGrades.addAll(grades);
        }
        if (grade != null && !combinedGrades.contains(grade)) {
            combinedGrades.add(grade);
        }

        List<String> combinedAreas = new java.util.ArrayList<>();
        if (areas != null) {
            combinedAreas.addAll(areas);
        }
        if (area != null && !area.isBlank() && !combinedAreas.contains(area.trim())) {
            combinedAreas.add(area.trim());
        }

        return service.searchAlternativeRoutes(studentId, query, combinedGrades, combinedAreas, limit);
    }

    @GetMapping("/suggestions")
    public List<String> suggestions(
            @org.springframework.web.bind.annotation.RequestParam(value = "q", required = false, defaultValue = "") String query,
            @org.springframework.web.bind.annotation.RequestParam(value = "limit", required = false, defaultValue = "6") int limit,
            Principal principal
    ) {
        studentIdFrom(principal);
        return service.getSubjectSuggestions(query, limit);
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
