package pe.rupi.api.learningroute;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    public RouteDetailResponse getForStudent(UUID studentId, UUID versionRouteId) {
        var route = repository.findRouteForStudent(studentId, versionRouteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var progress = repository.findProgress(route.enrollment().id(), versionRouteId);
        var nodes = repository.findNodes(route.enrollment().id(), versionRouteId);
        return route.toResponse(progress, nodes);
    }
}
