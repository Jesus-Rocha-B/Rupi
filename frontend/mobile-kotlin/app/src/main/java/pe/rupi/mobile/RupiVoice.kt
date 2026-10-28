package pe.rupi.mobile

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Reads instructions and explanations aloud with the device voice (HU-39). Prefers Peruvian Spanish. */
class RupiVoice(context: Context) : TextToSpeech.OnInitListener {
 private val tts = TextToSpeech(context.applicationContext, this)
 var ready = false
  private set

 override fun onInit(status: Int) {
  if(status != TextToSpeech.SUCCESS) return
  val peru = Locale.forLanguageTag("es-PE")
  val result = tts.setLanguage(if(tts.isLanguageAvailable(peru) >= TextToSpeech.LANG_AVAILABLE) peru else Locale.forLanguageTag("es"))
  ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
  // Same calm pace as the web voice for primary school children.
  tts.setSpeechRate(0.92f); tts.setPitch(1.05f)
 }

 /** Returns false when no Spanish voice is installed, so the screen can explain it. */
 fun speak(text: String): Boolean {
  if(!ready) return false
  tts.speak(clean(text), TextToSpeech.QUEUE_FLUSH, null, "rupi")
  return true
 }
 fun stop() { if(ready) tts.stop() }
 fun shutdown() { tts.stop(); tts.shutdown() }

 companion object {
  fun clean(text: String) = text.replace("**", "").replace(Regex("[✦★·#•✓↑↓]"), " ").replace(Regex("\\s+"), " ").trim()
  /** "Question. Alternativa a: … Alternativa b: …", like the web reader. */
  fun question(statement: String, options: List<String>) =
   (listOf(statement.trim()) + options.mapIndexed { i, option -> "Alternativa ${'a' + i}: $option." }).joinToString(" ")
 }
}
