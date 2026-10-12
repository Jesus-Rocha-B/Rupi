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
        List<Integer> grades = (gradeNumber != null && gradeNumber > 0) ? List.of(gradeNumber) : List.of();
        return searchAlternativeRoutes(studentId, query, grades, List.of(), limit);
    }

    @Transactional(readOnly = true)
    public SearchAutocompleteResponse searchAlternativeRoutes(
            UUID studentId,
            String query,
            List<Integer> gradeNumbers,
            List<String> areaCodes,
            int limit
    ) {
        String sanitizedQuery = query == null ? "" : query.trim();
        List<Integer> validGrades = gradeNumbers == null ? List.of() : gradeNumbers.stream().filter(g -> g != null && g > 0).toList();
        List<String> validAreas = areaCodes == null ? List.of() : areaCodes.stream().filter(a -> a != null && !a.isBlank()).toList();

        List<String> suggestions = repository.findSubjectSuggestions(sanitizedQuery, 6);
        List<RouteSearchResult> routes = repository.searchAlternativeRoutes(studentId, sanitizedQuery, validGrades, validAreas, limit);
        return new SearchAutocompleteResponse(sanitizedQuery, suggestions, routes, validGrades, validAreas, routes.size());
    }

    @Transactional(readOnly = true)
    public List<String> getSubjectSuggestions(String query, int limit) {
        return repository.findSubjectSuggestions(query == null ? "" : query.trim(), limit);
    }

    @Transactional
    public LearningRouteDtos.JoinByCodeResponse joinByCode(UUID studentId, String rawCode) {
        if (rawCode == null || rawCode.trim().length() < 3 || rawCode.trim().length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código debe tener entre 3 y 30 caracteres alfanuméricos.");
        }
        String normalizedCode = rawCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalizedCode.matches("^[A-Z0-9_-]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código solo puede contener letras, números y guiones.");
        }

        var codeOpt = repository.findAccessCode(normalizedCode);
        if (codeOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El código de clase no existe. Revisa las letras o consulta con tu profesor.");
        }

        var code = codeOpt.get();
        java.time.Instant now = java.time.Instant.now();

        if (code.revokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este código de clase fue revocado por el profesor.");
        }
        if (code.validFrom() != null && code.validFrom().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este código de clase aún no está habilitado.");
        }
        if (code.validUntil() != null && code.validUntil().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este código de clase ya venció. Pide uno nuevo a tu profesor.");
        }
        if (code.maxUses() != null && code.currentUses() >= code.maxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este código de clase ya alcanzó el límite máximo de estudiantes.");
        }
        if (!code.routePublished() || code.routeArchived()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ruta de estudio asociada a este código no se encuentra disponible.");
        }

        boolean alreadyEnrolled = repository.isStudentEnrolled(studentId, code.versionRouteId());
        if (alreadyEnrolled) {
            repository.linkAccessCodeToEnrollment(studentId, code.versionRouteId(), code.id());
            return new LearningRouteDtos.JoinByCodeResponse(
                    code.versionRouteId(),
                    code.routeTitle(),
                    code.grade(),
                    code.area(),
                    true,
                    "¡Ya formas parte de esta clase! Te llevamos a tu ruta escolar."
            );
        }

        repository.enrollStudentWithAccessCode(studentId, code.versionRouteId(), code.id());
        return new LearningRouteDtos.JoinByCodeResponse(
                code.versionRouteId(),
                code.routeTitle(),
                code.grade(),
                code.area(),
                false,
                "¡Bienvenido a tu clase! Te has unido exitosamente al roadmap."
        );
    }
}

