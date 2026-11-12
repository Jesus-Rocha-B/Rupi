package pe.rupi.mobile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pe.rupi.mobile.data.local.LocalSummaryDataSource
import pe.rupi.mobile.data.local.SavedCopyCheck
import pe.rupi.mobile.data.local.SavedSummariesReadResult
import pe.rupi.mobile.data.local.SessionAlertsDataSource
import pe.rupi.mobile.data.model.*
import pe.rupi.mobile.data.remote.RupiApi
import pe.rupi.mobile.data.remote.RupiRemoteDataSource

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
 val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
 override fun starting(description: Description) {
  Dispatchers.setMain(testDispatcher)
 }

 override fun finished(description: Description) {
  Dispatchers.resetMain()
 }
}

open class FakeRupiRemoteDataSource : RupiRemoteDataSource {
 var routes: List<LearningRouteSummary> = emptyList()
 var visualSummaryHandler: suspend (String, String) -> VisualSummaryLookup = { _, _ ->
  VisualSummaryLookup(resource = null, configured = false, offline = false)
 }
 var startLessonHandler: suspend (String, String) -> LessonDetail = { _, nodeId ->
  LessonDetail("act-1", "Lección $nodeId", null, "EN_CURSO", emptyList(), emptyList())
 }
 var answerQuestionHandler: suspend (String, String, String) -> QuestionFeedback = { _, qId, optId ->
  QuestionFeedback(qId, true, optId, "¡Correcto!")
 }
 var dailyChallengeHandler: suspend () -> DailyChallenge = {
  DailyChallenge(
   id = "ch-1",
   date = "2026-11-12",
   activityVersionId = "av-1",
   skill = "RAZONAMIENTO_LOGICO",
   estimatedMinutes = 5,
   experienceReward = 30,
   title = "Feria de Huancayo",
   description = "Calcula el vuelto",
   readingText = "",
   isCompleted = false,
   answeredToday = emptyMap(),
   questions = emptyList()
  )
 }
 var notificationsDashboard: NotificationsDashboard = NotificationsDashboard(
  status = NotificationPreferencesStatus(
   completedToday = 1,
   preferences = StudyPreferences(2, true, true, false, "18:00", "21:00", "07:00", 1L)
  ),
  inbox = NotificationInbox(quiet = false, system = false, items = emptyList())
 )
 var savePrefsCalls = 0
 var joinCodeReceived: String? = null

 override suspend fun login(serverUrl: String, username: String, password: String): RupiApi =
  throw UnsupportedOperationException("Not configured in fake")

