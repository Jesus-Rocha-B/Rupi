package pe.rupi.mobile

import org.json.JSONObject
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID

/** Only talks to Spring Boot. No provider key or database credential belongs in the app. */
class RupiApi(server: String) {
 private val base = URI(server.trim().trimEnd('/'))
 private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)
 val pending = mutableMapOf<String, String>()
 init {
  require(base.host != null && base.userInfo == null && base.query == null && base.fragment == null && base.path.isNullOrEmpty()) { "Usa la dirección base del servidor Rupi." }
  require(base.scheme == "https" || (BuildConfig.DEBUG && base.scheme == "http" && base.host in setOf("10.0.2.2", "127.0.0.1", "localhost"))) { "Usa HTTPS. En debug se permite el emulador local." }
 }
 fun request(path: String, method: String = "GET", body: JSONObject? = null): String {
  require(path.startsWith("/api/v1/") && !path.contains(".."))
  val uri = base.resolve(path)
  val connection = uri.toURL().openConnection() as HttpURLConnection
  try {
   connection.instanceFollowRedirects = false
   connection.connectTimeout = 5000
   connection.readTimeout = 45000
   connection.requestMethod = method
   connection.setRequestProperty("Content-Type", "application/json")
   connection.setRequestProperty("Origin", base.toString())
   for ((name, values) in cookies.get(uri, emptyMap())) connection.setRequestProperty(name, values.joinToString("; "))
   if (body != null) {
    connection.doOutput = true
    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
   }
   val status = connection.responseCode
   if (status !in 200..299) {
    val text = connection.errorStream?.bufferedReader()?.use { it.readText().take(4000) }.orEmpty()
    val message = try { JSONObject(text).optString("detail") } catch (_: Exception) { "" }
    if (status == 401) cookies.cookieStore.removeAll()
    throw IllegalStateException(message.ifBlank { "No pudimos consultar Rupi. Revisa tu conexión o vuelve a iniciar sesión." })
   }
   cookies.put(uri, connection.headerFields)
   return if (status == 204) "" else connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
  } finally { connection.disconnect() }
 }
 fun login(username: String, password: String) {
  val user = JSONObject(request("/api/v1/auth/login", "POST", JSONObject().put("username", username).put("password", password)))
  if (user.optString("role") != "ESTUDIANTE") { cookies.cookieStore.removeAll(); error("Este cliente de práctica está destinado a estudiantes.") }
 }
 fun requestId(node: String): String = pending.getOrPut(node) { UUID.randomUUID().toString() }
 fun clear() { cookies.cookieStore.removeAll(); pending.clear() }
}
