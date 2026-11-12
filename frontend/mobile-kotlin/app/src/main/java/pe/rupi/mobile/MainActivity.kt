package pe.rupi.mobile

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import pe.rupi.mobile.data.model.RouteNodeState
import pe.rupi.mobile.data.repository.SessionState
import pe.rupi.mobile.ui.components.ScreenScaffold
import pe.rupi.mobile.ui.screens.mountLoginScreen
import pe.rupi.mobile.ui.screens.mountTopicsScreen
import pe.rupi.mobile.ui.screens.renderAchievementsBody
import pe.rupi.mobile.ui.screens.renderAiPracticeBody
import pe.rupi.mobile.ui.screens.renderDailyChallengeBody
import pe.rupi.mobile.ui.screens.renderDailyGoalBody
import pe.rupi.mobile.ui.screens.renderFlashcardsBody
import pe.rupi.mobile.ui.screens.renderLessonBody
import pe.rupi.mobile.ui.screens.renderNotificationsBody
import pe.rupi.mobile.ui.screens.renderRouteDetailBody
import pe.rupi.mobile.ui.screens.renderSavedSummariesBody
import pe.rupi.mobile.ui.screens.renderSavedSummaryDetailBody
import pe.rupi.mobile.ui.screens.renderTopicsRoutesList
import pe.rupi.mobile.ui.screens.renderVisualSummaryBody
import pe.rupi.mobile.ui.screens.showHintDialog
import pe.rupi.mobile.ui.state.NavDestination
import pe.rupi.mobile.ui.state.NoticeMessage
import pe.rupi.mobile.ui.state.NoticeTone
import pe.rupi.mobile.ui.state.NotificationEffect
import pe.rupi.mobile.ui.viewmodel.AuthViewModel
import pe.rupi.mobile.ui.viewmodel.GamificationViewModel
import pe.rupi.mobile.ui.viewmodel.NavigationViewModel
import pe.rupi.mobile.ui.viewmodel.NotificationsViewModel
import pe.rupi.mobile.ui.viewmodel.RouteLessonViewModel
import pe.rupi.mobile.ui.viewmodel.StudyResourcesViewModel
import pe.rupi.mobile.ui.viewmodel.TopicsViewModel

/**
 * Capa View principal en arquitectura MVVM + Repository.
 * Observa StateFlow/SharedFlow de los ViewModels y delega toda lógica de negocio y red.
 */
class MainActivity : ComponentActivity() {

 private val container by lazy { (application as RupiApplication).container }
 private val navigationVm: NavigationViewModel by viewModels { container.viewModelFactory }
 private val authVm: AuthViewModel by viewModels { container.viewModelFactory }
 private val topicsVm: TopicsViewModel by viewModels { container.viewModelFactory }
 private val routeLessonVm: RouteLessonViewModel by viewModels { container.viewModelFactory }
 private val gamificationVm: GamificationViewModel by viewModels { container.viewModelFactory }
 private val notificationsVm: NotificationsViewModel by viewModels { container.viewModelFactory }
 private val studyResourcesVm: StudyResourcesViewModel by viewModels { container.viewModelFactory }

 private var voice: RupiVoice? = null
 private lateinit var scaffold: ScreenScaffold

 private var mountedDestination: NavDestination? = null
 private var topicsRoutesContainer: LinearLayout? = null
 private var lastRenderedRoutesLoaded: Boolean = false
 private var lastRenderedRoutes: Any? = null
 private var lastRenderedRouteDetail: Any? = null
 private var lastRenderedLessonSignature: Any? = null
 private var lastRenderedAchievements: Any? = null
 private var lastRenderedDailyGoal: Any? = null
 private var lastRenderedChallengeSignature: Any? = null
 private var lastRenderedNotifications: Any? = null
 private var lastRenderedAiPractice: Any? = null
 private var lastRenderedFlashcards: Any? = null
 private var lastRenderedVisualSummary: Any? = null
 private var lastRenderedSavedSummaries: Any? = null

 private var networkLostDuringView = false
 private var connectivityRegistered = false

 private val notificationPermissionLauncher = registerForActivityResult(
  ActivityResultContracts.RequestPermission()
 ) { granted ->
  notificationsVm.onSystemPermissionResult(granted)
 }

