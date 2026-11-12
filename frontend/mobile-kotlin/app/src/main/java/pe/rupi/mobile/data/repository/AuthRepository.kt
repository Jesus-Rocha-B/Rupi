package pe.rupi.mobile.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.rupi.mobile.data.local.SessionAlertsDataSource
import pe.rupi.mobile.data.model.RupiSessionExpired
import pe.rupi.mobile.data.remote.RupiRemoteDataSource
import pe.rupi.mobile.data.remote.SessionClientHolder

sealed interface SessionState {
 data class Unauthenticated(val notice: String? = null) : SessionState
 data object Authenticated : SessionState
}

/**
 * Repositorio responsable de la autenticación, sesión activa en memoria y cierre por sesión única (HU-43).
 */
interface AuthRepository {
 val sessionState: StateFlow<SessionState>
 fun restoreInitialSession(): Boolean
 suspend fun login(serverUrl: String, username: String, password: String)
 suspend fun logout()
 suspend fun handleSessionExpired(error: RupiSessionExpired)
 fun consumeUnauthenticatedNotice()
}

class DefaultAuthRepository(
 private val remote: RupiRemoteDataSource,
 private val sessionHolder: SessionClientHolder,
 private val alertsDataSource: SessionAlertsDataSource
) : AuthRepository {

 private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated())
 override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

 override fun restoreInitialSession(): Boolean {
  if (sessionHolder.get() == null) {
   val restored = alertsDataSource.restoreSessionClient()
   if (restored != null) {
    sessionHolder.set(restored)
   }
  }
  val authenticated = sessionHolder.get() != null
  _sessionState.value = if (authenticated) {
   SessionState.Authenticated
  } else {
   SessionState.Unauthenticated()
  }
  return authenticated
 }

 override suspend fun login(serverUrl: String, username: String, password: String) {
  remote.login(serverUrl, username, password)
  alertsDataSource.disableDeviceAlerts()
  _sessionState.value = SessionState.Authenticated
 }

 override suspend fun logout() {
  alertsDataSource.disableDeviceAlerts()
  remote.logout()
  sessionHolder.clear()
  _sessionState.value = SessionState.Unauthenticated()
 }

 override suspend fun handleSessionExpired(error: RupiSessionExpired) {
  alertsDataSource.disableDeviceAlerts()
  sessionHolder.clear()
  _sessionState.value = SessionState.Unauthenticated(
   notice = error.message ?: "Vuelve a iniciar sesión."
  )
 }

 override fun consumeUnauthenticatedNotice() {
  val current = _sessionState.value
  if (current is SessionState.Unauthenticated && current.notice != null) {
   _sessionState.value = SessionState.Unauthenticated(notice = null)
  }
 }
}
