package pe.rupi.mobile

import org.json.JSONObject
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID

/** Only talks to Spring Boot. No provider key or database credential belongs in the app. */
class RupiSessionExpired(message: String = "Vuelve a iniciar sesión para recibir avisos.", val otherDevice: Boolean = false): IllegalStateException(message)

/** HU-43: Rupi abre una sola sesión por cuenta. El servidor dice por qué se cerró esta y dónde se abrió la nueva. */
fun sessionEnded(reason: String?, newClient: String?): RupiSessionExpired = when (reason) {
 "OTRO_DISPOSITIVO" -> RupiSessionExpired(
  if (newClient == "MOVIL") "Tu cuenta se abrió en otro celular, así que aquí se cerró la sesión. Si fuiste tú, todo está bien; si no, avísale a tu profe."
  else "Tu cuenta se abrió en la web, así que aquí se cerró la sesión. Vuelve a ingresar si quieres seguir en este celular.", true)
 "EXPIRADA" -> RupiSessionExpired("Pasó mucho tiempo y tu sesión terminó. Vuelve a ingresar.")
 else -> RupiSessionExpired("Vuelve a iniciar sesión.")
}
class RupiApi(server: String) {
 private val base = URI(server.trim().trimEnd('/'))
 private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)
 val pending = mutableMapOf<String, String>()
 init {
  require(base.host != null && base.userInfo == null && base.query == null && base.fragment == null && base.path.isNullOrEmpty()) { "Usa la dirección base del servidor Rupi." }
  require(base.scheme == "https" || (BuildConfig.DEBUG && base.scheme == "http" && base.host in setOf("10.0.2.2", "127.0.0.1", "localhost"))) { "Usa HTTPS. En debug se permite el emulador local." }
 }
 fun request(path: String, method: String = "GET", body: JSONObject? = null): String = requestBytes(path,method,body).toString(Charsets.UTF_8)
 fun download(path: String): ByteArray = requestBytes(path,"GET",null)
 private fun requestBytes(path: String, method: String, body: JSONObject?): ByteArray {
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
   connection.setRequestProperty("X-Rupi-Cliente", "MOVIL")
   for ((name, values) in cookies.get(uri, emptyMap())) connection.setRequestProperty(name, values.joinToString("; "))
   if (body != null) {
    connection.doOutput = true
    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
   }
   val status = connection.responseCode
   if (status !in 200..299) {
    val text = connection.errorStream?.bufferedReader()?.use { it.readText().take(4000) }.orEmpty()
    val message = try { JSONObject(text).optString("detail") } catch (_: Exception) { "" }
    if (status == 401) { cookies.cookieStore.removeAll(); throw sessionEnded(connection.getHeaderField("X-Rupi-Sesion-Cerrada"), connection.getHeaderField("X-Rupi-Sesion-Nueva")) }
    throw IllegalStateException(message.ifBlank { "No pudimos consultar Rupi. Revisa tu conexión o vuelve a iniciar sesión." })
   }
   cookies.put(uri, connection.headerFields)
   val bytes=if (status == 204) byteArrayOf() else connection.inputStream.use { it.readBytes() }
   val expected=connection.getHeaderField("X-Rupi-Paquete-Sha256")
   if(expected!=null){val actual=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)};check(actual.equals(expected,true)){"La descarga no pasó la verificación. Vuelve a intentarlo."}}
   return bytes
  } finally { connection.disconnect() }
 }
 fun login(username: String, password: String) {
  val user = JSONObject(request("/api/v1/auth/login", "POST", JSONObject().put("username", username).put("password", password)))
  if (user.optString("role") != "ESTUDIANTE") { cookies.cookieStore.removeAll(); error("Este cliente de práctica está destinado a estudiantes.") }
 }
 fun requestId(node: String): String = pending.getOrPut(node) { UUID.randomUUID().toString() }
 fun exportSession():JSONObject {
  val cookie=cookies.cookieStore.cookies.firstOrNull { !it.hasExpired() && it.name=="rupi_session" }
   ?: error("Vuelve a iniciar sesión.")
  val lifetime=if(cookie.maxAge>0)minOf(cookie.maxAge,86400L) else 86400L
  return JSONObject().put("server",base.toString()).put("name",cookie.name).put("value",cookie.value)
   .put("expires",System.currentTimeMillis()+lifetime*1000L)
 }
 fun importSession(value:JSONObject) {
  val cookie=java.net.HttpCookie(value.getString("name"),value.getString("value"))
  cookie.version=0;cookie.path="/";cookie.secure=base.scheme=="https";cookie.isHttpOnly=true
  cookie.maxAge=maxOf(0,(value.getLong("expires")-System.currentTimeMillis())/1000)
  cookies.cookieStore.add(base,cookie)
 }
 fun clear() { cookies.cookieStore.removeAll(); pending.clear() }
}
