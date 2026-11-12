package pe.rupi.mobile.data.model

data class ExperienceSummary(val totalXp: Int)

data class StreakSummary(val days: Int, val atRisk: Boolean)

data class StudentBadge(val name: String, val description: String?)

data class LeaderboardEntry(
 val rank: Int,
 val displayName: String,
 val totalXp: Int,
 val streakDays: Int,
 val isCurrentUser: Boolean
)

data class ClassroomLeaderboard(
 val classroomName: String,
 val myRank: Int,
 val totalStudents: Int,
 val entries: List<LeaderboardEntry>
)

data class AchievementsOverview(
 val experience: ExperienceSummary,
 val streak: StreakSummary,
 val leaderboard: ClassroomLeaderboard,
 val badges: List<StudentBadge>
)

data class DailyGoalDay(
 val isToday: Boolean,
 val dayOfWeek: String,
 val isGoalMet: Boolean,
 val completedLessons: Int,
 val targetLessons: Int
)

data class DailyGoalOverview(
 val completedLessons: Int,
 val targetLessons: Int,
 val pendingLessons: Int,
 val isGoalMet: Boolean,
 val motivationalMessage: String,
 val currentStreakDays: Int,
 val currentDailyTargetSetting: Int,
 val availableTargetOptions: List<Int>,
 val weeklyHistory: List<DailyGoalDay>
)

data class ScoreComparison(
 val todayExperience: Int,
 val yesterdayExperience: Int,
 val todayLessons: Int,
 val yesterdayLessons: Int,
 val trend: String,
 val message: String
)

data class DailyGoalDashboard(
 val overview: DailyGoalOverview,
 val comparison: ScoreComparison
)

data class DailyChallenge(
 val id: String,
 val date: String,
 val activityVersionId: String,
 val skill: String,
 val estimatedMinutes: Int,
 val experienceReward: Int,
 val title: String,
 val description: String,
 val readingText: String,
 val isCompleted: Boolean,
 val answeredToday: Map<String, QuestionFeedback>,
 val questions: List<LessonQuestion>
)

data class DailyChallengeCompletion(val rupiMessage: String)
