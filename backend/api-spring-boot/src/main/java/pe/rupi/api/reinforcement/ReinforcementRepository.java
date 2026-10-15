package pe.rupi.api.reinforcement;

import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class ReinforcementRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public ReinforcementRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Context(String activity, String title, int grade, String area) {}
    public record Evidence(String question, boolean correct, String hint) {}
    public record Link(String id, String resourceId, boolean completed) {}
    static MapSqlParameterSource params(UUID student, UUID route, UUID node) {
        return new MapSqlParameterSource("student", student.toString())
                .addValue("route", route.toString()).addValue("node", node.toString());
    }
    public Optional<Context> context(UUID student, UUID route, UUID node) {
        return jdbc.query("""
            SELECT va.id activity, va.titulo, g.numero_grado, area.codigo
            FROM aprendizaje_inscripcion_ruta i
            JOIN identidad_cuenta_usuario u ON u.id=i.estudiante_id AND u.estado='ACTIVO'
            JOIN aprendizaje_version_ruta v ON v.id=i.version_ruta_id AND v.estado='PUBLICADO'
            JOIN aprendizaje_ruta r ON r.id=v.ruta_id AND r.archivado_en IS NULL
            JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id=v.id AND n.id=:node
            JOIN aprendizaje_version_actividad va ON va.id=n.version_actividad_id AND va.estado='PUBLICADO'
            JOIN aprendizaje_actividad a ON a.id=va.actividad_id AND a.archivado_en IS NULL
            JOIN aprendizaje_progreso_nodo p ON p.inscripcion_id=i.id AND p.nodo_ruta_id=n.id
            JOIN curriculo_grado g ON g.id=v.grado_id
            JOIN curriculo_area area ON area.id=v.area_id
            WHERE i.estudiante_id=:student AND v.id=:route AND i.estado IN ('ACTIVO','COMPLETADO')
              AND p.estado IN ('DISPONIBLE','EN_CURSO','COMPLETADO')
            """,params(student,route,node),(rs,n)->new Context(rs.getString("activity"),rs.getString("titulo"),
                    rs.getInt("numero_grado"),rs.getString("codigo"))).stream().findFirst();
    }
    /** Una sola evidencia por pregunta: la respuesta más reciente, sin penalizar reintentos. */
    public List<Evidence> evidence(UUID student, String activity) {
        return jdbc.query("""
            SELECT enunciado, es_correcta, explicacion FROM (
              SELECT q.enunciado, r.es_correcta, q.explicacion,
                ROW_NUMBER() OVER (PARTITION BY q.id ORDER BY r.respondida_en DESC, i.iniciado_en DESC, r.id DESC) AS position
              FROM evaluacion_intento i
              JOIN evaluacion_respuesta r ON r.intento_id=i.id
              JOIN evaluacion_pregunta q ON q.id=r.pregunta_id AND q.version_actividad_id=i.version_actividad_id
              WHERE i.estudiante_id=:student AND i.version_actividad_id=:activity
                AND i.estado<>'ABANDONADO' AND r.es_correcta IS NOT NULL
                AND q.tipo IN ('OPCION_UNICA','VERDADERO_FALSO') AND q.puntos>0
            ) latest WHERE position=1
            """,new MapSqlParameterSource("student",student.toString()).addValue("activity",activity),
            (rs,n)->new Evidence(rs.getString("enunciado"),rs.getBoolean("es_correcta"),rs.getString("explicacion")));
    }
    public void link(UUID student, UUID node, String resource) {
        jdbc.update("""
            INSERT INTO aprendizaje_refuerzo_nodo(id,estudiante_id,nodo_id,recurso_id)
            VALUES (:id,:student,:node,:resource)
            ON DUPLICATE KEY UPDATE id=aprendizaje_refuerzo_nodo.id
            """,params(student,node,node).addValue("id",UUID.randomUUID().toString()).addValue("resource",resource));
    }
    public List<Link> links(UUID student, UUID node) {
        return jdbc.query("""
            SELECT id,recurso_id,realizado_en FROM aprendizaje_refuerzo_nodo
            WHERE estudiante_id=:student AND nodo_id=:node ORDER BY vinculado_en,id
            """,params(student,node,node),(rs,n)->new Link(rs.getString("id"),rs.getString("recurso_id"),rs.getTimestamp("realizado_en")!=null));
    }
    public void complete(UUID student, UUID node, UUID id) {
        jdbc.update("""
            UPDATE aprendizaje_refuerzo_nodo SET realizado_en=COALESCE(realizado_en,UTC_TIMESTAMP(6))
            WHERE estudiante_id=:student AND nodo_id=:node AND id=:id
            """,params(student,node,node).addValue("id",id.toString()));
    }
}
