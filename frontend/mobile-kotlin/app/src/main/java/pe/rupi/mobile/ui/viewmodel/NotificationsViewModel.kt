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
import pe.rupi.mobile.data.model.StudyPreferences
import pe.rupi.mobile.data.repository.AuthRepository
import pe.rupi.mobile.data.repository.NotificationRepository
import pe.rupi.mobile.ui.state.NoticeMessage
import pe.rupi.mobile.ui.state.NoticeTone
import pe.rupi.mobile.ui.state.NotificationEffect
import pe.rupi.mobile.ui.state.NotificationsUiState

class NotificationsViewModel(
 private val notificationRepository: NotificationRepository,
 private val authRepository: AuthRepository
) : ViewModel() {

 private val _uiState = MutableStateFlow(NotificationsUiState())
 val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

 private val _effects = MutableSharedFlow<NotificationEffect>(extraBufferCapacity = 1)
 val effects: SharedFlow<NotificationEffect> = _effects.asSharedFlow()

 fun load(force: Boolean = false) {
  val current = _uiState.value
  if (current.isLoading || (!force && current.dashboard != null)) return
  _uiState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _uiState.value = _uiState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val dashboard = notificationRepository.getDashboard()
   _uiState.value = NotificationsUiState(isLoading = false, dashboard = dashboard, notice = null)
  }
 }

 fun savePreferences(draft: StudyPreferences, hasSystemNotificationPermission: Boolean) {
  val current = _uiState.value
  if (current.isLoading) return
  if (draft.system && !draft.goals && !draft.evaluations) {
   _uiState.value = current.copy(
    notice = NoticeMessage(
     "Para recibir avisos en el teléfono, marca también qué avisos quieres: tu meta y racha, o los de tu docente.",
     NoticeTone.INFO,
     scrollToTop = true
    )
   )
   return
  }
  _uiState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _uiState.value = _uiState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val saved = notificationRepository.savePreferences(draft)
   val currentDashboard = _uiState.value.dashboard
   val updatedDashboard = currentDashboard?.copy(
    status = currentDashboard.status.copy(preferences = saved)
   )
   if (saved.system) {
    if (!hasSystemNotificationPermission) {
     _uiState.value = _uiState.value.copy(isLoading = false, dashboard = updatedDashboard, notice = null)
     viewModelScope.launch { _effects.emit(NotificationEffect.RequestSystemPermission) }
    } else {
     activateAlertsLocked(updatedDashboard)
    }
   } else {
    notificationRepository.disableDeviceAlerts()
    _uiState.value = _uiState.value.copy(
     isLoading = false,
     dashboard = updatedDashboard,
     notice = NoticeMessage(
      "Preferencias guardadas. Avisos del dispositivo desactivados.",
      NoticeTone.OK,
      scrollToTop = true
     )
    )
   }
  }
 }

 fun onSystemPermissionResult(granted: Boolean) {
  if (!granted) {
   _uiState.value = _uiState.value.copy(
    notice = NoticeMessage(
     "Puedes consultar tus avisos aquí aunque no concedas el permiso de Android.",
     NoticeTone.INFO,
     scrollToTop = true
    )
   )
   return
  }
  val current = _uiState.value
  if (current.isLoading) return
  _uiState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _uiState.value = _uiState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   activateAlertsLocked(_uiState.value.dashboard)
  }
 }

 private suspend fun activateAlertsLocked(dashboard: pe.rupi.mobile.data.model.NotificationsDashboard?) {
  if (!notificationRepository.areDeviceNotificationsAllowed()) {
   _uiState.value = _uiState.value.copy(
    isLoading = false,
    dashboard = dashboard,
    notice = NoticeMessage(
     "Preferencias guardadas. Permite las notificaciones en los ajustes de Android para recibir avisos.",
     NoticeTone.INFO,
     scrollToTop = true
    )
   )
   return
  }
  notificationRepository.enableDeviceAlerts()
  _uiState.value = _uiState.value.copy(
   isLoading = false,
   dashboard = dashboard,
   notice = NoticeMessage(
    "Preferencias guardadas. Avisos activados en este dispositivo.",
    NoticeTone.OK,
    scrollToTop = true
   )
  )
 }

 fun markAsRead(notificationId: String) {
  val current = _uiState.value
  if (current.isLoading) return
  _uiState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _uiState.value = _uiState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   notificationRepository.markAsRead(notificationId)
   val dashboard = notificationRepository.getDashboard()
   _uiState.value = NotificationsUiState(isLoading = false, dashboard = dashboard, notice = null)
  }
 }

 fun clear() {
  _uiState.value = NotificationsUiState()
 }
}
