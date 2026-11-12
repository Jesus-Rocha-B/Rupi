package pe.rupi.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.rupi.mobile.data.model.QuestionHint
import pe.rupi.mobile.data.model.StudyTopic
import pe.rupi.mobile.data.repository.AuthRepository
import pe.rupi.mobile.data.repository.StudyResourcesRepository
import pe.rupi.mobile.ui.state.AiPracticeUiState
import pe.rupi.mobile.ui.state.FlashcardsUiState
import pe.rupi.mobile.ui.state.NoticeMessage
import pe.rupi.mobile.ui.state.NoticeTone
import pe.rupi.mobile.ui.state.SavedSummariesUiState
import pe.rupi.mobile.ui.state.VisualSummaryUiState

class StudyResourcesViewModel(
 private val studyResourcesRepository: StudyResourcesRepository,
 private val authRepository: AuthRepository
) : ViewModel() {

 private val _aiPracticeState = MutableStateFlow(AiPracticeUiState())
 val aiPracticeState: StateFlow<AiPracticeUiState> = _aiPracticeState.asStateFlow()

 private val _hintEffect = MutableSharedFlow<QuestionHint>(extraBufferCapacity = 1)
 val hintEffect: SharedFlow<QuestionHint> = _hintEffect.asSharedFlow()

 private val _flashcardsState = MutableStateFlow(FlashcardsUiState())
 val flashcardsState: StateFlow<FlashcardsUiState> = _flashcardsState.asStateFlow()

 private val _visualSummaryState = MutableStateFlow(VisualSummaryUiState())
 val visualSummaryState: StateFlow<VisualSummaryUiState> = _visualSummaryState.asStateFlow()

 private val _savedSummariesState = MutableStateFlow(SavedSummariesUiState())
 val savedSummariesState: StateFlow<SavedSummariesUiState> = _savedSummariesState.asStateFlow()

 // --- Práctica IA y Pistas (HU-21, HU-37, HU-38) ---

 fun loadAiPractice(topic: StudyTopic, force: Boolean = false) {
  val current = _aiPracticeState.value
  if (current.isLoading) return
  if (!force && sameTopic(current.topic, topic) && current.status != null) return
  _aiPracticeState.value = AiPracticeUiState(
   isLoading = true,
   topic = topic,
   status = if (sameTopic(current.topic, topic)) current.status else null,
   notice = loadingNotice()
  )
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _aiPracticeState.value = _aiPracticeState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val status = studyResourcesRepository.getAiPractice(topic.routeId, topic.nodeId)
   _aiPracticeState.value = AiPracticeUiState(isLoading = false, topic = topic, status = status, notice = null)
  }
 }

 fun requestAiPractice(topic: StudyTopic) {
  val current = _aiPracticeState.value
  if (current.isLoading) return
  _aiPracticeState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _aiPracticeState.value = _aiPracticeState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   studyResourcesRepository.requestAiPractice(topic.routeId, topic.nodeId)
   val status = studyResourcesRepository.getAiPractice(topic.routeId, topic.nodeId)
   _aiPracticeState.value = AiPracticeUiState(isLoading = false, topic = topic, status = status, notice = null)
  }
 }

 fun answerAiQuestion(topic: StudyTopic, practiceId: String, questionId: String, optionId: String) {
  val current = _aiPracticeState.value
  if (current.isLoading) return
  _aiPracticeState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _aiPracticeState.value = _aiPracticeState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   studyResourcesRepository.answerAiQuestion(practiceId, questionId, optionId)
   val status = studyResourcesRepository.getAiPractice(topic.routeId, topic.nodeId)
   _aiPracticeState.value = AiPracticeUiState(isLoading = false, topic = topic, status = status, notice = null)
  }
 }

 fun reportAiQuestion(topic: StudyTopic, practiceId: String, questionId: String, reason: String) {
  if (reason.isBlank() || _aiPracticeState.value.isLoading) return
  _aiPracticeState.value = _aiPracticeState.value.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _aiPracticeState.value = _aiPracticeState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   studyResourcesRepository.reportAiQuestion(practiceId, questionId, reason)
   val status = studyResourcesRepository.getAiPractice(topic.routeId, topic.nodeId)
   _aiPracticeState.value = AiPracticeUiState(isLoading = false, topic = topic, status = status, notice = null)
  }
 }

 fun requestAiQuestionHint(practiceId: String, questionId: String) {
  val current = _aiPracticeState.value
  if (current.isLoading) return
  _aiPracticeState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _aiPracticeState.value = _aiPracticeState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val hint = studyResourcesRepository.getAiQuestionHint(practiceId, questionId)
   _aiPracticeState.value = _aiPracticeState.value.copy(isLoading = false, notice = null)
   viewModelScope.launch { _hintEffect.emit(hint) }
  }
 }

 // --- Tarjetas de memoria (HU-22) ---

 fun loadFlashcards(topic: StudyTopic, force: Boolean = false) {
  val current = _flashcardsState.value
  if (current.isLoading) return
  if (!force && sameTopic(current.topic, topic) && current.deck != null) return
  _flashcardsState.value = FlashcardsUiState(
   isLoading = true,
   topic = topic,
   deck = if (sameTopic(current.topic, topic)) current.deck else null,
   notice = loadingNotice()
  )
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _flashcardsState.value = _flashcardsState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val deck = studyResourcesRepository.getFlashcards(topic.routeId, topic.nodeId)
   _flashcardsState.value = FlashcardsUiState(isLoading = false, topic = topic, deck = deck, notice = null)
  }
 }

 fun sendFlashcardAction(topic: StudyTopic, revision: Long, action: String, cardId: String) {
  val current = _flashcardsState.value
  if (current.isLoading) return
  _flashcardsState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _flashcardsState.value = _flashcardsState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   studyResourcesRepository.performFlashcardAction(topic.routeId, topic.nodeId, revision, action, cardId)
   val deck = studyResourcesRepository.getFlashcards(topic.routeId, topic.nodeId)
   _flashcardsState.value = FlashcardsUiState(isLoading = false, topic = topic, deck = deck, notice = null)
  }
 }

 // --- Resúmenes visuales en línea y sin conexión (HU-23) ---

 fun loadVisualSummary(topic: StudyTopic, force: Boolean = false) {
  val current = _visualSummaryState.value
  if (current.isLoading) return
  if (!force && sameTopic(current.topic, topic) && current.lookup != null) return
  _visualSummaryState.value = VisualSummaryUiState(
   isLoading = true,
   topic = topic,
   lookup = if (sameTopic(current.topic, topic)) current.lookup else null,
   notice = loadingNotice()
  )
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _visualSummaryState.value = _visualSummaryState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val lookup = studyResourcesRepository.loadVisualSummary(topic)
   _visualSummaryState.value = VisualSummaryUiState(isLoading = false, topic = topic, lookup = lookup, notice = null)
  }
 }

 fun refreshVisualSummaryOnReconnect(topic: StudyTopic) {
  if (!_visualSummaryState.value.isLoading) {
   loadVisualSummary(topic, force = true)
  }
 }

 fun generateVisualSummary(topic: StudyTopic) {
  val current = _visualSummaryState.value
  if (current.isLoading) return
  _visualSummaryState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _visualSummaryState.value = _visualSummaryState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   studyResourcesRepository.generateVisualSummary(topic.routeId, topic.nodeId)
   val lookup = studyResourcesRepository.loadVisualSummary(topic)
   _visualSummaryState.value = VisualSummaryUiState(isLoading = false, topic = topic, lookup = lookup, notice = null)
  }
 }

 fun downloadVisualSummaryForOffline(topic: StudyTopic, resourceId: String) {
  val current = _visualSummaryState.value
  if (current.isLoading) return
  _visualSummaryState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _visualSummaryState.value = _visualSummaryState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   studyResourcesRepository.downloadAndSaveVisualSummary(topic, resourceId)
   _visualSummaryState.value = _visualSummaryState.value.copy(
    isLoading = false,
    notice = NoticeMessage(
     "Resumen guardado. Puedes abrirlo desde Resúmenes descargados incluso sin iniciar sesión.",
     NoticeTone.OK,
     scrollToTop = true
    )
   )
  }
 }

 fun reportVisualSummary(topic: StudyTopic, resourceId: String, reason: String) {
  if (reason.isBlank() || _visualSummaryState.value.isLoading) return
  _visualSummaryState.value = _visualSummaryState.value.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _visualSummaryState.value = _visualSummaryState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   studyResourcesRepository.reportVisualSummary(topic.routeId, topic.nodeId, resourceId, reason)
   val lookup = studyResourcesRepository.loadVisualSummary(topic)
   _visualSummaryState.value = VisualSummaryUiState(isLoading = false, topic = topic, lookup = lookup, notice = null)
  }
 }

 fun loadSavedSummaries() {
  if (_savedSummariesState.value.isLoading) return
  _savedSummariesState.value = _savedSummariesState.value.copy(isLoading = true)
  viewModelScope.launch {
   val result = studyResourcesRepository.listSavedSummaries()
   _savedSummariesState.value = SavedSummariesUiState(isLoading = false, result = result, notice = null)
  }
 }

 fun showTtsMissingNoticeOnCurrentScreen() {
  val message = NoticeMessage(
   "Para escuchar, instala o activa una voz en español en los ajustes de Android (Texto a voz).",
   NoticeTone.INFO,
   scrollToTop = true
  )
  _aiPracticeState.value = _aiPracticeState.value.copy(notice = message)
  _flashcardsState.value = _flashcardsState.value.copy(notice = message)
  _visualSummaryState.value = _visualSummaryState.value.copy(notice = message)
  _savedSummariesState.value = _savedSummariesState.value.copy(notice = message)
 }

 fun clear() {
  _aiPracticeState.value = AiPracticeUiState()
  _flashcardsState.value = FlashcardsUiState()
  _visualSummaryState.value = VisualSummaryUiState()
 }

 private fun sameTopic(a: StudyTopic?, b: StudyTopic): Boolean =
  a != null && a.routeId == b.routeId && a.nodeId == b.nodeId
}
