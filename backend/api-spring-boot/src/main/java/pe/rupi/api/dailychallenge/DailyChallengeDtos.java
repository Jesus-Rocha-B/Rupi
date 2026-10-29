package pe.rupi.api.dailychallenge;

import java.util.List;
import java.util.UUID;
import pe.rupi.api.evaluation.EvaluationDtos.AnswerFeedbackResponse;
import pe.rupi.api.evaluation.EvaluationDtos.QuestionDto;

public final class DailyChallengeDtos {
    private DailyChallengeDtos() {}

    /** Tipo de reto (HU-40): los de lectura traen su texto en {@code readingText}. */
    public static final String LOGICAL_REASONING = "RAZONAMIENTO_LOGICO";
    public static final String READING_COMPREHENSION = "COMPRENSION_LECTORA";

    public record DailyChallengeResponse(
            UUID id,
            String date,
            UUID activityVersionId,
            String title,
            String description,
            String skill,
            String readingText,
            Integer estimatedMinutes,
            boolean isCompleted,
            int experienceReward,
            List<QuestionDto> questions,
            /** Lo que ya respondió hoy: al recargar la web o reabrir la app no se pierde lo avanzado. */
            List<AnswerFeedbackResponse> answeredToday
    ) {}

    public record DailyChallengeCompleteResponse(
            UUID challengeId,
            boolean isCompleted,
            int experienceEarned,
            int totalExperience,
            int correctAnswers,
            int totalQuestions,
            String rupiMessage
    ) {}
}
