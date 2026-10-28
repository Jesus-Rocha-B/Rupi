package pe.rupi.api.quiz;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pe.rupi.api.quiz.QuizModels.*;

class QuizHintServiceTest {
 QuizRepository repo;ChatQuizProvider chat;QuizHintService service;
 static final List<Option> OPTIONS=List.of(new Option("o1","12"),new Option("o2","14"),new Option("o3","16"));
 static Question wrong=new Question("q1","¿Cuánto es 7 + 7?",OPTIONS,"o1",false,"Siete más siete es catorce.","o2");
 static Question right=new Question("q2","¿Cuánto es 2 + 2?",List.of(new Option("a","4"),new Option("b","5"),new Option("c","6")),"a",true,"Dos más dos es cuatro.","a");
 static Question open=new Question("q3","¿Cuánto es 3 + 3?",List.of(new Option("a","6"),new Option("b","7"),new Option("c","8")),null,null,null,null);

 @BeforeEach void setUp() {
  repo=mock(QuizRepository.class);chat=mock(ChatQuizProvider.class);service=new QuizHintService(repo,chat);
  when(repo.own("s","p",false)).thenReturn(new Practice("p","s","n","r","quiz","attempt","LISTO","GENERADO"));
  when(repo.context("s","r","n")).thenReturn(new Context("e","act","n","r",2,"MAT","Sumas en la feria","Sumamos frutas de la feria para practicar."));
  when(repo.quiz("quiz",false)).thenReturn(new Quiz("quiz","act","VALIDADO_AUTOMATICO","p","m","v"));
  when(repo.questions(any(),eq("attempt"),eq(false))).thenReturn(List.of(wrong,right,open));
 }

 @Test void levelsFollowGradeBands() {
  assertEquals("INICIAL",QuizHintService.level(1));assertEquals("INICIAL",QuizHintService.level(2));
  assertEquals("INTERMEDIO",QuizHintService.level(3));assertEquals("INTERMEDIO",QuizHintService.level(4));
  assertEquals("AVANZADO",QuizHintService.level(5));assertEquals("AVANZADO",QuizHintService.level(6));
 }
 @Test void reinforcesWhenLessThanHalfIsCorrect() {
  assertFalse(QuizHintService.reinforced(List.of(wrong,right,open)));
  var wrong2=new Question("q4","x",OPTIONS,"o3",false,"y","o2");
  assertTrue(QuizHintService.reinforced(List.of(wrong,wrong2,right)));
  assertFalse(QuizHintService.reinforced(List.of(wrong)));
 }
 @Test void fallbackExplainsTheErrorWithReviewedExplanation() throws Exception {
  when(chat.configured()).thenReturn(false);
  var hint=service.hint("s","p","q1");
  assertEquals("INICIAL",hint.level());assertEquals(2,hint.grade());assertEquals("EXPLICACION_REVISADA",hint.source());
  assertTrue(hint.hint().contains("«12»"));assertTrue(hint.hint().contains("«14»"));assertTrue(hint.hint().contains("catorce"));
  assertEquals(3,hint.steps().size());
  verify(chat,never()).requestJson(anyString(),any());
 }
 @Test void onlyExplainsAnsweredWrongQuestions() {
  var correct=assertThrows(ResponseStatusException.class,()->service.hint("s","p","q2"));
  assertEquals(HttpStatus.CONFLICT,correct.getStatusCode());
  var unanswered=assertThrows(ResponseStatusException.class,()->service.hint("s","p","q3"));
  assertEquals(HttpStatus.CONFLICT,unanswered.getStatusCode());
  var foreign=assertThrows(ResponseStatusException.class,()->service.hint("s","p","otra"));
  assertEquals(HttpStatus.NOT_FOUND,foreign.getStatusCode());
 }
 @Test void usesValidatedAiHintAndCachesIt() throws Exception {
  when(chat.configured()).thenReturn(true);
  when(chat.requestJson(contains("Explica un error"),any())).thenReturn("""
   {"hint":"Contaste un grupo de siete y te faltó el otro.","steps":["Cuenta siete.","Suma siete más."]}""");
  when(chat.requestJson(contains("Revisa esta pista"),any())).thenReturn("{\"correct\":true,\"levelAppropriate\":true,\"safe\":true}");
  var hint=service.hint("s","p","q1");
  assertEquals("IA",hint.source());assertEquals(List.of("Cuenta siete.","Suma siete más."),hint.steps());
  service.hint("s","p","q1");
  verify(chat,times(2)).requestJson(anyString(),any());
 }
 @Test void rejectedAiHintFallsBackToReviewedExplanation() throws Exception {
  when(chat.configured()).thenReturn(true);
  when(chat.requestJson(contains("Explica un error"),any())).thenReturn("{\"hint\":\"Mira https://ejemplo.com\",\"steps\":[\"a\",\"b\"]}");
  assertEquals("EXPLICACION_REVISADA",service.hint("s","p","q1").source());
 }
 @Test void validationRejectsIncompleteDrafts() {
  assertThrows(IllegalArgumentException.class,()->QuizHintService.validate(new QuizHintService.Draft("Muy bien pensado",List.of("Solo un paso"))));
  assertThrows(IllegalArgumentException.class,()->QuizHintService.validate(new QuizHintService.Draft(null,List.of("Paso uno","Paso dos"))));
 }
}
