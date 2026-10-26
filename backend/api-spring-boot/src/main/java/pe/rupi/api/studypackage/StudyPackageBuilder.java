package pe.rupi.api.studypackage;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import pe.rupi.api.studypackage.StudyPackageModel.Block;
import pe.rupi.api.studypackage.StudyPackageModel.FileRef;
import pe.rupi.api.studypackage.StudyPackageModel.Option;
import pe.rupi.api.studypackage.StudyPackageModel.Question;
import pe.rupi.api.studypackage.StudyPackageModel.Resource;
import pe.rupi.api.studypackage.StudyPackageModel.Stop;
import pe.rupi.api.studypackage.StudyPackageModel.Unit;

/** Arma el zip de una unidad: guía PDF imprimible, materiales del docente, contenido.json y manifest.json. */
public class StudyPackageBuilder {
    /** Subirlo cuando cambie la plantilla del PDF o la estructura del zip, para invalidar la caché. */
    public static final int FORMAT_VERSION = 3;

    public static final String GUIDE_ENTRY = "guia-de-estudio.pdf";
    public static final String CONTENT_ENTRY = "contenido.json";
    public static final String MANIFEST_ENTRY = "manifest.json";
    public static final String README_ENTRY = "LEEME.txt";

    static final JsonMapper JSON = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private static final Color FOREST = new Color(0x17, 0x3e, 0x30);
    private static final Color LEAF = new Color(0x31, 0x72, 0x4b);
    private static final Color ORANGE = new Color(0xf9, 0x57, 0x38);
    private static final Color SUN = new Color(0xff, 0xd2, 0x6a);
    private static final Color SKY = new Color(0x24, 0x6a, 0x91);
    private static final Color PAPER = new Color(0xfa, 0xf8, 0xf5);
    private static final Color INK = new Color(0x22, 0x22, 0x22);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-PE"))
            .withZone(ZoneId.of("America/Lima"));

    @FunctionalInterface
    public interface FileSource {
        Optional<Path> pathOf(FileRef file);
    }

    public record ManifestEntry(String ruta, long bytes, String sha256) {}

    public record Manifest(
            String formato,
            int version,
            String generadoEn,
            String ruta,
            String grado,
            String area,
            String unidad,
            List<ManifestEntry> archivos,
            List<String> materialesNoIncluidos
    ) {}

    record Content(int version, String generadoEn, Unit unidad, Map<String, String> archivosPorId) {}

