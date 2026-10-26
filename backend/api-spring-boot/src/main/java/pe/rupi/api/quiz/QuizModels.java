package pe.rupi.api.quiz;
import java.util.List;
public final class QuizModels {
 private QuizModels() {}
 public record Context(String enrollment,String activity,String node,String route,int grade,String area,String topic,String content) {}
 /** Only this projection is sent to the provider: no accounts, answers, grades, IDs or credentials. */
 public record LearningContext(int grade,String area,String topic,String content) {}
 public record DraftQuestion(String text,List<String> options,Integer correctIndex,String explanation,String evidence) {}
 public record Draft(List<DraftQuestion> questions) {}
 public record Check(boolean suitable,boolean onTopic,boolean ageAppropriate,boolean correct) {}
 public record Option(String id,String text) {}
 public record Question(String id,String text,List<Option> options,String selectedOption,Boolean correct,String explanation,String correctOption) {}
 public record Practice(String id,String student,String node,String route,String quiz,String attempt,String state,String origin) {}
 public record Quiz(String id,String activity,String state,String provider,String model,String prompt) {}
 public record View(String status,String practiceId,String source,String reviewState,int answered,int correct,List<Question> questions) {}
 public record Availability(boolean configured,boolean reviewedAvailable,View practice) {}
 public record Review(String id,String topic,String state,String provider,String model,List<Question> questions,List<String> reports) {}
}
