package pe.rupi.mobile

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.rupi.mobile.data.model.DailyChallenge
import pe.rupi.mobile.data.model.LearningRouteSummary
import pe.rupi.mobile.data.model.QuestionFeedback
import pe.rupi.mobile.data.model.RouteNode
import pe.rupi.mobile.data.model.RouteNodeState
import pe.rupi.mobile.data.model.RupiSessionExpired
import pe.rupi.mobile.data.model.StudyPreferences
import pe.rupi.mobile.data.remote.SessionClientHolder
import pe.rupi.mobile.data.repository.DefaultAuthRepository
import pe.rupi.mobile.data.repository.DefaultGamificationRepository
import pe.rupi.mobile.data.repository.DefaultLearningRepository
import pe.rupi.mobile.data.repository.DefaultNotificationRepository
import pe.rupi.mobile.data.repository.DefaultStudyResourcesRepository
import pe.rupi.mobile.data.repository.SessionState
import pe.rupi.mobile.ui.viewmodel.GamificationViewModel
import pe.rupi.mobile.ui.viewmodel.NotificationsViewModel
import pe.rupi.mobile.ui.viewmodel.RouteLessonViewModel
import pe.rupi.mobile.ui.viewmodel.StudyResourcesViewModel
import pe.rupi.mobile.ui.viewmodel.TopicsViewModel

class MvvmViewModelsTest {

 @get:Rule
 val mainDispatcherRule = MainDispatcherRule()

 @Test
 fun `NotificationsViewModel exige categoria marcada antes de activar avisos en Android`() = runTest {
  val remote = FakeRupiRemoteDataSource()
  val alerts = FakeSessionAlertsDataSource()
  val sessionHolder = SessionClientHolder()
  val authRepo = DefaultAuthRepository(remote, sessionHolder, alerts)
  val notifRepo = DefaultNotificationRepository(remote, sessionHolder, alerts, mainDispatcherRule.testDispatcher)
  val vm = NotificationsViewModel(notifRepo, authRepo)

  vm.savePreferences(
   StudyPreferences(
    dailyGoal = 2,
    goals = false,
    evaluations = false,
    system = true,
    remindAt = "18:00",
    quietFrom = "21:00",
    quietUntil = "07:00",
    revision = 1L
   ),
   hasSystemNotificationPermission = true
  )

  assertEquals(0, remote.savePrefsCalls)
  assertTrue(vm.uiState.value.notice?.text?.contains("marca también qué avisos quieres") == true)
 }

 @Test
 fun `RouteLessonViewModel maneja 401 HU-43 propagando RupiSessionExpired a AuthRepository`() = runTest {
  val remote = FakeRupiRemoteDataSource().apply {
   startLessonHandler = { _, _ -> throw RupiSessionExpired("Tu cuenta se abrió en la web", otherDevice = true) }
  }
  val alerts = FakeSessionAlertsDataSource()
  val sessionHolder = SessionClientHolder()
  val authRepo = DefaultAuthRepository(remote, sessionHolder, alerts)
  val learningRepo = DefaultLearningRepository(remote)
  val vm = RouteLessonViewModel(learningRepo, authRepo)

  vm.openLesson(
   LearningRouteSummary("r-1", "Matemática"),
   RouteNode("n-1", "Sumas", 1, RouteNodeState.DISPONIBLE)
  )

  val session = authRepo.sessionState.value
  assertTrue(session is SessionState.Unauthenticated)
  assertEquals("Tu cuenta se abrió en la web", (session as SessionState.Unauthenticated).notice)
  assertTrue(alerts.disableCalls >= 1)
 }

 @Test
 fun `RouteLessonViewModel conserva respuestas de la parada y las limpia al cambiar de nodo`() = runTest {
  val remote = FakeRupiRemoteDataSource()
  val alerts = FakeSessionAlertsDataSource()
  val sessionHolder = SessionClientHolder()
  val authRepo = DefaultAuthRepository(remote, sessionHolder, alerts)
  val vm = RouteLessonViewModel(DefaultLearningRepository(remote), authRepo)
  val route = LearningRouteSummary("r-1", "Comunicación")
  val node1 = RouteNode("n-1", "Lectura 1", 1, RouteNodeState.DISPONIBLE)
  val node2 = RouteNode("n-2", "Lectura 2", 2, RouteNodeState.DISPONIBLE)

  vm.openLesson(route, node1)
  vm.submitLessonAnswer(route, node1, "act-1", "q-1", "opt-a")
  assertTrue(vm.lessonState.value.answers.containsKey("q-1"))

  vm.openLesson(route, node2)
  assertFalse(vm.lessonState.value.answers.containsKey("q-1"))
  assertEquals("n-2", vm.lessonState.value.node?.id)
 }

