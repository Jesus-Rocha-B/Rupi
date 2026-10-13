package pe.rupi.api.material;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Principal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.shared.StudentSessionService;

@RestController
public class MaterialController {

    private final MaterialStorageService storage;

    public MaterialController(MaterialStorageService storage) {
        this.storage = storage;
    }

    public record UploadMaterialResponse(
            String id,
            String originalName,
            String mimeType,
            long sizeBytes,
            String downloadUrl
    ) {}

    @PostMapping(value = "/api/v1/teacher/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadMaterialResponse upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request,
            Principal principal
    ) {
        var user = (StudentSessionService.Student) request.getAttribute("teacher");
        if (user == null || !"DOCENTE".equalsIgnoreCase(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un profesor puede subir materiales educativos.");
        }

        var stored = storage.storeFile(file, principal != null ? principal.getName() : user.id());
        return new UploadMaterialResponse(
                stored.id(),
                stored.originalName(),
                stored.mimeType(),
                stored.sizeBytes(),
                stored.downloadUrl()
        );
    }

    @GetMapping("/api/v1/materials/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable("id") String id) {
        var material = storage.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El material educativo no existe o fue retirado.")
        );

        if (!Files.exists(material.diskPath())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo físico no se encuentra en el servidor.");
        }

        try {
            byte[] bytes = Files.readAllBytes(material.diskPath());
            ByteArrayResource resource = new ByteArrayResource(bytes);
            String encodedName = URLEncoder.encode(material.originalName(), StandardCharsets.UTF_8).replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(material.mimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                    .contentLength(bytes.length)
                    .body(resource);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el archivo educativo.", e);
        }
    }
}
