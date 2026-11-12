package pe.rupi.mobile

import android.app.Application
import kotlinx.coroutines.Dispatchers
import pe.rupi.mobile.data.local.AndroidSessionAlertsDataSource
import pe.rupi.mobile.data.local.FileLocalSummaryDataSource
import pe.rupi.mobile.data.remote.DefaultRupiRemoteDataSource
import pe.rupi.mobile.data.remote.SessionClientHolder
import pe.rupi.mobile.data.repository.AuthRepository
import pe.rupi.mobile.data.repository.DefaultAuthRepository
import pe.rupi.mobile.data.repository.DefaultGamificationRepository
import pe.rupi.mobile.data.repository.DefaultLearningRepository
import pe.rupi.mobile.data.repository.DefaultNotificationRepository
import pe.rupi.mobile.data.repository.DefaultStudyResourcesRepository
import pe.rupi.mobile.data.repository.GamificationRepository
import pe.rupi.mobile.data.repository.LearningRepository
import pe.rupi.mobile.data.repository.NotificationRepository
import pe.rupi.mobile.data.repository.StudyResourcesRepository
import pe.rupi.mobile.ui.viewmodel.RupiViewModelFactory

/**
 * Contenedor de dependencias de la aplicación (DataSources -> Repositories -> ViewModelFactory).
 */
class AppContainer(application: Application) {
 val sessionHolder = SessionClientHolder()
 private val alertsDataSource = AndroidSessionAlertsDataSource(application)
 private val localSummaryDataSource = FileLocalSummaryDataSource(application.filesDir, Dispatchers.IO)
 private val remoteDataSource = DefaultRupiRemoteDataSource(sessionHolder, Dispatchers.IO)

 val authRepository: AuthRepository = DefaultAuthRepository(
  remote = remoteDataSource,
  sessionHolder = sessionHolder,
  alertsDataSource = alertsDataSource
 )

 val learningRepository: LearningRepository = DefaultLearningRepository(remoteDataSource)

 val gamificationRepository: GamificationRepository = DefaultGamificationRepository(remoteDataSource)

 val notificationRepository: NotificationRepository = DefaultNotificationRepository(
  remote = remoteDataSource,
  sessionHolder = sessionHolder,
  alertsDataSource = alertsDataSource,
  ioDispatcher = Dispatchers.IO
 )

 val studyResourcesRepository: StudyResourcesRepository = DefaultStudyResourcesRepository(
  remote = remoteDataSource,
  localSummaries = localSummaryDataSource
 )

 val viewModelFactory = RupiViewModelFactory(
  authRepository = authRepository,
  learningRepository = learningRepository,
  gamificationRepository = gamificationRepository,
  notificationRepository = notificationRepository,
  studyResourcesRepository = studyResourcesRepository
 )
}

class RupiApplication : Application() {
 val container: AppContainer by lazy { AppContainer(this) }
}
