package pe.rupi.mobile.data.local

import android.content.Context
import pe.rupi.mobile.StudyAlerts
import pe.rupi.mobile.data.remote.RupiApi

/**
 * Fuente local para persistencia cifrada de sesión y programación de recordatorios del dispositivo.
 */
interface SessionAlertsDataSource {
 fun restoreSessionClient(): RupiApi?
 fun areDeviceNotificationsAllowed(): Boolean
 fun enableDeviceAlerts(client: RupiApi)
 fun disableDeviceAlerts()
}

class AndroidSessionAlertsDataSource(context: Context) : SessionAlertsDataSource {
 private val appContext = context.applicationContext

 override fun restoreSessionClient(): RupiApi? = StudyAlerts.restore(appContext)

 override fun areDeviceNotificationsAllowed(): Boolean = StudyAlerts.allowed(appContext)

 override fun enableDeviceAlerts(client: RupiApi) {
  StudyAlerts.enable(appContext, client)
 }

 override fun disableDeviceAlerts() {
  StudyAlerts.disable(appContext)
 }
}
