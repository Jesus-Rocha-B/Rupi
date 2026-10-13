package pe.rupi.api.material;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class MaterialStorageServiceTest {

    private Path tempDir;
    private JdbcTemplate jdbc;
    private MaterialStorageService storage;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("rupi-materials-test");
        jdbc = Mockito.mock(JdbcTemplate.class);
        storage = new MaterialStorageService(jdbc, tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
        }
    }

    @Test
    @DisplayName("Guarda archivo PDF válido exitosamente")
    void storePdfFileSuccess() {
        byte[] content = "%PDF-1.4 prueba de documento escolar".getBytes();
        var file = new MockMultipartFile("file", "lectura-ayacucho.pdf", "application/pdf", content);

        var stored = storage.storeFile(file, "profesor-1");

        assertNotNull(stored);
        assertNotNull(stored.id());
        assertEquals("lectura-ayacucho.pdf", stored.originalName());
        assertEquals("application/pdf", stored.mimeType());
        assertTrue(Files.exists(stored.diskPath()));
    }

    @Test
    @DisplayName("Guarda presentación PPTX válida exitosamente")
    void storePptxFileSuccess() {
        byte[] content = "PK presentacion escolar".getBytes();
        var file = new MockMultipartFile("file", "clase-ciencias.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", content);

        var stored = storage.storeFile(file, "profesor-1");

        assertNotNull(stored);
        assertEquals("clase-ciencias.pptx", stored.originalName());
        assertTrue(Files.exists(stored.diskPath()));
    }

    @Test
    @DisplayName("Rechaza archivos vacíos")
    void rejectEmptyFile() {
        var file = new MockMultipartFile("file", "vacio.pdf", "application/pdf", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storage.storeFile(file, "profesor-1")
        );

        assertTrue(ex.getReason().contains("no puede estar vacío"));
    }

    @Test
    @DisplayName("Rechaza extensiones no permitidas (ej. .exe, .sh)")
    void rejectDisallowedExtension() {
        var file = new MockMultipartFile("file", "virus.exe", "application/x-msdownload", "contenido binario".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                storage.storeFile(file, "profesor-1")
        );

        assertTrue(ex.getReason().contains("Formato de archivo no permitido"));
    }
}
