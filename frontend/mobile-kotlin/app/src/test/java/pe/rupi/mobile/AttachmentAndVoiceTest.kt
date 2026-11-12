package pe.rupi.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.rupi.mobile.data.remote.AttachmentParser

class AttachmentAndVoiceTest {

 @Test
 fun `parse extrae enlace relativo y limpia prefijo docente en ADJUNTO`() {
  val parsed = AttachmentParser.parse(
   type = "ADJUNTO",
   rawBody = "Material adjunto por tu profesor: Guía de fracciones (/api/v1/materials/abc-123.pdf)",
   rawUrl = null
  )
  assertEquals("Guía de fracciones", parsed.displayText)
  assertEquals("/api/v1/materials/abc-123.pdf", parsed.rawUrl)
 }

 @Test
 fun `RupiVoice formatea pregunta y limpia signos decorativos`() {
  val text = RupiVoice.question(
   statement = "**¿Cuánto es 2 + 2?** · ✓",
   options = listOf("3", "4")
  )
  assertEquals("**¿Cuánto es 2 + 2?** · ✓ Alternativa a: 3. Alternativa b: 4.", text)
  assertEquals("¿Cuánto es 2 + 2?", RupiVoice.clean("**¿Cuánto es 2 + 2?** · ✓"))
 }
}
