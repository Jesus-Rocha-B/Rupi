package pe.rupi.api.learningroute;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rupi.api.learningroute.LearningRouteDtos.Progress;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.learningroute.LearningRouteDtos.RouteDetailResponse;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteSummary;

@Service
public class LearningRouteService {
    private final LearningRouteRepository repository;

    public LearningRouteService(LearningRouteRepository repository) {
        this.repository = repository;
    }

    public List<RouteSummary> listForStudent(UUID studentId) {
        return repository.findRoutesForStudent(studentId);
    }

    @Transactional(readOnly = true)
    public RouteDetailResponse getForStudent(UUID studentId, UUID versionRouteId) {
        var route = repository.findRouteForStudent(studentId, versionRouteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
        return new RouteDetailResponse(route.versionRouteId(), route.title(), route.grade(), route.area(),
                enrollment, progress, nodes, culturalContext,
                RouteCurriculum.build(repository.findCurriculum(versionRouteId), nodes));
    }
}
