package pe.rupi.api.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handleDatabaseUnavailable(DataAccessException exception) {
        log.error("Error de acceso a datos", exception);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Servicio temporalmente no disponible");
        problem.setDetail("No se pudo consultar la información. Inténtalo nuevamente.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(org.springframework.web.server.ResponseStatusException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(ex.getStatusCode());
        String reason = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        problem.setTitle(reason);
        problem.setDetail(reason);
        problem.setProperty("message", reason);
        problem.setProperty("error", reason);
        return ResponseEntity.status(ex.getStatusCode()).body(problem);
    }
}

