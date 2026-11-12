package pe.rupi.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pe.rupi.mobile.data.model.RupiSessionExpired
import pe.rupi.mobile.data.repository.AuthRepository
import pe.rupi.mobile.ui.state.NoticeMessage
import pe.rupi.mobile.ui.state.NoticeTone

internal const val LOADING_NOTICE_TEXT = "Un momento, estamos consultando Rupi…"

internal inline fun ViewModel.launchSessionAware(
 authRepository: AuthRepository,
 crossinline onError: (String) -> Unit,
 crossinline block: suspend () -> Unit
) {
 viewModelScope.launch {
  try {
   block()
  } catch (e: CancellationException) {
   throw e
  } catch (e: RupiSessionExpired) {
   authRepository.handleSessionExpired(e)
  } catch (e: Exception) {
   onError(e.message ?: "No pudimos continuar. Revisa tu conexión.")
  }
 }
}

internal fun loadingNotice(): NoticeMessage =
 NoticeMessage(text = LOADING_NOTICE_TEXT, tone = NoticeTone.INFO, scrollToTop = false)

internal fun errorNotice(message: String): NoticeMessage =
 NoticeMessage(text = message, tone = NoticeTone.ERROR, scrollToTop = true)
