package pe.rupi.api.studypackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

import pe.rupi.api.studypackage.StudyPackageModel.Block;
import pe.rupi.api.studypackage.StudyPackageModel.FileRef;
import pe.rupi.api.studypackage.StudyPackageModel.Option;
import pe.rupi.api.studypackage.StudyPackageModel.Question;
import pe.rupi.api.studypackage.StudyPackageModel.Resource;
import pe.rupi.api.studypackage.StudyPackageModel.Stop;
import pe.rupi.api.studypackage.StudyPackageModel.Unit;

import static org.junit.jupiter.api.Assertions.*;

class StudyPackageBuilderTest {
    private static final String FILE_ID = "aa000000-0000-4000-8000-000000000001";
    private static final String MISSING_ID = "aa000000-0000-4000-8000-000000000002";
    private static final Instant NOW = Instant.parse("2026-10-13T15:00:00Z");

    @TempDir
    Path tempDir;

    private Path material;
    private byte[] materialBytes;

    @BeforeEach
    void setUp() throws IOException {
        materialBytes = "%PDF-1.4 lectura del retablo ayacuchano".getBytes(StandardCharsets.UTF_8);
        material = tempDir.resolve("lectura.pdf");
        Files.write(material, materialBytes);
    }

    private Unit unit() {
        var questions = List.of(new Question(1, "OPCION_UNICA", "¿Cuántos retablos vio María en total?",
                "Sumamos 5 + 4 = 9.", List.of(new Option(1, "8 retablos", false), new Option(2, "9 retablos", true))));
        var stop = new Stop(1, "Suma en la feria de Huamanga", false, 10, "Lee y cuenta con calma.",
                List.of(new Block("TEXTO", "María visita la feria de artesanía.", null, null),
                        new Block("DESTACADO", "Sumar es juntar cantidades.", null, null)),
                questions,
                List.of(new Resource("DOCUMENTO", "Lectura del retablo", "/api/v1/materials/x/download", FILE_ID),
                        new Resource("ENLACE", "Video del Minedu", "https://www.perueduca.pe/", null),
                        new Resource("DOCUMENTO", "Ficha perdida", "/api/v1/materials/y/download", MISSING_ID)));
        var openStop = new Stop(2, "Cuéntanos tu feria", true, null, null,
                List.of(new Block("TEXTO", "Escribe qué viste en la feria.", null, null)),
                List.of(new Question(1, "TEXTO_CORTO", "¿Qué artesanía te gustó más?", null, List.of())),
                List.of());
        return new Unit("Matemática en Ayacucho", "3.° grado", "Matemática", "MAT-3-U1", "Números en la feria",
                List.of("Resuelve problemas de cantidad"), List.of(stop, openStop));
    }

    private List<FileRef> files(String hash) {
        return List.of(new FileRef(FILE_ID, "Lectura Retablo Ñaño.pdf", "application/pdf", materialBytes.length, hash),
                new FileRef(MISSING_ID, "ficha.pdf", "application/pdf", 10, null));
    }

