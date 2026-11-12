package pe.rupi.mobile.ui.state

import pe.rupi.mobile.data.local.SavedSummariesReadResult
import pe.rupi.mobile.data.model.*
import java.io.Serializable

enum class NoticeTone {
 INFO,
 ERROR,
 OK,
 RETRY
}

data class NoticeMessage(
 val text: String,
 val tone: NoticeTone = NoticeTone.INFO,
 val scrollToTop: Boolean = false
)

sealed interface NavDestination : Serializable {
 data object Login : NavDestination
 data object Topics : NavDestination
 data object Achievements : NavDestination
 data object DailyGoal : NavDestination
 data object DailyChallenge : NavDestination
 data object Notifications : NavDestination
 data class RouteDetail(val route: LearningRouteSummary) : NavDestination
 data class Lesson(val route: LearningRouteSummary, val node: RouteNode) : NavDestination
 data class AiPractice(val topic: StudyTopic) : NavDestination
 data class Flashcards(val topic: StudyTopic) : NavDestination
 data class VisualSummary(val topic: StudyTopic) : NavDestination
 data object SavedSummaries : NavDestination
 data class SavedSummaryDetail(val summary: SavedVisualSummary) : NavDestination
}

data class LoginUiState(
 val isLoading: Boolean = false,
 val notice: NoticeMessage? = null
)

data class TopicsUiState(
 val isLoading: Boolean = false,
 val routes: List<LearningRouteSummary> = emptyList(),
 val routesLoaded: Boolean = false,
 val notice: NoticeMessage? = null
)

data class RouteDetailUiState(
 val isLoading: Boolean = false,
 val route: LearningRouteSummary? = null,
 val detail: pe.rupi.mobile.data.model.RouteDetail? = null,
 val notice: NoticeMessage? = null
)

data class LessonUiState(
 val isLoading: Boolean = false,
 val route: LearningRouteSummary? = null,
 val node: RouteNode? = null,
 val lesson: LessonDetail? = null,
 val answers: Map<String, QuestionFeedback> = emptyMap(),
 val completionResult: NodeCompletionResult? = null,
 val notice: NoticeMessage? = null
)

data class AchievementsUiState(
 val isLoading: Boolean = false,
 val overview: AchievementsOverview? = null,
 val notice: NoticeMessage? = null
)

data class DailyGoalUiState(
 val isLoading: Boolean = false,
 val dashboard: DailyGoalDashboard? = null,
 val notice: NoticeMessage? = null
)

data class DailyChallengeUiState(
 val isLoading: Boolean = false,
 val challenge: DailyChallenge? = null,
 val answers: Map<String, QuestionFeedback> = emptyMap(),
 val activeDay: String = "",
 val completion: DailyChallengeCompletion? = null,
 val notice: NoticeMessage? = null
)

data class NotificationsUiState(
 val isLoading: Boolean = false,
 val dashboard: NotificationsDashboard? = null,
 val notice: NoticeMessage? = null
)

sealed interface NotificationEffect {
 data object RequestSystemPermission : NotificationEffect
}

data class AiPracticeUiState(
 val isLoading: Boolean = false,
 val topic: StudyTopic? = null,
 val status: AiPracticeStatus? = null,
 val notice: NoticeMessage? = null
)

data class FlashcardsUiState(
 val isLoading: Boolean = false,
 val topic: StudyTopic? = null,
 val deck: FlashcardDeck? = null,
 val notice: NoticeMessage? = null
)

data class VisualSummaryUiState(
 val isLoading: Boolean = false,
 val topic: StudyTopic? = null,
 val lookup: VisualSummaryLookup? = null,
 val notice: NoticeMessage? = null
)

data class SavedSummariesUiState(
 val isLoading: Boolean = false,
 val result: SavedSummariesReadResult = SavedSummariesReadResult(emptyList(), 0),
 val notice: NoticeMessage? = null
)
