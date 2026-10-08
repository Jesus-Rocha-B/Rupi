package pe.rupi.api.learningroute;

import java.security.Principal;
import java.util.UUID;
<<<<<<< HEAD
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import pe.rupi.api.shared.SessionController;
=======
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.learningroute.LearningRouteDtos.ActivityCompleteResponse;
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)

@RestController
@RequestMapping("/api/v1/student/learning-routes")
public class ActivityController {
    private final ActivityService service;
<<<<<<< HEAD
    public ActivityController(ActivityService service) { this.service = service; }
    @PostMapping("/{routeId}/nodes/{nodeId}/start")
    public ActivityService.Activity start(@PathVariable UUID routeId, @PathVariable UUID nodeId,
            Principal principal, HttpServletRequest request) {
        SessionController.checkOrigin(request);
        return service.start(UUID.fromString(principal.getName()), routeId, nodeId);
=======

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
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
    }
}
