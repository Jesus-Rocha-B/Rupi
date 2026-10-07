package pe.rupi.api.learningroute;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivityService {
    private final NamedParameterJdbcTemplate jdbc;
    public ActivityService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Block(String id, String type, String text, String url, String accessibleText) {}
    public record Activity(String nodeId, String title, String instructions, String state, List<Block> blocks) {}
    record Access(String enrollmentId, String activityId, String title, String instructions, String state) {}

    @Transactional
    public Activity start(UUID studentId, UUID routeId, UUID nodeId) {
        var params = new MapSqlParameterSource("student", studentId.toString())
                .addValue("route", routeId.toString()).addValue("node", nodeId.toString());
        // Lock the enrollment and progress together. Repeated starts preserve first access and completion.
        var access = jdbc.query("""
                SELECT i.id AS enrollment_id, va.id AS activity_id, va.titulo, va.instrucciones, p.estado
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_version_ruta vr ON vr.id = i.version_ruta_id AND vr.estado = 'PUBLICADO'
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id AND r.archivado_en IS NULL
                JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id = vr.id AND n.id = :node
                JOIN aprendizaje_version_actividad va ON va.id = n.version_actividad_id AND va.estado = 'PUBLICADO'
                JOIN aprendizaje_actividad a ON a.id = va.actividad_id AND a.archivado_en IS NULL
                JOIN aprendizaje_progreso_nodo p ON p.inscripcion_id = i.id
                    AND p.version_ruta_id = vr.id AND p.nodo_ruta_id = n.id
                WHERE i.estudiante_id = :student AND i.version_ruta_id = :route
                    AND i.estado IN ('ACTIVO','COMPLETADO')
                FOR UPDATE OF i, p
                """, params, (rs, row) -> new Access(rs.getString("enrollment_id"), rs.getString("activity_id"),
                    rs.getString("titulo"), rs.getString("instrucciones"), rs.getString("estado")));
        if (access.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var item = access.getFirst();
        if (!List.of("DISPONIBLE", "EN_CURSO", "COMPLETADO").contains(item.state()))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        params.addValue("activity", item.activityId()).addValue("enrollment", item.enrollmentId());
        var blocks = jdbc.query("""
                SELECT b.id, b.tipo, b.texto_cuerpo, b.texto_accesibilidad, m.url_externa
                FROM aprendizaje_bloque_contenido b
                LEFT JOIN recurso_archivo_multimedia m ON m.id = b.archivo_multimedia_id
                WHERE b.version_actividad_id = :activity ORDER BY b.numero_secuencia, b.id
                """, params, (rs, row) -> new Block(rs.getString("id"), rs.getString("tipo"),
                    rs.getString("texto_cuerpo"), rs.getString("url_externa"), rs.getString("texto_accesibilidad")));
        if (blocks.isEmpty() && (item.instructions() == null || item.instructions().isBlank()))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        jdbc.update("""
                UPDATE aprendizaje_progreso_nodo SET estado = CASE WHEN estado = 'DISPONIBLE' THEN 'EN_CURSO' ELSE estado END,
                    primer_acceso_en = COALESCE(primer_acceso_en, UTC_TIMESTAMP(6))
                WHERE inscripcion_id = :enrollment AND version_ruta_id = :route AND nodo_ruta_id = :node
                """, params);
        jdbc.update("""
                UPDATE aprendizaje_inscripcion_ruta SET ultimo_nodo_visitado_id = :node, ultima_visita_en = UTC_TIMESTAMP(6)
                WHERE id = :enrollment AND estudiante_id = :student AND version_ruta_id = :route
                """, params);
        return new Activity(nodeId.toString(), item.title(), item.instructions(),
                "DISPONIBLE".equals(item.state()) ? "EN_CURSO" : item.state(), blocks);
    }
}
