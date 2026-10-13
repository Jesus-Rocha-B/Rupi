package pe.rupi.api.material;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MaterialStorageService {

    public static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15 MB

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "pdf", "application/pdf",
            "ppt", "application/vnd.ms-powerpoint",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png"
    );

    private final JdbcTemplate jdbc;
    private final Path storageDirectory;

    public MaterialStorageService(
            JdbcTemplate jdbc,
            @Value("${rupi.storage.materials-path:uploads/materials}") String storagePath
    ) {
        this.jdbc = jdbc;
        this.storageDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo inicializar la carpeta de almacenamiento de materiales: " + this.storageDirectory, e);
        }
    }

    public record StoredMaterial(
            String id,
            String originalName,
            String mimeType,
            long sizeBytes,
            String downloadUrl,
            Path diskPath
    ) {}

    public StoredMaterial storeFile(MultipartFile file, String userId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo educativo no puede estar vacío.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo supera el tamaño máximo permitido de 15 MB.");
        }

        String rawName = file.getOriginalFilename();
        if (rawName == null || rawName.isBlank()) {
            rawName = "documento-educativo";
        }
        String sanitizedName = sanitizeFilename(rawName);

        String extension = extractExtension(sanitizedName).toLowerCase();
        if (!ALLOWED_TYPES.containsKey(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de archivo no permitido. Solo se aceptan documentos PDF (.pdf), presentaciones (.ppt, .pptx) e imágenes (.png, .jpg).");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isBlank() || "application/octet-stream".equals(mimeType)) {
            mimeType = ALLOWED_TYPES.get(extension);
        }

        String materialId = UUID.randomUUID().toString();
        String diskFileName = materialId + "." + extension;
        Path targetPath = storageDirectory.resolve(diskFileName).normalize();

        if (!targetPath.startsWith(storageDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo inválido o peligroso.");
        }

        byte[] hash;
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            hash = digest.digest();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error calculando la integridad del archivo.", e);
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error guardando el archivo en el servidor.", e);
        }

        String downloadUrl = "/api/v1/materials/" + materialId + "/download";

        jdbc.update("""
                INSERT INTO recurso_archivo_multimedia (
                    id, nombre_original, tipo_mime, tamanio_bytes, url_almacenamiento, hash_sha256, creado_por, creado_en
                ) VALUES (?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, materialId, sanitizedName, mimeType, file.getSize(), downloadUrl, hash, userId);

        return new StoredMaterial(materialId, sanitizedName, mimeType, file.getSize(), downloadUrl, targetPath);
    }

    public Optional<StoredMaterial> findById(String materialId) {
        if (materialId == null || !materialId.matches("[a-fA-F0-9\\-]{36}")) {
            return Optional.empty();
        }

        return jdbc.query("""
                SELECT id, nombre_original, tipo_mime, tamanio_bytes, url_almacenamiento
                FROM recurso_archivo_multimedia
                WHERE id = ?
                """, (rs, rowNum) -> {
            String id = rs.getString("id");
            String originalName = rs.getString("nombre_original");
            Path diskPath = diskPathFor(id, originalName);
            return new StoredMaterial(
                    id,
                    originalName,
                    rs.getString("tipo_mime"),
                    rs.getLong("tamanio_bytes"),
                    rs.getString("url_almacenamiento"),
                    diskPath
            );
        }, materialId).stream().findFirst();
    }

    /** Ruta en disco de un material subido; no consulta la base. */
    public Path diskPathFor(String materialId, String originalName) {
        String ext = extractExtension(originalName == null ? "" : originalName).toLowerCase();
        return storageDirectory.resolve(materialId + "." + ext).normalize();
    }

    private static String sanitizeFilename(String filename) {
        String clean = Paths.get(filename).getFileName().toString();
        clean = clean.replaceAll("[^a-zA-Z0-9._\\-\\sáéíóúÁÉÍÓÚñÑ]", "_").trim();
        if (clean.length() > 200) {
            clean = clean.substring(clean.length() - 200);
        }
        return clean.isBlank() ? "recurso-educativo" : clean;
    }

    private static String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }
}
