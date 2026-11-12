package pe.rupi.mobile.ui.screens

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import pe.rupi.mobile.DailyGoalRing
import pe.rupi.mobile.R
import pe.rupi.mobile.RupiUi
import pe.rupi.mobile.RupiVoice
import pe.rupi.mobile.data.model.ScoreComparison
import pe.rupi.mobile.ui.components.ScreenScaffold
import pe.rupi.mobile.ui.state.AchievementsUiState
import pe.rupi.mobile.ui.state.DailyChallengeUiState
import pe.rupi.mobile.ui.state.DailyGoalUiState
import pe.rupi.mobile.ui.state.NoticeTone

fun renderAchievementsBody(
 scaffold: ScreenScaffold,
 state: AchievementsUiState,
 onBackToTopics: () -> Unit
) {
 scaffold.rebuildBody(preserveScroll = true) {
  backButton("Volver a mis temas") { onBackToTopics() }
  val overview = state.overview ?: return@rebuildBody

  heroCard {
   text("${overview.experience.totalXp} XP", 28)
   muted("Tu experiencia total en Rupi: sube con cada parada, reto y práctica.")
   val days = overview.streak.days
   if (days > 0) {
    val risk = if (overview.streak.atRisk) " · ¡estudia hoy para no perderla!" else ""
    chip(
     "Racha: $days ${if (days == 1) "día seguido" else "días seguidos"}$risk",
     Color.rgb(255, 248, 230),
     RupiUi.SUN,
     Color.rgb(107, 71, 5),
     iconRes = R.drawable.ic_flame
    )
   } else {
    chip("Empieza tu racha completando una parada hoy", Color.WHITE, RupiUi.LINE, RupiUi.MUTED)
   }
  }

  card {
   text("Mis insignias", 20)
   if (overview.badges.isEmpty()) {
    muted("Todavía no tienes insignias. Termina paradas y rutas para ganarlas.")
   }
   overview.badges.forEach { badge ->
    chip(badge.name, Color.rgb(255, 248, 230), RupiUi.SUN, Color.rgb(107, 71, 5), iconRes = R.drawable.ic_award)
    badge.description?.let { muted(it) }
   }
  }

  card {
   val board = overview.leaderboard
   text("Tabla de mi aula", 20)
   muted("${board.classroomName} · puesto ${board.myRank} de ${board.totalStudents}")
   board.entries.forEach { entry ->
    val textColor = if (entry.isCurrentUser) RupiUi.SKY_DARK else RupiUi.INK
    val backColor = if (entry.isCurrentUser) Color.rgb(238, 246, 251) else Color.WHITE
    val borderColor = if (entry.isCurrentUser) Color.rgb(147, 196, 223) else RupiUi.LINE
    val line = "${entry.rank}. ${entry.displayName} · ${entry.totalXp} XP"
    val trailingStreak = if (entry.streakDays > 0) {
     SpannableStringBuilder(" · ").append(
      iconLabel(R.drawable.ic_flame, RupiUi.ORANGE, "${entry.streakDays}", iconDp = 14)
     )
    } else {
     null
    }
    chip(line, backColor, borderColor, textColor, trailingValue = trailingStreak)
   }
   muted("Tus compañeros te ven con tu alias solo si lo eliges en la web (Tabla de Aula).")
  }
 }
}

