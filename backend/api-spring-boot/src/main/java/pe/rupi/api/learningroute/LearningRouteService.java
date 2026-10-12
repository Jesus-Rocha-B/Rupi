package pe.rupi.api.learningroute;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rupi.api.learningroute.LearningRouteDtos.Progress;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.learningroute.LearningRouteDtos.RouteDetailResponse;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteSearchResult;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteSummary;
import pe.rupi.api.learningroute.LearningRouteDtos.SearchAutocompleteResponse;

@Service
public class LearningRouteService {
    private final LearningRouteRepository repository;

    public LearningRouteService(LearningRouteRepository repository) {
        this.repository = repository;
    }

    public List<RouteSummary> listForStudent(UUID studentId) {
        return repository.findRoutesForStudent(studentId);
    }

    @Transactional
    public RouteDetailResponse getForStudent(UUID studentId, UUID versionRouteId) {
        var routeOpt = repository.findRouteForStudent(studentId, versionRouteId);
        if (routeOpt.isEmpty() && repository.isPublishedRoute(versionRouteId)) {
            repository.enrollStudentInRoute(studentId, versionRouteId);
            routeOpt = repository.findRouteForStudent(studentId, versionRouteId);
        }
        var route = routeOpt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var nodes = repository.findNodes(route.enrollment().id(), versionRouteId);
        if (nodes.stream().anyMatch(node -> node.state() == null)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
        var progress = new Progress(nodes.stream().filter(node -> "COMPLETADO".equals(node.state())).count(), nodes.size());
        var culturalContext = repository.findCulturalContextForRoute(route.routeId()).orElse(null);
        var remembered = route.enrollment().lastVisitedNodeId();
        var resume = nodes.stream().filter(n -> n.id().equals(remembered) && !"BLOQUEADO".equals(n.state()))
                .findFirst().orElseGet(() -> nodes.stream().filter(n -> "EN_CURSO".equals(n.state()))
                .findFirst().orElseGet(() -> nodes.stream().filter(n -> "DISPONIBLE".equals(n.state())).findFirst().orElse(null)));
        var enrollment = new LearningRouteDtos.Enrollment(route.enrollment().id(), route.enrollment().state(),
                resume == null ? null : resume.id());
        var curriculum = RouteCurriculum.build(repository.findCurriculumData(versionRouteId), nodes);
        return new RouteDetailResponse(route.versionRouteId(), route.title(), route.grade(), route.area(),
                enrollment, progress, nodes, culturalContext, curriculum);
    }

    @Transactional
    public RouteDetailResponse enroll(UUID studentId, UUID versionRouteId) {
        if (!repository.isPublishedRoute(versionRouteId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La ruta solicitada no existe o no está publicada.");
        }
        repository.enrollStudentInRoute(studentId, versionRouteId);
        return getForStudent(studentId, versionRouteId);
    }

    @Transactional(readOnly = true)
    public SearchAutocompleteResponse searchAlternativeRoutes(UUID studentId, String query, Integer gradeNumber, int limit) {
        String sanitizedQuery = query == null ? "" : query.trim();
        List<String> suggestions = repository.findSubjectSuggestions(sanitizedQuery, 6);
        List<RouteSearchResult> routes = repository.searchAlternativeRoutes(studentId, sanitizedQuery, gradeNumber, limit);
        return new SearchAutocompleteResponse(sanitizedQuery, suggestions, routes);
    }

    @Transactional(readOnly = true)
    public List<String> getSubjectSuggestions(String query, int limit) {
        return repository.findSubjectSuggestions(query == null ? "" : query.trim(), limit);
    }
}
