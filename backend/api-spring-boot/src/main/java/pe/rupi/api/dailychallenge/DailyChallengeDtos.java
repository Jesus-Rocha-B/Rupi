package pe.rupi.api.dailychallenge;

import java.util.List;
import java.util.UUID;
import pe.rupi.api.evaluation.EvaluationDtos.QuestionDto;

public final class DailyChallengeDtos {
    private DailyChallengeDtos() {}

    public record DailyChallengeResponse(
            UUID id,
            String date,
            UUID activityVersionId,
            String title,
            String description,
            Integer estimatedMinutes,
            boolean isCompleted,
            int experienceReward,
            List<QuestionDto> questions
    ) {}

    public record DailyChallengeCompleteResponse(
            UUID challengeId,
            boolean isCompleted,
            int experienceEarned,
            int totalExperience,
            String rupiMessage
    ) {}
}
