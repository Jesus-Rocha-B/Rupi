package pe.rupi.mobile.data.model

import java.io.Serializable

data class AiQuizQuestion(
 val id: String,
 val text: String,
 val options: List<QuestionOption>,
 val selectedOptionId: String?,
 val isCorrect: Boolean,
 val explanation: String
)

data class AiPracticeDetail(
 val practiceId: String,
 val status: String,
 val source: String,
 val reviewState: String,
 val correctCount: Int,
 val questions: List<AiQuizQuestion>
)

data class AiPracticeStatus(
 val configured: Boolean,
 val reviewedAvailable: Boolean,
 val practice: AiPracticeDetail?
)

data class QuestionHint(
 val level: String,
 val steps: List<String>,
 val hint: String,
 val realLifeExample: String?,
 val source: String,
 val reinforced: Boolean
)

data class FlashcardItem(
 val id: String,
 val state: String,
 val question: String,
 val answer: String,
 val source: String
)

data class FlashcardDeck(
 val currentCardId: String,
 val revealed: Boolean,
 val revision: Long,
 val cards: List<FlashcardItem>
)

data class VisualSummaryConcept(
 val title: String,
 val text: String
) : Serializable

data class VisualSummaryContent(
 val title: String,
 val concepts: List<VisualSummaryConcept>,
 val steps: List<String>,
 val example: String,
 val remember: String
) : Serializable

data class VisualSummaryResource(
 val id: String,
 val grade: Int,
 val version: String,
 val state: String,
 val source: String,
 val content: VisualSummaryContent?,
 val rawJson: String
) : Serializable

data class VisualSummaryLookup(
 val resource: VisualSummaryResource?,
 val configured: Boolean,
 val offline: Boolean,
 val invalidatedPreviousCopy: Boolean = false
)

data class SavedVisualSummary(
 val topic: StudyTopic,
 val resource: VisualSummaryResource
) : Serializable
