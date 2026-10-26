package pe.rupi.api.quiz;
import java.util.*;
import org.junit.jupiter.api.Test;
import pe.rupi.api.visualsummary.*;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
class SummaryProviderTest {
 final QuizModels.LearningContext context=new QuizModels.LearningContext(3,"MAT","Sumamos","Hay cuatro puestos junto a la fuente y tres junto a los arcos. En total hay siete puestos.");
 @Test void validatesAndPerformsIndependentAutomaticReview()throws Exception{
  var chat=mock(ChatQuizProvider.class);when(chat.configured()).thenReturn(true);
  when(chat.requestJson(anyString(),any())).thenReturn(SummaryProvider.JSON.writeValueAsString(SummaryIntegrationTest.draft(context)),"{\"correct\":true,\"onTopic\":true,\"ageAppropriate\":true,\"safe\":true}");
  assertNotNull(new SummaryProvider(chat).generate(context));verify(chat,times(2)).requestJson(anyString(),any());
 }
 @Test void rejectsUnsupportedClaimsAndDuplicateConcepts(){
  var d=SummaryIntegrationTest.draft(context);
  assertThrows(IllegalArgumentException.class,()->SummaryProvider.validate(new SummaryProvider.Draft(d.title(),List.of(d.concepts().getFirst(),d.concepts().getFirst()),d.steps(),d.example(),d.remember()),context));
  assertThrows(IllegalArgumentException.class,()->SummaryProvider.validate(new SummaryProvider.Draft(d.title(),List.of(new SummaryProvider.Section("Uno","Texto","Esta evidencia no pertenece al contenido"),d.concepts().getLast()),d.steps(),d.example(),d.remember()),context));
 }
 @Test void rejectsFailedReviewMalformedAndUnavailableProvider()throws Exception{
  var chat=mock(ChatQuizProvider.class);var provider=new SummaryProvider(chat);
  assertThrows(IllegalStateException.class,()->provider.generate(context));
  when(chat.configured()).thenReturn(true);
  when(chat.requestJson(anyString(),any())).thenReturn(SummaryProvider.JSON.writeValueAsString(SummaryIntegrationTest.draft(context)),"{\"correct\":false}");
  assertThrows(IllegalStateException.class,()->provider.generate(context));
  when(chat.requestJson(anyString(),any())).thenReturn("not json");assertThrows(IllegalStateException.class,()->provider.generate(context));
 }
}
