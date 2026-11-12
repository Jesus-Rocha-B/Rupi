package pe.rupi.mobile.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import pe.rupi.mobile.data.model.*
import java.util.UUID

/**
 * Mantiene en memoria la instancia activa del cliente REST autenticado.
 */
class SessionClientHolder {
 @Volatile
 private var current: RupiApi? = null

 fun get(): RupiApi? = current

 fun require(): RupiApi = current ?: throw RupiSessionExpired("Vuelve a iniciar sesión.")

 fun set(client: RupiApi?) {
  current = client
 }

 fun clear() {
  current?.clear()
  current = null
 }
}

/**
 * Fuente de datos remota sobre la API REST de Spring Boot.
 */
interface RupiRemoteDataSource {
 suspend fun login(serverUrl: String, username: String, password: String): RupiApi
 suspend fun logout()
 suspend fun getLearningRoutes(): List<LearningRouteSummary>
 suspend fun joinRouteByCode(code: String): JoinRouteResult
 suspend fun getRouteDetail(routeId: String): RouteDetail
 suspend fun startLessonNode(routeId: String, nodeId: String): LessonDetail
 suspend fun answerQuestion(activityId: String, questionId: String, selectedOptionId: String): QuestionFeedback
 suspend fun completeNode(routeId: String, nodeId: String): NodeCompletionResult
 suspend fun getExperience(): ExperienceSummary
 suspend fun getStreak(): StreakSummary
 suspend fun getLeaderboard(): ClassroomLeaderboard
 suspend fun getBadges(): List<StudentBadge>
 suspend fun getDailyGoalDashboard(): DailyGoalDashboard
 suspend fun updateDailyGoalTarget(target: Int)
 suspend fun getDailyChallenge(): DailyChallenge
 suspend fun completeDailyChallenge(challengeId: String): DailyChallengeCompletion
 suspend fun getNotificationsDashboard(): NotificationsDashboard
 suspend fun saveNotificationPreferences(preferences: StudyPreferences): StudyPreferences
 suspend fun markNotificationAsRead(notificationId: String)
 suspend fun getAiPractice(routeId: String, nodeId: String): AiPracticeStatus
 suspend fun generateAiPractice(routeId: String, nodeId: String): String
 suspend fun answerAiQuestion(practiceId: String, questionId: String, optionId: String)
 suspend fun reportAiQuestion(practiceId: String, questionId: String, reason: String)
 suspend fun getAiQuestionHint(practiceId: String, questionId: String): QuestionHint
 suspend fun getFlashcards(routeId: String, nodeId: String): FlashcardDeck
 suspend fun postFlashcardAction(routeId: String, nodeId: String, revision: Long, action: String, cardId: String)
 suspend fun getVisualSummary(routeId: String, nodeId: String): VisualSummaryLookup
 suspend fun generateVisualSummary(routeId: String, nodeId: String)
 suspend fun downloadVisualSummaryPackage(routeId: String, nodeId: String, resourceId: String): ByteArray
 suspend fun reportVisualSummary(routeId: String, nodeId: String, resourceId: String, reason: String)
}