fun renderDailyGoalBody(
 context: Context,
 scaffold: ScreenScaffold,
 state: DailyGoalUiState,
 onBackToTopics: () -> Unit,
 onRefresh: () -> Unit,
 onSelectTarget: (Int) -> Unit
) {
 scaffold.rebuildBody(preserveScroll = true) {
  backButton("Volver a mis temas") { onBackToTopics() }
  button("Actualizar mi avance", kind = RupiUi.Kind.LEAF) { onRefresh() }
  val dashboard = state.dashboard ?: return@rebuildBody
  val today = dashboard.overview
  val comparison = dashboard.comparison
  val lessonsWord = if (today.targetLessons == 1) "lección" else "lecciones"

  goalCard(today.isGoalMet) {
   addView(
    DailyGoalRing(context, today.completedLessons, today.targetLessons, today.isGoalMet),
    LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(8) }
   )
   val maxVal = today.targetLessons.coerceAtLeast(1)
   val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
    max = maxVal
    progress = today.completedLessons.coerceAtMost(maxVal)
    minimumHeight = dp(16)
    progressTintList = ColorStateList.valueOf(if (today.isGoalMet) RupiUi.GOLD_STROKE else RupiUi.LEAF)
    progressBackgroundTintList = ColorStateList.valueOf(RupiUi.PAPER_DEEP)
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
   }
   addView(bar, LinearLayout.LayoutParams(-1, dp(16)))
   text(
    if (today.isGoalMet) {
     "¡Meta del día lograda! Completaste ${today.completedLessons} de ${today.targetLessons} $lessonsWord."
    } else {
     "Llevas ${today.completedLessons} de ${today.targetLessons} $lessonsWord. Te faltan ${today.pendingLessons}."
    },
    20
   )
   muted(today.motivationalMessage)
   if (today.currentStreakDays > 0) {
    chip(
     "Racha: ${today.currentStreakDays} ${if (today.currentStreakDays == 1) "día seguido" else "días seguidos"}",
     Color.rgb(255, 248, 230),
     RupiUi.SUN,
     Color.rgb(107, 71, 5),
     iconRes = R.drawable.ic_flame
    )
   }
   listen("mi avance de hoy") {
    "Llevas ${today.completedLessons} de ${today.targetLessons} $lessonsWord. ${today.motivationalMessage} ${comparison.message}"
   }
  }

  card { renderScoreComparison(context, scaffold, comparison) }

  card {
   text("Tu semana de estudio", 20)
   today.weeklyHistory.forEach { day ->
    val name = if (day.isToday) "Hoy" else day.dayOfWeek
    if (day.isGoalMet) {
     chip(
      "$name · meta cumplida",
      Color.rgb(238, 247, 237),
      Color.rgb(148, 208, 161),
      Color.rgb(27, 83, 41),
      iconRes = R.drawable.ic_check
     )
    } else {
     chip("$name · ${day.completedLessons} de ${day.targetLessons}", Color.WHITE, RupiUi.LINE, RupiUi.MUTED)
    }
   }
  }

  section("¿Cuántas paradas quieres completar por día?", "Es la misma meta que ves en la web y en tus avisos. No cambia tus notas.")
  today.availableTargetOptions.forEach { option ->
   val current = option == today.currentDailyTargetSetting
   button(
    label = "$option ${if (option == 1) "parada" else "paradas"}",
    enabled = true,
    kind = if (current) RupiUi.Kind.LEAF else RupiUi.Kind.QUIET,
    iconRes = if (current) R.drawable.ic_check else null
   ) {
    if (!current) onSelectTarget(option)
   }
  }
 }
}

private fun renderScoreComparison(
 context: Context,
 scaffold: ScreenScaffold,
 comparison: ScoreComparison
) {
 scaffold.text("Hoy vs. ayer", 20)
 val maxXp = maxOf(comparison.todayExperience, comparison.yesterdayExperience, 1)
 fun row(label: String, xp: Int, lessons: Int, color: Int) {
  scaffold.text("$label · $xp XP · $lessons ${if (lessons == 1) "parada" else "paradas"}", 16)
  val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
   max = maxXp
   progress = xp
   progressTintList = ColorStateList.valueOf(color)
   progressBackgroundTintList = ColorStateList.valueOf(RupiUi.PAPER_DEEP)
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  scaffold.addView(bar, LinearLayout.LayoutParams(-1, scaffold.dp(16)))
 }
 row("Hoy", comparison.todayExperience, comparison.todayLessons, RupiUi.LEAF)
 row("Ayer", comparison.yesterdayExperience, comparison.yesterdayLessons, Color.rgb(167, 154, 133))
 val trendIcon = when (comparison.trend) {
  "SUBE" -> R.drawable.ic_trending_up
  "BAJA" -> R.drawable.ic_trending_down
  else -> R.drawable.ic_trend_equal
 }
 scaffold.feedback(comparison.message, if (comparison.trend == "BAJA") NoticeTone.RETRY else NoticeTone.OK, iconRes = trendIcon)
}

