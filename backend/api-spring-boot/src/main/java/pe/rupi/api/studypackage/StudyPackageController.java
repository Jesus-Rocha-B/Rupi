package pe.rupi.api.studypackage;

import java.security.Principal;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StudyPackageController {
    private final StudyPackageService service;

    public StudyPackageController(StudyPackageService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/student/learning-routes/{versionRouteId}/study-resources")
    public java.util.List<PrimaryStudyCatalog.Entry> resources(@PathVariable UUID versionRouteId, Principal principal) {
        return service.resourcesForRoute(studentIdFrom(principal), versionRouteId);
    }

    @GetMapping("/api/v1/student/learning-routes/{versionRouteId}/units/{unitKey}/package")
    public ResponseEntity<Resource> download(@PathVariable UUID versionRouteId, @PathVariable String unitKey,
                                             Principal principal) {
        var pack = service.packageForUnit(studentIdFrom(principal), versionRouteId, unitKey);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(pack.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(pack.fileName()).build().toString())
                .header("X-Rupi-Paquete-Sha256", pack.sha256())
                .header("X-Rupi-Paquete-Version", pack.contentKey())
                .header("X-Rupi-Paquete-Cache", pack.fromCache() ? "HIT" : "MISS")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        "Content-Disposition, X-Rupi-Paquete-Sha256, X-Rupi-Paquete-Version, X-Rupi-Paquete-Cache")
                .body(new FileSystemResource(pack.path()));
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
