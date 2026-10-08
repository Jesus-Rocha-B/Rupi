package pe.rupi.api.evaluation;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.evaluation.EvaluationDtos.AnswerFeedbackResponse;
import pe.rupi.api.evaluation.EvaluationDtos.QuestionDto;
import pe.rupi.api.evaluation.EvaluationDtos.SubmitAnswerRequest;

@RestController
@RequestMapping("/api/v1/student/activities")
public class EvaluationController {
    private final EvaluationService service;

    public EvaluationController(EvaluationService service) {
        this.service = service;
    }

    @GetMapping("/{activityVersionId}/questions")
    public List<QuestionDto> listQuestions(@PathVariable UUID activityVersionId, Principal principal) {
        studentIdFrom(principal); // Verifica autenticación activa
        return service.findQuestionsForActivity(activityVersionId);
    }

    @PostMapping("/{activityVersionId}/questions/answer")
    public AnswerFeedbackResponse submitAnswer(
            @PathVariable UUID activityVersionId,
            @RequestBody SubmitAnswerRequest request,
            Principal principal
    ) {
        UUID studentId = studentIdFrom(principal);
        return service.checkAndRecordAnswer(studentId, activityVersionId, request);
    }

    private static UUID studentIdFrom(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
