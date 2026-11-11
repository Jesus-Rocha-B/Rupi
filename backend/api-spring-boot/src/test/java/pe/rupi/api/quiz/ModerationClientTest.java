package pe.rupi.api.quiz;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModerationClientTest {
    @Test void realHttpRequiresExplicitApprovalAndSendsToken() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var status=new java.util.concurrent.atomic.AtomicInteger(200);
        var body=new java.util.concurrent.atomic.AtomicReference<>("{\"permitida\":true}");
        var payloadSeen=new java.util.concurrent.atomic.AtomicReference<String>();
        var tokenSeen=new java.util.concurrent.atomic.AtomicBoolean();
        server.createContext("/api/internal/moderacion/evaluar", exchange->{
            tokenSeen.set("fixture-token".equals(exchange.getRequestHeaders().getFirst("X-Rupi-Monitor-Token")));
            payloadSeen.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] data=body.get().getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status.get(),data.length);
            exchange.getResponseBody().write(data);exchange.close();
        });
        server.start();
        try {
            var client=new ModerationClient("http://127.0.0.1:"+server.getAddress().getPort(),"fixture-token");
            assertDoesNotThrow(()->client.requireAllowed("Respuesta escolar",java.util.Map.of("modelo","fixture","duracionMs",45)));assertTrue(tokenSeen.get());
            assertEquals("fixture",QuizValidation.JSON.readTree(payloadSeen.get()).path("auditoria").path("modelo").asText());
            for(String invalid:new String[]{"{\"permitida\":false}","{}","not json","{\"permitida\":\"true\"}"}) {
                body.set(invalid);assertThrows(IllegalStateException.class,()->client.requireAllowed("Texto"));
            }
            status.set(503);body.set("{\"permitida\":true}");assertThrows(IllegalStateException.class,()->client.requireAllowed("Texto"));
        } finally { server.stop(0); }
    }
    @Test void missingConfigurationBlocks() {
        assertThrows(IllegalStateException.class,()->new ModerationClient("","").requireAllowed("Texto"));
    }
}
