package pe.rupi.mobile.data.remote

/**
 * Extrae el nombre visible y la URL embebida de bloques tipo ADJUNTO generados por el panel docente.
 */
object AttachmentParser {
 private val embeddedUrlRegex = Regex("""\(((?:https?://|/api/)[^()\s]+)\)\s*$""")

 data class ParsedAttachment(val displayText: String?, val rawUrl: String?)

 fun parse(type: String, rawBody: String?, rawUrl: String?): ParsedAttachment {
  var body = rawBody
  var url = rawUrl
  if (type == "ADJUNTO" && body != null) {
   val embedded = embeddedUrlRegex.find(body)
   if (embedded != null) {
    if (url == null) url = embedded.groupValues[1]
    body = body.removeRange(embedded.range).trim()
   }
   body = body.removePrefix("Material adjunto por tu profesor:").trim().ifBlank { null }
  }
  return ParsedAttachment(displayText = body, rawUrl = url)
 }
}
