package pe.rupi.api.dashboard;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.dashboard.DashboardDtos.DashboardSummaryResponse;
import pe.rupi.api.shared.StudentSessionService;

/**
 * Controlador de Métricas y Panel Principal del Estudiante (HU-33).
 *
 * Expone el endpoint de resumen rápido con soporte para revalidación ETag (HTTP 304)
 * a fin de garantizar sincronización inmediata en dispositivos móviles y web.
 */
@RestController
@RequestMapping("/api/v1/student/dashboard")
public class DashboardSummaryController {
    private final DashboardSummaryService dashboardService;

    public DashboardSummaryController(DashboardSummaryService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            HttpServletRequest request
    ) {
        StudentSessionService.Student student = (StudentSessionService.Student) request.getAttribute("student");
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        DashboardSummaryResponse response = dashboardService.getSummary(student);
        String etag = "\"" + response.sync().dataHash() + "\"";

        if (ifNoneMatch != null) {
            String cleanHeader = ifNoneMatch.trim();
            if (cleanHeader.equals(etag) || cleanHeader.equals("W/" + etag) || cleanHeader.equals(response.sync().dataHash())) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(etag)
                        .cacheControl(CacheControl.noCache().mustRevalidate())
                        .build();
            }
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.noCache().mustRevalidate())
                .body(response);
    }
}
