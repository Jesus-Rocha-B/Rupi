package pe.rupi.api.studypackage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.*;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pe.rupi.api.studypackage.StudyPackageModel.Unit;

class PrimaryStudyCatalogTest {
    @Test void coversAllPrimaryGradesWithoutMixingSubjects() {
        assertEquals(18, PrimaryStudyCatalog.all().size());
        for (int grade = 1; grade <= 6; grade++) {
            for (String area : List.of("MATEMATICA", "COMUNICACION", "CYT")) {
                var entries = PrimaryStudyCatalog.forGradeAndArea(grade, area);
                assertEquals(1, entries.size());
                assertEquals(grade, entries.getFirst().grade());
                assertEquals(area, entries.getFirst().areaCode());
                assertEquals(2, entries.getFirst().exercises().size());
                assertTrue(entries.getFirst().sourceUrl().startsWith("https://repositorio.minedu.gob.pe/handle/"));
            }
        }
    }

    @Test void aliasesMatchAndUnknownGradesOrAreasStayEmpty() {
        assertEquals(PrimaryStudyCatalog.forGradeAndArea(2,"MAT"), PrimaryStudyCatalog.forGradeAndArea(2,"MATEMATICA"));
        assertEquals(PrimaryStudyCatalog.forGradeAndArea(2,"COM"), PrimaryStudyCatalog.forGradeAndArea(2,"COMUNICACION"));
        assertEquals(PrimaryStudyCatalog.forGradeAndArea(2,"CT"), PrimaryStudyCatalog.forGradeAndArea(2,"CYT"));
        assertTrue(PrimaryStudyCatalog.forGradeAndArea(7,"MAT").isEmpty());
        assertTrue(PrimaryStudyCatalog.forGradeAndArea(0,"MAT").isEmpty());
        assertTrue(PrimaryStudyCatalog.forGradeAndArea(2,"INGLES").isEmpty());
        assertTrue(PrimaryStudyCatalog.forGradeAndArea(2,null).isEmpty());
    }

    @Test void allHandoutsHaveSeparateAnswerPageAndProduceVerifiablePackages() throws Exception {
        var output = Path.of("target/hu12-evidence");
        Files.createDirectories(output);
        for (var entry : PrimaryStudyCatalog.all()) {
            var unit = new Unit("Ruta de prueba", entry.grade()+".° de primaria", switch (entry.areaCode()) { case "MATEMATICA" -> "Matemática"; case "COMUNICACION" -> "Comunicación"; default -> "Ciencia y Tecnología"; }, "U1",
                    "Unidad de prueba", List.of(), List.of(), List.of(entry));
            var bytes = new ByteArrayOutputStream();
            new StudyPackageBuilder().write(unit, List.of(), f -> Optional.empty(), Instant.parse("2026-10-13T17:00:00Z"), bytes);
            var files = new HashMap<String,byte[]>();
            try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()), StandardCharsets.UTF_8)) {
                ZipEntry part;
                while ((part=zip.getNextEntry())!=null) files.put(part.getName(),zip.readAllBytes());
            }
            assertTrue(files.containsKey("repaso-rupi.pdf"));
            assertTrue(files.containsKey("fuentes-minedu.html"));
            assertFalse(files.keySet().stream().anyMatch(name -> name.startsWith("materiales/")));
            var manifest = StudyPackageBuilder.JSON.readTree(files.get("manifest.json"));
            for (var file : manifest.get("archivos")) {
                var data=files.get(file.get("ruta").asString());
                assertEquals(data.length,file.get("bytes").asLong());
                assertEquals(StudyPackageBuilder.sha256(data),file.get("sha256").asString());
            }
            var reader = new PdfReader(files.get("repaso-rupi.pdf"));
            assertEquals(2, reader.getNumberOfPages(), entry.title());
            var extractor = new PdfTextExtractor(reader);
            String page1=extractor.getTextFromPage(1), page2=extractor.getTextFromPage(2);
            assertTrue(page1.contains("Ejercicios originales de Rupi"));
            assertFalse(page1.contains("Revisamos lo aprendido"));
            assertTrue(page2.contains("Revisamos lo aprendido"));
            assertTrue(page2.contains(entry.sourceUrl()));
            assertTrue(page2.replaceAll("\\s+", " ").contains(entry.exercises().getFirst().answer()), entry.title());
            reader.close();
            String stem=entry.areaCode().toLowerCase()+"-"+entry.grade();
            Files.write(output.resolve(stem+".pdf"), files.get("repaso-rupi.pdf"));
            Files.write(output.resolve(stem+".zip"), bytes.toByteArray());
        }
    }

    @Test void supplementalContentChangesInvalidatePackageCache() {
        var a = new Unit("R", "2", "MAT", "U1", "U", List.of(), List.of(), PrimaryStudyCatalog.forGradeAndArea(2,"MAT"));
        var b = new Unit("R", "2", "MAT", "U1", "U", List.of(), List.of(), PrimaryStudyCatalog.forGradeAndArea(3,"MAT"));
        assertNotEquals(StudyPackageService.contentKey(a,List.of()),StudyPackageService.contentKey(b,List.of()));
    }

    @Test void htmlEscapesRouteMetadataAndExplainsOnlineSources() {
        var unit = new Unit("R", "<script>", "A&B", "U", "U", List.of(), List.of(), PrimaryStudyCatalog.forGradeAndArea(1,"COM"));
        String html=PrimaryStudyHandout.sources(unit);
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("A&amp;B"));
        assertTrue(html.contains("no están incluidos en este ZIP"));
        assertTrue(html.contains("repaso-rupi.pdf"));
    }
}
