package pe.rupi.mobile.data.model

import java.io.Serializable

/**
 * Excepción emitida cuando el backend cierra o invalida la sesión del estudiante (HU-43).
 */
class RupiSessionExpired(
 message: String = "Vuelve a iniciar sesión para recibir avisos.",
 val otherDevice: Boolean = false
) : IllegalStateException(message)

/**
 * Identifica el motivo de cierre de una sesión móvil según las cabeceras HU-43.
 */
fun sessionEnded(reason: String?, newClient: String?): RupiSessionExpired = when (reason) {
 "OTRO_DISPOSITIVO" -> RupiSessionExpired(
  if (newClient == "MOVIL") {
   "Tu cuenta se abrió en otro celular, así que aquí se cerró la sesión. Si fuiste tú, todo está bien; si no, avísale a tu profe."
  } else {
   "Tu cuenta se abrió en la web, así que aquí se cerró la sesión. Vuelve a ingresar si quieres seguir en este celular."
  },
  otherDevice = true
 )
 "EXPIRADA" -> RupiSessionExpired("Pasó mucho tiempo y tu sesión terminó. Vuelve a ingresar.")
 else -> RupiSessionExpired("Vuelve a iniciar sesión.")
}

/**
 * Referencia de un tema/parada abierta para práctica con IA, tarjetas o resumen visual.
 */
data class StudyTopic(
 val routeId: String,
 val nodeId: String,
 val title: String,
 val routeTitle: String,
 val route: LearningRouteSummary? = null
) : Serializable