 private val networkCallback = object : ConnectivityManager.NetworkCallback() {
  override fun onAvailable(network: Network) {
   runOnUiThread {
    val dest = navigationVm.current
    if (dest is NavDestination.VisualSummary) {
     val isOffline = studyResourcesVm.visualSummaryState.value.lookup?.offline == true
     if (networkLostDuringView || isOffline) {
      networkLostDuringView = false
      studyResourcesVm.refreshVisualSummaryOnReconnect(dest.topic)
     }
    }
   }
  }

  override fun onLost(network: Network) {
   networkLostDuringView = true
  }
 }

 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  styleSystemBars()
  voice = RupiVoice(this)
  scaffold = ScreenScaffold(this, voice)

  onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
   override fun handleOnBackPressed() {
    if (scaffold.isBusy) return
    if (!navigationVm.navigateUp()) {
     finish()
    }
   }
  })

  val authenticated = authVm.restoreSession()
  val openNotifications = intent.getBooleanExtra("notifications", false)
  navigationVm.initialize(authenticated = authenticated, openNotifications = openNotifications)

  observeState()
 }

 override fun onStart() {
  super.onStart()
  registerConnectivityObserver()
 }

 override fun onStop() {
  unregisterConnectivityObserver()
  super.onStop()
 }

 override fun onDestroy() {
  voice?.shutdown()
  super.onDestroy()
 }

 private fun observeState() {
  lifecycleScope.launch {
   repeatOnLifecycle(Lifecycle.State.STARTED) {
    launch {
     authVm.sessionState.collect { session ->
      if (session is SessionState.Unauthenticated) {
       val current = navigationVm.current
       if (current !is NavDestination.Login &&
        current !is NavDestination.SavedSummaries &&
        current !is NavDestination.SavedSummaryDetail
       ) {
        clearAllFeatureViewModels()
        navigationVm.resetTo(NavDestination.Login)
       }
       session.notice?.let { msg -> authVm.showSessionNotice(msg) }
      }
     }
    }

    launch {
     navigationVm.backStack.collect { stack ->
      val destination = stack.lastOrNull() ?: NavDestination.Login
      renderDestination(destination)
     }
    }

    launch {
     authVm.uiState.collect { state ->
      if (navigationVm.current is NavDestination.Login) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
      }
     }
    }

    launch {
     topicsVm.uiState.collect { state ->
      if (navigationVm.current is NavDestination.Topics) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       val containerLayout = topicsRoutesContainer
       if (containerLayout != null &&
        (lastRenderedRoutesLoaded != state.routesLoaded || lastRenderedRoutes != state.routes)
       ) {
        lastRenderedRoutesLoaded = state.routesLoaded
        lastRenderedRoutes = state.routes
        renderTopicsRoutesList(scaffold, containerLayout, state) { route ->
         routeLessonVm.loadRouteDetail(route, force = true)
         navigationVm.navigateTo(NavDestination.RouteDetail(route))
        }
       }
      }
     }
    }

    launch {
     routeLessonVm.routeDetailState.collect { state ->
      if (navigationVm.current is NavDestination.RouteDetail) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       if (lastRenderedRouteDetail !== state.detail) {
        lastRenderedRouteDetail = state.detail
        renderRouteDetailBody(
         context = this@MainActivity,
         scaffold = scaffold,
         state = state,
         onBackToTopics = { backToTopics() },
         onOpenNode = { route, node ->
          routeLessonVm.openLesson(route, node, force = true)
          navigationVm.navigateTo(NavDestination.Lesson(route, node))
         },
         onBlockedNode = { routeLessonVm.showBlockedNodeNotice() }
        )
       }
      }
     }
    }

    launch {
     routeLessonVm.lessonState.collect { state ->
      if (navigationVm.current is NavDestination.Lesson) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       val signature = Triple(state.lesson, state.answers, state.completionResult)
       if (lastRenderedLessonSignature != signature) {
        lastRenderedLessonSignature = signature
        renderLessonBody(
         context = this@MainActivity,
         scaffold = scaffold,
         state = state,
         onBackToRoute = { route ->
          routeLessonVm.loadRouteDetail(route, force = false)
          navigationVm.popTo { it is NavDestination.RouteDetail }
         },
         onOpenExternalLink = { link -> openExternalLink(link) },
         onSubmitAnswer = { activityId, questionId, optionId ->
          val r = state.route ?: return@renderLessonBody
          val n = state.node ?: return@renderLessonBody
          routeLessonVm.submitLessonAnswer(r, n, activityId, questionId, optionId)
         },
         onCompleteNode = {
          val r = state.route ?: return@renderLessonBody
          val n = state.node ?: return@renderLessonBody
          routeLessonVm.completeLessonNode(r, n) { topicsVm.invalidateCache() }
         },
         onOpenNextNode = { route, next ->
          routeLessonVm.openLesson(route, next, force = true)
          navigationVm.replaceTop(NavDestination.Lesson(route, next))
         },
         onOpenAiPractice = { topic ->
          studyResourcesVm.loadAiPractice(topic, force = true)
          navigationVm.navigateTo(NavDestination.AiPractice(topic))
         },
         onOpenFlashcards = { topic ->
          studyResourcesVm.loadFlashcards(topic, force = true)
          navigationVm.navigateTo(NavDestination.Flashcards(topic))
         }
        )
       }
      }
     }
    }

    launch {
     gamificationVm.achievementsState.collect { state ->
      if (navigationVm.current is NavDestination.Achievements) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       if (lastRenderedAchievements !== state.overview) {
        lastRenderedAchievements = state.overview
        renderAchievementsBody(scaffold, state) { backToTopics() }
       }
      }
     }
    }

    launch {
     gamificationVm.dailyGoalState.collect { state ->
      if (navigationVm.current is NavDestination.DailyGoal) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       if (lastRenderedDailyGoal !== state.dashboard) {
        lastRenderedDailyGoal = state.dashboard
        renderDailyGoalBody(
         context = this@MainActivity,
         scaffold = scaffold,
         state = state,
         onBackToTopics = { backToTopics() },
         onRefresh = { gamificationVm.loadDailyGoal(force = true) },
         onSelectTarget = { target -> gamificationVm.updateDailyTarget(target) }
        )
       }
      }
     }
    }

    launch {
     gamificationVm.dailyChallengeState.collect { state ->
      if (navigationVm.current is NavDestination.DailyChallenge) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       val signature = Triple(state.challenge, state.answers, state.completion)
       if (lastRenderedChallengeSignature != signature) {
        lastRenderedChallengeSignature = signature
        renderDailyChallengeBody(
         context = this@MainActivity,
         scaffold = scaffold,
         state = state,
         onBackToTopics = { backToTopics() },
         onSubmitAnswer = { activityVersionId, questionId, optionId ->
          gamificationVm.submitChallengeAnswer(activityVersionId, questionId, optionId)
         },
         onCompleteChallenge = { challengeId ->
          gamificationVm.completeChallenge(challengeId)
         }
        )
       }
      }
     }
    }

    launch {
     notificationsVm.uiState.collect { state ->
      if (navigationVm.current is NavDestination.Notifications) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       if (lastRenderedNotifications !== state.dashboard) {
        lastRenderedNotifications = state.dashboard
        renderNotificationsBody(
         context = this@MainActivity,
         scaffold = scaffold,
         state = state,
         onBackToTopics = { backToTopics() },
         onRefresh = { notificationsVm.load(force = true) },
         onSavePreferences = { prefs ->
          val hasPerm = Build.VERSION.SDK_INT < 33 ||
           checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
          notificationsVm.savePreferences(prefs, hasSystemNotificationPermission = hasPerm)
         },
         onMarkAsRead = { id -> notificationsVm.markAsRead(id) }
        )
       }
      }
     }
    }

    launch {
     notificationsVm.effects.collect { effect ->
      when (effect) {
       NotificationEffect.RequestSystemPermission -> {
        if (Build.VERSION.SDK_INT >= 33) {
         notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
       }
      }
     }
    }

    launch {
     studyResourcesVm.aiPracticeState.collect { state ->
      if (navigationVm.current is NavDestination.AiPractice) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       if (lastRenderedAiPractice !== state.status) {
        lastRenderedAiPractice = state.status
        renderAiPracticeBody(
         context = this@MainActivity,
         scaffold = scaffold,
         state = state,
         onBackToLesson = { navigationVm.navigateUp() },
         onRefresh = { topic -> studyResourcesVm.loadAiPractice(topic, force = true) },
         onOpenFlashcards = { topic ->
          studyResourcesVm.loadFlashcards(topic, force = true)
          navigationVm.navigateTo(NavDestination.Flashcards(topic))
         },
         onStartPractice = { topic -> studyResourcesVm.requestAiPractice(topic) },
         onSubmitAnswer = { topic, practiceId, qId, optId ->
          studyResourcesVm.answerAiQuestion(topic, practiceId, qId, optId)
         },
         onRequestHint = { practiceId, qId ->
          studyResourcesVm.requestAiQuestionHint(practiceId, qId)
         },
         onReportQuestion = { topic, practiceId, qId, reason ->
          studyResourcesVm.reportAiQuestion(topic, practiceId, qId, reason)
         }
        )
       }
      }
     }
    }

    launch {
     studyResourcesVm.hintEffect.collect { hint ->
      showHintDialog(this@MainActivity, hint, voice) {
       studyResourcesVm.showTtsMissingNoticeOnCurrentScreen()
      }
     }
    }

    launch {
     studyResourcesVm.flashcardsState.collect { state ->
      if (navigationVm.current is NavDestination.Flashcards) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       if (lastRenderedFlashcards !== state.deck) {
        lastRenderedFlashcards = state.deck
        renderFlashcardsBody(
         scaffold = scaffold,
         state = state,
         onBackToTopic = { navigationVm.navigateUp() },
         onRefresh = { topic -> studyResourcesVm.loadFlashcards(topic, force = true) },
         onAction = { topic, rev, action, cardId ->
          studyResourcesVm.sendFlashcardAction(topic, rev, action, cardId)
         }
        )
       }
      }
     }
    }

    launch {
     studyResourcesVm.visualSummaryState.collect { state ->
      if (navigationVm.current is NavDestination.VisualSummary) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       if (lastRenderedVisualSummary !== state.lookup) {
        lastRenderedVisualSummary = state.lookup
        renderVisualSummaryBody(
         context = this@MainActivity,
         scaffold = scaffold,
         state = state,
         onBackToTopic = { navigationVm.navigateUp() },
         onRefresh = { topic -> studyResourcesVm.loadVisualSummary(topic, force = true) },
         onGenerate = { topic -> studyResourcesVm.generateVisualSummary(topic) },
         onSaveOffline = { topic, resId -> studyResourcesVm.downloadVisualSummaryForOffline(topic, resId) },
         onReport = { topic, resId, reason -> studyResourcesVm.reportVisualSummary(topic, resId, reason) }
        )
       }
      }
     }
    }

    launch {
     studyResourcesVm.savedSummariesState.collect { state ->
      if (navigationVm.current is NavDestination.SavedSummaries) {
       scaffold.isBusy = state.isLoading
       scaffold.updateNotice(state.notice)
       if (lastRenderedSavedSummaries !== state.result) {
        lastRenderedSavedSummaries = state.result
        renderSavedSummariesBody(
         scaffold = scaffold,
         state = state,
         onBack = { navigationVm.navigateUp() },
         onOpenSaved = { saved -> navigationVm.navigateTo(NavDestination.SavedSummaryDetail(saved)) }
        )
       }
      }
     }
    }
   }
  }
 }

 private fun renderDestination(destination: NavDestination) {
  if (mountedDestination == destination) return
  mountedDestination = destination
  networkLostDuringView = false
  topicsRoutesContainer = null

  when (destination) {
   NavDestination.Login -> {
    mountLoginScreen(
     scaffold = scaffold,
     onLogin = { serverText, username, secret ->
      authVm.login(serverText, username, secret) {
       topicsVm.invalidateCache()
       topicsVm.loadRoutes(force = true)
       navigationVm.resetTo(NavDestination.Topics)
      }
     },
     onOpenSavedSummaries = {
      studyResourcesVm.loadSavedSummaries()
      navigationVm.navigateTo(NavDestination.SavedSummaries)
     }
    )
    scaffold.updateNotice(authVm.uiState.value.notice)
   }

   NavDestination.Topics -> {
    lastRenderedRoutesLoaded = false
    lastRenderedRoutes = null
    topicsRoutesContainer = mountTopicsScreen(
     scaffold = scaffold,
     onOpenDailyChallenge = {
      gamificationVm.loadDailyChallenge(force = true)
      navigationVm.navigateTo(NavDestination.DailyChallenge)
     },
     onOpenDailyGoal = {
      gamificationVm.loadDailyGoal(force = true)
      navigationVm.navigateTo(NavDestination.DailyGoal)
     },
     onOpenAchievements = {
      gamificationVm.loadAchievements(force = true)
      navigationVm.navigateTo(NavDestination.Achievements)
     },
     onJoinByCode = { code ->
      topicsVm.joinByCode(code) { joined ->
       routeLessonVm.loadRouteDetail(joined.route, welcomeNotice = joined.message, force = true)
       navigationVm.navigateTo(NavDestination.RouteDetail(joined.route))
      }
     },
     onOpenNotifications = {
      notificationsVm.load(force = true)
      navigationVm.navigateTo(NavDestination.Notifications)
     },
     onOpenSavedSummaries = {
      studyResourcesVm.loadSavedSummaries()
      navigationVm.navigateTo(NavDestination.SavedSummaries)
     },
     onRefresh = { topicsVm.loadRoutes(force = true) },
     onLogout = {
      authVm.logout(
       onLoggedOut = {
        clearAllFeatureViewModels()
        navigationVm.resetTo(NavDestination.Login)
       },
       onError = { msg -> topicsVm.showError(msg) }
      )
     }
    )
    val tState = topicsVm.uiState.value
    if (!tState.routesLoaded && !tState.isLoading) {
     topicsVm.loadRoutes()
    } else if (tState.routesLoaded) {
     topicsRoutesContainer?.let { containerLayout ->
      lastRenderedRoutesLoaded = tState.routesLoaded
      lastRenderedRoutes = tState.routes
      renderTopicsRoutesList(scaffold, containerLayout, tState) { route ->
       routeLessonVm.loadRouteDetail(route, force = true)
       navigationVm.navigateTo(NavDestination.RouteDetail(route))
      }
     }
    }
    scaffold.updateNotice(tState.notice)
   }

   NavDestination.Achievements -> {
    scaffold.mountPage("Mis logros")
    gamificationVm.loadAchievements()
    val state = gamificationVm.achievementsState.value
    lastRenderedAchievements = state.overview
    renderAchievementsBody(scaffold, state) { backToTopics() }
    scaffold.updateNotice(state.notice)
   }

   NavDestination.DailyGoal -> {
    scaffold.mountPage("Mi meta de hoy")
    gamificationVm.loadDailyGoal()
    val state = gamificationVm.dailyGoalState.value
    lastRenderedDailyGoal = state.dashboard
    renderDailyGoalBody(
     context = this,
     scaffold = scaffold,
     state = state,
     onBackToTopics = { backToTopics() },
     onRefresh = { gamificationVm.loadDailyGoal(force = true) },
     onSelectTarget = { target -> gamificationVm.updateDailyTarget(target) }
    )
    scaffold.updateNotice(state.notice)
   }

   NavDestination.DailyChallenge -> {
    scaffold.mountPage("Reto del día")
    gamificationVm.loadDailyChallenge()
    val state = gamificationVm.dailyChallengeState.value
    lastRenderedChallengeSignature = Triple(state.challenge, state.answers, state.completion)
    renderDailyChallengeBody(
     context = this,
     scaffold = scaffold,
     state = state,
     onBackToTopics = { backToTopics() },
     onSubmitAnswer = { activityVersionId, questionId, optionId ->
      gamificationVm.submitChallengeAnswer(activityVersionId, questionId, optionId)
     },
     onCompleteChallenge = { challengeId -> gamificationVm.completeChallenge(challengeId) }
    )
    scaffold.updateNotice(state.notice)
   }

   NavDestination.Notifications -> {
    scaffold.mountPage("Mi ritmo de estudio")
    notificationsVm.load()
    val state = notificationsVm.uiState.value
    lastRenderedNotifications = state.dashboard
    renderNotificationsBody(
     context = this,
     scaffold = scaffold,
     state = state,
     onBackToTopics = { backToTopics() },
     onRefresh = { notificationsVm.load(force = true) },
     onSavePreferences = { prefs ->
      val hasPerm = Build.VERSION.SDK_INT < 33 ||
       checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
      notificationsVm.savePreferences(prefs, hasSystemNotificationPermission = hasPerm)
     },
     onMarkAsRead = { id -> notificationsVm.markAsRead(id) }
    )
    scaffold.updateNotice(state.notice)
   }

   is NavDestination.RouteDetail -> {
    scaffold.mountPage(destination.route.title)
    routeLessonVm.loadRouteDetail(destination.route)
    val state = routeLessonVm.routeDetailState.value
    lastRenderedRouteDetail = state.detail
    renderRouteDetailBody(
     context = this,
     scaffold = scaffold,
     state = state,
     onBackToTopics = { backToTopics() },
     onOpenNode = { route, node ->
      routeLessonVm.openLesson(route, node, force = true)
      navigationVm.navigateTo(NavDestination.Lesson(route, node))
     },
     onBlockedNode = { routeLessonVm.showBlockedNodeNotice() }
    )
    scaffold.updateNotice(state.notice)
   }

   is NavDestination.Lesson -> {
    scaffold.mountPage(destination.node.title)
    routeLessonVm.openLesson(destination.route, destination.node)
    val state = routeLessonVm.lessonState.value
    lastRenderedLessonSignature = Triple(state.lesson, state.answers, state.completionResult)
    renderLessonBody(
     context = this,
     scaffold = scaffold,
     state = state,
     onBackToRoute = { route ->
      routeLessonVm.loadRouteDetail(route, force = false)
      navigationVm.popTo { it is NavDestination.RouteDetail }
     },
     onOpenExternalLink = { link -> openExternalLink(link) },
     onSubmitAnswer = { activityId, questionId, optionId ->
      routeLessonVm.submitLessonAnswer(destination.route, destination.node, activityId, questionId, optionId)
     },
     onCompleteNode = {
      routeLessonVm.completeLessonNode(destination.route, destination.node) { topicsVm.invalidateCache() }
     },
     onOpenNextNode = { route, next ->
      routeLessonVm.openLesson(route, next, force = true)
      navigationVm.replaceTop(NavDestination.Lesson(route, next))
     },
     onOpenAiPractice = { topic ->
      studyResourcesVm.loadAiPractice(topic, force = true)
      navigationVm.navigateTo(NavDestination.AiPractice(topic))
     },
     onOpenFlashcards = { topic ->
      studyResourcesVm.loadFlashcards(topic, force = true)
      navigationVm.navigateTo(NavDestination.Flashcards(topic))
     }
    )
    scaffold.updateNotice(state.notice)
   }

   is NavDestination.AiPractice -> {
    scaffold.mountPage(destination.topic.title)
    studyResourcesVm.loadAiPractice(destination.topic)
    val state = studyResourcesVm.aiPracticeState.value
    lastRenderedAiPractice = state.status
    renderAiPracticeBody(
     context = this,
     scaffold = scaffold,
     state = state,
     onBackToLesson = { navigationVm.navigateUp() },
     onRefresh = { topic -> studyResourcesVm.loadAiPractice(topic, force = true) },
     onOpenFlashcards = { topic ->
      studyResourcesVm.loadFlashcards(topic, force = true)
      navigationVm.navigateTo(NavDestination.Flashcards(topic))
     },
     onStartPractice = { topic -> studyResourcesVm.requestAiPractice(topic) },
     onSubmitAnswer = { topic, pId, qId, oId -> studyResourcesVm.answerAiQuestion(topic, pId, qId, oId) },
     onRequestHint = { pId, qId -> studyResourcesVm.requestAiQuestionHint(pId, qId) },
     onReportQuestion = { topic, pId, qId, reason -> studyResourcesVm.reportAiQuestion(topic, pId, qId, reason) }
    )
    scaffold.updateNotice(state.notice)
   }

   is NavDestination.Flashcards -> {
    scaffold.mountPage("Repasar con tarjetas")
    studyResourcesVm.loadFlashcards(destination.topic)
    val state = studyResourcesVm.flashcardsState.value
    lastRenderedFlashcards = state.deck
    renderFlashcardsBody(
     scaffold = scaffold,
     state = state,
     onBackToTopic = { navigationVm.navigateUp() },
     onRefresh = { topic -> studyResourcesVm.loadFlashcards(topic, force = true) },
     onAction = { topic, rev, action, cardId ->
      studyResourcesVm.sendFlashcardAction(topic, rev, action, cardId)
     }
    )
    scaffold.updateNotice(state.notice)
   }

   is NavDestination.VisualSummary -> {
    scaffold.mountPage("Ver resumen visual")
    studyResourcesVm.loadVisualSummary(destination.topic)
    val state = studyResourcesVm.visualSummaryState.value
    lastRenderedVisualSummary = state.lookup
    renderVisualSummaryBody(
     context = this,
     scaffold = scaffold,
     state = state,
     onBackToTopic = { navigationVm.navigateUp() },
     onRefresh = { topic -> studyResourcesVm.loadVisualSummary(topic, force = true) },
     onGenerate = { topic -> studyResourcesVm.generateVisualSummary(topic) },
     onSaveOffline = { topic, resId -> studyResourcesVm.downloadVisualSummaryForOffline(topic, resId) },
     onReport = { topic, resId, reason -> studyResourcesVm.reportVisualSummary(topic, resId, reason) }
    )
    scaffold.updateNotice(state.notice)
   }

   NavDestination.SavedSummaries -> {
    scaffold.mountPage("Resúmenes descargados")
    studyResourcesVm.loadSavedSummaries()
    val state = studyResourcesVm.savedSummariesState.value
    lastRenderedSavedSummaries = state.result
    renderSavedSummariesBody(
     scaffold = scaffold,
     state = state,
     onBack = { navigationVm.navigateUp() },
     onOpenSaved = { saved -> navigationVm.navigateTo(NavDestination.SavedSummaryDetail(saved)) }
    )
   }

   is NavDestination.SavedSummaryDetail -> {
    scaffold.mountPage("Copia descargada")
    renderSavedSummaryDetailBody(
     context = this,
     scaffold = scaffold,
     saved = destination.summary,
     isAuthenticated = authVm.sessionState.value is SessionState.Authenticated,
     onBackToSavedList = { navigationVm.navigateUp() },
     onVerifyOnline = { topic ->
      studyResourcesVm.loadVisualSummary(topic, force = true)
      navigationVm.navigateTo(NavDestination.VisualSummary(topic))
     }
    )
   }
  }
 }

 private fun backToTopics() {
  if (!navigationVm.popTo { it is NavDestination.Topics }) {
   navigationVm.resetTo(NavDestination.Topics)
  }
 }

 private fun clearAllFeatureViewModels() {
  topicsVm.clear()
  routeLessonVm.clear()
  gamificationVm.clear()
  notificationsVm.clear()
  studyResourcesVm.clear()
 }

 private fun openExternalLink(link: String) {
  try {
   startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
  } catch (_: ActivityNotFoundException) {
   scaffold.showLocalNotice("No hay una app para abrir este enlace en el teléfono.", NoticeTone.ERROR)
  }
 }

 private fun registerConnectivityObserver() {
  if (connectivityRegistered) return
  val cm = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
  cm.registerDefaultNetworkCallback(networkCallback)
  connectivityRegistered = true
 }

 private fun unregisterConnectivityObserver() {
  if (!connectivityRegistered) return
  val cm = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
  cm?.unregisterNetworkCallback(networkCallback)
  connectivityRegistered = false
 }

 @Suppress("DEPRECATION")
 private fun styleSystemBars() {
  window.statusBarColor = RupiUi.FOREST
  val decor = window.decorView
  if (Build.VERSION.SDK_INT >= 30) {
   decor.windowInsetsController?.setSystemBarsAppearance(
    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
   )
  } else {
   decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  }
 }
}
