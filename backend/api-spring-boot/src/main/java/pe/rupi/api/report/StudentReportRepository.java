package pe.rupi.api.report;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StudentReportRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public StudentReportRepository(NamedParameterJdbcTemplate jdbc) {this.jdbc=jdbc;}
    public record Link(String id,String node,String title,String resource,String linkedAt,String completedAt,int grade,String area) {}
    public record Stop(String id,int sequence,String title,String activity,String unitId,String unitTitle,boolean optional,String state,String completedAt) {}
    /** Paradas de la ruta en el orden del mapa, con el estado del estudiante; sin inscripción el estado queda nulo. */
    public List<Stop> roadmap(String route,String student) {
        return jdbc.query("""
            SELECT n.id,n.numero_secuencia,activity.titulo,n.version_actividad_id,u.id unit_id,
              COALESCE(u.titulo,'Sin unidad curricular válida') unit_title,n.es_opcional,
              CASE WHEN i.id IS NULL THEN NULL ELSE COALESCE(p.estado,'BLOQUEADO') END estado,
              DATE_FORMAT(p.completado_en,'%Y-%m-%dT%H:%i:%s.%fZ') completed_at
            FROM aprendizaje_nodo_ruta n
            JOIN aprendizaje_version_ruta v ON v.id=n.version_ruta_id
            JOIN aprendizaje_version_actividad activity ON activity.id=n.version_actividad_id
            LEFT JOIN curriculo_unidad u ON u.id=n.unidad_id AND u.grado_id=v.grado_id
                 AND u.area_id=v.area_id AND u.version_catalogo_id=v.version_catalogo_id
            LEFT JOIN aprendizaje_inscripcion_ruta i ON i.version_ruta_id=v.id AND i.estudiante_id=:student
            LEFT JOIN aprendizaje_progreso_nodo p ON p.inscripcion_id=i.id AND p.nodo_ruta_id=n.id
            WHERE v.id=:route ORDER BY n.numero_secuencia,n.id
            """,Map.of("route",route,"student",student),(rs,n)->new Stop(rs.getString("id"),rs.getInt("numero_secuencia"),
                rs.getString("titulo"),rs.getString("version_actividad_id"),rs.getString("unit_id"),rs.getString("unit_title"),
                rs.getBoolean("es_opcional"),rs.getString("estado"),rs.getString("completed_at")));
    }
    public List<Link> reinforcements(String room,String route,String student) {
        return jdbc.query("""
            SELECT ref.id,n.id node,activity.titulo,ref.recurso_id,
              DATE_FORMAT(ref.vinculado_en,'%Y-%m-%dT%H:%i:%s.%fZ') linked_at,
              DATE_FORMAT(ref.realizado_en,'%Y-%m-%dT%H:%i:%s.%fZ') completed_at,g.numero_grado,area.codigo
            FROM aprendizaje_refuerzo_nodo ref
            JOIN aprendizaje_nodo_ruta n ON n.id=ref.nodo_id AND n.version_ruta_id=:route
            JOIN aprendizaje_version_ruta v ON v.id=n.version_ruta_id
            JOIN aprendizaje_version_actividad activity ON activity.id=n.version_actividad_id
            JOIN curriculo_grado g ON g.id=v.grado_id JOIN curriculo_area area ON area.id=v.area_id
            JOIN aprendizaje_inscripcion_ruta i ON i.version_ruta_id=v.id AND i.estudiante_id=ref.estudiante_id
            WHERE ref.estudiante_id=:student AND EXISTS (
              SELECT 1 FROM escuela_membresia_aula m JOIN identidad_cuenta_usuario account ON account.id=m.usuario_id AND account.estado='ACTIVO'
              WHERE m.aula_id=:room AND m.usuario_id=ref.estudiante_id AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO')
            ORDER BY n.numero_secuencia,ref.vinculado_en,ref.id
            """,Map.of("room",room,"route",route,"student",student),(rs,n)->new Link(rs.getString("id"),rs.getString("node"),
                rs.getString("titulo"),rs.getString("recurso_id"),rs.getString("linked_at"),rs.getString("completed_at"),rs.getInt("numero_grado"),rs.getString("codigo")));
    }
}
