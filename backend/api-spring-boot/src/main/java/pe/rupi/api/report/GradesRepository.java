package pe.rupi.api.report;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class GradesRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public GradesRepository(NamedParameterJdbcTemplate jdbc) {this.jdbc=jdbc;}
    public record Attempt(String id,String student,String activity,String title,String date,
                          BigDecimal earned,BigDecimal possible,int questions,int graded,int invalid,int unsupported) {
        boolean eligible() {return questions>0 && questions==graded && invalid==0 && unsupported==0
            && GradeCalculator.valid(new GradeCalculator.Score(earned,possible));}
    }
    public List<Attempt> attempts(String room,String route) {return attempts(room,route,null);}
    public List<Attempt> attempts(String room,String route,String student) {
        // Una fila por intento: no multiplicar puntos por nodos que reutilizan una actividad.
        return jdbc.query("""
            SELECT i.id,i.estudiante_id,i.version_actividad_id,v.titulo,
              DATE_FORMAT(MAX(r.respondida_en),'%Y-%m-%dT%H:%i:%s.%fZ') graded_at,
              SUM(r.puntaje_obtenido) earned,SUM(q.puntos) possible,COUNT(q.id) questions,
              SUM(CASE WHEN r.puntaje_obtenido IS NOT NULL AND r.es_correcta IS NOT NULL THEN 1 ELSE 0 END) graded,
              SUM(CASE WHEN r.puntaje_obtenido<0 OR r.puntaje_obtenido>q.puntos THEN 1 ELSE 0 END) invalid,
              SUM(CASE WHEN q.tipo NOT IN ('OPCION_UNICA','VERDADERO_FALSO') THEN 1 ELSE 0 END) unsupported
            FROM evaluacion_intento i
            JOIN aprendizaje_version_actividad v ON v.id=i.version_actividad_id
            JOIN aprendizaje_inscripcion_ruta enrollment ON enrollment.estudiante_id=i.estudiante_id AND enrollment.version_ruta_id=:route
            JOIN evaluacion_pregunta q ON q.version_actividad_id=i.version_actividad_id AND q.puntos>0
            LEFT JOIN evaluacion_respuesta r ON r.intento_id=i.id AND r.pregunta_id=q.id
            WHERE i.estado<>'ABANDONADO' AND (:student IS NULL OR i.estudiante_id=:student)
              AND EXISTS (SELECT 1 FROM escuela_membresia_aula m
                JOIN identidad_cuenta_usuario account ON account.id=m.usuario_id AND account.estado='ACTIVO'
                WHERE m.aula_id=:room AND m.usuario_id=i.estudiante_id AND m.rol='ESTUDIANTE' AND m.estado='ACTIVO')
              AND EXISTS (SELECT 1 FROM aprendizaje_nodo_ruta n WHERE n.version_ruta_id=:route AND n.version_actividad_id=i.version_actividad_id)
              AND (i.inscripcion_id=enrollment.id OR (i.inscripcion_id IS NULL AND NOT EXISTS (
                SELECT 1 FROM aprendizaje_nodo_ruta other WHERE other.version_actividad_id=i.version_actividad_id AND other.version_ruta_id<>:route)))
            GROUP BY i.id,i.estudiante_id,i.version_actividad_id,v.titulo,i.iniciado_en
            ORDER BY MAX(r.respondida_en) DESC,i.iniciado_en DESC,i.id DESC
            """,new MapSqlParameterSource("room",room).addValue("route",route).addValue("student",student),(rs,n)->new Attempt(rs.getString("id"),rs.getString("estudiante_id"),
                rs.getString("version_actividad_id"),rs.getString("titulo"),rs.getString("graded_at"),rs.getBigDecimal("earned"),
                rs.getBigDecimal("possible"),rs.getInt("questions"),rs.getInt("graded"),rs.getInt("invalid"),rs.getInt("unsupported")));
    }
}
