package pe.rupi.api.learningroute;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import pe.rupi.api.evaluation.EvaluationDtos.*;

/** Shared course/grade practice bank, separate from graded lesson questions. */
final class ReviewExerciseBank {
    static ActivityService.Review load(NamedParameterJdbcTemplate jdbc, String route) {
        var questions = new ArrayList<QuestionDto>();
        var answers = new LinkedHashMap<String, ActivityService.ReviewAnswer>();
        jdbc.query("""
            SELECT b.* FROM evaluacion_banco_repaso b
            JOIN aprendizaje_version_ruta vr ON vr.id=:route
            JOIN curriculo_grado g ON g.id=vr.grado_id AND g.numero_grado=b.grado
            JOIN curriculo_area a ON a.id=vr.area_id
            WHERE b.activo=TRUE AND b.curso=CASE
              WHEN UPPER(a.codigo) IN ('MAT','MATEMATICA') THEN 'MATEMATICA'
              WHEN UPPER(a.codigo) IN ('COM','COMUNICACION') THEN 'COMUNICACION'
              WHEN UPPER(a.codigo) IN ('CYT','CT','CIENCIA_TECNOLOGIA','CIENCIA_Y_TECNOLOGIA') THEN 'CYT'
              ELSE '' END
            ORDER BY b.orden, b.id
            """, new MapSqlParameterSource("route", route), (rs, row) -> {
                var id = UUID.fromString(rs.getString("id"));
                var options = new ArrayList<OptionDto>();
                for (int i=1;i<=3;i++) options.add(new OptionDto(UUID.nameUUIDFromBytes((id+":"+i).getBytes(StandardCharsets.UTF_8)),i,rs.getString("opcion_"+i)));
                String explanation=rs.getString("explicacion");
                questions.add(new QuestionDto(id,rs.getInt("orden"),"OPCION_MULTIPLE",rs.getString("tema")+": "+rs.getString("enunciado"),explanation,BigDecimal.ZERO,options));
                answers.put(id.toString(),new ActivityService.ReviewAnswer(options.get(rs.getInt("correcta")-1).id().toString(),explanation));
                return id;
            });
        return new ActivityService.Review(questions, answers);
    }
}
