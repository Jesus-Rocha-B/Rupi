package pe.rupi.api.quiz;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static pe.rupi.api.quiz.QuizModels.*;
class QuizValidationTest {
 final LearningContext context=new LearningContext(3,"MAT","Sumas","Hay cuatro puestos junto a la fuente y tres junto a los arcos. En total hay siete puestos.");
 Draft valid(){return new Draft(java.util.stream.IntStream.range(0,3).mapToObj(i->new DraftQuestion("¿Cuántos puestos hay en el grupo "+i+"?",List.of("Siete","Seis","Ocho"),0,"Cuatro más tres son siete puestos.","cuatro puestos junto a la fuente")).toList());}
 @Test void validatesBriefGroundedQuiz(){assertEquals(3,QuizValidation.validate(valid(),context).questions().size());}
 @Test void rejectsMissingOrExcessiveQuestions(){assertThrows(IllegalArgumentException.class,()->QuizValidation.validate(new Draft(List.of()),context));assertThrows(IllegalArgumentException.class,()->QuizValidation.validate(null,context));}
 @Test void rejectsDuplicatesAndUnsourcedQuestions(){
  var q=valid().questions().getFirst();
  assertThrows(IllegalArgumentException.class,()->QuizValidation.validate(new Draft(List.of(q,q,q)),context));
  var bad=new DraftQuestion(q.text(),q.options(),0,q.explanation(),"Una afirmacion que no existe en la leccion");
  assertThrows(IllegalArgumentException.class,()->QuizValidation.validate(new Draft(List.of(bad,valid().questions().get(1),valid().questions().get(2))),context));
 }
 @Test void rejectsInvalidAlternativesAndMissingAnswer(){
  for(Integer index:Arrays.asList(-1,3,null)){
   var q=valid().questions().getFirst();var bad=new DraftQuestion(q.text(),q.options(),index,q.explanation(),q.evidence());
   assertThrows(IllegalArgumentException.class,()->QuizValidation.validate(new Draft(List.of(bad,valid().questions().get(1),valid().questions().get(2))),context));
  }
  var q=valid().questions().getFirst();var bad=new DraftQuestion(q.text(),List.of("Si"," Sí ","No"),0,q.explanation(),q.evidence());
  assertThrows(IllegalArgumentException.class,()->QuizValidation.validate(new Draft(List.of(bad,valid().questions().get(1),valid().questions().get(2))),context));
 }
 @Test void stripsMarkupAndPersonalContactData(){var clean=QuizValidation.clean("<b>Hola</b> usuario@escuela.pe 999888777",200);assertFalse(clean.contains("@"));assertFalse(clean.contains("999888777"));assertFalse(clean.contains("<b>"));}
 @Test void unconfiguredProviderDoesNotPretendToGenerate(){
  var provider=new ChatQuizProvider("","","","");
  assertFalse(provider.configured());assertThrows(IllegalStateException.class,()->provider.generate(context));
 }
 @Test void providerRequiresHttpsAndNoEmbeddedCredentials(){
  assertFalse(new ChatQuizProvider("http://example.test","key","test","model").configured());
  assertFalse(new ChatQuizProvider("https://user:pass@example.test","key","test","model").configured());
  assertTrue(new ChatQuizProvider("https://example.test/v1/chat/completions","key","test","model").configured());
 }
}
