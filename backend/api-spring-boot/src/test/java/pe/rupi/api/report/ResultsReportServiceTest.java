package pe.rupi.api.report;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static pe.rupi.api.report.ResultsReportRepository.*;

class ResultsReportServiceTest {
    Student student=new Student("student","Lucía","enrollment","ACTIVO");
    Node node=new Node("node","activity","unit","Números");
    Data data(List<Student> students,List<Node> nodes,List<Answer> answers) {
        return new Data(students,nodes,List.of(new Competency("unit","MAT-CANT","Resuelve problemas de cantidad")),answers,
            List.of(new Progress("student","node")),List.of(new Question("activity","q1"),new Question("activity","q2")));
    }
    List<Answer> answers(int correct,int total) {
        var rows=new ArrayList<Answer>();for(int i=0;i<total;i++)rows.add(new Answer("student","activity","q"+i,i<correct));return rows;
    }
    @Test void lackOfEvidenceIsNotLowPerformance() {
        var unit=ResultsReportService.summarize(data(List.of(student),List.of(node),List.of())).getFirst();
        var row=unit.students().getFirst();assertNull(row.accuracy());assertEquals("SIN_RESPUESTAS",row.status());assertEquals(0,unit.attentionCount());
        assertEquals("EVIDENCIA_INSUFICIENTE",ResultsReportService.summarize(data(List.of(student),List.of(node),answers(0,1))).getFirst().students().getFirst().status());
    }
    @Test void thresholdIsStrictAndCompletionIsSeparateFromCorrectness() {
        var unit=ResultsReportService.summarize(data(List.of(student),List.of(node),answers(1,2))).getFirst();
        var row=unit.students().getFirst();assertEquals(50.0,row.accuracy());assertEquals(1,row.completedNodes());assertEquals(1,unit.attentionCount());
        assertEquals("MAT-CANT",unit.competencies().getFirst().code());assertEquals("REQUIERE_ATENCION",row.status());
        assertEquals("EN_AVANCE",ResultsReportService.summarize(data(List.of(student),List.of(node),answers(7,10))).getFirst().students().getFirst().status());
    }
    @Test void repeatedActivityInSameUnitDoesNotDoubleCountQuestions() {
        var duplicate=new Node("node2","activity","unit","Números");
        var row=ResultsReportService.summarize(data(List.of(student),List.of(node,duplicate),answers(1,2))).getFirst().students().getFirst();
        assertEquals(2,row.answered());assertEquals(2,row.totalQuestions());assertEquals(2,row.totalNodes());
    }
    @Test void isolatesUnitsAndStudents() {
        var other=new Student("other","José","e2","ACTIVO");
        var otherNode=new Node("n2","a2","u2","Lectura");
        var report=ResultsReportService.summarize(data(List.of(student,other),List.of(node,otherNode),answers(0,2)));
        assertEquals(1,report.getFirst().attentionCount());assertEquals("SIN_RESPUESTAS",report.getFirst().students().get(1).status());
        assertEquals(0,report.get(1).attentionCount());assertTrue(report.get(1).competencies().isEmpty());
    }
    @Test void includesUnenrolledStudentsWithoutClaimingFailure() {
        var noEnrollment=new Student("student","Lucía",null,null);
        var row=ResultsReportService.summarize(data(List.of(noEnrollment),List.of(node),answers(0,2))).getFirst().students().getFirst();
        assertEquals("SIN_INSCRIPCION",row.status());assertEquals(0,row.answered());assertEquals(0,row.completedNodes());
    }
    @Test void unassignedUnitIsExplicitAndHasNoInventedCompetencies() {
        var missing=new Node("node","activity",null,"Sin unidad curricular válida");
        var unit=ResultsReportService.summarize(data(List.of(student),List.of(missing),answers(0,2))).getFirst();
        assertEquals("SIN_UNIDAD",unit.id());assertTrue(unit.competencies().isEmpty());
    }
    @Test void deniesForeignClassroomBeforeReadingAnyResults() {
        var repo=mock(ResultsReportRepository.class);when(repo.classrooms("teacher")).thenReturn(List.of());
        var service=new ResultsReportService(repo);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.report("teacher","foreign","route")).getStatusCode().value());
        verify(repo,never()).data(any(),any());verify(repo,never()).routes(any());
    }
    @Test void deniesRouteNotAssociatedWithClassroom() {
        var repo=mock(ResultsReportRepository.class);when(repo.classrooms("teacher")).thenReturn(List.of(new Classroom("room","2 A",2)));
        when(repo.routes("room")).thenReturn(List.of());var service=new ResultsReportService(repo);
        assertThrows(ResponseStatusException.class,()->service.report("teacher","room","route"));verify(repo,never()).data(any(),any());
    }
    @Test void csvEscapesQuotesNewlinesAndSpreadsheetFormulas() {
        assertEquals("\"' =1+1\"",ResultsReportService.cell(" =1+1"));
        assertEquals("\"'@SUM(A1)\"",ResultsReportService.cell("@SUM(A1)"));
        assertEquals("\"'+123\"",ResultsReportService.cell("+123"));
        assertEquals("\"'-123\"",ResultsReportService.cell("-123"));
        assertEquals("\"Lucía, \"\"Luz\"\"\nPérez\"",ResultsReportService.cell("Lucía, \"Luz\"\nPérez"));
    }
    @Test void csvContainsSameRowsAndKeepsEmptyAccuracyEmpty() {
        var units=ResultsReportService.summarize(data(List.of(student),List.of(node),List.of()));
        var report=new ResultsReportService.Report(new Classroom("room","2 A",2),new Route("route","Matemática",1,"PUBLICADO"),"2026-10-14T10:00:00Z",1,units);
        var csv=ResultsReportService.csv(report);assertTrue(csv.startsWith("\ufeffAula,"));assertTrue(csv.contains("Lucía"));
        assertTrue(csv.contains("\"\",\"SIN_RESPUESTAS\""));assertTrue(csv.contains("MAT-CANT"));assertTrue(csv.endsWith("\r\n"));
    }
}
