package pe.rupi.mobile.ui.screens

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import pe.rupi.mobile.R
import pe.rupi.mobile.RupiUi
import pe.rupi.mobile.data.model.StudyPreferences
import pe.rupi.mobile.ui.components.ScreenScaffold
import pe.rupi.mobile.ui.state.NoticeTone
import pe.rupi.mobile.ui.state.NotificationsUiState

fun renderNotificationsBody(
 context: Context,
 scaffold: ScreenScaffold,
 state: NotificationsUiState,
 onBackToTopics: () -> Unit,
 onRefresh: () -> Unit,
 onSavePreferences: (StudyPreferences) -> Unit,
 onMarkAsRead: (String) -> Unit
) {
 scaffold.rebuildBody(preserveScroll = true) {
  backButton("Volver a mis temas") { onBackToTopics() }
  val dashboard = state.dashboard ?: return@rebuildBody
  val status = dashboard.status
  val prefs = status.preferences
  val inbox = dashboard.inbox

  card {
   text("Mi meta y mis avisos", 22)
   muted("Completaste ${status.completedToday} actividades hoy. Las metas no cambian tus notas; las horas son de Perú.")
   addView(TextView(context).apply {
    text = "Mi meta diaria"
    typeface = RupiUi.BOLD
    textSize = 15f
    setTextColor(RupiUi.FOREST)
    setPadding(0, dp(12), 0, dp(6))
   })
   val goal = Spinner(context).apply {
    minimumHeight = dp(52)
    contentDescription = "Mi meta diaria"
    background = RupiUi.field(context)
    setPadding(dp(10), dp(6), dp(10), dp(6))
    adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, (1..5).map { "$it actividad(es)" })
    setSelection((prefs.dailyGoal - 1).coerceIn(0, 4))
   }
   addView(goal)

   addView(TextView(context).apply {
    text = "¿Qué avisos quieres?"
    typeface = RupiUi.BOLD
    textSize = 15f
    setTextColor(RupiUi.FOREST)
    setPadding(0, dp(16), 0, dp(2))
   })

   fun toggle(label: String, checked: Boolean) = CheckBox(context).apply {
    text = label
    isChecked = checked
    minHeight = dp(52)
    setSingleLine(false)
    setHorizontallyScrolling(false)
    setPadding(dp(8), dp(12), dp(8), dp(12))
    textSize = 16f
    typeface = RupiUi.fontBold(context)
    setTextColor(RupiUi.INK)
    buttonTintList = ColorStateList.valueOf(RupiUi.LEAF)
    addView(this)
   }

   val goals = toggle("Recordar mi meta pendiente y mi racha", prefs.goals)
   val evaluations = toggle("Avisos de mi docente: fechas de evaluación y materiales nuevos", prefs.evaluations)
   val system = toggle("Permitir avisos en Android", prefs.system)
   val at = input("Hora del recordatorio (HH:mm)").apply { setText(prefs.remindAt) }
   val from = input("Descansar desde (HH:mm)").apply { setText(prefs.quietFrom) }
   val until = input("Volver a recibir avisos (HH:mm)").apply { setText(prefs.quietUntil) }

   button("Guardar mis preferencias") {
    onSavePreferences(
     StudyPreferences(
      dailyGoal = goal.selectedItemPosition + 1,
      goals = goals.isChecked,
      evaluations = evaluations.isChecked,
      system = system.isChecked,
      remindAt = at.text.toString(),
      quietFrom = from.text.toString(),
      quietUntil = until.text.toString(),
      revision = prefs.revision
     )
    )
   }
  }

  button("Recargar preferencias guardadas", kind = RupiUi.Kind.QUIET) { onRefresh() }
  muted("Android consulta periódicamente con conexión. El ahorro de batería puede retrasar los avisos. Si cierras la sesión, se detienen.")
  section("Mis avisos")
  if (inbox.quiet) {
   feedback("Horario de descanso: los avisos del dispositivo están pausados.", NoticeTone.INFO)
  }
  if (inbox.items.isEmpty()) {
   card { muted("No tienes recordatorios pendientes.") }
  }
  inbox.items.forEach { item ->
   card {
    text(item.title, 20)
    text(item.body, 16)
    if (item.read) {
     chip(
      "Leído",
      Color.rgb(238, 247, 237),
      Color.rgb(148, 208, 161),
      Color.rgb(27, 83, 41),
      iconRes = R.drawable.ic_check
     )
    } else {
     button("Marcar como leído", kind = RupiUi.Kind.QUIET) { onMarkAsRead(item.id) }
    }
   }
  }
 }
}
