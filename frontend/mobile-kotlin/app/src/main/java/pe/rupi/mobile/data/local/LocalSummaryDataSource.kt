package pe.rupi.mobile.data.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pe.rupi.mobile.data.model.SavedVisualSummary
import pe.rupi.mobile.data.model.StudyTopic
import pe.rupi.mobile.data.remote.RupiApi
import pe.rupi.mobile.data.remote.RupiJsonMapper
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipInputStream

data class SavedSummariesReadResult(
 val items: List<SavedVisualSummary>,
 val unreadableCount: Int
)

sealed interface SavedCopyCheck {
 data object Missing : SavedCopyCheck
 data object Invalid : SavedCopyCheck
 data class Valid(val version: String) : SavedCopyCheck
}

/**
 * Fuente de datos local para resúmenes visuales descargados sin conexión (HU-23).
 * Protegida contra ZIP Slip, ZIP Bomb (CWE-409) y escape de directorio en almacenamiento interno.
 */
interface LocalSummaryDataSource {
 suspend fun listSaved(): SavedSummariesReadResult
 suspend fun readSaved(routeId: String, nodeId: String): SavedVisualSummary?
 suspend fun checkSavedCopy(routeId: String, nodeId: String): SavedCopyCheck
 suspend fun saveFromZipPackage(topic: StudyTopic, zipBytes: ByteArray)
 suspend fun deleteSaved(routeId: String, nodeId: String)
}

class FileLocalSummaryDataSource(
 private val filesDir: File,
 private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalSummaryDataSource {

 companion object {
  const val MAX_ZIP_ENTRIES = 100
  const val MAX_UNCOMPRESSED_BYTES = 5 * 1024 * 1024
 }

 private fun folder(): File = File(filesDir, "visual-summaries")

 private fun sanitizeFileToken(raw: String): String {
  val trimmed = raw.trim()
  require(trimmed.isNotEmpty()) { "Identificador de archivo vacío." }
  return try {
   UUID.fromString(trimmed).toString()
  } catch (_: Exception) {
   trimmed.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(80).ifBlank {
    throw IllegalArgumentException("Identificador de archivo inválido.")
   }
  }
 }

 private fun summaryFile(routeId: String, nodeId: String): File {
  val dir = folder()
  dir.mkdirs()
  val safeRoute = sanitizeFileToken(routeId)
  val safeNode = sanitizeFileToken(nodeId)
  val baseDir = dir.canonicalFile
  val target = File(baseDir, "$safeRoute-$safeNode.json").canonicalFile
  require(target.path.startsWith(baseDir.path + File.separator)) {
   "Ruta de almacenamiento fuera del directorio autorizado."
  }
  return target
 }

 private fun readJsonSafely(file: File): JSONObject {
  require(file.length() <= MAX_UNCOMPRESSED_BYTES) { "Archivo local excede el límite permitido." }
  return JSONObject(file.readText(Charsets.UTF_8))
 }

 override suspend fun listSaved(): SavedSummariesReadResult = withContext(ioDispatcher) {
  val files = folder().listFiles().orEmpty()
  val items = mutableListOf<SavedVisualSummary>()
  var unreadable = 0
  files.forEach { file ->
   if (!file.isFile || !file.name.endsWith(".json")) return@forEach
   try {
    val saved = readJsonSafely(file)
    val topic = RupiJsonMapper.parseStudyTopic(saved.getJSONObject("topic"))
    val resource = RupiJsonMapper.parseVisualSummaryResource(saved.getJSONObject("resource"))
    items.add(SavedVisualSummary(topic = topic, resource = resource))
   } catch (_: Exception) {
    unreadable++
   }
  }
  SavedSummariesReadResult(items = items, unreadableCount = unreadable)
 }

 override suspend fun readSaved(routeId: String, nodeId: String): SavedVisualSummary? = withContext(ioDispatcher) {
  val file = summaryFile(routeId, nodeId)
  if (!file.exists()) return@withContext null
  val saved = readJsonSafely(file)
  SavedVisualSummary(
   topic = RupiJsonMapper.parseStudyTopic(saved.getJSONObject("topic")),
   resource = RupiJsonMapper.parseVisualSummaryResource(saved.getJSONObject("resource"))
  )
 }

 override suspend fun checkSavedCopy(routeId: String, nodeId: String): SavedCopyCheck = withContext(ioDispatcher) {
  val file = summaryFile(routeId, nodeId)
  if (!file.exists()) return@withContext SavedCopyCheck.Missing
  try {
   val saved = readJsonSafely(file)
   val version = saved.getJSONObject("resource").optString("version")
   SavedCopyCheck.Valid(version)
  } catch (_: Exception) {
   SavedCopyCheck.Invalid
  }
 }

 override suspend fun saveFromZipPackage(topic: StudyTopic, zipBytes: ByteArray) = withContext(ioDispatcher) {
  var payload: JSONObject? = null
  var entryCount = 0
  ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
   var entry = zip.nextEntry
   while (entry != null) {
    entryCount++
    check(entryCount <= MAX_ZIP_ENTRIES) { "Paquete ZIP rechazado por exceder el máximo de entradas permitidas." }
    val name = entry.name
    require(!name.contains("..") && !name.contains('\\') && !name.startsWith("/")) {
     "Entrada ZIP insegura detectada."
    }
    if (!entry.isDirectory && name == "contenido.json") {
     val contentBytes = RupiApi.readBoundedBytes(zip, MAX_UNCOMPRESSED_BYTES)
     payload = JSONObject(contentBytes.toString(Charsets.UTF_8))
    }
    zip.closeEntry()
    entry = zip.nextEntry
   }
  }
  val saved = payload ?: throw IllegalStateException("Paquete incompleto.")
  val file = summaryFile(topic.routeId, topic.nodeId)
  val temporary = File(file.parentFile, file.name + ".tmp")
  val wrapped = JSONObject()
   .put("topic", RupiJsonMapper.topicToJson(topic))
   .put("resource", saved.getJSONObject("resource"))
  temporary.writeText(wrapped.toString(), Charsets.UTF_8)
  Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
  Unit
 }

 override suspend fun deleteSaved(routeId: String, nodeId: String) {
  withContext(ioDispatcher) {
   summaryFile(routeId, nodeId).delete()
  }
 }
}
