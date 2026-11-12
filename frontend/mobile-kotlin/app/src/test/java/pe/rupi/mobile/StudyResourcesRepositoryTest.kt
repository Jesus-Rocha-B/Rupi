package pe.rupi.mobile

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.rupi.mobile.data.model.RupiSessionExpired
import pe.rupi.mobile.data.model.SavedVisualSummary
import pe.rupi.mobile.data.model.StudyTopic
import pe.rupi.mobile.data.model.VisualSummaryLookup
import pe.rupi.mobile.data.model.VisualSummaryResource
import pe.rupi.mobile.data.repository.DefaultLearningRepository
import pe.rupi.mobile.data.repository.DefaultStudyResourcesRepository
import java.io.IOException

class StudyResourcesRepositoryTest {

 private val topic = StudyTopic(routeId = "r-1", nodeId = "n-1", title = "Fracciones", routeTitle = "Matemática")

 private fun resource(version: String, state: String = "REVISADO_DOCENTE") = VisualSummaryResource(
  id = "vs-1",
  grade = 4,
  version = version,
  state = state,
  source = "MINEDU",
  content = null,
  rawJson = "{}"
 )

 @Test
 fun `joinRouteByCode normaliza espacios y mayusculas antes de llamar al API REST`() = runTest {
  val remote = FakeRupiRemoteDataSource()
  val repo = DefaultLearningRepository(remote)

  val joined = repo.joinRouteByCode("  aula3a-2026 ")

  assertEquals("AULA3A-2026", remote.joinCodeReceived)
  assertEquals("r-joined", joined.route.versionRouteId)
 }

 @Test(expected = IllegalArgumentException::class)
 fun `joinRouteByCode rechaza codigo vacio sin llamar red`() = runTest {
  val repo = DefaultLearningRepository(FakeRupiRemoteDataSource())
  repo.joinRouteByCode("   ")
 }

 @Test
 fun `loadVisualSummary elimina copia local cuando servidor reporta nueva version o retiro`() = runTest {
  val remote = FakeRupiRemoteDataSource().apply {
   visualSummaryHandler = { _, _ ->
    VisualSummaryLookup(resource = resource(version = "v2"), configured = true, offline = false)
   }
  }
  val local = FakeLocalSummaryDataSource().apply {
   stored["r-1" to "n-1"] = SavedVisualSummary(topic, resource(version = "v1"))
  }
  val repo = DefaultStudyResourcesRepository(remote, local)

  val result = repo.loadVisualSummary(topic)

  assertTrue(result.invalidatedPreviousCopy)
  assertFalse(result.offline)
  assertTrue(("r-1" to "n-1") in local.deletedKeys)
  assertFalse(local.stored.containsKey("r-1" to "n-1"))
 }

 @Test
 fun `loadVisualSummary usa copia offline ante falla de red`() = runTest {
  val remote = FakeRupiRemoteDataSource().apply {
   visualSummaryHandler = { _, _ -> throw IOException("Sin internet") }
  }
  val local = FakeLocalSummaryDataSource().apply {
   stored["r-1" to "n-1"] = SavedVisualSummary(topic, resource(version = "v1"))
  }
  val repo = DefaultStudyResourcesRepository(remote, local)

  val result = repo.loadVisualSummary(topic)

  assertTrue(result.offline)
  assertEquals("v1", result.resource?.version)
 }

 @Test(expected = RupiSessionExpired::class)
 fun `loadVisualSummary propaga RupiSessionExpired en lugar de ocultarlo con copia offline`() = runTest {
  val remote = FakeRupiRemoteDataSource().apply {
   visualSummaryHandler = { _, _ -> throw RupiSessionExpired("Sesión cerrada en web", otherDevice = true) }
  }
  val local = FakeLocalSummaryDataSource().apply {
   stored["r-1" to "n-1"] = SavedVisualSummary(topic, resource(version = "v1"))
  }
  val repo = DefaultStudyResourcesRepository(remote, local)

  repo.loadVisualSummary(topic)
 }
}
