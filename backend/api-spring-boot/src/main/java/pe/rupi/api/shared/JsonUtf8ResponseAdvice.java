package pe.rupi.api.shared;

import java.nio.charset.StandardCharsets;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Garantiza que todas las respuestas JSON incluyan explícitamente charset=UTF-8 en Content-Type.
 * Esto asegura compatibilidad estricta con Windows PowerShell 5.1 nativo (Invoke-RestMethod)
 * y clientes HTTP en entornos escolares con Windows.
 */
@ControllerAdvice
public class JsonUtf8ResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (selectedContentType != null && "application".equalsIgnoreCase(selectedContentType.getType())
                && selectedContentType.getSubtype().toLowerCase().contains("json")) {
            response.getHeaders().setContentType(APPLICATION_JSON_UTF8);
        }
        return body;
    }
}