    public void write(Unit unit, List<FileRef> files, FileSource source, Instant generatedAt, OutputStream out)
            throws IOException {
        Map<String, FileRef> filesById = new LinkedHashMap<>();
        files.forEach(file -> filesById.put(file.id(), file));

        Map<String, String> packagedNames = new LinkedHashMap<>();
        Map<String, byte[]> packagedBytes = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        for (Stop stop : unit.stops()) {
            for (String fileId : referencedFiles(stop)) {
                if (packagedNames.containsKey(fileId)) continue;
                FileRef file = filesById.get(fileId);
                byte[] bytes = file == null ? null : readVerified(file, source);
                if (bytes == null) {
                    missing.add("Parada " + stop.sequence() + ": " + titleOfFile(stop, fileId));
                    continue;
                }
                String name = "materiales/parada-%02d-%s".formatted(stop.sequence(), safeFileName(file.originalName()));
                if (packagedBytes.containsKey(name)) name = "materiales/" + safeFileName(fileId) + "-" + safeFileName(file.originalName());
                packagedNames.put(fileId, name);
                packagedBytes.put(name, bytes);
            }
        }

        List<ManifestEntry> entries = new ArrayList<>();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.setComment("Paquete de estudio de Rupi");
            putEntry(zip, GUIDE_ENTRY, renderPdf(unit, packagedNames, generatedAt), entries);
            if (!unit.supplementaryResources().isEmpty()) {
                putEntry(zip, "repaso-rupi.pdf", new PrimaryStudyHandout().render(unit), entries);
                putEntry(zip, "fuentes-minedu.html", PrimaryStudyHandout.sources(unit).getBytes(StandardCharsets.UTF_8), entries);
            }
            for (var file : packagedBytes.entrySet()) {
                putEntry(zip, file.getKey(), file.getValue(), entries);
            }
            byte[] content = JSON.writeValueAsBytes(new Content(FORMAT_VERSION, generatedAt.toString(), unit, packagedNames));
            putEntry(zip, CONTENT_ENTRY, content, entries);
            putEntry(zip, "ABRIR.html", OfflineStudyPage.render(unit, packagedNames, missing).getBytes(StandardCharsets.UTF_8), entries);
            putEntry(zip, README_ENTRY, (readme(unit) + "\r\nMateriales no incluidos: " + (missing.isEmpty() ? "ninguno" : String.join("; ", missing))).getBytes(StandardCharsets.UTF_8), entries);

            var manifest = new Manifest("rupi-paquete-unidad", FORMAT_VERSION, generatedAt.toString(),
                    unit.routeTitle(), unit.gradeName(), unit.areaName(), unit.unitTitle(), entries, missing);
            byte[] manifestBytes = JSON.writeValueAsBytes(manifest);
            zip.putNextEntry(new ZipEntry(MANIFEST_ENTRY));
            zip.write(manifestBytes);
            zip.closeEntry();
        }
    }

    private static List<String> referencedFiles(Stop stop) {
        List<String> ids = new ArrayList<>();
        for (Block block : stop.blocks()) if (block.fileId() != null) ids.add(block.fileId());
        for (Resource resource : stop.resources()) if (resource.fileId() != null) ids.add(resource.fileId());
        return ids;
    }

    private static String titleOfFile(Stop stop, String fileId) {
        return stop.resources().stream().filter(r -> fileId.equals(r.fileId())).map(Resource::title).findFirst()
                .orElseGet(() -> stop.blocks().stream().filter(b -> fileId.equals(b.fileId()))
                        .map(b -> b.accessibleText() != null ? b.accessibleText() : "archivo de la lección")
                        .findFirst().orElse("archivo de la lección"));
    }

    /** Devuelve los bytes solo si el archivo existe y coincide con el SHA-256 guardado al subirlo. */
    private static byte[] readVerified(FileRef file, FileSource source) throws IOException {
        Optional<Path> path = source.pathOf(file);
        if (path.isEmpty() || !Files.isRegularFile(path.get())) return null;
        byte[] bytes = Files.readAllBytes(path.get());
        if (file.sha256() != null && !file.sha256().equalsIgnoreCase(sha256(bytes))) return null;
        return bytes;
    }

    private static void putEntry(ZipOutputStream zip, String name, byte[] bytes, List<ManifestEntry> entries)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
        entries.add(new ManifestEntry(name, bytes.length, sha256(bytes)));
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String safeFileName(String original) {
        String name = original == null || original.isBlank() ? "material" : original;
        name = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        name = name.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("-{2,}", "-").replaceAll("-+\\.", ".").replaceAll("^[-.]+|-+$", "");
        if (name.length() > 80) name = name.substring(name.length() - 80);
        return name.isBlank() ? "material" : name;
    }

    private static String readme(Unit unit) {
        String text = """
                Paquete de estudio de Rupi

                Ruta: %s (%s, %s)
                Unidad: %s

                Primero extrae TODO el ZIP y abre ABRIR.html en tu navegador, incluso sin internet.
                También puedes abrir directamente los PDF. No necesitas iniciar sesión.
                El estudio sin conexión no registra respuestas ni cambia tu avance en Rupi.

                Qué hay aquí:
                - guia-de-estudio.pdf: lecturas, ejercicios y claves para revisar. Puedes imprimirla.
                - materiales/: archivos que dejó tu profesor para cada parada.
                - contenido.json y manifest.json: Rupi los usa para comprobar que el paquete está completo.
                  No necesitas abrirlos.

                Si tu grado y materia tienen material complementario:
                - repaso-rupi.pdf: ejercicios propios de Rupi y respuestas. Se puede usar sin internet.
                  Es repaso general del grado, no una evaluación de la unidad ni una publicación del MINEDU.
                - fuentes-minedu.html: referencias oficiales. Sus enlaces necesitan internet.
                  Abre la ficha del MINEDU y guarda el PDF antes de desconectarte.
                  Los libros externos no están incluidos dentro de este ZIP.

                Para abrir la guía y los materiales incluidos no necesitas internet.
                Los enlaces a páginas web que aparecen en la guía sí necesitan conexión.
                """.formatted(unit.routeTitle(), unit.gradeName(), unit.areaName(), unit.unitTitle());
        return text.replace("\n", "\r\n");
    }

    byte[] renderPdf(Unit unit, Map<String, String> packagedNames, Instant generatedAt) {
        var buffer = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 54, 54, 56, 64);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, buffer);
            writer.setPageEvent(new Footer(unit.unitTitle()));
            document.addTitle("Guía de estudio · " + unit.unitTitle());
            document.addSubject(unit.routeTitle());
            document.addAuthor("Rupi");
            document.addCreator("Rupi");
            document.open();

            writeCover(document, unit, generatedAt);
            for (Stop stop : unit.stops()) {
                writeStop(document, stop, packagedNames);
            }
            writeAnswerKey(document, unit);
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo armar la guía en PDF.", e);
        }
        return buffer.toByteArray();
    }

    private static Font font(String name, float size, Color color) {
        return FontFactory.getFont(name, size, Font.NORMAL, color);
    }

    private static final Font KICKER = font(FontFactory.HELVETICA_BOLD, 9, SKY);
    private static final Font TITLE = font(FontFactory.HELVETICA_BOLD, 22, FOREST);
    private static final Font SUBTITLE = font(FontFactory.HELVETICA, 12, FOREST);
    private static final Font UNIT = font(FontFactory.HELVETICA_BOLD, 15, FOREST);
    private static final Font SMALL = font(FontFactory.HELVETICA, 10, FOREST);
    private static final Font STOP = font(FontFactory.HELVETICA_BOLD, 15, LEAF);
    private static final Font LABEL = font(FontFactory.HELVETICA_BOLD, 11, FOREST);
    private static final Font BODY = font(FontFactory.HELVETICA, 12, INK);
    private static final Font BODY_BOLD = font(FontFactory.HELVETICA_BOLD, 12, INK);
    private static final Font NOTE = font(FontFactory.HELVETICA_OBLIQUE, 10, FOREST);

    private static void writeCover(Document document, Unit unit, Instant generatedAt) throws DocumentException {
        document.add(new Paragraph("RUPI · GUÍA PARA ESTUDIAR SIN CONEXIÓN", KICKER));
        Paragraph title = new Paragraph(unit.routeTitle(), TITLE);
        title.setSpacingBefore(6);
        document.add(title);
        document.add(new Paragraph(unit.gradeName() + " · " + unit.areaName(), SUBTITLE));

        PdfPTable band = new PdfPTable(1);
        band.setWidthPercentage(100);
        band.setSpacingBefore(14);
        band.setSpacingAfter(12);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(SUN);
        cell.setBorderColor(FOREST);
        cell.setBorderWidth(1.5f);
        cell.setPadding(12);
        Paragraph unitTitle = new Paragraph(unit.unitTitle(), UNIT);
        cell.addElement(unitTitle);
        if (unit.unitCode() != null && !"OTRAS".equals(unit.unitCode())) {
            cell.addElement(new Paragraph("Código de unidad: " + unit.unitCode(), SMALL));
        }
        if (!unit.competencies().isEmpty()) {
            Paragraph competencies = new Paragraph("Currículo MINEDU: " + String.join(" · ", unit.competencies()), SMALL);
            competencies.setSpacingBefore(4);
            cell.addElement(competencies);
        }
        band.addCell(cell);
        document.add(band);

        document.add(new Paragraph("Cómo usar esta guía", LABEL));
        Paragraph howTo = new Paragraph(
                "Lee cada parada con calma. Responde en tu cuaderno o en esta misma hoja. "
                        + "Al final están las claves para que revises tus respuestas cuando termines.", BODY);
        howTo.setLeading(17);
        howTo.setSpacingAfter(4);
        document.add(howTo);
        document.add(new Paragraph("Preparada el " + DATE.format(generatedAt) + ". Este paquete tiene "
                + unit.stops().size() + (unit.stops().size() == 1 ? " parada." : " paradas."), NOTE));
    }

    private static void writeStop(Document document, Stop stop, Map<String, String> packagedNames) throws DocumentException {
        Paragraph heading = new Paragraph();
        heading.setSpacingBefore(20);
        heading.add(new Chunk("Parada " + stop.sequence() + " · ", STOP));
        heading.add(new Chunk(stop.title(), STOP));
        if (stop.optional()) heading.add(new Chunk("  (opcional)", NOTE));
        heading.setKeepTogether(true);
        document.add(heading);
        document.add(rule());
        if (stop.minutes() != null) {
            document.add(new Paragraph("Tiempo aproximado: " + stop.minutes() + " minutos", NOTE));
        }

        if (hasText(stop.instructions())) {
            document.add(bodyParagraph(stop.instructions(), BODY_BOLD, 8));
        }
        for (Block block : stop.blocks()) {
            writeBlock(document, block, packagedNames);
        }

        if (!stop.questions().isEmpty()) {
            Paragraph practice = new Paragraph("Practica", LABEL);
            practice.setSpacingBefore(12);
            document.add(practice);
            for (Question question : stop.questions()) {
                writeQuestion(document, question);
            }
        }

        if (!stop.resources().isEmpty()) {
            Paragraph label = new Paragraph("Materiales de tu profesor", LABEL);
            label.setSpacingBefore(12);
            document.add(label);
            for (Resource resource : stop.resources()) {
                document.add(bodyParagraph("•  " + resource.title() + " — " + whereIs(resource, packagedNames), BODY, 3));
            }
        }
    }

    private static void writeBlock(Document document, Block block, Map<String, String> packagedNames) throws DocumentException {
        String type = block.type() == null ? "TEXTO" : block.type();
        if ("DESTACADO".equals(type) && hasText(block.text())) {
            PdfPTable box = new PdfPTable(1);
            box.setWidthPercentage(100);
            box.setSpacingBefore(8);
            PdfPCell cell = new PdfPCell(new Phrase(block.text(), BODY_BOLD));
            cell.setBackgroundColor(PAPER);
            cell.setBorderColor(ORANGE);
            cell.setBorderWidth(0);
            cell.setBorderWidthLeft(4);
            cell.setPadding(10);
            cell.setLeading(0, 1.35f);
            box.addCell(cell);
            document.add(box);
            return;
        }
        if (hasText(block.text())) {
            document.add(bodyParagraph(block.text(), BODY, 8));
        }
        if (block.fileId() != null || !"TEXTO".equals(type)) {
            String label = switch (type) {
                case "IMAGEN" -> "Imagen";
                case "AUDIO" -> "Audio";
                case "VIDEO" -> "Video";
                case "ADJUNTO" -> "Archivo";
                default -> null;
            };
            if (label == null) return;
            StringBuilder note = new StringBuilder("[").append(label).append("] ");
            if (hasText(block.accessibleText())) note.append(block.accessibleText()).append(". ");
            String packaged = block.fileId() == null ? null : packagedNames.get(block.fileId());
            note.append(packaged != null ? "Está en la carpeta del paquete: " + packaged : "Solo se puede ver en Rupi con internet.");
            document.add(bodyParagraph(note.toString(), NOTE, 6));
        }
    }

    private static void writeQuestion(Document document, Question question) throws DocumentException {
        Paragraph statement = bodyParagraph(question.sequence() + ".  " + question.statement(), BODY_BOLD, 8);
        statement.setKeepTogether(true);
        document.add(statement);
        if ("OPCION_MULTIPLE".equals(question.type())) {
            document.add(indented(new Paragraph("Puedes marcar más de una respuesta.", NOTE)));
        }
        if (question.options().isEmpty()) {
            document.add(indented(bodyParagraph("Tu respuesta: ____________________________________________", BODY, 6)));
            document.add(indented(bodyParagraph("_______________________________________________________", BODY, 6)));
            return;
        }
        for (int i = 0; i < question.options().size(); i++) {
            Option option = question.options().get(i);
            document.add(indented(bodyParagraph("(   )  " + letter(i) + ")  " + option.text(), BODY, 3)));
        }
    }

    private static void writeAnswerKey(Document document, Unit unit) throws DocumentException {
        boolean anyQuestion = unit.stops().stream().anyMatch(stop -> !stop.questions().isEmpty());
        if (!anyQuestion) return;
        document.newPage();
        document.add(new Paragraph("CLAVES PARA REVISAR", KICKER));
        Paragraph title = new Paragraph("¿Cómo te fue?", TITLE);
        title.setSpacingBefore(4);
        document.add(title);
        document.add(bodyParagraph("Primero intenta resolver todo tú. Después compara aquí. "
                + "Si te equivocaste, lee la explicación y vuelve a intentarlo: así se aprende.", BODY, 6));

        for (Stop stop : unit.stops()) {
            if (stop.questions().isEmpty()) continue;
            Paragraph heading = new Paragraph("Parada " + stop.sequence() + " · " + stop.title(), LABEL);
            heading.setSpacingBefore(14);
            document.add(heading);
            for (Question question : stop.questions()) {
                List<String> correct = new ArrayList<>();
                for (int i = 0; i < question.options().size(); i++) {
                    Option option = question.options().get(i);
                    if (option.correct()) correct.add(letter(i) + ") " + option.text());
                }
                String answer = correct.isEmpty() ? "Respuesta libre: revísala con tu profesor." : String.join("  ·  ", correct);
                Paragraph line = new Paragraph();
                line.setLeading(17);
                line.setSpacingBefore(6);
                line.add(new Chunk(question.sequence() + ".  " + answer, BODY_BOLD));
                if (hasText(question.explanation())) line.add(new Chunk("\n" + question.explanation(), BODY));
                document.add(indented(line));
            }
        }
    }

    private static String whereIs(Resource resource, Map<String, String> packagedNames) {
        if (resource.fileId() != null) {
            String packaged = packagedNames.get(resource.fileId());
            return packaged != null ? "está en " + packaged : "no se pudo incluir; pídeselo a tu profesor.";
        }
        if ("ENLACE".equals(resource.type()) && hasText(resource.url())) {
            return "enlace web (necesita internet): " + resource.url();
        }
        return "solo disponible en Rupi con internet.";
    }

    private static Paragraph bodyParagraph(String text, Font font, float spacingBefore) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setLeading(17);
        paragraph.setSpacingBefore(spacingBefore);
        return paragraph;
    }

    private static Paragraph indented(Paragraph paragraph) {
        paragraph.setIndentationLeft(18);
        return paragraph;
    }

    private static Paragraph rule() {
        Paragraph line = new Paragraph(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(1.2f, 100, ORANGE, Element.ALIGN_LEFT, -4)));
        line.setSpacingAfter(4);
        return line;
    }

    private static String letter(int index) {
        return String.valueOf((char) ('a' + index));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static final class Footer extends PdfPageEventHelper {
        private final String unitTitle;

        Footer(String unitTitle) {
            this.unitTitle = unitTitle;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            Phrase text = new Phrase("Rupi · " + unitTitle + " · página " + writer.getPageNumber(), SMALL);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, text,
                    (page.getLeft() + page.getRight()) / 2, page.getBottom() + 30, 0);
        }
    }

    /** HU-23 reuses the HU-12 ZIP entries, checksums and manifest conventions. */
    public static byte[] visualSupplement(String html, Object content) throws IOException {
        var out = new ByteArrayOutputStream();
        var entries = new ArrayList<ManifestEntry>();
        try (var zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            putEntry(zip, "ABRIR.html", html.getBytes(StandardCharsets.UTF_8), entries);
            putEntry(zip, CONTENT_ENTRY, JSON.writeValueAsBytes(content), entries);
            putEntry(zip, README_ENTRY, ("Extrae el ZIP y abre ABRIR.html. No requiere internet. "+
                "Es una copia descargada: al volver a conectarte, abre el tema en Rupi para comprobar su vigencia. "+
                "Una retirada no puede eliminar copias de dispositivos desconectados.").getBytes(StandardCharsets.UTF_8), entries);
            zip.putNextEntry(new ZipEntry(MANIFEST_ENTRY));
            zip.write(JSON.writeValueAsBytes(Map.of("formato","rupi-resumen-visual","version",1,"archivos",entries)));
            zip.closeEntry();
        }
        return out.toByteArray();
    }
}