 @Test
 fun `GamificationViewModel reinicia respuestas si cambia el dia del reto`() = runTest {
  var day = "2026-11-11"
  val remote = FakeRupiRemoteDataSource().apply {
   dailyChallengeHandler = {
    DailyChallenge(
     id = "ch-$day",
     date = day,
     activityVersionId = "av-$day",
     skill = "COMPRENSION_LECTORA",
     estimatedMinutes = 5,
     experienceReward = 30,
     title = "Reto $day",
     description = "Desc",
     readingText = "Texto",
     isCompleted = false,
     answeredToday = emptyMap(),
     questions = emptyList()
    )
   }
  }
  val authRepo = DefaultAuthRepository(remote, SessionClientHolder(), FakeSessionAlertsDataSource())
  val vm = GamificationViewModel(DefaultGamificationRepository(remote), authRepo)

  vm.loadDailyChallenge()
  vm.submitChallengeAnswer("av-2026-11-11", "q-day1", "opt-1")
  assertTrue(vm.dailyChallengeState.value.answers.containsKey("q-day1"))

  day = "2026-11-12"
  vm.loadDailyChallenge(force = true)
  assertNull(vm.dailyChallengeState.value.answers["q-day1"])
  assertEquals("2026-11-12", vm.dailyChallengeState.value.activeDay)
 }

 @Test
 fun `TopicsViewModel rechaza codigo en blanco sin invocar repositorio`() = runTest {
  val remote = FakeRupiRemoteDataSource()
  val authRepo = DefaultAuthRepository(remote, SessionClientHolder(), FakeSessionAlertsDataSource())
  val learningRepo = DefaultLearningRepository(remote)
  val vm = TopicsViewModel(learningRepo, authRepo)

  var joinedCalled = false
  vm.joinByCode("   ") { joinedCalled = true }

  assertFalse(joinedCalled)
  assertTrue(vm.uiState.value.notice?.text?.contains("Escribe el código") == true)
 }

 @Test
 fun `GamificationViewModel valida rango de meta diaria entre 1 y 20`() = runTest {
  val remote = FakeRupiRemoteDataSource()
  val authRepo = DefaultAuthRepository(remote, SessionClientHolder(), FakeSessionAlertsDataSource())
  val gamificationRepo = DefaultGamificationRepository(remote)
  val vm = GamificationViewModel(gamificationRepo, authRepo)

  vm.updateDailyTarget(0)
  assertTrue(vm.dailyGoalState.value.notice?.text?.contains("entre 1 y 20") == true)

  vm.updateDailyTarget(25)
  assertTrue(vm.dailyGoalState.value.notice?.text?.contains("entre 1 y 20") == true)
 }

 @Test
 fun `RouteLessonViewModel rechaza comprobacion si alternativa esta en blanco`() = runTest {
  val remote = FakeRupiRemoteDataSource()
  val authRepo = DefaultAuthRepository(remote, SessionClientHolder(), FakeSessionAlertsDataSource())
  val learningRepo = DefaultLearningRepository(remote)
  val vm = RouteLessonViewModel(learningRepo, authRepo)

  val route = LearningRouteSummary("r-1", "Matemática")
  val node = RouteNode("n-1", "Sumas", 1, RouteNodeState.DISPONIBLE)

  vm.submitLessonAnswer(route, node, "act-1", "q-1", "")
  assertTrue(vm.lessonState.value.notice?.text?.contains("Elige una alternativa") == true)
 }

 @Test
 fun `StudyResourcesViewModel reinicia todos los estados en clear`() = runTest {
  val remote = FakeRupiRemoteDataSource()
  val authRepo = DefaultAuthRepository(remote, SessionClientHolder(), FakeSessionAlertsDataSource())
  val repo = DefaultStudyResourcesRepository(remote, FakeLocalSummaryDataSource())
  val vm = StudyResourcesViewModel(repo, authRepo)

  vm.showTtsMissingNoticeOnCurrentScreen()
  assertTrue(vm.aiPracticeState.value.notice != null)
  assertTrue(vm.flashcardsState.value.notice != null)
  assertTrue(vm.visualSummaryState.value.notice != null)
  assertTrue(vm.savedSummariesState.value.notice != null)

  vm.clear()
  assertNull(vm.aiPracticeState.value.notice)
  assertNull(vm.flashcardsState.value.notice)
  assertNull(vm.visualSummaryState.value.notice)
  assertNull(vm.savedSummariesState.value.notice)
 }
}
