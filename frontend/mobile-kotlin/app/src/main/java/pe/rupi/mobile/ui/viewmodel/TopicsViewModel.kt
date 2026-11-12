package pe.rupi.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.rupi.mobile.data.model.JoinRouteResult
import pe.rupi.mobile.data.repository.AuthRepository
import pe.rupi.mobile.data.repository.LearningRepository
import pe.rupi.mobile.ui.state.TopicsUiState

class TopicsViewModel(
 private val learningRepository: LearningRepository,
 private val authRepository: AuthRepository
) : ViewModel() {

 private val _uiState = MutableStateFlow(TopicsUiState())
 val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

 fun loadRoutes(force: Boolean = false) {
  val current = _uiState.value
  if (current.isLoading || (!force && current.routesLoaded)) return
  _uiState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _uiState.value = _uiState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val items = learningRepository.getLearningRoutes()
   _uiState.value = TopicsUiState(isLoading = false, routes = items, routesLoaded = true, notice = null)
  }
 }

 fun joinByCode(rawCode: String, onJoined: (JoinRouteResult) -> Unit) {
  if (_uiState.value.isLoading) return
  val trimmed = rawCode.trim().uppercase()
  if (trimmed.isEmpty()) {
   _uiState.value = _uiState.value.copy(notice = errorNotice("Escribe el código que te dio tu profe."))
   return
  }
  _uiState.value = _uiState.value.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _uiState.value = _uiState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val joined = learningRepository.joinRouteByCode(trimmed)
   _uiState.value = _uiState.value.copy(isLoading = false, routesLoaded = false, notice = null)
   onJoined(joined)
  }
 }

 fun invalidateCache() {
  _uiState.value = _uiState.value.copy(routesLoaded = false)
 }

 fun showError(message: String) {
  _uiState.value = _uiState.value.copy(isLoading = false, notice = errorNotice(message))
 }

 fun clear() {
  _uiState.value = TopicsUiState()
 }
}
