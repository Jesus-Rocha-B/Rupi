package pe.rupi.api.evaluation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class EvaluationDtos {
    private EvaluationDtos() {}

    public record OptionDto(UUID id, int sequence, String text) {}

    public record QuestionDto(
            UUID id,
            int sequence,
            String type,
            String statement,
            String explanation,
            BigDecimal points,
            List<OptionDto> options
    ) {}

    public record SubmitAnswerRequest(
            UUID questionId,
            UUID selectedOptionId,
            String textAnswer
    ) {}

    public record AnswerFeedbackResponse(
            UUID questionId,
            boolean isCorrect,
            String feedbackMessage,
            UUID correctOptionId,
            String explanation
    ) {}

    public record EvaluationSummaryDto(
            int totalQuestions,
            int correctAnswers,
            BigDecimal totalScore,
            BigDecimal maxScore,
            String rupiFeedback
    ) {}

    public record BadgeDto(
            String code,
            String name,
            String description,
            String icon
    ) {}
}
