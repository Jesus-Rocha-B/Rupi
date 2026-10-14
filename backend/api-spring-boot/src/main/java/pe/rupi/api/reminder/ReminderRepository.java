package pe.rupi.api.reminder;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReminderRepository {
    private final JdbcTemplate jdbc;
    public ReminderRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Reminder(String id, String classroomId, String classroomName, String title,
                           String startsAt, boolean read) {}

    // La misma elegibilidad se aplica al envío y a la lectura: una baja de aula o ruta revoca la visibilidad.
    private static final String ELIGIBLE = """
        JOIN escuela_aula a ON a.id=p.aula_id AND a.activa=TRUE
        JOIN escuela_membresia_aula m ON m.aula_id=a.id AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO'
        JOIN identidad_cuenta_usuario u ON u.id=m.usuario_id AND u.estado='ACTIVO' AND u.desactivado_en IS NULL
        JOIN aprendizaje_inscripcion_ruta i ON i.estudiante_id=m.usuario_id AND i.version_ruta_id=p.version_ruta_id
            AND i.estado IN ('ACTIVO','COMPLETADO')
        JOIN aprendizaje_version_ruta v ON v.id=p.version_ruta_id AND v.estado='PUBLICADO' AND v.grado_id=a.grado_id
        JOIN aprendizaje_ruta ruta ON ruta.id=v.ruta_id AND ruta.archivado_en IS NULL
        JOIN aprendizaje_nodo_ruta n ON n.id=p.referencia_id AND n.version_ruta_id=v.id
        JOIN aprendizaje_version_actividad av ON av.id=n.version_actividad_id AND av.estado='PUBLICADO'
        JOIN aprendizaje_actividad actividad ON actividad.id=av.actividad_id
            AND actividad.tipo IN ('CUESTIONARIO','RETO') AND actividad.archivado_en IS NULL
        """;

    /** INSERT atómico y clave única: repetir el barrido o ejecutarlo en dos servidores no duplica avisos. */
    public void generate(Instant now, Instant until) {
        jdbc.update("""
            INSERT INTO escuela_recordatorio_evaluacion
                (id,estudiante_id,aula_id,version_ruta_id,nodo_id,fecha_evaluacion,creado_en)
            SELECT UUID(),m.usuario_id,p.aula_id,p.version_ruta_id,p.referencia_id,p.inicia_en,?
            FROM escuela_planificacion_ruta p
            """ + ELIGIBLE + """
            WHERE p.tipo='EVALUACION' AND p.inicia_en>? AND p.inicia_en<=?
            ON DUPLICATE KEY UPDATE id=escuela_recordatorio_evaluacion.id
            """, timestamp(now), timestamp(now), timestamp(until));
    }

    public List<Reminder> visible(String student, String route, Instant now) {
        return jdbc.query("""
            SELECT r.id,p.aula_id,a.nombre,p.titulo,p.inicia_en,r.leido_en
            FROM escuela_planificacion_ruta p
            """ + ELIGIBLE + """
            JOIN escuela_recordatorio_evaluacion r ON r.estudiante_id=m.usuario_id
                AND r.aula_id=p.aula_id AND r.version_ruta_id=p.version_ruta_id
                AND r.nodo_id=p.referencia_id AND r.fecha_evaluacion=p.inicia_en
            WHERE p.tipo='EVALUACION' AND m.usuario_id=? AND p.version_ruta_id=? AND p.inicia_en>?
            ORDER BY p.inicia_en,p.titulo,r.id
            """, (rs,row)->new Reminder(rs.getString("id"),rs.getString("aula_id"),rs.getString("nombre"),
                rs.getString("titulo"),rs.getTimestamp("inicia_en").toLocalDateTime().atOffset(ZoneOffset.UTC).toString(),
                rs.getTimestamp("leido_en")!=null), student,route,timestamp(now));
    }

    public void markRead(String student, String id, Instant now) {
        jdbc.update("UPDATE escuela_recordatorio_evaluacion SET leido_en=COALESCE(leido_en,?) WHERE id=? AND estudiante_id=?",
                timestamp(now),id,student);
    }

    static Timestamp timestamp(Instant instant) { return Timestamp.valueOf(instant.atOffset(ZoneOffset.UTC).toLocalDateTime()); }
}
