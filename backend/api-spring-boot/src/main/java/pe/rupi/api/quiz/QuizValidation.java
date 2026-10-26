package pe.rupi.api.quiz;
import java.text.Normalizer;
import java.util.*;
import static pe.rupi.api.quiz.QuizModels.*;

public final class QuizValidation {
 private QuizValidation() {}
 static final tools.jackson.databind.json.JsonMapper JSON=tools.jackson.databind.json.JsonMapper.builder().build();
 public static String normalize(String text) {
  return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").strip();
 }
 public static String clean(String text,int max) {
  if(text==null) return "";
  String value=text.replaceAll("<[^>]*>"," ").replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}","[correo omitido]")
   .replaceAll("(?<!\\d)\\d{8,}(?!\\d)","[numero omitido]").replaceAll("[\\p{Cntrl}&&[^\\n\\t]]"," ").strip();
  return value.substring(0,Math.min(value.length(),max));
 }
 private static void text(String value,int min,int max) {
  if(value==null||value.strip().length()<min||value.length()>max||value.contains("<")||value.contains("http://")||value.contains("https://"))
   throw new IllegalArgumentException("Contenido de cuestionario no valido");
 }
 public static Draft validate(Draft draft,LearningContext context) {
  if(context.grade()<1||context.grade()>6||context.content().length()<40||draft==null||draft.questions()==null
     ||draft.questions().size()<3||draft.questions().size()>5) throw new IllegalArgumentException("Cuestionario incompleto");
  Set<String> statements=new HashSet<>();
  String source=normalize(context.topic()+" "+context.content());
  for(var q:draft.questions()) {
   if(q==null) throw new IllegalArgumentException("Pregunta vacia");
   text(q.text(),10,500);text(q.explanation(),10,800);text(q.evidence(),10,400);
   if(!statements.add(normalize(q.text()))||!source.contains(normalize(q.evidence())))
    throw new IllegalArgumentException("Pregunta repetida o sin sustento en el contenido");
   if(q.options()==null||q.options().size()<3||q.options().size()>4||q.correctIndex()==null||q.correctIndex()<0||q.correctIndex()>=q.options().size())
    throw new IllegalArgumentException("Alternativas invalidas");
   Set<String> options=new HashSet<>();
   for(String option:q.options()) { text(option,1,200);if(!options.add(normalize(option)))throw new IllegalArgumentException("Alternativas repetidas"); }
  }
  return draft;
 }
}
