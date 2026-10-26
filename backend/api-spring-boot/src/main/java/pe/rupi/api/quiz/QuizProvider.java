package pe.rupi.api.quiz;
import static pe.rupi.api.quiz.QuizModels.*;
public interface QuizProvider {
 boolean configured();
 String provider();
 String model();
 Draft generate(LearningContext context);
}
