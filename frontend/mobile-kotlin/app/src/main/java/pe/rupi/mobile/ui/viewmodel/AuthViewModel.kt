package pe.rupi.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.rupi.mobile.data.model.RupiSessionExpired
import pe.rupi.mobile.data.repository.AuthRepository
import pe.rupi.mobile.data.repository.SessionState
import pe.rupi.mobile.ui.state.LoginUiState
import pe.rupi.mobile.ui.state.NoticeMessage
import pe.rupi.mobile.ui.state.NoticeTone

class AuthViewModel(
 private val authRepository: AuthRepository
) : ViewModel() {

 val sessionState: StateFlow<SessionState> = authRepository.sessionState

 private val _uiState = MutableStateFlow(LoginUiState())
 val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

 fun restoreSession(): Boolean = authRepository.restoreInitialSession()

 fun showSessionNotice(message: String) {
  _uiState.value = _uiState.value.copy(
   isLoading = false,
   notice = NoticeMessage(text = message, tone = NoticeTone.INFO, scrollToTop = true)
  )
  authRepository.consumeUnauthenticatedNotice()
 }

 fun login(serverUrl: String, username: String, password: String, onSuccess: () -> Unit) {
  if (_uiState.value.isLoading) return
  _uiState.value = LoginUiState(isLoading = true, notice = loadingNotice())
  viewModelScope.launch {
   try {
    authRepository.login(serverUrl, username, password)
    _uiState.value = LoginUiState(isLoading = false, notice = null)
    onSuccess()
   } catch (e: CancellationException) {
    throw e
   } catch (e: Exception) {
    val rawMsg = e.message ?: ""
    val friendly = when {
     rawMsg.contains("failed to connect", ignoreCase = true) ||
      rawMsg.contains("ECONNREFUSED", ignoreCase = true) ||
      rawMsg.contains("ConnectException", ignoreCase = true) ->
      "No pudimos conectar con el servidor escolar en $serverUrl (puerto 8081). Verifica que el backend esté encendido (Iniciar-Rupi). Si usas celular físico por USB ejecuta 'adb reverse' o usa la IP de tu PC."
     rawMsg.contains("timeout", ignoreCase = true) ->
      "El servidor demoró demasiado en responder. Revisa la conexión de red de tu dispositivo."
     rawMsg.isNotBlank() -> rawMsg
     else -> "No pudimos iniciar sesión. Revisa tus credenciales o conexión."
    }
    _uiState.value = LoginUiState(
     isLoading = false,
     notice = errorNotice(friendly)
    )
   }
  }
 }

 fun logout(onLoggedOut: () -> Unit, onError: (String) -> Unit) {
  viewModelScope.launch {
   try {
    authRepository.logout()
    onLoggedOut()
   } catch (e: CancellationException) {
    throw e
   } catch (e: RupiSessionExpired) {
    authRepository.handleSessionExpired(e)
   } catch (e: Exception) {
    onError(e.message ?: "No pudimos cerrar sesión. Revisa tu conexión.")
   }
  }
 }
}
