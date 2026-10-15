package pe.rupi.api.teacher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherRouteService {

    private static final int MAX_TITLE = 180;
    private static final Set<String> ACTIVITY_TYPES = Set.of("LECCION", "RETO", "CUESTIONARIO", "TARJETAS_MEMORIA", "INFOGRAFIA");
    private static final Set<String> RESOURCE_TYPES = Set.of("DOCUMENTO", "ENLACE", "PRESENTACION", "IMAGEN");
    private static final Pattern MATERIAL_DOWNLOAD = Pattern.compile("^/api/v1/materials/[A-Za-z0-9-]{1,64}/download$");

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
        if (req.title().trim().length() > MAX_TITLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título de la parada no puede superar los 180 caracteres.");
        }
        // Valores del ENUM aprendizaje_actividad.tipo: un tipo desconocido terminaba en un error 500 de la base.
        if (req.activityType() != null && !req.activityType().isBlank()
                && !ACTIVITY_TYPES.contains(req.activityType().trim().toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de actividad no válido. Usa lección, reto, cuestionario, tarjetas de memoria o infografía.");
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
        if (req.title().trim().length() > MAX_TITLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título del material no puede superar los 180 caracteres.");
        }
        if (req.type() != null && !req.type().isBlank()
                && !RESOURCE_TYPES.contains(req.type().trim().toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de material no válido. Usa documento, enlace, presentación o imagen.");
        }
        // Los alumnos abren este enlace: solo se aceptan direcciones web y descargas propias de RUPI
        // (nada de javascript:, data: ni rutas relativas arbitrarias).
        if (!isSafeResourceUrl(req.url().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace debe empezar con https:// o http://, o ser un material subido a RUPI.");
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

    public TeacherDtos.PublishRouteResponse pauseRoute(String teacherId, String routeId) {
        try {
            return repository.pauseRoute(teacherId, routeId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    static boolean isSafeResourceUrl(String url) {
        if (url.length() > 500) return false;
        if (MATERIAL_DOWNLOAD.matcher(url).matches()) return true;
        try {
            var uri = new URI(url);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))
                    && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException e) {
            return false;
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
