package pe.rupi.api.learningroute;

import java.sql.ResultSet;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewExerciseBankTest {
    @Test void mapsBankAnswersWithoutUsingLessonQuestionsOrChangingGrades() throws Exception {
        var jdbc=mock(NamedParameterJdbcTemplate.class);
        var rs=mock(ResultSet.class);
        var id=UUID.randomUUID();
        when(rs.getString("id")).thenReturn(id.toString());
        when(rs.getInt("orden")).thenReturn(1);
        when(rs.getInt("correcta")).thenReturn(2);
        when(rs.getString("opcion_1")).thenReturn("3");
        when(rs.getString("opcion_2")).thenReturn("4");
        when(rs.getString("opcion_3")).thenReturn("5");
        when(rs.getString("tema")).thenReturn("Patrones");
        when(rs.getString("enunciado")).thenReturn("2, 4, 6");
        doAnswer(call -> List.of(((RowMapper<?>)call.getArgument(2)).mapRow(rs,0)))
            .when(jdbc).query(anyString(),any(SqlParameterSource.class),any(RowMapper.class));
        var review=ReviewExerciseBank.load(jdbc,UUID.randomUUID().toString());
        var q=review.questions().getFirst();
        assertEquals(3,q.options().size());
        assertEquals("4",q.options().get(1).text());
        assertEquals(q.options().get(1).id().toString(),review.feedback().get(id.toString()).correctOptionId());
        assertEquals(0,q.points().intValue());
        verify(jdbc,never()).update(anyString(),any(SqlParameterSource.class));
    }
}
