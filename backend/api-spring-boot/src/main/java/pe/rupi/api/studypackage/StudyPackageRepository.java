package pe.rupi.api.studypackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.rupi.api.studypackage.StudyPackageModel.Block;
import pe.rupi.api.studypackage.StudyPackageModel.FileRef;
import pe.rupi.api.studypackage.StudyPackageModel.Option;
import pe.rupi.api.studypackage.StudyPackageModel.Question;
import pe.rupi.api.studypackage.StudyPackageModel.Resource;
import pe.rupi.api.studypackage.StudyPackageModel.Stop;

@Repository
public class StudyPackageRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public StudyPackageRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private record StopRow(String nodeId, String activityId, int sequence, String title, boolean optional,
                           Integer minutes, String instructions) {}

    /** Paradas publicadas de la ruta entre {@code nodeIds}, en el orden de la ruta. Solo lee. */
    public List<Stop> findStops(UUID versionRouteId, Collection<UUID> nodeIds) {
        if (nodeIds.isEmpty()) return List.of();
        var params = new MapSqlParameterSource("route", versionRouteId.toString())
                .addValue("nodes", nodeIds.stream().map(UUID::toString).toList());

        List<StopRow> rows = jdbc.query("""
                SELECT n.id AS node_id, va.id AS activity_id, n.numero_secuencia, va.titulo,
                       n.es_opcional, va.minutos_estimados, va.instrucciones
                FROM aprendizaje_nodo_ruta n
                JOIN aprendizaje_version_actividad va ON va.id = n.version_actividad_id
                JOIN aprendizaje_actividad act ON act.id = va.actividad_id
                WHERE n.version_ruta_id = :route
                  AND n.id IN (:nodes)
                  AND va.estado = 'PUBLICADO'
                  AND act.archivado_en IS NULL
                ORDER BY n.numero_secuencia
                """, params, (rs, i) -> new StopRow(
                rs.getString("node_id"),
                rs.getString("activity_id"),
                rs.getInt("numero_secuencia"),
                rs.getString("titulo"),
                rs.getBoolean("es_opcional"),
                (Integer) rs.getObject("minutos_estimados"),
                rs.getString("instrucciones")
        ));
        if (rows.isEmpty()) return List.of();

        List<String> activityIds = rows.stream().map(StopRow::activityId).distinct().toList();
        Map<String, List<Block>> blocks = findBlocks(activityIds);
        Map<String, List<Question>> questions = findQuestions(activityIds);
        Map<String, List<Resource>> resources = findResources(rows.stream().map(StopRow::nodeId).toList());

        List<Stop> stops = new ArrayList<>();
        for (StopRow row : rows) {
            stops.add(new Stop(row.sequence(), row.title(), row.optional(), row.minutes(), row.instructions(),
                    blocks.getOrDefault(row.activityId(), List.of()),
                    questions.getOrDefault(row.activityId(), List.of()),
                    resources.getOrDefault(row.nodeId(), List.of())));
        }
        return stops;
    }

    private Map<String, List<Block>> findBlocks(List<String> activityIds) {
        Map<String, List<Block>> byActivity = new HashMap<>();
        jdbc.query("""
                SELECT version_actividad_id, tipo, texto_cuerpo, texto_accesibilidad, archivo_multimedia_id
                FROM aprendizaje_bloque_contenido
                WHERE version_actividad_id IN (:ids)
                ORDER BY version_actividad_id, numero_secuencia
                """, new MapSqlParameterSource("ids", activityIds), rs -> {
            byActivity.computeIfAbsent(rs.getString("version_actividad_id"), k -> new ArrayList<>())
                    .add(new Block(rs.getString("tipo"), rs.getString("texto_cuerpo"),
                            rs.getString("texto_accesibilidad"), rs.getString("archivo_multimedia_id")));
        });
        return byActivity;
    }

    private Map<String, List<Question>> findQuestions(List<String> activityIds) {
        record QuestionRow(String id, String activityId, int sequence, String type, String statement, String explanation) {}
        var params = new MapSqlParameterSource("ids", activityIds);
        List<QuestionRow> rows = jdbc.query("""
                SELECT id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion
                FROM evaluacion_pregunta
                WHERE version_actividad_id IN (:ids)
                ORDER BY version_actividad_id, numero_secuencia
                """, params, (rs, i) -> new QuestionRow(rs.getString("id"), rs.getString("version_actividad_id"),
                rs.getInt("numero_secuencia"), rs.getString("tipo"), rs.getString("enunciado"), rs.getString("explicacion")));

        Map<String, List<Option>> options = new HashMap<>();
        jdbc.query("""
                SELECT o.pregunta_id, o.numero_secuencia, o.texto, o.es_correcta
                FROM evaluacion_opcion_respuesta o
                JOIN evaluacion_pregunta q ON q.id = o.pregunta_id
                WHERE q.version_actividad_id IN (:ids)
                ORDER BY o.pregunta_id, o.numero_secuencia
                """, params, rs -> {
            options.computeIfAbsent(rs.getString("pregunta_id"), k -> new ArrayList<>())
                    .add(new Option(rs.getInt("numero_secuencia"), rs.getString("texto"), rs.getBoolean("es_correcta")));
        });

        Map<String, List<Question>> byActivity = new HashMap<>();
        for (QuestionRow q : rows) {
            byActivity.computeIfAbsent(q.activityId(), k -> new ArrayList<>())
                    .add(new Question(q.sequence(), q.type(), q.statement(), q.explanation(),
                            options.getOrDefault(q.id(), List.of())));
        }
        return byActivity;
    }

    private Map<String, List<Resource>> findResources(List<String> nodeIds) {
        Map<String, List<Resource>> byNode = new HashMap<>();
        jdbc.query("""
                SELECT nodo_id, tipo, titulo, url_recurso, archivo_multimedia_id
                FROM aprendizaje_nodo_recurso
                WHERE nodo_id IN (:ids)
                ORDER BY nodo_id, creado_en, id
                """, new MapSqlParameterSource("ids", nodeIds), rs -> {
            byNode.computeIfAbsent(rs.getString("nodo_id"), k -> new ArrayList<>())
                    .add(new Resource(rs.getString("tipo"), rs.getString("titulo"),
                            rs.getString("url_recurso"), rs.getString("archivo_multimedia_id")));
        });
        return byNode;
    }

    public List<FileRef> findFiles(Collection<String> fileIds) {
        if (fileIds.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT id, nombre_original, tipo_mime, COALESCE(tamano_bytes, 0) AS tamano_bytes, HEX(hash_sha256) AS hash_hex
                FROM recurso_archivo_multimedia
                WHERE id IN (:ids)
                ORDER BY id
                """, new MapSqlParameterSource("ids", List.copyOf(fileIds)), (rs, i) -> {
            String hash = rs.getString("hash_hex");
            return new FileRef(rs.getString("id"), rs.getString("nombre_original"), rs.getString("tipo_mime"),
                    rs.getLong("tamano_bytes"), hash == null ? null : hash.toLowerCase(Locale.ROOT));
        });
    }
}
