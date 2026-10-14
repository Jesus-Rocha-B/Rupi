package pe.rupi.api.studypackage;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Referencias verificadas por grado. No descarga ni reproduce libros externos. */
public final class PrimaryStudyCatalog {
    public record Exercise(String prompt, String answer) {}
    public record Entry(int grade, String areaCode, String title, String publisher, int year,
                        String sourceUrl, String checkedOn, String note, List<Exercise> exercises) {}
    private static final List<Entry> ENTRIES = load();
    private PrimaryStudyCatalog() {}

    static List<Entry> all() { return ENTRIES; }

    public static List<Entry> forGradeAndArea(int grade, String areaCode) {
        String area = normalizeArea(areaCode);
        return ENTRIES.stream().filter(e -> e.grade() == grade && e.areaCode().equals(area)).toList();
    }

    private static String normalizeArea(String value) {
        if (value == null) return "";
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "MAT", "MATEMATICA" -> "MATEMATICA";
            case "COM", "COMUNICACION" -> "COMUNICACION";
            case "CYT", "CT", "CIENCIA", "CIENCIA_TECNOLOGIA" -> "CYT";
            default -> "";
        };
    }

    private static List<Entry> load() {
        try (var input = PrimaryStudyCatalog.class.getResourceAsStream("/study/primary-resources.json")) {
            if (input == null) throw new IllegalStateException("Falta el catálogo de repaso de primaria");
            var entries = Arrays.asList(StudyPackageBuilder.JSON.readValue(input, Entry[].class));
            var keys = new HashSet<String>();
            for (var e : entries) {
                URI uri = URI.create(e.sourceUrl());
                if (e.grade() < 1 || e.grade() > 6 || !e.areaCode().equals(normalizeArea(e.areaCode()))
                        || !"https".equals(uri.getScheme()) || !"repositorio.minedu.gob.pe".equals(uri.getHost())
                        || uri.getUserInfo() != null || uri.getPort() != -1
                        || !uri.getPath().matches("/handle/20\\.500\\.12799/[0-9]+")
                        || !keys.add(e.grade() + ":" + e.areaCode()) || e.exercises().isEmpty()) {
                    throw new IllegalStateException("Referencia de primaria inválida: " + e.title());
                }
            }
            return List.copyOf(entries);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el catálogo de primaria", e);
        }
    }
}
