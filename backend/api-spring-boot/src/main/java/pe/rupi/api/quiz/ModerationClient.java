package pe.rupi.api.quiz;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** HU-53: consulta la política publicada. Error, timeout o respuesta dudosa bloquean la salida. */
@Component
public class ModerationClient {
    private final String base, token;
    public ModerationClient(@Value("${rupi.ai.moderation-url:}") String base,
                            @Value("${rupi.monitor.token:}") String token) {
        this.base=base; this.token=token;
    }
    public void requireAllowed(String text) {
        if (base.isBlank() || token.isBlank()) throw new IllegalStateException("Moderación no configurada");
        try (var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var uri=URI.create(base.replaceAll("/+$", "")+"/api/internal/moderacion/evaluar");
            if (!("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && java.util.Set.of("localhost","127.0.0.1").contains(uri.getHost()))))
                throw new IllegalStateException("URL de moderación no segura");
            var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(4))
                .header("X-Rupi-Monitor-Token",token).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(QuizValidation.JSON.writeValueAsString(Map.of("texto",text)))).build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if (response.statusCode()!=200 || response.body().length()>8000) throw new IllegalStateException("Moderación no disponible");
            var decision=QuizValidation.JSON.readTree(response.body()).path("permitida");
            if (!decision.isBoolean() || !decision.asBoolean()) throw new IllegalStateException("Respuesta bloqueada por moderación");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Evaluación interrumpida");
        } catch (Exception e) { throw new IllegalStateException("No se pudo aprobar la respuesta de IA"); }
    }
}
