package pe.rupi.api.report;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static pe.rupi.api.report.GradeCalculator.*;
import static pe.rupi.api.report.ResultsReportRepository.*;

class GradesServiceTest {
    Score score(String earned,String possible) {return new Score(new BigDecimal(earned),new BigDecimal(possible));}
    @Test void emptyIsNotZero() {
        assertNull(average(List.of()));assertEquals("C",average(List.of(score("0","10"))).letter());
        assertEquals(new BigDecimal("0.0"),average(List.of(score("0","10"))).value());
    }
    @Test void thresholdsUseExactAverageBeforePresentationRounding() {
        String[][] cases={{"0","C"},{"10.999","C"},{"11","B"},{"13.999","B"},{"14","A"},{"17.999","A"},{"18","AD"},{"20","AD"}};
        for(var item:cases) assertEquals(item[1],average(List.of(score(item[0],"20"))).letter());
        assertEquals(new BigDecimal("18.0"),average(List.of(score("17.999","20"))).value());
    }
    @Test void activitiesHaveEqualWeightRegardlessOfMaximumPoints() {
        var result=average(List.of(score("1","2"),score("100","100")));
        assertEquals(new BigDecimal("15.0"),result.value());assertEquals("A",result.letter());
        assertEquals(new BigDecimal("13.3"),average(List.of(score("2","3"))).value());
    }
    @Test void invalidPointsAreRejected() {
        for(var bad:List.of(score("-1","2"),score("3","2"),score("0","0"),new Score(null,BigDecimal.ONE)))
            assertThrows(IllegalArgumentException.class,()->average(List.of(bad)));
    }
    GradesRepository.Attempt attempt(String id,String student,String activity,String earned,int graded) {
        return new GradesRepository.Attempt(id,student,activity,"Actividad","2026-10-15T10:00:00.000000Z",new BigDecimal(earned),new BigDecimal("2"),2,graded,0,0);
    }
    GradesService.Report report(List<GradesRepository.Attempt> attempts) {
        var data=new Data(List.of(new Student("s","Ana","e","ACTIVO"),new Student("s2","Luis","e2","ACTIVO"),new Student("s3","Eva",null,null)),
            List.of(new Node("n","a","u","Números"),new Node("n2","a","u","Números"),new Node("n3","a2","u2","Formas"),new Node("n4","a",null,"Sin unidad")),
            List.of(),List.of(),List.of(),List.of());
        return GradesService.summarize(new Classroom("room","Aula",2),new Route("route","Ruta",1,"PUBLICADO"),data,attempts);
    }
    @Test void latestCompleteAttemptIsUsedOnceAndPartialRetryDoesNotReplaceIt() {
        var result=report(List.of(attempt("partial","s","a","0",1),attempt("new","s","a","1",2),attempt("old","s","a","2",2)));
        var cell=result.students().getFirst().cells().getFirst();assertEquals(1,cell.totalActivities());
        assertEquals(1,cell.details().size());assertEquals("new",cell.details().getFirst().attemptId());
        assertEquals(new BigDecimal("10.0"),cell.average().value());assertEquals(cell.average().value(),cell.details().getFirst().grade());
        assertEquals(1,result.unassignedNodes());
    }
    @Test void studentsUnitsAndMissingEnrollmentRemainIsolated() {
        var result=report(List.of(attempt("a","s","a","2",2),attempt("foreign","s3","a","2",2)));
        assertNull(result.students().getFirst().cells().get(1).average());
        assertNull(result.students().get(1).cells().getFirst().average());
        assertNull(result.students().get(2).cells().getFirst().average());
    }
    @Test void invalidOrUnsupportedAttemptsCannotBecomeGrades() {
        assertFalse(attempt("bad","s","a","3",2).eligible());
        assertFalse(new GradesRepository.Attempt("bad","s","a","Title","date",BigDecimal.ONE,BigDecimal.TEN,1,1,0,1).eligible());
        assertNull(report(List.of(attempt("bad","s","a","3",2))).students().getFirst().cells().getFirst().average());
    }
    @Test void unauthorizedClassroomNeverReadsGrades() {
        var repo=mock(ResultsReportRepository.class);var grades=mock(GradesRepository.class);
        var service=new GradesService(new ResultsReportService(repo),repo,grades);
        assertThrows(ResponseStatusException.class,()->service.report("teacher","foreign","route"));
        verifyNoInteractions(grades);verify(repo,never()).data(any(),any());
    }
    @Test void unauthorizedRouteNeverReadsGrades() {
        var repo=mock(ResultsReportRepository.class);var grades=mock(GradesRepository.class);
        when(repo.classrooms("teacher")).thenReturn(List.of(new Classroom("room","Aula",2)));
        var service=new GradesService(new ResultsReportService(repo),repo,grades);
        assertThrows(ResponseStatusException.class,()->service.report("teacher","room","foreign"));verifyNoInteractions(grades);
    }
}
