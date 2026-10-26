package pe.rupi.api.quiz;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static pe.rupi.api.quiz.QuizModels.*;

class ChatQuizProviderTest {
 @SuppressWarnings("unchecked")
 HttpResponse<String> response(int status,String text) {
  var response=(HttpResponse<String>)mock(HttpResponse.class);when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(text);return response;
 }
 String wrap(String text){return QuizRepository.json(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("content",text)))));}
 LearningContext context=new LearningContext(3,"MAT","Sumas","Hay cuatro puestos junto a la fuente y tres junto a los arcos. En total hay siete puestos.");
 Draft draft(){return new QuizValidationTest().valid();}
 ChatQuizProvider provider(HttpClient client){return new ChatQuizProvider("https://example.test/v1/chat/completions","test-fixture-not-real","TEST_ONLY","fixture"){@Override HttpClient client(){return client;}};}
 @Test void generationAndReviewUseTwoBoundedRequests() {
  var client=mock(HttpClient.class);
  when(client.sendAsync(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
   .thenAnswer(call -> CompletableFuture.completedFuture(response(200,wrap(QuizRepository.json(draft())))))
   .thenAnswer(call -> CompletableFuture.completedFuture(response(200,wrap("{\"suitable\":true,\"onTopic\":true,\"ageAppropriate\":true,\"correct\":true}"))));
  assertEquals(3,provider(client).generate(context).questions().size());
  verify(client,times(2)).sendAsync(argThat(r->r.timeout().orElseThrow().toSeconds()==12&&r.uri().getScheme().equals("https")),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
 }
 @Test void rejectedReviewIsNeverReturnedToStudent() {
  var client=mock(HttpClient.class);
  when(client.sendAsync(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
   .thenAnswer(call -> CompletableFuture.completedFuture(response(200,wrap(QuizRepository.json(draft())))))
   .thenAnswer(call -> CompletableFuture.completedFuture(response(200,wrap("{\"suitable\":false,\"onTopic\":true,\"ageAppropriate\":false,\"correct\":true}"))));
  assertThrows(IllegalStateException.class,()->provider(client).generate(context));
 }
 @Test void badProviderOutputAndTimeoutDoNotTriggerRetries() {
  for(String body:List.of("not json","{\"choices\":[]}","{\"choices\":[{\"finish_reason\":\"length\"}]}")){
   var client=mock(HttpClient.class);
   when(client.sendAsync(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
    .thenAnswer(call -> CompletableFuture.completedFuture(response(200,body)));
   assertThrows(IllegalStateException.class,()->provider(client).generate(context));
   verify(client,times(1)).sendAsync(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
  }
  var client=mock(HttpClient.class);
  when(client.sendAsync(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(CompletableFuture.failedFuture(new TimeoutException()));
  assertThrows(IllegalStateException.class,()->provider(client).generate(context));
  verify(client,times(1)).sendAsync(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
 }
}
