package pe.rupi.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.rupi.mobile.data.model.DailyChallengeCompletion
import pe.rupi.mobile.data.repository.AuthRepository
import pe.rupi.mobile.data.repository.GamificationRepository
import pe.rupi.mobile.ui.state.AchievementsUiState
import pe.rupi.mobile.ui.state.DailyChallengeUiState
import pe.rupi.mobile.ui.state.DailyGoalUiState

class GamificationViewModel(
 private val gamificationRepository: GamificationRepository,
 private val authRepository: AuthRepository
) : ViewModel() {

 private val _achievementsState = MutableStateFlow(AchievementsUiState())
 val achievementsState: StateFlow<AchievementsUiState> = _achievementsState.asStateFlow()

 private val _dailyGoalState = MutableStateFlow(DailyGoalUiState())
 val dailyGoalState: StateFlow<DailyGoalUiState> = _dailyGoalState.asStateFlow()

 private val _dailyChallengeState = MutableStateFlow(DailyChallengeUiState())
 val dailyChallengeState: StateFlow<DailyChallengeUiState> = _dailyChallengeState.asStateFlow()

 fun loadAchievements(force: Boolean = false) {
  val current = _achievementsState.value
  if (current.isLoading || (!force && current.overview != null)) return
  _achievementsState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _achievementsState.value = _achievementsState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val overview = gamificationRepository.getAchievements()
   _achievementsState.value = AchievementsUiState(isLoading = false, overview = overview, notice = null)
  }
 }

 fun loadDailyGoal(force: Boolean = false) {
  val current = _dailyGoalState.value
  if (current.isLoading || (!force && current.dashboard != null)) return
  _dailyGoalState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _dailyGoalState.value = _dailyGoalState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val dashboard = gamificationRepository.getDailyGoalDashboard()
   _dailyGoalState.value = DailyGoalUiState(isLoading = false, dashboard = dashboard, notice = null)
  }
 }

 fun updateDailyTarget(target: Int) {
  val current = _dailyGoalState.value
  if (current.isLoading) return
  _dailyGoalState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _dailyGoalState.value = _dailyGoalState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   gamificationRepository.updateDailyGoalTarget(target)
   val dashboard = gamificationRepository.getDailyGoalDashboard()
   _dailyGoalState.value = DailyGoalUiState(isLoading = false, dashboard = dashboard, notice = null)
  }
 }

 fun loadDailyChallenge(
  completion: DailyChallengeCompletion? = null,
  force: Boolean = false
 ) {
  val current = _dailyChallengeState.value
  if (current.isLoading) return
  if (!force && current.challenge != null && completion == null) return
  _dailyChallengeState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _dailyChallengeState.value = _dailyChallengeState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val challenge = gamificationRepository.getDailyChallenge()
   val prev = _dailyChallengeState.value
   val baseAnswers = if (challenge.date != prev.activeDay) emptyMap() else prev.answers
   val mergedAnswers = baseAnswers.toMutableMap().apply {
    challenge.answeredToday.forEach { (qid, fb) -> putIfAbsent(qid, fb) }
   }
   _dailyChallengeState.value = DailyChallengeUiState(
    isLoading = false,
    challenge = challenge,
    answers = mergedAnswers,
    activeDay = challenge.date,
    completion = completion ?: prev.completion,
    notice = null
   )
  }
 }

 fun submitChallengeAnswer(activityVersionId: String, questionId: String, selectedOptionId: String) {
  val current = _dailyChallengeState.value
  if (current.isLoading) return
  _dailyChallengeState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _dailyChallengeState.value = _dailyChallengeState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val feedback = gamificationRepository.answerChallengeQuestion(activityVersionId, questionId, selectedOptionId)
   val updatedAnswers = _dailyChallengeState.value.answers + (questionId to feedback)
   val refreshed = gamificationRepository.getDailyChallenge()
   val merged = updatedAnswers.toMutableMap().apply {
    refreshed.answeredToday.forEach { (qid, fb) -> putIfAbsent(qid, fb) }
   }
   _dailyChallengeState.value = _dailyChallengeState.value.copy(
    isLoading = false,
    challenge = refreshed,
    answers = merged,
    activeDay = refreshed.date,
    notice = null
   )
  }
 }

 fun completeChallenge(challengeId: String) {
  val current = _dailyChallengeState.value
  if (current.isLoading) return
  _dailyChallengeState.value = current.copy(isLoading = true, notice = loadingNotice())
  launchSessionAware(
   authRepository = authRepository,
   onError = { msg -> _dailyChallengeState.value = _dailyChallengeState.value.copy(isLoading = false, notice = errorNotice(msg)) }
  ) {
   val completion = gamificationRepository.completeDailyChallenge(challengeId)
   val refreshed = gamificationRepository.getDailyChallenge()
   _dailyChallengeState.value = _dailyChallengeState.value.copy(
    isLoading = false,
    challenge = refreshed,
    completion = completion,
    notice = null
   )
  }
 }

 fun clear() {
  _achievementsState.value = AchievementsUiState()
  _dailyGoalState.value = DailyGoalUiState()
  _dailyChallengeState.value = DailyChallengeUiState()
 }
}
