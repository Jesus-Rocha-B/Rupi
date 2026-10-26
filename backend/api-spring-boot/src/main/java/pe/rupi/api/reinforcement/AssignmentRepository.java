package pe.rupi.api.reinforcement;

import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class AssignmentRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public AssignmentRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Node(String id,String title,int grade,String area) {}
    public record Assignment(String id,String studentId,String studentName,String room,String route,String node,
        String title,String instructions,String reason,String resourceTitle,String url,String nodeTitle,
        int answered,int correct,String assignedAt,String startedAt,String completedAt,String reflection) {}
    public boolean available() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='aprendizaje_asignacion_refuerzo'",Map.of(),Integer.class)==1;
    }
    public List<Node> nodes(String route) {
        return jdbc.query("""
            SELECT n.id,va.titulo,g.numero_grado,a.codigo
            FROM aprendizaje_nodo_ruta n
            JOIN aprendizaje_version_ruta v ON v.id=n.version_ruta_id
            JOIN aprendizaje_ruta r ON r.id=v.ruta_id AND r.archivado_en IS NULL
            JOIN aprendizaje_version_actividad va ON va.id=n.version_actividad_id AND va.estado='PUBLICADO'
            JOIN aprendizaje_actividad activity ON activity.id=va.actividad_id AND activity.archivado_en IS NULL
            JOIN curriculo_grado g ON g.id=v.grado_id JOIN curriculo_area a ON a.id=v.area_id
            WHERE v.id=:route AND v.estado='PUBLICADO' ORDER BY n.numero_secuencia,n.id
            """,Map.of("route",route),(rs,n)->new Node(rs.getString(1),rs.getString(2),rs.getInt(3),rs.getString(4)));
    }
    public boolean insert(MapSqlParameterSource p) {
        // A unique active key protects concurrent requests. A request key also protects retries after completion.
        int count=jdbc.update("""
            INSERT INTO aprendizaje_asignacion_refuerzo
             (id,solicitud_id,aula_id,docente_id,estudiante_id,ruta_id,nodo_id,recurso_id,titulo,
              instrucciones,motivo,recurso_titulo,recurso_url,nodo_titulo,respondidas_iniciales,aciertos_iniciales)
            VALUES (:id,:request,:room,:teacher,:student,:route,:node,:resource,:title,
              :instructions,:reason,:resourceTitle,:url,:nodeTitle,:answered,:correct)
            ON DUPLICATE KEY UPDATE id=aprendizaje_asignacion_refuerzo.id
            """,p);
        // MySQL affected-row modes differ. Compare the generated ID rather than the affected count.
        return count>=0 && jdbc.queryForObject("SELECT COUNT(*) FROM aprendizaje_asignacion_refuerzo WHERE id=:id",p,Integer.class)==1;
    }
    private static final String SELECT = """
        SELECT x.*,COALESCE(NULLIF(TRIM(CONCAT_WS(' ',p.nombre,p.apellido)),''),u.nombre_usuario) student_name
        FROM aprendizaje_asignacion_refuerzo x
        JOIN identidad_cuenta_usuario u ON u.id=x.estudiante_id AND u.estado='ACTIVO' AND u.desactivado_en IS NULL
        LEFT JOIN identidad_perfil_usuario p ON p.usuario_id=u.id
        JOIN escuela_aula a ON a.id=x.aula_id AND a.activa=TRUE
        WHERE EXISTS (SELECT 1 FROM escuela_membresia_aula m WHERE m.aula_id=x.aula_id
           AND m.usuario_id=x.estudiante_id AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO')
        """;
    public List<Assignment> teacher(String room,String route) {
        return jdbc.query(SELECT+" AND x.aula_id=:room AND x.ruta_id=:route ORDER BY x.asignado_en DESC,x.id",
            Map.of("room",room,"route",route),(rs,n)->map(rs));
    }
    public List<Assignment> student(String student) {
        return jdbc.query(SELECT+"""
             AND x.estudiante_id=:student
             AND EXISTS (SELECT 1 FROM aprendizaje_inscripcion_ruta i
               JOIN aprendizaje_version_ruta v ON v.id=i.version_ruta_id AND v.estado='PUBLICADO'
               JOIN aprendizaje_ruta r ON r.id=v.ruta_id AND r.archivado_en IS NULL
               JOIN aprendizaje_progreso_nodo pn ON pn.inscripcion_id=i.id AND pn.nodo_ruta_id=x.nodo_id
               JOIN aprendizaje_nodo_ruta nr ON nr.id=x.nodo_id AND nr.version_ruta_id=v.id
               JOIN aprendizaje_version_actividad va ON va.id=nr.version_actividad_id AND va.estado='PUBLICADO'
               JOIN aprendizaje_actividad ac ON ac.id=va.actividad_id AND ac.archivado_en IS NULL
               WHERE i.estudiante_id=x.estudiante_id AND i.version_ruta_id=x.ruta_id
                 AND i.estado IN ('ACTIVO','COMPLETADO') AND pn.estado IN ('DISPONIBLE','EN_CURSO','COMPLETADO'))
             ORDER BY x.completado_en IS NOT NULL,x.asignado_en DESC,x.id
            """,Map.of("student",student),(rs,n)->map(rs));
    }
    public void start(String student,String id) {
        jdbc.update("""
            UPDATE aprendizaje_asignacion_refuerzo SET iniciado_en=COALESCE(iniciado_en,UTC_TIMESTAMP(6))
            WHERE id=:id AND estudiante_id=:student AND completado_en IS NULL
            """,Map.of("student",student,"id",id));
    }
    public int complete(String student,String id,String reflection) {
        return jdbc.update("""
            UPDATE aprendizaje_asignacion_refuerzo SET completado_en=UTC_TIMESTAMP(6),reflexion=:reflection
            WHERE id=:id AND estudiante_id=:student AND iniciado_en IS NOT NULL AND completado_en IS NULL
            """,Map.of("student",student,"id",id,"reflection",reflection));
    }
    private static String date(ResultSet rs,String field) throws SQLException {
        var value=rs.getTimestamp(field);return value==null?null:value.toInstant().toString();
    }
    private static Assignment map(ResultSet r) throws SQLException {
        return new Assignment(r.getString("id"),r.getString("estudiante_id"),r.getString("student_name"),
            r.getString("aula_id"),r.getString("ruta_id"),r.getString("nodo_id"),r.getString("titulo"),
            r.getString("instrucciones"),r.getString("motivo"),r.getString("recurso_titulo"),r.getString("recurso_url"),
            r.getString("nodo_titulo"),r.getInt("respondidas_iniciales"),r.getInt("aciertos_iniciales"),
            date(r,"asignado_en"),date(r,"iniciado_en"),date(r,"completado_en"),r.getString("reflexion"));
    }
}
