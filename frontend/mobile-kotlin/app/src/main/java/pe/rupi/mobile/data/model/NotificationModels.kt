package pe.rupi.mobile.data.model

data class StudyPreferences(
 val dailyGoal: Int,
 val goals: Boolean,
 val evaluations: Boolean,
 val system: Boolean,
 val remindAt: String,
 val quietFrom: String,
 val quietUntil: String,
 val revision: Long
)

data class NotificationPreferencesStatus(
 val completedToday: Int,
 val preferences: StudyPreferences
)

data class StudyNotificationItem(
 val id: String,
 val title: String,
 val body: String,
 val read: Boolean
)

data class NotificationInbox(
 val quiet: Boolean,
 val system: Boolean,
 val items: List<StudyNotificationItem>
)

data class NotificationsDashboard(
 val status: NotificationPreferencesStatus,
 val inbox: NotificationInbox
)