    private Map<String, byte[]> build(List<FileRef> files) throws IOException {
        var out = new ByteArrayOutputStream();
        new StudyPackageBuilder().write(unit(), files,
                file -> FILE_ID.equals(file.id()) ? Optional.of(material) : Optional.of(tempDir.resolve("no-existe.pdf")),
                NOW, out);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), zip.readAllBytes());
            }
        }
        return entries;
    }

    @Test
    void offlinePageHasLocalMaterialsExercisesAndVisibleMissingFiles() throws IOException {
        var entries = build(files(StudyPackageBuilder.sha256(materialBytes)));
        var html = new String(entries.get("ABRIR.html"), StandardCharsets.UTF_8);
        assertTrue(html.contains("María visita la feria"));
        assertTrue(html.contains("9 retablos"));
        assertTrue(html.contains("href=\"materiales/parada-01-Lectura-Retablo-Nano.pdf\""));
        assertTrue(html.contains("Ficha perdida"));
        assertTrue(html.contains("necesita internet"));
        assertFalse(html.contains("<script"));
        var evidence = Path.of("target", "hu12-evidence", "offline-reader");
        Files.createDirectories(evidence);
        Files.write(evidence.resolve("ABRIR.html"), entries.get("ABRIR.html"));
        assertEquals("&lt;script&gt;&amp;&quot;", OfflineStudyPage.escape("<script>&\""));
    }

    @Test
    @DisplayName("El zip trae guía, material, contenido, LEEME y manifest")
    void packageHasExpectedEntries() throws IOException {
        var entries = build(files(StudyPackageBuilder.sha256(materialBytes)));

        assertEquals(List.of("guia-de-estudio.pdf", "materiales/parada-01-Lectura-Retablo-Nano.pdf", "contenido.json",
                "ABRIR.html", "LEEME.txt", "manifest.json"), List.copyOf(entries.keySet()));
        assertArrayEquals(materialBytes, entries.get("materiales/parada-01-Lectura-Retablo-Nano.pdf"));
    }

    @Test
    @DisplayName("El manifest guarda el SHA-256 y el tamaño real de cada archivo")
    void manifestHashesMatchEntries() throws IOException {
        var entries = build(files(StudyPackageBuilder.sha256(materialBytes)));
        JsonNode manifest = StudyPackageBuilder.JSON.readTree(entries.get("manifest.json"));

        assertEquals("rupi-paquete-unidad", manifest.get("formato").asString());
        assertEquals(5, manifest.get("archivos").size());
        for (JsonNode file : manifest.get("archivos")) {
            byte[] bytes = entries.get(file.get("ruta").asString());
            assertNotNull(bytes, file.toString());
            assertEquals(bytes.length, file.get("bytes").asLong());
            assertEquals(StudyPackageBuilder.sha256(bytes), file.get("sha256").asString());
        }
        assertEquals(1, manifest.get("materialesNoIncluidos").size());
        assertTrue(manifest.get("materialesNoIncluidos").get(0).asString().contains("Ficha perdida"));
    }

    @Test
    @DisplayName("Un material que no coincide con su hash no entra al paquete")
    void corruptedMaterialIsLeftOut() throws IOException {
        var entries = build(files("00".repeat(32)));

        assertFalse(entries.keySet().stream().anyMatch(name -> name.startsWith("materiales/")));
        JsonNode manifest = StudyPackageBuilder.JSON.readTree(entries.get("manifest.json"));
        assertEquals(2, manifest.get("materialesNoIncluidos").size());
    }

    @Test
    @DisplayName("La guía PDF se lee sin conexión: texto con tildes, preguntas, claves y materiales")
    void pdfIsReadable() throws IOException {
        var entries = build(files(StudyPackageBuilder.sha256(materialBytes)));
        byte[] pdf = entries.get("guia-de-estudio.pdf");
        assertTrue(new String(pdf, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));

        var reader = new PdfReader(pdf);
        var extractor = new PdfTextExtractor(reader);
        StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        String all = text.toString();
        reader.close();

        assertTrue(all.contains("Números en la feria"), all);
        assertTrue(all.contains("Cómo usar esta guía"));
        assertTrue(all.contains("Parada 1"));
        assertTrue(all.contains("¿Cuántos retablos vio María en total?"));
        assertTrue(all.contains("materiales/parada-01-Lectura-Retablo-Nano.pdf"));
        assertTrue(all.contains("necesita internet"));
        assertTrue(all.contains("pídeselo a tu profesor"));
        assertTrue(all.contains("CLAVES PARA REVISAR"));
        assertTrue(all.contains("b) 9 retablos"));
        assertTrue(all.contains("Respuesta libre"));
        assertTrue(all.contains("(opcional)"));
        assertTrue(all.contains("página 1"));
    }

    @Test
    @DisplayName("Las claves van en una página aparte, después de los ejercicios")
    void answersAreOnSeparatePage() throws IOException {
        var entries = build(files(StudyPackageBuilder.sha256(materialBytes)));
        var reader = new PdfReader(entries.get("guia-de-estudio.pdf"));
        var extractor = new PdfTextExtractor(reader);
        String firstPage = extractor.getTextFromPage(1);
        String lastPage = extractor.getTextFromPage(reader.getNumberOfPages());
        reader.close();

        assertFalse(firstPage.contains("CLAVES PARA REVISAR"));
        assertFalse(firstPage.contains("Sumamos 5 + 4 = 9."));
        assertTrue(lastPage.contains("CLAVES PARA REVISAR"));
        assertTrue(lastPage.contains("Sumamos 5 + 4 = 9."));
    }

    @Test
    @DisplayName("Los nombres de archivo quedan en ASCII seguro")
    void safeFileNames() {
        assertEquals("Ficha-de-ninos.pdf", StudyPackageBuilder.safeFileName("Ficha de niños.pdf"));
        assertEquals("etc-passwd", StudyPackageBuilder.safeFileName("../../etc/passwd"));
        assertEquals("material", StudyPackageBuilder.safeFileName("  "));
    }
}