fun renderDailyChallengeBody(
 context: Context,
 scaffold: ScreenScaffold,
 state: DailyChallengeUiState,
 onBackToTopics: () -> Unit,
 onSubmitAnswer: (activityVersionId: String, questionId: String, optionId: String) -> Unit,
 onCompleteChallenge: (challengeId: String) -> Unit
) {
 scaffold.rebuildBody(preserveScroll = true) {
  backButton("Volver a mis temas") { onBackToTopics() }
  val challenge = state.challenge ?: return@rebuildBody

  accentCard(if (challenge.skill == "COMPRENSION_LECTORA") RupiUi.SKY else RupiUi.ORANGE) {
   if (challenge.skill == "COMPRENSION_LECTORA") {
    chip(
     "Comprensión lectora · ${challenge.estimatedMinutes} min · +${challenge.experienceReward} XP",
     Color.rgb(227, 240, 248),
     Color.rgb(147, 196, 223),
     RupiUi.SKY_DARK
    )
   } else {
    chip(
     "Razonamiento lógico · ${challenge.estimatedMinutes} min · +${challenge.experienceReward} XP",
     Color.rgb(255, 227, 217),
     Color.rgb(240, 160, 140),
     Color.rgb(138, 42, 18)
    )
   }
   text(challenge.title, 22)
   if (challenge.description.isNotBlank()) muted(challenge.description)
   listen("el reto") {
    challenge.title + ". " + challenge.description +
     (if (challenge.readingText.isNotBlank()) " Lectura. ${challenge.readingText}" else "")
   }
  }

  if (state.completion != null || challenge.isCompleted) {
   feedback(
    state.completion?.rupiMessage ?: "¡Ya completaste el reto de hoy! Mañana Rupi te trae uno nuevo.",
    NoticeTone.OK
   )
   return@rebuildBody
  }

  if (challenge.readingText.isNotBlank()) {
   readingCard {
    text("Lectura", 20)
    challenge.readingText.split("\n\n").forEach { text(it) }
    listen("la lectura") { challenge.readingText }
   }
  }

  challenge.questions.forEachIndexed { i, q ->
   card {
    text("${i + 1}. ${q.statement}", 20)
    listen("la pregunta ${i + 1}") { RupiVoice.question(q.statement, q.options.map { it.text }) }
    val fb = state.answers[q.id]
    val group = RadioGroup(context)
    val ids = mutableMapOf<Int, String>()
    q.options.forEachIndexed { j, option ->
     val radio = RadioButton(context).apply {
      id = j + 1
      text = if (fb != null && option.id == fb.correctOptionId) {
       iconLabel(R.drawable.ic_check, RupiUi.LEAF_DARK, option.text, iconDp = 16)
      } else {
       option.text
      }
      minHeight = dp(48)
      minWidth = dp(48)
      setSingleLine(false)
      setHorizontallyScrolling(false)
      setPadding(dp(12), dp(12), dp(12), dp(12))
      isEnabled = fb == null
      setTextColor(RupiUi.INK)
     }
     styleOption(radio)
     ids[j + 1] = option.id
     group.addView(radio)
    }
    addView(group)
    if (fb != null) {
     val explanation = fb.explanation.ifBlank { q.explanation }
     feedback(
      (if (fb.isCorrect) "¡Muy bien! " else "Sigamos aprendiendo. ") + explanation,
      if (fb.isCorrect) NoticeTone.OK else NoticeTone.RETRY
     )
     listen("la explicación ${i + 1}") { explanation }
    } else {
     val submit = button("Comprobar mi respuesta", enabled = false) {
      val selected = ids[group.checkedRadioButtonId] ?: return@button
      onSubmitAnswer(challenge.activityVersionId, q.id, selected)
     }
     group.setOnCheckedChangeListener { _, checked -> submit.isEnabled = ids.containsKey(checked) }
    }
   }
  }

  val ready = challenge.questions.all { state.answers.containsKey(it.id) }
  button(
   label = if (ready) "Terminar el reto" else "Responde las ${challenge.questions.size} preguntas para terminar",
   enabled = ready
  ) { onCompleteChallenge(challenge.id) }
 }
}
