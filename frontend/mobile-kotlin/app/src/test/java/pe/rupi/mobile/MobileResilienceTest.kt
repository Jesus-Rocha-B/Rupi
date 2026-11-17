package pe.rupi.mobile

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import pe.rupi.mobile.data.remote.RupiApi
import pe.rupi.mobile.data.remote.RupiJsonMapper

class MobileResilienceTest {

 @Test
 fun `RupiApi isLocalOrLanHost valida emuladores y rangos privados LAN`() {
  assertTrue(RupiApi.isLocalOrLanHost("10.0.2.2"))
  assertTrue(RupiApi.isLocalOrLanHost("127.0.0.1"))
  assertTrue(RupiApi.isLocalOrLanHost("localhost"))
  assertTrue(RupiApi.isLocalOrLanHost("192.168.1.100"))
  assertTrue(RupiApi.isLocalOrLanHost("10.0.0.15"))
  assertTrue(RupiApi.isLocalOrLanHost("172.16.0.1"))
  assertTrue(RupiApi.isLocalOrLanHost("172.31.255.254"))

  assertFalse(RupiApi.isLocalOrLanHost("google.com"))
  assertFalse(RupiApi.isLocalOrLanHost("8.8.8.8"))
  assertFalse(RupiApi.isLocalOrLanHost("192.168.1.999"))
  assertFalse(RupiApi.isLocalOrLanHost("172.32.0.1"))
  assertFalse(RupiApi.isLocalOrLanHost(null))
 }

 @Test
 fun `RupiJsonMapper parseRouteDetail es tolerante ante objeto progress ausente o nulo`() {
  val jsonSinProgreso = JSONObject().apply {
   put("versionRouteId", "route-123")
   put("title", "Matemática Andina")
   put("nodes", JSONArray())
  }

  val detail = RupiJsonMapper.parseRouteDetail(jsonSinProgreso)
  assertEquals("route-123", detail.route.versionRouteId)
  assertEquals("Matemática Andina", detail.route.title)
  assertEquals(0, detail.route.completedNodes)
  assertEquals(0, detail.route.totalNodes)
  assertTrue(detail.ungroupedNodes.isEmpty())
 }

 @Test
 fun `RupiJsonMapper parseLearningRoutes tolera lista vacia o ausente de items`() {
  val jsonSinItems = JSONObject()
  val routes = RupiJsonMapper.parseLearningRoutes(jsonSinItems)
  assertTrue(routes.isEmpty())
 }
}
