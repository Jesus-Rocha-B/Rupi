package pe.rupi.mobile

import pe.rupi.mobile.data.remote.RupiApi
import java.net.URI
import java.util.Locale

/**
 * Política de seguridad para la apertura de enlaces externos mediante Intents (OWASP MASVS-PLATFORM).
 * Bloquea esquemas peligrosos (`intent://`, `file://`, `content://`, `javascript:`, `tel:`, `sms:`)
 * y enlaces con `userInfo` que podrían usarse para suplantación de identidad.
 */
object ExternalLinkPolicy {
 fun sanitizeExternalUrl(rawUrl: String, allowDebugLocalHttp: Boolean = BuildConfig.DEBUG): String? {
  val trimmed = rawUrl.trim()
  if (trimmed.isEmpty() || trimmed.any { it.code < 0x20 || it.code == 0x7F }) return null
  val uri = try {
   URI(trimmed).normalize()
  } catch (_: Exception) {
   return null
  }
  val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
  val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
  if (uri.userInfo != null) return null
  val allowed = when (scheme) {
   "https" -> true
   "http" -> allowDebugLocalHttp && RupiApi.isLocalOrLanHost(host)
   else -> false
  }
  return if (allowed) uri.toString() else null
 }
}
