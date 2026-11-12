package pe.rupi.mobile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.rupi.mobile.data.local.SessionAlertsDataSource
import pe.rupi.mobile.data.model.NotificationsDashboard
import pe.rupi.mobile.data.model.StudyPreferences
import pe.rupi.mobile.data.remote.RupiRemoteDataSource
import pe.rupi.mobile.data.remote.SessionClientHolder

/**
 * Repositorio para preferencias de estudio, bandeja de avisos y alertas locales en Android.
 */
interface NotificationRepository {
 suspend fun getDashboard(): NotificationsDashboard
 suspend fun savePreferences(preferences: StudyPreferences): StudyPreferences
 suspend fun markAsRead(notificationId: String)
 fun areDeviceNotificationsAllowed(): Boolean
 suspend fun enableDeviceAlerts()
 fun disableDeviceAlerts()
}

class DefaultNotificationRepository(
 private val remote: RupiRemoteDataSource,
 private val sessionHolder: SessionClientHolder,
 private val alertsDataSource: SessionAlertsDataSource,
 private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NotificationRepository {

 override suspend fun getDashboard(): NotificationsDashboard =
  remote.getNotificationsDashboard()

 override suspend fun savePreferences(preferences: StudyPreferences): StudyPreferences =
  remote.saveNotificationPreferences(preferences)

 override suspend fun markAsRead(notificationId: String) {
  remote.markNotificationAsRead(notificationId)
 }

 override fun areDeviceNotificationsAllowed(): Boolean =
  alertsDataSource.areDeviceNotificationsAllowed()

 override suspend fun enableDeviceAlerts() {
  withContext(ioDispatcher) {
   alertsDataSource.enableDeviceAlerts(sessionHolder.require())
  }
 }

 override fun disableDeviceAlerts() {
  alertsDataSource.disableDeviceAlerts()
 }
}