 override suspend fun logout() {}
 override suspend fun getLearningRoutes(): List<LearningRouteSummary> = routes
 override suspend fun joinRouteByCode(code: String): JoinRouteResult {
  joinCodeReceived = code
  return JoinRouteResult(LearningRouteSummary("r-joined", "Ruta $code"), "¡Listo!")
 }
 override suspend fun getRouteDetail(routeId: String): RouteDetail =
  RouteDetail(LearningRouteSummary(routeId, "Ruta"), emptyList(), emptyList())
 override suspend fun startLessonNode(routeId: String, nodeId: String): LessonDetail =
  startLessonHandler(routeId, nodeId)
 override suspend fun answerQuestion(activityId: String, questionId: String, selectedOptionId: String): QuestionFeedback =
  answerQuestionHandler(activityId, questionId, selectedOptionId)
 override suspend fun completeNode(routeId: String, nodeId: String): NodeCompletionResult =
  NodeCompletionResult(20, "¡Bien hecho!", null, false, 0, null)
 override suspend fun getExperience(): ExperienceSummary = ExperienceSummary(100)
 override suspend fun getStreak(): StreakSummary = StreakSummary(3, false)
 override suspend fun getLeaderboard(): ClassroomLeaderboard =
  ClassroomLeaderboard("3A", 1, 20, emptyList())
 override suspend fun getBadges(): List<StudentBadge> = emptyList()
 override suspend fun getDailyGoalDashboard(): DailyGoalDashboard = DailyGoalDashboard(
  DailyGoalOverview(1, 2, 1, false, "Sigue así", 3, 2, listOf(1, 2, 3, 5), emptyList()),
  ScoreComparison(20, 10, 1, 0, "SUBE", "Subiste hoy")
 )
 override suspend fun updateDailyGoalTarget(target: Int) {}
 override suspend fun getDailyChallenge(): DailyChallenge = dailyChallengeHandler()
 override suspend fun completeDailyChallenge(challengeId: String): DailyChallengeCompletion =
  DailyChallengeCompletion("¡Completado! +30 XP")
 override suspend fun getNotificationsDashboard(): NotificationsDashboard = notificationsDashboard
 override suspend fun saveNotificationPreferences(preferences: StudyPreferences): StudyPreferences {
  savePrefsCalls++
  return preferences.copy(revision = preferences.revision + 1)
 }
 override suspend fun markNotificationAsRead(notificationId: String) {}
 override suspend fun getAiPractice(routeId: String, nodeId: String): AiPracticeStatus =
  AiPracticeStatus(configured = false, reviewedAvailable = false, practice = null)
 override suspend fun generateAiPractice(routeId: String, nodeId: String): String = "LISTO"
 override suspend fun answerAiQuestion(practiceId: String, questionId: String, optionId: String) {}
 override suspend fun reportAiQuestion(practiceId: String, questionId: String, reason: String) {}
 override suspend fun getAiQuestionHint(practiceId: String, questionId: String): QuestionHint =
  QuestionHint("INICIAL", listOf("Paso 1"), "Pista", "Ejemplo", "IA", false)
 override suspend fun getFlashcards(routeId: String, nodeId: String): FlashcardDeck =
  FlashcardDeck("", false, 1L, emptyList())
 override suspend fun postFlashcardAction(routeId: String, nodeId: String, revision: Long, action: String, cardId: String) {}
 override suspend fun getVisualSummary(routeId: String, nodeId: String): VisualSummaryLookup =
  visualSummaryHandler(routeId, nodeId)
 override suspend fun generateVisualSummary(routeId: String, nodeId: String) {}
 override suspend fun downloadVisualSummaryPackage(routeId: String, nodeId: String, resourceId: String): ByteArray =
  byteArrayOf()
 override suspend fun reportVisualSummary(routeId: String, nodeId: String, resourceId: String, reason: String) {}
}

class FakeLocalSummaryDataSource : LocalSummaryDataSource {
 val stored = mutableMapOf<Pair<String, String>, SavedVisualSummary>()
 val deletedKeys = mutableListOf<Pair<String, String>>()
 var invalidKeys = mutableSetOf<Pair<String, String>>()

 override suspend fun listSaved(): SavedSummariesReadResult =
  SavedSummariesReadResult(items = stored.values.toList(), unreadableCount = invalidKeys.size)

 override suspend fun readSaved(routeId: String, nodeId: String): SavedVisualSummary? =
  stored[routeId to nodeId]

 override suspend fun checkSavedCopy(routeId: String, nodeId: String): SavedCopyCheck {
  val key = routeId to nodeId
  if (key in invalidKeys) return SavedCopyCheck.Invalid
  val saved = stored[key] ?: return SavedCopyCheck.Missing
  return SavedCopyCheck.Valid(saved.resource.version)
 }

 override suspend fun saveFromZipPackage(topic: StudyTopic, zipBytes: ByteArray) {}

 override suspend fun deleteSaved(routeId: String, nodeId: String) {
  val key = routeId to nodeId
  stored.remove(key)
  invalidKeys.remove(key)
  deletedKeys.add(key)
 }
}

class FakeSessionAlertsDataSource : SessionAlertsDataSource {
 var restoredClient: RupiApi? = null
 var allowed = true
 var enableCalls = 0
 var disableCalls = 0

 override fun restoreSessionClient(): RupiApi? = restoredClient
 override fun areDeviceNotificationsAllowed(): Boolean = allowed
 override fun enableDeviceAlerts(client: RupiApi) {
  enableCalls++
 }
 override fun disableDeviceAlerts() {
  disableCalls++
 }
}
