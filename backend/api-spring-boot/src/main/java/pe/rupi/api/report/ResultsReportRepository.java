package pe.rupi.api.report;

import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class ResultsReportRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public ResultsReportRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Classroom(String id,String name,int grade) {}
    public record Route(String id,String title,int version,String state) {}
    public record Student(String id,String name,String enrollment,String enrollmentState) {}
    public record Node(String id,String activity,String unit,String unitTitle) {}
    public record Competency(String unit,String code,String name) {}
    public record Answer(String student,String activity,String question,boolean correct) {}
    public record Progress(String student,String node) {}
    public record Question(String activity,String id) {}
    public record Data(List<Student> students,List<Node> nodes,List<Competency> competencies,
                       List<Answer> answers,List<Progress> completed,List<Question> questions) {}
    private static final String MEMBERS = """
        FROM escuela_membresia_aula m
        JOIN identidad_cuenta_usuario account ON account.id=m.usuario_id AND account.estado='ACTIVO'
        WHERE m.aula_id=:room AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO'
        """;
    private MapSqlParameterSource params(String room,String route) {
        return new MapSqlParameterSource("room",room).addValue("route",route);
    }
    public List<Classroom> classrooms(String teacher) {
        return jdbc.query("""
            SELECT a.id,a.nombre,g.numero_grado FROM escuela_aula a JOIN curriculo_grado g ON g.id=a.grado_id
            WHERE a.activa=TRUE AND (a.docente_responsable_id=:teacher OR EXISTS (
              SELECT 1 FROM escuela_membresia_aula m WHERE m.aula_id=a.id AND m.usuario_id=:teacher
              AND m.rol='DOCENTE' AND m.estado='ACTIVO')) ORDER BY a.nombre,a.id
            """,Map.of("teacher",teacher),(rs,n)->new Classroom(rs.getString(1),rs.getString(2),rs.getInt(3)));
    }
    public List<Route> routes(String room) {
        return jdbc.query("""
            SELECT DISTINCT v.id,r.titulo,v.numero_version,v.estado
            FROM aprendizaje_version_ruta v JOIN aprendizaje_ruta r ON r.id=v.ruta_id
            JOIN escuela_aula a ON a.grado_id=v.grado_id AND a.id=:room
            JOIN aprendizaje_inscripcion_ruta i ON i.version_ruta_id=v.id
            WHERE i.estudiante_id IN (SELECT m.usuario_id
            """+MEMBERS+") ORDER BY r.titulo,v.numero_version,v.id",params(room,null),
            (rs,n)->new Route(rs.getString(1),rs.getString(2),rs.getInt(3),rs.getString(4)));
    }
    public Data data(String room,String route) {
        var p=params(room,route);
        var students=jdbc.query("""
            SELECT account.id,COALESCE(NULLIF(TRIM(CONCAT_WS(' ',profile.nombre,profile.apellido)),''),account.nombre_usuario) name,
                   i.id enrollment,i.estado enrollment_state
            FROM escuela_membresia_aula m
            JOIN identidad_cuenta_usuario account ON account.id=m.usuario_id AND account.estado='ACTIVO'
            LEFT JOIN identidad_perfil_usuario profile ON profile.usuario_id=account.id
            LEFT JOIN aprendizaje_inscripcion_ruta i ON i.estudiante_id=account.id AND i.version_ruta_id=:route
            WHERE m.aula_id=:room AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO'
            ORDER BY name,account.id
            """,p,(rs,n)->new Student(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)));
        var nodes=jdbc.query("""
            SELECT n.id,n.version_actividad_id,u.id unit_id,COALESCE(u.titulo,'Sin unidad curricular válida') unit_title
            FROM aprendizaje_nodo_ruta n JOIN aprendizaje_version_ruta v ON v.id=n.version_ruta_id
            LEFT JOIN curriculo_unidad u ON u.id=n.unidad_id AND u.grado_id=v.grado_id
                 AND u.area_id=v.area_id AND u.version_catalogo_id=v.version_catalogo_id
            WHERE v.id=:route ORDER BY COALESCE(u.numero_secuencia,65535),n.numero_secuencia,n.id
            """,p,(rs,n)->new Node(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)));
        var competencies=jdbc.query("""
            SELECT DISTINCT u.id,c.codigo,c.nombre FROM aprendizaje_nodo_ruta n
            JOIN aprendizaje_version_ruta v ON v.id=n.version_ruta_id
            JOIN curriculo_unidad u ON u.id=n.unidad_id AND u.grado_id=v.grado_id
                AND u.area_id=v.area_id AND u.version_catalogo_id=v.version_catalogo_id
            JOIN curriculo_unidad_competencia uc ON uc.unidad_id=u.id
            JOIN curriculo_competencia c ON c.id=uc.competencia_id AND c.area_id=v.area_id
            WHERE v.id=:route ORDER BY c.codigo,u.id
            """,p,(rs,n)->new Competency(rs.getString(1),rs.getString(2),rs.getString(3)));
        var answers=jdbc.query("""
            SELECT estudiante_id,version_actividad_id,pregunta_id,es_correcta FROM (
              SELECT attempt.estudiante_id,attempt.version_actividad_id,r.pregunta_id,r.es_correcta,
                ROW_NUMBER() OVER(PARTITION BY attempt.estudiante_id,r.pregunta_id
                  ORDER BY r.respondida_en DESC,attempt.iniciado_en DESC,r.id DESC) position
              FROM evaluacion_intento attempt
              JOIN evaluacion_respuesta r ON r.intento_id=attempt.id
              JOIN evaluacion_pregunta q ON q.id=r.pregunta_id AND q.version_actividad_id=attempt.version_actividad_id
              JOIN aprendizaje_inscripcion_ruta enrollment ON enrollment.estudiante_id=attempt.estudiante_id
                AND enrollment.version_ruta_id=:route
              WHERE attempt.estado<>'ABANDONADO' AND r.es_correcta IS NOT NULL
                AND q.tipo IN ('OPCION_UNICA','VERDADERO_FALSO') AND q.puntos>0
                AND EXISTS (SELECT 1 FROM aprendizaje_nodo_ruta n WHERE n.version_ruta_id=:route AND n.version_actividad_id=attempt.version_actividad_id)
                AND (attempt.inscripcion_id=enrollment.id OR (attempt.inscripcion_id IS NULL AND NOT EXISTS (
                    SELECT 1 FROM aprendizaje_nodo_ruta other WHERE other.version_actividad_id=attempt.version_actividad_id
                    AND other.version_ruta_id<>:route)))
                AND attempt.estudiante_id IN (SELECT m.usuario_id
            """+MEMBERS+") ) latest WHERE position=1",p,(rs,n)->new Answer(rs.getString(1),rs.getString(2),rs.getString(3),rs.getBoolean(4)));
        var completed=jdbc.query("""
            SELECT i.estudiante_id,p.nodo_ruta_id FROM aprendizaje_progreso_nodo p
            JOIN aprendizaje_inscripcion_ruta i ON i.id=p.inscripcion_id AND i.version_ruta_id=:route
            WHERE p.estado='COMPLETADO' AND i.estudiante_id IN (SELECT m.usuario_id
            """+MEMBERS+")",p,(rs,n)->new Progress(rs.getString(1),rs.getString(2)));
        var questions=jdbc.query("""
            SELECT DISTINCT q.version_actividad_id,q.id FROM evaluacion_pregunta q
            JOIN aprendizaje_nodo_ruta n ON n.version_actividad_id=q.version_actividad_id
            WHERE n.version_ruta_id=:route AND q.tipo IN ('OPCION_UNICA','VERDADERO_FALSO') AND q.puntos>0
            """,p,(rs,n)->new Question(rs.getString(1),rs.getString(2)));
        return new Data(students,nodes,competencies,answers,completed,questions);
    }
}
