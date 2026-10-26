package pe.rupi.api.reinforcement;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import pe.rupi.api.report.*;
import static pe.rupi.api.report.ResultsReportRepository.*;
import pe.rupi.api.studypackage.PrimaryStudyCatalog;

class AssignmentServiceTest {
    AssignmentRepository repo=mock(AssignmentRepository.class);
    ResultsReportService access=mock(ResultsReportService.class);
    ResultsReportRepository reports=mock(ResultsReportRepository.class);
    ReinforcementRepository reinforcement=mock(ReinforcementRepository.class);
    AssignmentService service=new AssignmentService(repo,access,reports,reinforcement);
    String room=UUID.randomUUID().toString(),route=UUID.randomUUID().toString(),teacher=UUID.randomUUID().toString();
    UUID student=UUID.randomUUID(),other=UUID.randomUUID(),node=UUID.randomUUID();
    String resource=ReinforcementService.resourceId(PrimaryStudyCatalog.forGradeAndArea(3,"MAT").getFirst());
    Data evidence(int count,int correct) {
        var answers=new ArrayList<Answer>();
        for(int i=0;i<count;i++) answers.add(new Answer(student.toString(),"activity","q"+i,i<correct));
        return new Data(List.of(new Student(student.toString(),"Ana","enrollment","ACTIVO")),
            List.of(new Node(node.toString(),"activity","unit","Numeros")),List.of(),answers,List.of(),List.of());
    }
    AssignmentService.Request request(List<UUID> students) {
        return new AssignmentService.Request(UUID.randomUUID(),node,UUID.fromString(resource),students,"Practica","Resuelve dos ejercicios en tu cuaderno.","Dificultad con cantidades");
    }
    @BeforeEach void setup() {
        when(access.routes(teacher,room)).thenReturn(List.of(new Route(route,"Matematica",1,"PUBLICADO")));
        when(access.members(teacher,room)).thenReturn(List.of(new Member(student.toString(),"Ana","ana")));
        when(reports.data(room,route)).thenReturn(evidence(2,0));
        when(repo.nodes(route)).thenReturn(List.of(new AssignmentRepository.Node(node.toString(),"Cantidades",3,"MAT")));
        when(repo.available()).thenReturn(true);
        when(repo.teacher(room,route)).thenReturn(List.of());
        when(reinforcement.context(student,UUID.fromString(route),node)).thenReturn(Optional.of(new ReinforcementRepository.Context("activity","Cantidades",3,"MAT")));
        when(repo.insert(any())).thenReturn(true);
    }
    @Test void zeroAndInsufficientEvidenceNeverSuggestLowPerformance() {
        when(reports.data(room,route)).thenReturn(evidence(0,0));
        var empty=service.board(teacher,room,route).candidates().getFirst();
        assertEquals("SIN_RESPUESTAS",empty.status());assertEquals(0,empty.answered());
        when(reports.data(room,route)).thenReturn(evidence(1,0));
        assertEquals("EVIDENCIA_INSUFICIENTE",service.board(teacher,room,route).candidates().getFirst().status());
        when(reports.data(room,route)).thenReturn(evidence(10,7));
        assertEquals("EN_AVANCE",service.board(teacher,room,route).candidates().getFirst().status());
    }
    @Test void assignsSelectedResourceAndPreservesEvidence() {
        assertEquals(1,service.assign(teacher,room,route,request(List.of(student))).created());
        verify(repo).insert(argThat(p->p.getValue("answered").equals(2)&&p.getValue("correct").equals(0)&&p.getValue("resource").equals(resource)));
    }
    @Test void outsiderInBatchPreventsAllInserts() {
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.assign(teacher,room,route,request(List.of(student,other)))).getStatusCode().value());
        verify(repo,never()).insert(any());
    }
    @Test void unauthorizedTeacherCannotReadAssignments() {
        when(access.routes(teacher,room)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        assertThrows(ResponseStatusException.class,()->service.board(teacher,room,route));
        verifyNoInteractions(repo,reports);
    }
    @Test void noEvidenceCannotBeAssigned() {
        when(reports.data(room,route)).thenReturn(evidence(0,0));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.assign(teacher,room,route,request(List.of(student)))).getStatusCode().value());
        verify(repo,never()).insert(any());
    }
    @Test void blockedNodeCannotBeAssigned() {
        when(reinforcement.context(any(),any(),any())).thenReturn(Optional.empty());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.assign(teacher,room,route,request(List.of(student)))).getStatusCode().value());
        verify(repo,never()).insert(any());
    }
    @Test void wrongGradeResourceRejected() {
        var r=request(List.of(student));
        var invalid=new AssignmentService.Request(r.requestId(),node,UUID.randomUUID(),r.students(),r.title(),r.instructions(),r.reason());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.assign(teacher,room,route,invalid)).getStatusCode().value());
        verify(repo,never()).insert(any());
    }
    @Test void databaseDuplicateReportedWithoutResettingTask() {
        when(repo.insert(any())).thenReturn(false);
        var result=service.assign(teacher,room,route,request(List.of(student,student)));
        assertEquals(0,result.created());assertEquals(1,result.existing());verify(repo,times(1)).insert(any());
    }
    @Test void missingMigrationDoesNotQueryMissingTable() {
        when(repo.available()).thenReturn(false);
        assertFalse(service.board(teacher,room,route).available());
        assertFalse(service.student(student.toString()).available());
        verify(repo,never()).teacher(any(),any());verify(repo,never()).student(any());
        assertEquals(503,assertThrows(ResponseStatusException.class,()->service.assign(teacher,room,route,request(List.of(student)))).getStatusCode().value());
    }
    AssignmentRepository.Assignment task(String started,String completed) {
        return new AssignmentRepository.Assignment("task",student.toString(),"Ana",room,route,node.toString(),"Tarea","Practica","Motivo","Libro","https://example.test","Nodo",2,0,"date",started,completed,null);
    }
    @Test void studentCannotReadOrChangeSomeoneElsesAssignment() {
        when(repo.student(student.toString())).thenReturn(List.of());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.start(student.toString(),"foreign")).getStatusCode().value());
        verify(repo,never()).start(any(),any());
    }
    @Test void readingDoesNotCompleteAndCompletionNeedsReflection() {
        when(repo.student(student.toString())).thenReturn(List.of(task(null,null)));
        assertEquals(1,service.student(student.toString()).assignments().size());verify(repo,never()).complete(any(),any(),any());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.complete(student.toString(),"task","Practique sumas")).getStatusCode().value());
        when(repo.student(student.toString())).thenReturn(List.of(task("date",null)));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.complete(student.toString(),"task","")).getStatusCode().value());
        service.complete(student.toString(),"task","Practique sumas con mi cuaderno");
        verify(repo).complete(student.toString(),"task","Practique sumas con mi cuaderno");
    }
    @Test void repeatedCompletionPreservesOriginalReflection() {
        when(repo.student(student.toString())).thenReturn(List.of(task("date","date")));
        service.complete(student.toString(),"task","otro texto");verify(repo,never()).complete(any(),any(),any());
    }
    @Test void differentStudentsReceiveDifferentInstructions() {
        when(access.members(teacher,room)).thenReturn(List.of(new Member(student.toString(),"Ana","ana"),new Member(other.toString(),"Luis","luis")));
        var d=evidence(2,0);
        var answers=new ArrayList<Answer>(d.answers());
        answers.add(new Answer(other.toString(),"activity","q0",false));
        answers.add(new Answer(other.toString(),"activity","q1",true));
        when(reports.data(room,route)).thenReturn(new Data(
            List.of(d.students().getFirst(),new Student(other.toString(),"Luis","other-enrollment","ACTIVO")),
            d.nodes(),d.competencies(),answers,d.completed(),d.questions()));
        when(reinforcement.context(other,UUID.fromString(route),node)).thenReturn(Optional.of(new ReinforcementRepository.Context("activity","Cantidades",3,"MAT")));
        service.assign(teacher,room,route,request(List.of(student)));
        var second=request(List.of(other));
        service.assign(teacher,room,route,new AssignmentService.Request(second.requestId(),node,UUID.fromString(resource),
            second.students(),"Agrupaciones","Agrupa diez objetos de dos en dos.","Dificultad para agrupar"));
        verify(repo).insert(argThat(p->p.getValue("student").equals(student.toString())&&p.getValue("instructions").equals("Resuelve dos ejercicios en tu cuaderno.")));
        verify(repo).insert(argThat(p->p.getValue("student").equals(other.toString())&&p.getValue("instructions").equals("Agrupa diez objetos de dos en dos.")));
    }
    @Test void unsupportedAreaShowsNoResourcesAndRejectsAssignment() {
        when(repo.nodes(route)).thenReturn(List.of(new AssignmentRepository.Node(node.toString(),"Parada",3,"OTRA")));
        assertTrue(service.board(teacher,room,route).candidates().getFirst().resources().isEmpty());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.assign(teacher,room,route,request(List.of(student)))).getStatusCode().value());
    }
}