class DefaultRupiRemoteDataSource(
 private val sessionHolder: SessionClientHolder,
 private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RupiRemoteDataSource {

 override suspend fun login(serverUrl: String, username: String, password: String): RupiApi =
  withContext(ioDispatcher) {
   val client = RupiApi(serverUrl)
   client.login(username, password)
   sessionHolder.set(client)
   client
  }

 override suspend fun logout() = withContext(ioDispatcher) {
  sessionHolder.get()?.request("/api/v1/auth/logout", "POST")
  sessionHolder.clear()
 }

 override suspend fun getLearningRoutes(): List<LearningRouteSummary> = withContext(ioDispatcher) {
  val client = sessionHolder.require()
  RupiJsonMapper.parseLearningRoutes(JSONObject(client.request("/api/v1/student/learning-routes")))
 }

 override suspend fun joinRouteByCode(code: String): JoinRouteResult = withContext(ioDispatcher) {
  val client = sessionHolder.require()
  val json = JSONObject(
   client.request("/api/v1/student/learning-routes/join-by-code", "POST", JSONObject().put("code", code))
  )
  RupiJsonMapper.parseJoinRouteResult(json)
 }

 override suspend fun getRouteDetail(routeId: String): RouteDetail = withContext(ioDispatcher) {
  val client = sessionHolder.require()
  val json = JSONObject(client.request("/api/v1/student/learning-routes/$routeId"))
  RupiJsonMapper.parseRouteDetail(json)
 }

 override suspend fun startLessonNode(routeId: String, nodeId: String): LessonDetail = withContext(ioDispatcher) {
  val client = sessionHolder.require()
  val json = JSONObject(client.request("/api/v1/student/learning-routes/$routeId/nodes/$nodeId/start", "POST"))
  RupiJsonMapper.parseLessonDetail(json, client::resource)
 }

 override suspend fun answerQuestion(
  activityId: String,
  questionId: String,
  selectedOptionId: String
 ): QuestionFeedback = withContext(ioDispatcher) {
  val client = sessionHolder.require()
  val body = JSONObject().put("questionId", questionId).put("selectedOptionId", selectedOptionId)
  val json = JSONObject(client.request("/api/v1/student/activities/$activityId/questions/answer", "POST", body))
  RupiJsonMapper.parseQuestionFeedback(json, questionId)
 }

 override suspend fun completeNode(routeId: String, nodeId: String): NodeCompletionResult =
  withContext(ioDispatcher) {
   val client = sessionHolder.require()
   val json = JSONObject(client.request("/api/v1/student/learning-routes/$routeId/nodes/$nodeId/complete", "POST"))
   RupiJsonMapper.parseNodeCompletion(json)
  }

 override suspend fun getExperience(): ExperienceSummary = withContext(ioDispatcher) {
  val json = JSONObject(sessionHolder.require().request("/api/v1/student/experience"))
  ExperienceSummary(totalXp = json.optInt("totalXp"))
 }

 override suspend fun getStreak(): StreakSummary = withContext(ioDispatcher) {
  val json = JSONObject(sessionHolder.require().request("/api/v1/student/streak"))
  StreakSummary(days = json.optInt("days"), atRisk = json.optBoolean("atRisk"))
 }

 override suspend fun getLeaderboard(): ClassroomLeaderboard = withContext(ioDispatcher) {
  val json = JSONObject(sessionHolder.require().request("/api/v1/student/leaderboard"))
  RupiJsonMapper.parseAchievements(JSONObject(), JSONObject(), json, JSONArray()).leaderboard
 }

 override suspend fun getBadges(): List<StudentBadge> = withContext(ioDispatcher) {
  val json = JSONArray(sessionHolder.require().request("/api/v1/student/badges"))
  RupiJsonMapper.parseAchievements(JSONObject(), JSONObject(), JSONObject(), json).badges
 }

 override suspend fun getDailyGoalDashboard(): DailyGoalDashboard = withContext(ioDispatcher) {
  val client = sessionHolder.require()
  val overviewJson = JSONObject(client.request("/api/v1/student/daily-goal"))
  val comparisonJson = JSONObject(client.request("/api/v1/student/daily-goal/comparison"))
  RupiJsonMapper.parseDailyGoalDashboard(overviewJson, comparisonJson)
 }

 override suspend fun updateDailyGoalTarget(target: Int) {
  withContext(ioDispatcher) {
   sessionHolder.require().request("/api/v1/student/daily-goal/target?target=$target", "PUT")
  }
 }

 override suspend fun getDailyChallenge(): DailyChallenge = withContext(ioDispatcher) {
  val json = JSONObject(sessionHolder.require().request("/api/v1/student/daily-challenge"))
  RupiJsonMapper.parseDailyChallenge(json)
 }

 override suspend fun completeDailyChallenge(challengeId: String): DailyChallengeCompletion =
  withContext(ioDispatcher) {
   val json = JSONObject(sessionHolder.require().request("/api/v1/student/daily-challenge/$challengeId/complete", "POST"))
   DailyChallengeCompletion(rupiMessage = json.getString("rupiMessage"))
  }

 override suspend fun getNotificationsDashboard(): NotificationsDashboard = withContext(ioDispatcher) {
  val client = sessionHolder.require()
  val statusJson = JSONObject(client.request("/api/v1/student/notifications/preferences"))
  val inboxJson = JSONObject(client.request("/api/v1/student/notifications"))
  RupiJsonMapper.parseNotificationsDashboard(statusJson, inboxJson)
 }

 override suspend fun saveNotificationPreferences(preferences: StudyPreferences): StudyPreferences =
  withContext(ioDispatcher) {
   val body = RupiJsonMapper.preferencesToJson(preferences)
   val json = JSONObject(sessionHolder.require().request("/api/v1/student/notifications/preferences", "PUT", body))
   RupiJsonMapper.parsePreferences(json.getJSONObject("preferences"))
  }

 override suspend fun markNotificationAsRead(notificationId: String) {
  withContext(ioDispatcher) {
   sessionHolder.require().request("/api/v1/student/notifications/$notificationId/read", "PUT")
  }
 }

 private fun nodePath(routeId: String, nodeId: String, suffix: String) =
  "/api/v1/student/learning-routes/$routeId/nodes/$nodeId/$suffix"

 override suspend fun getAiPractice(routeId: String, nodeId: String): AiPracticeStatus = withContext(ioDispatcher) {
  val json = JSONObject(sessionHolder.require().request(nodePath(routeId, nodeId, "ai-quiz")))
  RupiJsonMapper.parseAiPracticeStatus(json)
 }

 override suspend fun generateAiPractice(routeId: String, nodeId: String): String = withContext(ioDispatcher) {
  val client = sessionHolder.require()
  val json = JSONObject(
   client.request(nodePath(routeId, nodeId, "ai-quiz"), "POST", JSONObject().put("requestId", client.requestId(nodeId)))
  )
  val status = json.optString("status")
  if (status != "GENERANDO") client.pending.remove(nodeId)
  status
 }

 override suspend fun answerAiQuestion(practiceId: String, questionId: String, optionId: String) {
  withContext(ioDispatcher) {
   sessionHolder.require().request(
    "/api/v1/student/ai-quizzes/$practiceId/answers",
    "POST",
    JSONObject().put("questionId", questionId).put("optionId", optionId)
   )
  }
 }

 override suspend fun reportAiQuestion(practiceId: String, questionId: String, reason: String) {
  withContext(ioDispatcher) {
   sessionHolder.require().request(
    "/api/v1/student/ai-quizzes/$practiceId/reports",
    "POST",
    JSONObject().put("questionId", questionId).put("reason", reason)
   )
  }
 }

 override suspend fun getAiQuestionHint(practiceId: String, questionId: String): QuestionHint =
  withContext(ioDispatcher) {
   val json = JSONObject(
    sessionHolder.require().request("/api/v1/student/ai-quizzes/$practiceId/questions/$questionId/hint", "POST")
   )
   RupiJsonMapper.parseQuestionHint(json)
  }

 override suspend fun getFlashcards(routeId: String, nodeId: String): FlashcardDeck = withContext(ioDispatcher) {
  val json = JSONObject(sessionHolder.require().request(nodePath(routeId, nodeId, "flashcards")))
  RupiJsonMapper.parseFlashcardDeck(json)
 }

 override suspend fun postFlashcardAction(
  routeId: String,
  nodeId: String,
  revision: Long,
  action: String,
  cardId: String
 ) {
  withContext(ioDispatcher) {
   val body = JSONObject()
    .put("requestId", UUID.randomUUID().toString())
    .put("revision", revision)
    .put("action", action)
    .put("cardId", cardId)
   sessionHolder.require().request(nodePath(routeId, nodeId, "flashcards"), "POST", body)
  }
 }

 override suspend fun getVisualSummary(routeId: String, nodeId: String): VisualSummaryLookup =
  withContext(ioDispatcher) {
   val json = JSONObject(sessionHolder.require().request(nodePath(routeId, nodeId, "visual-summary")))
   RupiJsonMapper.parseVisualSummaryResponse(json)
  }

 override suspend fun generateVisualSummary(routeId: String, nodeId: String) {
  withContext(ioDispatcher) {
   sessionHolder.require().request(
    nodePath(routeId, nodeId, "visual-summary"),
    "POST",
    JSONObject().put("requestId", UUID.randomUUID().toString())
   )
  }
 }

 override suspend fun downloadVisualSummaryPackage(routeId: String, nodeId: String, resourceId: String): ByteArray =
  withContext(ioDispatcher) {
   sessionHolder.require().download("${nodePath(routeId, nodeId, "visual-summary")}/$resourceId/package")
  }

 override suspend fun reportVisualSummary(routeId: String, nodeId: String, resourceId: String, reason: String) {
  withContext(ioDispatcher) {
   sessionHolder.require().request(
    "${nodePath(routeId, nodeId, "visual-summary")}/$resourceId/report",
    "POST",
    JSONObject().put("reason", reason)
   )
  }
 }
}
