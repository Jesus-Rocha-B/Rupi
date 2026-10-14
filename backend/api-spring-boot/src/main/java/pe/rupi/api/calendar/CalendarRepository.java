package pe.rupi.api.calendar;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import static pe.rupi.api.calendar.CalendarDtos.*;

@Repository
public class CalendarRepository {
    private final JdbcTemplate jdbc;
    public CalendarRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public boolean owns(String teacher, String route) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM aprendizaje_version_ruta v JOIN aprendizaje_ruta r ON r.id=v.ruta_id
            WHERE v.id=? AND r.propietario_id=? AND r.archivado_en IS NULL
            """, Integer.class, route, teacher) > 0;
    }
    public List<Classroom> classrooms(String user, String route, boolean teacher) {
        String access = teacher ? "(a.docente_responsable_id=? OR EXISTS (SELECT 1 FROM escuela_membresia_aula m WHERE m.aula_id=a.id AND m.usuario_id=? AND m.rol='DOCENTE' AND m.estado='ACTIVO'))"
            : "EXISTS (SELECT 1 FROM escuela_membresia_aula m WHERE m.aula_id=a.id AND m.usuario_id=? AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO')";
        String sql = "SELECT a.id,a.nombre FROM escuela_aula a JOIN aprendizaje_version_ruta v ON v.grado_id=a.grado_id WHERE v.id=? AND a.activa=TRUE AND " + access + " ORDER BY a.nombre,a.id";
        Object[] params = teacher ? new Object[]{route,user,user} : new Object[]{route,user};
        return jdbc.query(sql, (rs,n)->new Classroom(rs.getString(1),rs.getString(2)),params);
    }
    public List<Target> targets(String route) {
        return jdbc.query("""
            SELECT DISTINCT u.id,u.titulo,'UNIDAD' tipo FROM aprendizaje_nodo_ruta n
            JOIN curriculo_unidad u ON u.id=n.unidad_id WHERE n.version_ruta_id=?
            UNION ALL
            SELECT n.id,v.titulo,'EVALUACION' tipo FROM aprendizaje_nodo_ruta n
            JOIN aprendizaje_version_actividad v ON v.id=n.version_actividad_id
            JOIN aprendizaje_actividad a ON a.id=v.actividad_id
            WHERE n.version_ruta_id=? AND a.tipo IN ('CUESTIONARIO','RETO') AND a.archivado_en IS NULL
            ORDER BY tipo,titulo,id
            """, (rs,n)->new Target(rs.getString(1),rs.getString(2),rs.getString(3)),route,route);
    }
    public List<Event> events(String route, List<Classroom> rooms) {
        if (rooms.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(rooms.size(), "?"));
        var args = new java.util.ArrayList<Object>(); args.add(route); rooms.forEach(a->args.add(a.id()));
        return jdbc.query("""
            SELECT p.*,a.nombre aula_nombre FROM escuela_planificacion_ruta p
            JOIN escuela_aula a ON a.id=p.aula_id WHERE p.version_ruta_id=? AND p.aula_id IN (
            """ + placeholders + ") ORDER BY p.inicia_en,p.titulo,p.id", (rs,n)->new Event(
                rs.getString("id"),rs.getString("aula_id"),rs.getString("aula_nombre"),rs.getString("tipo"),
                rs.getString("referencia_id"),rs.getString("titulo"),
                rs.getTimestamp("inicia_en").toLocalDateTime().atOffset(java.time.ZoneOffset.UTC).toString(),
                rs.getTimestamp("termina_en").toLocalDateTime().atOffset(java.time.ZoneOffset.UTC).toString()), args.toArray());
    }
    public void save(String teacher, String route, Dates dates, Target target) {
        jdbc.update("""
            INSERT INTO escuela_planificacion_ruta
            (id,aula_id,version_ruta_id,tipo,referencia_id,titulo,inicia_en,termina_en,actualizado_por)
            VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE titulo=VALUES(titulo),inicia_en=VALUES(inicia_en),
            termina_en=VALUES(termina_en),actualizado_por=VALUES(actualizado_por),actualizado_en=UTC_TIMESTAMP(6)
            """, UUID.randomUUID().toString(),dates.classroomId(),route,dates.type(),dates.targetId(),target.title(),
            Timestamp.valueOf(dates.start().withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime()),
            Timestamp.valueOf(dates.end().withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime()),teacher);
    }
    public int delete(String route, String classroom, String event) {
        return jdbc.update("DELETE FROM escuela_planificacion_ruta WHERE id=? AND version_ruta_id=? AND aula_id=?",event,route,classroom);
    }
}
