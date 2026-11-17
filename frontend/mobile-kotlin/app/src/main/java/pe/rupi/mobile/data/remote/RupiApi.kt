package pe.rupi.mobile.data.remote

import org.json.JSONObject
import pe.rupi.mobile.BuildConfig
import pe.rupi.mobile.data.model.sessionEnded
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * Cliente HTTP REST que conversa únicamente con Spring Boot.
 * Mantiene cookies de sesión en memoria, cabecera HU-43 (`X-Rupi-Cliente: MOVIL`),
 * validación HTTPS en release, límites de respuesta anti-DoS y comprobación SHA-256 en tiempo constante.
 */
class RupiApi(server: String) {
 private val base = URI(server.trim().trimEnd('/'))
 private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)
 val pending = mutableMapOf<String, String>()

 init {
  require(
   base.host != null &&
    base.userInfo == null &&
    base.query == null &&
    base.fragment == null &&
    base.path.isNullOrEmpty()
  ) { "Usa la dirección base del servidor Rupi." }
  require(
   base.scheme == "https" ||
    (BuildConfig.DEBUG && base.scheme == "http" && isLocalOrLanHost(base.host))
  ) { "Usa HTTPS. En debug se permite el emulador local o red LAN de desarrollo." }
 }

 companion object {
  const val MAX_JSON_RESPONSE_BYTES = 2 * 1024 * 1024
  const val MAX_DOWNLOAD_BYTES = 15 * 1024 * 1024
  private const val SESSION_COOKIE_NAME = "rupi_session"

  fun isLocalOrLanHost(host: String?): Boolean {
   if (host == null) return false
   if (host in setOf("10.0.2.2", "127.0.0.1", "localhost")) return true
   val parts = host.split('.')
   if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) {
    val b0 = parts[0].toInt()
    val b1 = parts[1].toInt()
    if (b0 == 192 && b1 == 168) return true
    if (b0 == 10) return true
    if (b0 == 172 && b1 in 16..31) return true
   }
   return false
  }

  /**
   * Valida que la ruta API sea relativa a `/api/v1/`, libre de path traversal (crudo o codificado),
   * caracteres de control CR/LF y que tras normalizar mantenga el mismo origen (`scheme`, `host`, `port`).
   */
  fun validateApiPath(baseUri: URI, path: String): URI {
   val lowered = path.lowercase(Locale.ROOT)
   require(
    path.startsWith("/api/v1/") &&
     !path.contains("..") &&
     !lowered.contains("%2e") &&
     !path.contains('\\') &&
     !lowered.contains("%5c") &&
     path.none { it == '\r' || it == '\n' || it.code < 0x20 || it.code == 0x7F }
   ) { "Ruta API no permitida." }
   val resolved = baseUri.resolve(path).normalize()
   require(
    resolved.scheme == baseUri.scheme &&
     resolved.host == baseUri.host &&
     resolved.port == baseUri.port &&
     resolved.userInfo == null &&
     (resolved.path ?: "").startsWith("/api/v1/")
   ) { "La ruta resuelta sale del origen autorizado de Rupi." }
   return resolved
  }

  /** Lee un flujo HTTP imponiendo un tope de memoria para prevenir ataques DoS por respuestas ilimitadas. */
  fun readBoundedBytes(input: InputStream, maxBytes: Int): ByteArray {
   val buffer = ByteArray(8192)
   val output = ByteArrayOutputStream()
   var total = 0
   while (true) {
    val read = input.read(buffer)
    if (read == -1) break
    total += read
    check(total <= maxBytes) { "La respuesta del servidor excede el tamaño máximo permitido." }
    output.write(buffer, 0, read)
   }
   return output.toByteArray()
  }

  /** Compara un digest SHA-256 en tiempo constante para mitigar ataques de temporización. */
  fun verifySha256ConstantTime(bytes: ByteArray, expectedHex: String): Boolean {
   val normalized = expectedHex.trim().lowercase(Locale.ROOT)
   if (normalized.length != 64 || normalized.any { it !in '0'..'9' && it !in 'a'..'f' }) return false
   val expectedBytes = ByteArray(32) { i ->
    normalized.substring(i * 2, i * 2 + 2).toInt(16).toByte()
   }
   val actualBytes = MessageDigest.getInstance("SHA-256").digest(bytes)
   return MessageDigest.isEqual(actualBytes, expectedBytes)
  }
 }

 fun request(path: String, method: String = "GET", body: JSONObject? = null): String =
  requestBytes(path, method, body, MAX_JSON_RESPONSE_BYTES).toString(Charsets.UTF_8)

 fun download(path: String): ByteArray = requestBytes(path, "GET", null, MAX_DOWNLOAD_BYTES)

 private fun requestBytes(path: String, method: String, body: JSONObject?, maxBytes: Int): ByteArray {
  val uri = validateApiPath(base, path)
  val connection = uri.toURL().openConnection() as HttpURLConnection
  try {
   connection.instanceFollowRedirects = false
   connection.connectTimeout = 5000
   connection.readTimeout = 45000
   connection.requestMethod = method
   connection.setRequestProperty("Content-Type", "application/json")
   connection.setRequestProperty("Origin", base.toString())
   connection.setRequestProperty("X-Rupi-Cliente", "MOVIL")
   connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")
   for ((name, values) in cookies.get(uri, emptyMap())) {
    connection.setRequestProperty(name, values.joinToString("; "))
   }
   if (body != null) {
    connection.doOutput = true
    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
   }
   val status = connection.responseCode
   if (status !in 200..299) {
    val text = connection.errorStream?.bufferedReader()?.use { it.readText().take(4000) }.orEmpty()
    val message = try {
     val json = JSONObject(text)
     json.optString("detail").ifBlank {
      json.optString("message").ifBlank {
       json.optString("error")
      }
     }
    } catch (_: Exception) {
     ""
    }
    if (status == 401) {
     cookies.cookieStore.removeAll()
     throw sessionEnded(
      connection.getHeaderField("X-Rupi-Sesion-Cerrada"),
      connection.getHeaderField("X-Rupi-Sesion-Nueva")
     )
    }
    throw IllegalStateException(message.ifBlank { "No pudimos consultar Rupi. Revisa tu conexión o vuelve a iniciar sesión." })
   }
   cookies.put(uri, connection.headerFields)
   val bytes = if (status == 204) byteArrayOf() else connection.inputStream.use { readBoundedBytes(it, maxBytes) }
   val expected = connection.getHeaderField("X-Rupi-Paquete-Sha256")
   if (expected != null) {
    check(verifySha256ConstantTime(bytes, expected)) {
     "La descarga no pasó la verificación de integridad. Vuelve a intentarlo."
    }
   }
   return bytes
  } finally {
   connection.disconnect()
  }
 }

 fun login(username: String, password: String) {
  val user = JSONObject(
   request(
    "/api/v1/auth/login",
    "POST",
    JSONObject().put("username", username).put("password", password)
   )
  )
  if (user.optString("role") != "ESTUDIANTE") {
   cookies.cookieStore.removeAll()
   error("Este cliente de práctica está destinado a estudiantes.")
  }
 }

 fun requestId(node: String): String = pending.getOrPut(node) { UUID.randomUUID().toString() }

 /**
  * Enlace para abrir fuera de la app (video o material del docente): los relativos van al mismo servidor.
  * Exige HTTPS en producción (HTTP solo en debug local/LAN) y bloquea `userInfo` contra phishing.
  */
 fun resource(url: String): String? {
  val raw = url.trim()
  if (raw.isEmpty() || raw.any { it.code < 0x20 || it.code == 0x7F }) return null
  val target = try {
   base.resolve(raw).normalize()
  } catch (_: Exception) {
   return null
  }
  val scheme = target.scheme?.lowercase(Locale.ROOT) ?: return null
  val host = target.host?.takeIf { it.isNotBlank() } ?: return null
  if (target.userInfo != null) return null
  val allowed = scheme == "https" || (BuildConfig.DEBUG && scheme == "http" && isLocalOrLanHost(host))
  return if (allowed) target.toString() else null
 }

 fun exportSession(): JSONObject {
  val cookie = cookies.cookieStore.cookies.firstOrNull { !it.hasExpired() && it.name == SESSION_COOKIE_NAME }
   ?: error("Vuelve a iniciar sesión.")
  val lifetime = if (cookie.maxAge > 0) minOf(cookie.maxAge, 86400L) else 86400L
  return JSONObject()
   .put("server", base.toString())
   .put("name", cookie.name)
   .put("value", cookie.value)
   .put("expires", System.currentTimeMillis() + lifetime * 1000L)
 }

 fun importSession(value: JSONObject) {
  val name = value.getString("name")
  val cookieValue = value.getString("value")
  require(name == SESSION_COOKIE_NAME && cookieValue.isNotBlank()) { "Sesión inválida." }
  val cookie = HttpCookie(name, cookieValue)
  cookie.version = 0
  cookie.domain = base.host
  cookie.path = "/"
  cookie.secure = base.scheme == "https"
  cookie.isHttpOnly = true
  cookie.maxAge = maxOf(0, (value.getLong("expires") - System.currentTimeMillis()) / 1000)
  cookies.cookieStore.add(base, cookie)
 }

 fun clear() {
  cookies.cookieStore.removeAll()
  pending.clear()
 }
}
