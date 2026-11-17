package pe.rupi.mobile

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import pe.rupi.mobile.data.local.FileLocalSummaryDataSource
import pe.rupi.mobile.data.model.StudyTopic
import pe.rupi.mobile.data.remote.DefaultRupiRemoteDataSource
import pe.rupi.mobile.data.remote.RupiApi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SecurityHardeningTest {

 @Test
 fun `RupiApi validateApiPath rechaza path traversal crudo, codificado y CRLF`() {
  val base = URI("https://rupi.edu.pe")
  val valid = RupiApi.validateApiPath(base, "/api/v1/student/learning-routes")
  assertEquals("https://rupi.edu.pe/api/v1/student/learning-routes", valid.toString())

  val attacks = listOf(
   "/api/v1/../../admin/users",
   "/api/v1/%2e%2e/admin",
   "/api/v1/%2E%2E/secret",
   "/api/v1/student\\..\\admin",
   "/api/v1/student/routes\r\nX-Injected: true",
   "/admin/v1/cuentas"
  )
  attacks.forEach { payload ->
   try {
    RupiApi.validateApiPath(base, payload)
    fail("Debió rechazar ruta insegura: $payload")
   } catch (_: IllegalArgumentException) {
    // Esperado
   }
  }
 }

 @Test
 fun `RupiApi readBoundedBytes detiene respuestas que exceden limite anti-DoS`() {
  val allowed = RupiApi.readBoundedBytes(ByteArrayInputStream(ByteArray(1024)), 2048)
  assertEquals(1024, allowed.size)

  try {
   RupiApi.readBoundedBytes(ByteArrayInputStream(ByteArray(4096)), 1024)
   fail("Debió rechazar flujo mayor al límite permitido")
  } catch (_: IllegalStateException) {
   // Esperado
  }
 }

 @Test
 fun `RupiApi verifySha256ConstantTime valida digest correcto y rechaza alteraciones`() {
  val data = "paquete-educativo-rupi".toByteArray(Charsets.UTF_8)
  val digest = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
  assertTrue(RupiApi.verifySha256ConstantTime(data, digest))
  assertFalse(RupiApi.verifySha256ConstantTime(data, "0".repeat(64)))
  assertFalse(RupiApi.verifySha256ConstantTime(data, "invalido"))
 }

 @Test
 fun `ExternalLinkPolicy bloquea esquemas peligrosos, HTTP externo y userInfo phishing`() {
  assertEquals(
   "https://www.youtube.com/watch?v=rupi123",
   ExternalLinkPolicy.sanitizeExternalUrl("https://www.youtube.com/watch?v=rupi123", allowDebugLocalHttp = false)
  )
  assertNull(ExternalLinkPolicy.sanitizeExternalUrl("intent://scan/#Intent;scheme=zxing;end", allowDebugLocalHttp = false))
  assertNull(ExternalLinkPolicy.sanitizeExternalUrl("javascript:alert(1)", allowDebugLocalHttp = false))
  assertNull(ExternalLinkPolicy.sanitizeExternalUrl("file:///data/data/pe.rupi.mobile/files/study-session", allowDebugLocalHttp = false))
  assertNull(ExternalLinkPolicy.sanitizeExternalUrl("content://com.android.contacts/data", allowDebugLocalHttp = false))
  assertNull(ExternalLinkPolicy.sanitizeExternalUrl("tel:*#06#", allowDebugLocalHttp = false))
  assertNull(ExternalLinkPolicy.sanitizeExternalUrl("http://sitio-externo.com/video", allowDebugLocalHttp = false))
  assertNull(ExternalLinkPolicy.sanitizeExternalUrl("https://minedu.gob.pe@atacante.example/login", allowDebugLocalHttp = false))
  assertNotNull(ExternalLinkPolicy.sanitizeExternalUrl("http://192.168.1.20:8081/api/v1/materials/1.pdf", allowDebugLocalHttp = true))
 }

 @Test
 fun `DefaultRupiRemoteDataSource safePathSegment bloquea inyeccion de rutas y parametros`() {
  assertEquals("a1b2-c3d4", DefaultRupiRemoteDataSource.safePathSegment("a1b2-c3d4"))
  val invalidSegments = listOf(
   "",
   "   ",
   "../admin",
   "123/../../secret",
   "123?role=ADMIN",
   "123#fragment",
   "123%2fadmin",
   "123\\evil"
  )
  invalidSegments.forEach { bad ->
   try {
    DefaultRupiRemoteDataSource.safePathSegment(bad)
    fail("Debió rechazar segmento inseguro: $bad")
   } catch (_: IllegalArgumentException) {
    // Esperado
   }
  }
 }

 @Test
 fun `FileLocalSummaryDataSource rechaza ZIP Slip y ZIP Bomb`() = runTest {
  val tempDir = Files.createTempDirectory("rupi-sec-test").toFile()
  val topic = StudyTopic(routeId = "r-1", nodeId = "n-1", title = "Seguridad", routeTitle = "Prueba")
  try {
   val ds = FileLocalSummaryDataSource(tempDir)

   // 1) ZIP Slip
   val zipSlipBytes = ByteArrayOutputStream().apply {
    ZipOutputStream(this).use { zos ->
     zos.putNextEntry(ZipEntry("../../etc/passwd"))
     zos.write("malicioso".toByteArray())
     zos.closeEntry()
    }
   }.toByteArray()

   try {
    ds.saveFromZipPackage(topic, zipSlipBytes)
    fail("Debió rechazar entrada ZIP Slip")
   } catch (_: IllegalArgumentException) {
    // Esperado
   }

   // 2) ZIP Bomb (> 5 MB descomprimidos)
   val oversized = ByteArray(FileLocalSummaryDataSource.MAX_UNCOMPRESSED_BYTES + 1024) { 'A'.code.toByte() }
   val zipBombBytes = ByteArrayOutputStream().apply {
    ZipOutputStream(this).use { zos ->
     zos.putNextEntry(ZipEntry("contenido.json"))
     zos.write(oversized)
     zos.closeEntry()
    }
   }.toByteArray()

   try {
    ds.saveFromZipPackage(topic, zipBombBytes)
    fail("Debió rechazar ZIP Bomb que excede límite de memoria")
   } catch (_: IllegalStateException) {
    // Esperado
   }
  } finally {
   tempDir.deleteRecursively()
  }
 }
}
