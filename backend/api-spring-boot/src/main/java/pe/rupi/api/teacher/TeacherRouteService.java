package pe.rupi.api.teacher;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherRouteService {

    private final TeacherRouteRepository repository;

    public TeacherRouteService(TeacherRouteRepository repository) {
        this.repository = repository;
    }

    public List<TeacherDtos.TeacherRouteSummary> listRoutes(String teacherId) {
        return repository.findTeacherRoutes(teacherId);
    }

    public TeacherDtos.TeacherRouteDetail getRouteDetail(String teacherId, String routeId) {
        return repository.findTeacherRouteDetail(teacherId, routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El roadmap solicitado no existe o no pertenece a su autoría."));
    }

    public TeacherDtos.CreateRouteResponse createRoute(String teacherId, TeacherDtos.CreateRouteRequest req) {
        if (req == null || req.title() == null || req.title().trim().length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título del roadmap es obligatorio y debe tener al menos 3 caracteres.");
        }
        if (req.title().trim().length() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título del roadmap no puede superar los 180 caracteres.");
        }
        if (req.gradeNumber() == null || req.gradeNumber() < 1 || req.gradeNumber() > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El grado escolar debe ser entre 1.° y 6.° de primaria.");
        }
        if (req.areaCode() == null || req.areaCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un área curricular del CNEB (Matemática, Comunicación, etc.).");
        }

        try {
            return repository.createRoute(teacherId, req);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public String addNode(String teacherId, String routeId, TeacherDtos.CreateNodeRequest req) {
        if (req == null || req.title() == null || req.title().trim().length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título de la parada pedagógica debe tener al menos 3 caracteres.");
        }

        try {
            return repository.addNodeToRoute(teacherId, routeId, req);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    public String attachResource(String teacherId, String nodeId, TeacherDtos.AttachResourceRequest req) {
        if (req == null || req.title() == null || req.title().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título del material didáctico es obligatorio.");
        }
        if (req.url() == null || req.url().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La dirección web o URL de descarga del recurso es obligatoria.");
        }

        try {
            return repository.attachResourceToNode(teacherId, nodeId, req);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    public TeacherDtos.PublishRouteResponse publishRoute(String teacherId, String routeId) {
        try {
            return repository.publishRoute(teacherId, routeId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public TeacherDtos.TeacherRouteDetail reorderNodes(String teacherId, String routeId, TeacherDtos.ReorderNodesRequest req) {
        if (req == null || req.nodeIds() == null || req.nodeIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe proporcionar la lista ordenada de IDs de las paradas.");
        }

        try {
            repository.reorderNodes(teacherId, routeId, req.nodeIds());
            return repository.findTeacherRouteDetail(teacherId, routeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el roadmap después de reordenar."));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
