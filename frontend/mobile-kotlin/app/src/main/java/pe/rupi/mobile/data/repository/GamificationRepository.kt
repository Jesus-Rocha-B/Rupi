package pe.rupi.mobile.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pe.rupi.mobile.data.model.AchievementsOverview
import pe.rupi.mobile.data.model.DailyChallenge
import pe.rupi.mobile.data.model.DailyChallengeCompletion
import pe.rupi.mobile.data.model.DailyGoalDashboard
import pe.rupi.mobile.data.model.QuestionFeedback
import pe.rupi.mobile.data.remote.RupiRemoteDataSource

/**
 * Repositorio de gamificación: logros/aula, meta diaria y reto diario.
 */
interface GamificationRepository {
 suspend fun getAchievements(): AchievementsOverview
 suspend fun getDailyGoalDashboard(): DailyGoalDashboard
 suspend fun updateDailyGoalTarget(target: Int)
 suspend fun getDailyChallenge(): DailyChallenge
 suspend fun answerChallengeQuestion(activityVersionId: String, questionId: String, selectedOptionId: String): QuestionFeedback
 suspend fun completeDailyChallenge(challengeId: String): DailyChallengeCompletion
}

class DefaultGamificationRepository(
 private val remote: RupiRemoteDataSource
) : GamificationRepository {

 override suspend fun getAchievements(): AchievementsOverview = coroutineScope {
  val experienceDeferred = async { remote.getExperience() }
  val streakDeferred = async { remote.getStreak() }
  val leaderboardDeferred = async { remote.getLeaderboard() }
  val badgesDeferred = async { remote.getBadges() }
  AchievementsOverview(
   experience = experienceDeferred.await(),
   streak = streakDeferred.await(),
   leaderboard = leaderboardDeferred.await(),
   badges = badgesDeferred.await()
  )
 }

 override suspend fun getDailyGoalDashboard(): DailyGoalDashboard =
  remote.getDailyGoalDashboard()

 override suspend fun updateDailyGoalTarget(target: Int) {
  remote.updateDailyGoalTarget(target)
 }

 override suspend fun getDailyChallenge(): DailyChallenge =
  remote.getDailyChallenge()

 override suspend fun answerChallengeQuestion(
  activityVersionId: String,
  questionId: String,
  selectedOptionId: String
 ): QuestionFeedback = remote.answerQuestion(activityVersionId, questionId, selectedOptionId)

 override suspend fun completeDailyChallenge(challengeId: String): DailyChallengeCompletion =
  remote.completeDailyChallenge(challengeId)
}
