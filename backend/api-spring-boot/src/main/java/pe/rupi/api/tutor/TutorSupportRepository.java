package pe.rupi.api.tutor;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

/** HU-15: modo de asistencia del tutor Rupi por aula. Spring escribe solo escuela_aula_tutor_ia. */
@Repository
public class TutorSupportRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public TutorSupportRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }

    public record Classroom(String id,String name,int grade) {}
    public record Setting(String mode,Instant updatedAt,String updatedBy) {}

    private static final String TEACHER_ACCESS = """
        (a.docente_responsable_id=:user OR EXISTS (SELECT 1 FROM escuela_membresia_aula m
          WHERE m.aula_id=a.id AND m.usuario_id=:user AND m.rol='DOCENTE' AND m.estado='ACTIVO'))
        """;

    /** Aula activa a cargo del docente, o vacío si no le pertenece. */
    public Optional<Classroom> teacherClassroom(String teacher,String room) {
        return jdbc.query("SELECT a.id,a.nombre,g.numero_grado FROM escuela_aula a JOIN curriculo_grado g ON g.id=a.grado_id"
                + " WHERE a.id=:room AND a.activa=TRUE AND " + TEACHER_ACCESS,
            new MapSqlParameterSource("room",room).addValue("user",teacher),
            (rs,n)->new Classroom(rs.getString(1),rs.getString(2),rs.getInt(3))).stream().findFirst();
    }

    /**
     * Aulas activas del grado de la ruta donde el usuario estudia. El docente que revisa su ruta
     * en la vista de estudiante ve sus propias aulas de ese grado.
     */
    public List<Classroom> routeClassrooms(String user,String route) {
        return jdbc.query("""
            SELECT a.id,a.nombre,g.numero_grado FROM escuela_aula a
            JOIN curriculo_grado g ON g.id=a.grado_id
            JOIN aprendizaje_version_ruta v ON v.grado_id=a.grado_id
            WHERE v.id=:route AND a.activa=TRUE AND (EXISTS (SELECT 1 FROM escuela_membresia_aula m
              WHERE m.aula_id=a.id AND m.usuario_id=:user AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO') OR """
            + TEACHER_ACCESS + ") ORDER BY a.nombre,a.id",
            new MapSqlParameterSource("route",route).addValue("user",user),
            (rs,n)->new Classroom(rs.getString(1),rs.getString(2),rs.getInt(3)));
    }

    public Optional<Setting> setting(String room) {
        return jdbc.query("""
            SELECT t.modo_asistencia,t.actualizado_en,
              COALESCE(NULLIF(TRIM(CONCAT_WS(' ',p.nombre,p.apellido)),''),c.nombre_usuario)
            FROM escuela_aula_tutor_ia t
            JOIN identidad_cuenta_usuario c ON c.id=t.actualizado_por
            LEFT JOIN identidad_perfil_usuario p ON p.usuario_id=c.id
            WHERE t.aula_id=:room
            """,Map.of("room",room),(rs,n)->new Setting(rs.getString(1),
                rs.getObject(2,LocalDateTime.class).toInstant(ZoneOffset.UTC),rs.getString(3))).stream().findFirst();
    }

    public void save(String room,String mode,String teacher) {
        jdbc.update("""
            INSERT INTO escuela_aula_tutor_ia (aula_id,modo_asistencia,actualizado_por,actualizado_en)
            VALUES (:room,:mode,:teacher,UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE modo_asistencia=VALUES(modo_asistencia),
              actualizado_por=VALUES(actualizado_por),actualizado_en=VALUES(actualizado_en)
            """,new MapSqlParameterSource("room",room).addValue("mode",mode).addValue("teacher",teacher));
    }
}
