package pe.rupi.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import pe.rupi.mobile.data.repository.AuthRepository
import pe.rupi.mobile.data.repository.GamificationRepository
import pe.rupi.mobile.data.repository.LearningRepository
import pe.rupi.mobile.data.repository.NotificationRepository
import pe.rupi.mobile.data.repository.StudyResourcesRepository

/**
 * Fábrica MVVM que inyecta los repositorios y SavedStateHandle a cada ViewModel.
 */
class RupiViewModelFactory(
 private val authRepository: AuthRepository,
 private val learningRepository: LearningRepository,
 private val gamificationRepository: GamificationRepository,
 private val notificationRepository: NotificationRepository,
 private val studyResourcesRepository: StudyResourcesRepository
) : ViewModelProvider.Factory {

 @Suppress("UNCHECKED_CAST")
 override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = when {
  modelClass.isAssignableFrom(NavigationViewModel::class.java) ->
   NavigationViewModel(extras.createSavedStateHandle()) as T
  modelClass.isAssignableFrom(AuthViewModel::class.java) ->
   AuthViewModel(authRepository) as T
  modelClass.isAssignableFrom(TopicsViewModel::class.java) ->
   TopicsViewModel(learningRepository, authRepository) as T
  modelClass.isAssignableFrom(RouteLessonViewModel::class.java) ->
   RouteLessonViewModel(learningRepository, authRepository) as T
  modelClass.isAssignableFrom(GamificationViewModel::class.java) ->
   GamificationViewModel(gamificationRepository, authRepository) as T
  modelClass.isAssignableFrom(NotificationsViewModel::class.java) ->
   NotificationsViewModel(notificationRepository, authRepository) as T
  modelClass.isAssignableFrom(StudyResourcesViewModel::class.java) ->
   StudyResourcesViewModel(studyResourcesRepository, authRepository) as T
  else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
 }
}
