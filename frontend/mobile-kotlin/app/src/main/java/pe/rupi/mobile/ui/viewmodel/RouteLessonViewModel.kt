package pe.rupi.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.rupi.mobile.data.model.LearningRouteSummary
import pe.rupi.mobile.data.model.NodeCompletionResult
import pe.rupi.mobile.data.model.RouteNode
import pe.rupi.mobile.data.model.RouteNodeState
import pe.rupi.mobile.data.repository.AuthRepository
import pe.rupi.mobile.data.repository.LearningRepository
import pe.rupi.mobile.ui.state.LessonUiState
import pe.rupi.mobile.ui.state.NoticeMessage
import pe.rupi.mobile.ui.state.NoticeTone
import pe.rupi.mobile.ui.state.RouteDetailUiState

class RouteLessonViewModel(
 private val learningRepository: LearningRepository,
 private val authRepository: AuthRepository
) : ViewModel() {

 private val _routeDetailState = MutableStateFlow(RouteDetailUiState())
 val routeDetailState: StateFlow<RouteDetailUiState> = _routeDetailState.asStateFlow()

 private val _lessonState = MutableStateFlow(LessonUiState())
 val lessonState: StateFlow<LessonUiState> = _lessonState.asStateFlow()

 fun loadRouteDetail(
  route: LearningRouteSummary,
  welcomeNotice: String? = null,
  force: Boolean = false
 ) {
  val current = _routeDetailState.value
  if (current.isLoading) return
  if (!force && current.route?.versionRouteId == route.versionRouteId && current.detail != null && welcomeNotice == null) {
   return
  }
  _routeDetailState.value = RouteDetailUiState(
   isLoading = true,
   route = route,
   detail = if (current.route?.versionRouteId == route.versionRouteId) current.detail else null,
   notice = loadingNotice()
  )
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg ->
    _routeDetailState.value = _routeDetailState.value.copy(isLoading = false, notice = errorNotice(msg))
   }
  ) {
   val detail = learningRepository.getRouteDetail(route.versionRouteId)
   _routeDetailState.value = RouteDetailUiState(
    isLoading = false,
    route = route,
    detail = detail,
    notice = welcomeNotice?.let { NoticeMessage(it, NoticeTone.OK, scrollToTop = true) }
   )
  }
 }

 fun showBlockedNodeNotice() {
  _routeDetailState.value = _routeDetailState.value.copy(
   notice = NoticeMessage("Esta parada se abre cuando completes la anterior. ¡Tú puedes!", NoticeTone.INFO, scrollToTop = true)
  )
 }

 fun openLesson(
  route: LearningRouteSummary,
  node: RouteNode,
  completionResult: NodeCompletionResult? = null,
  force: Boolean = false
 ) {
  val current = _lessonState.value
  if (current.isLoading) return
  val sameNode = current.node?.id == node.id && current.route?.versionRouteId == route.versionRouteId
  if (!force && sameNode && current.lesson != null && completionResult == null) {
   return
  }
  val preservedAnswers = if (sameNode) current.answers else emptyMap()
  _lessonState.value = LessonUiState(
   isLoading = true,
   route = route,
   node = node,
   lesson = if (sameNode) current.lesson else null,
   answers = preservedAnswers,
   completionResult = completionResult,
   notice = loadingNotice()
  )
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg ->
    _lessonState.value = _lessonState.value.copy(isLoading = false, notice = errorNotice(msg))
   }
  ) {
   val lesson = learningRepository.startLesson(route.versionRouteId, node.id)
   _lessonState.value = LessonUiState(
    isLoading = false,
    route = route,
    node = node,
    lesson = lesson,
    answers = preservedAnswers,
    completionResult = completionResult,
    notice = null
   )
  }
 }

 fun submitLessonAnswer(
  route: LearningRouteSummary,
  node: RouteNode,
  activityId: String,
  questionId: String,
  selectedOptionId: String
 ) {
  val current = _lessonState.value
  if (current.isLoading) return
  _lessonState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg ->
    _lessonState.value = _lessonState.value.copy(isLoading = false, notice = errorNotice(msg))
   }
  ) {
   val feedback = learningRepository.answerLessonQuestion(activityId, questionId, selectedOptionId)
   val updatedAnswers = _lessonState.value.answers + (questionId to feedback)
   val refreshedLesson = learningRepository.startLesson(route.versionRouteId, node.id)
   _lessonState.value = _lessonState.value.copy(
    isLoading = false,
    lesson = refreshedLesson,
    answers = updatedAnswers,
    notice = null
   )
  }
 }

 fun completeLessonNode(
  route: LearningRouteSummary,
  node: RouteNode,
  onCompleted: () -> Unit
 ) {
  val current = _lessonState.value
  if (current.isLoading) return
  _lessonState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg ->
    _lessonState.value = _lessonState.value.copy(isLoading = false, notice = errorNotice(msg))
   }
  ) {
   val result = learningRepository.completeNode(route.versionRouteId, node.id)
   val completedNode = node.copy(state = RouteNodeState.COMPLETADO)
   val refreshedLesson = learningRepository.startLesson(route.versionRouteId, node.id)
   _routeDetailState.value = _routeDetailState.value.copy(detail = null)
   _lessonState.value = _lessonState.value.copy(
    isLoading = false,
    node = completedNode,
    lesson = refreshedLesson,
    completionResult = result,
    notice = null
   )
   onCompleted()
  }
 }

 fun clear() {
  _routeDetailState.value = RouteDetailUiState()
  _lessonState.value = LessonUiState()
 }
}
