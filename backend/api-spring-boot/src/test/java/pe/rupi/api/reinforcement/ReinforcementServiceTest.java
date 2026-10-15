package pe.rupi.api.reinforcement;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.reinforcement.ReinforcementRepository.*;
import pe.rupi.api.studypackage.PrimaryStudyCatalog;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReinforcementServiceTest {
    final UUID student=UUID.randomUUID(),route=UUID.randomUUID(),node=UUID.randomUUID();
    ReinforcementRepository repo; ReinforcementService service;
    @BeforeEach void setup() {
        repo=mock(ReinforcementRepository.class);service=new ReinforcementService(repo);
        when(repo.context(student,route,node)).thenReturn(Optional.of(new Context("activity","Sumas",2,"MAT")));
        when(repo.evidence(student,"activity")).thenReturn(List.of());when(repo.links(student,node)).thenReturn(List.of());
    }
    List<Evidence> answers(int correct,int total) {
        var values=new ArrayList<Evidence>();for(int i=0;i<total;i++) values.add(new Evidence("Pregunta "+i,i<correct,"Pista"));return values;
    }
    @Test void needsTwoAnswersAndStrictlyLessThanSeventyPercent() {
        assertFalse(ReinforcementService.needsPractice(answers(0,0)));
        assertFalse(ReinforcementService.needsPractice(answers(0,1)));
        assertTrue(ReinforcementService.needsPractice(answers(1,2)));
        assertFalse(ReinforcementService.needsPractice(answers(7,10)));
        assertTrue(ReinforcementService.needsPractice(answers(6,10)));
    }
    @Test void refusesUnavailableOrForeignNodeBeforeReadingEvidence() {
        when(repo.context(student,route,node)).thenReturn(Optional.empty());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.refresh(student,route,node)).getStatusCode().value());
        verify(repo,never()).evidence(any(),any());verify(repo,never()).links(any(),any());
    }
    @Test void noEvidenceDoesNotInventRecommendations() {
        assertEquals("SIN_EVIDENCIA",service.refresh(student,route,node).status());verify(repo,never()).link(any(),any(),any());
    }
    @Test void linksOnlySameGradeAndAreaAndShowsMissedQuestions() {
        when(repo.evidence(student,"activity")).thenReturn(answers(1,2));
        String key=ReinforcementService.resourceId(PrimaryStudyCatalog.forGradeAndArea(2,"MAT").getFirst());
        when(repo.links(student,node)).thenReturn(List.of(new Link(UUID.randomUUID().toString(),key,false)));
        var view=service.refresh(student,route,node);assertEquals("REFUERZO",view.status());
        assertEquals(1,view.practice().size());assertEquals("Cuadernillo de Matemática 2",view.resources().getFirst().title());
        verify(repo).link(student,node,key);
    }
    @Test void unsupportedAreaHasNoUnrelatedMaterial() {
        when(repo.context(student,route,node)).thenReturn(Optional.of(new Context("activity","Arte",2,"ARTE")));
        when(repo.evidence(student,"activity")).thenReturn(answers(0,2));
        assertEquals("SIN_RECURSO",service.refresh(student,route,node).status());verify(repo,never()).link(any(),any(),any());
    }
    @Test void improvementKeepsCompletedHistoryWithoutCreatingNewLink() {
        when(repo.evidence(student,"activity")).thenReturn(answers(2,2));
        String key=ReinforcementService.resourceId(PrimaryStudyCatalog.forGradeAndArea(2,"MAT").getFirst());
        when(repo.links(student,node)).thenReturn(List.of(new Link(UUID.randomUUID().toString(),key,true)));
        var view=service.refresh(student,route,node);assertEquals("AVANCE",view.status());assertTrue(view.resources().getFirst().completed());
        verify(repo,never()).link(any(),any(),any());
    }
    @Test void cannotCompleteSomeoneElsesRecommendation() {
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.complete(student,route,node,UUID.randomUUID())).getStatusCode().value());
        verify(repo,never()).complete(any(),any(),any());
    }
    @Test void completionIsScopedToStudentAndNode() {
        UUID id=UUID.randomUUID();String key=ReinforcementService.resourceId(PrimaryStudyCatalog.forGradeAndArea(2,"MAT").getFirst());
        when(repo.links(student,node)).thenReturn(List.of(new Link(id.toString(),key,false)));
        service.complete(student,route,node,id);verify(repo).complete(student,node,id);
    }
}
