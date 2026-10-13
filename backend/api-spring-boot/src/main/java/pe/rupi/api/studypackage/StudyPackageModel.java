package pe.rupi.api.studypackage;

import java.util.List;

/** Contenido de una unidad tal como entra al paquete. No lleva datos del estudiante, así la caché se comparte. */
public final class StudyPackageModel {
    private StudyPackageModel() {}

    public record Unit(
            String routeTitle,
            String gradeName,
            String areaName,
            String unitCode,
            String unitTitle,
            List<String> competencies,
            List<Stop> stops
    ) {}

    public record Stop(
            int sequence,
            String title,
            boolean optional,
            Integer minutes,
            String instructions,
            List<Block> blocks,
            List<Question> questions,
            List<Resource> resources
    ) {}

    public record Block(String type, String text, String accessibleText, String fileId) {}

    public record Question(int sequence, String type, String statement, String explanation, List<Option> options) {}

    public record Option(int sequence, String text, boolean correct) {}

    public record Resource(String type, String title, String url, String fileId) {}

    public record FileRef(String id, String originalName, String mimeType, long sizeBytes, String sha256) {}
}
