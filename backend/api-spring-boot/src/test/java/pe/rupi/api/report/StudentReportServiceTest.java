package pe.rupi.api.report;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static pe.rupi.api.report.ResultsReportRepository.*;

class StudentReportServiceTest {
    ResultsReportRepository repo=mock(ResultsReportRepository.class);
    GradesRepository grades=mock(GradesRepository.class);
    StudentReportRepository links=mock(StudentReportRepository.class);
    ResultsReportService access=new ResultsReportService(repo);
    StudentReportService service=new StudentReportService(access,repo,grades,links);
    @BeforeEach void setup() {
        when(repo.classrooms("t")).thenReturn(List.of(new Classroom("room","Aula",2)));
        when(repo.routes("room")).thenReturn(List.of(new Route("route","Ruta",1,"PUBLICADO")));
        when(repo.members("room")).thenReturn(List.of(new Member("s","Ana","ana01")));
    }
    Data data(String enrollment) {
        return new Data(List.of(new Student("s","Ana",enrollment,enrollment==null?null:"ACTIVO")),
            List.of(new Node("n","a","u","Números"),new Node("n2","a","u2","Repaso")),List.of(),
            enrollment==null?List.of():List.of(new Answer("s","a","q1",true),new Answer("s","a","q2",false)),
            enrollment==null?List.of():List.of(new Progress("s","n")),List.of(new Question("a","q1"),new Question("a","q2")));
    }
    @Test void overviewDeduplicatesEvidenceAcrossUnitsAndUsesExistingRules() {
        when(repo.data("room","route","s")).thenReturn(data("e"));
        when(grades.attempts("room","route","s")).thenReturn(List.of(new GradesRepository.Attempt("i","s","a","Sumas","2026-10-15T10:00:00Z",BigDecimal.ONE,new BigDecimal("2"),2,2,0,0)));
        var result=service.overview("t","room","route","s");
        assertEquals(2,result.answered());assertEquals(2,result.totalQuestions());assertEquals(1,result.correct());assertEquals(1,result.completedNodes());
        assertEquals(2,result.units().size());assertTrue(result.units().stream().allMatch(u->u.status().equals("REQUIERE_ATENCION")));
        assertEquals("C",result.units().getFirst().average().letter());assertEquals(1,result.activities().size());
        assertEquals("Sumas",result.activities().getFirst().title());
        verify(repo,times(1)).data("room","route","s");verify(grades,times(1)).attempts("room","route","s");
    }
    @Test void missingEnrollmentIsNotAZeroGrade() {
        when(repo.data("room","route","s")).thenReturn(data(null));
        var result=service.overview("t","room","route","s");assertNull(result.enrollmentState());assertEquals(0,result.answered());
        assertNull(result.units().getFirst().average());assertEquals("SIN_INSCRIPCION",result.units().getFirst().status());
    }
    @Test void foreignClassroomBlocksOverviewAndRoster() {
        assertThrows(ResponseStatusException.class,()->service.overview("t","other","route","s"));
        assertThrows(ResponseStatusException.class,()->access.members("t","other"));verifyNoInteractions(grades,links);
    }
    @Test void foreignStudentBlocksResultsGradesOverviewAndDataAccess() {
        assertThrows(ResponseStatusException.class,()->access.report("t","room","route","other"));
        assertThrows(ResponseStatusException.class,()->new GradesService(access,repo,grades).report("t","room","route","other"));
        assertThrows(ResponseStatusException.class,()->service.overview("t","room","route","other"));
        verify(repo,never()).data(any(),any(),any());verifyNoInteractions(grades,links);
    }
    @Test void foreignRouteBlocksBeforeReadingStudentEvidence() {
        assertThrows(ResponseStatusException.class,()->service.overview("t","room","other","s"));verifyNoInteractions(grades,links);
        verify(repo,never()).data(any(),any(),any());
    }
    @Test void unknownHistoricalResourceRetainsStatusWithoutInventingUrl() {
        when(repo.data("room","route","s")).thenReturn(data("e"));
        when(links.reinforcements("room","route","s")).thenReturn(List.of(new StudentReportRepository.Link("r","n","Sumas","unknown","2026-10-15T10:00:00Z",null,2,"MAT")));
        var resource=service.overview("t","room","route","s").reinforcements().getFirst();assertNull(resource.url());assertNull(resource.completedAt());
        assertEquals("Recurso fuera del catálogo actual",resource.title());
    }
    @Test void gradeCsvUsesDisplayedGradesAndEscapesFormulaNames() {
        var report=new GradesService.Report(new Classroom("r","Aula",2),new Route("v","Ruta",1,"PUBLICADO"),"date",List.of(new GradesService.Unit("u","Unidad")),
            List.of(new GradesService.Row("s","=SUM(1)",List.of(new GradesService.Cell("u",new GradeCalculator.Average(new BigDecimal("14.0"),14,"A"),2,List.of())))),0);
        var csv=GradesService.csv(report);assertTrue(csv.startsWith("\ufeffAula,"));assertTrue(csv.contains("'=SUM(1)"));assertTrue(csv.contains("\"14.0\",\"A\""));assertTrue(csv.endsWith("\r\n"));
    }
}
