package pe.rupi.mobile.ui.screens

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import pe.rupi.mobile.R
import pe.rupi.mobile.RupiUi
import pe.rupi.mobile.RupiVoice
import pe.rupi.mobile.data.model.ActivityType
import pe.rupi.mobile.data.model.LearningRouteSummary
import pe.rupi.mobile.data.model.LessonBlockType
import pe.rupi.mobile.data.model.RouteNode
import pe.rupi.mobile.data.model.RouteNodeState
import pe.rupi.mobile.data.model.StudyTopic
import pe.rupi.mobile.ui.components.ScreenScaffold
import pe.rupi.mobile.ui.state.LessonUiState
import pe.rupi.mobile.ui.state.NoticeTone
import pe.rupi.mobile.ui.state.RouteDetailUiState

fun renderRouteDetailBody(
 context: Context,
 scaffold: ScreenScaffold,
 state: RouteDetailUiState,
 onBackToTopics: () -> Unit,
 onOpenNode: (LearningRouteSummary, RouteNode) -> Unit,
 onBlockedNode: () -> Unit
) {
 scaffold.rebuildBody(preserveScroll = true) {
  backButton("Volver a mis temas") { onBackToTopics() }
  val detail = state.detail ?: return@rebuildBody
  val route = state.route ?: detail.route
  val summary = detail.route

  heroCard {
   val areaGrade = listOf(summary.areaName, summary.gradeName).filter { it.isNotBlank() }.joinToString(" · ")
   chip(areaGrade, Color.rgb(238, 246, 251), Color.rgb(147, 196, 223), RupiUi.SKY_DARK)
   text(summary.title, 22)
   val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
    max = summary.totalNodes.coerceAtLeast(1)
    progress = summary.completedNodes
    progressTintList = ColorStateList.valueOf(RupiUi.LEAF)
    progressBackgroundTintList = ColorStateList.valueOf(RupiUi.PAPER_DEEP)
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
   }
   addView(bar, LinearLayout.LayoutParams(-1, dp(16)).apply { topMargin = dp(8) })
   muted(
    if (summary.totalNodes > 0 && summary.completedNodes >= summary.totalNodes) {
     "¡Terminaste esta ruta! Puedes repasar cualquier parada."
    } else {
     "${summary.completedNodes} de ${summary.totalNodes} paradas completadas."
    }
   )
  }

  detail.unitGroups.forEach { group ->
   section(group.title)
   group.nodes.forEach { node ->
    renderNodeTile(scaffold, route, node, onOpenNode, onBlockedNode)
   }
  }
  if (detail.ungroupedNodes.isNotEmpty()) {
   if (detail.unitGroups.isNotEmpty()) section("Otras paradas")
   detail.ungroupedNodes.forEach { node ->
    renderNodeTile(scaffold, route, node, onOpenNode, onBlockedNode)
   }
  }
 }
}

private fun renderNodeTile(
 scaffold: ScreenScaffold,
 route: LearningRouteSummary,
 node: RouteNode,
 onOpenNode: (LearningRouteSummary, RouteNode) -> Unit,
 onBlockedNode: () -> Unit
) {
 scaffold.routeStopTile(
  sequence = node.sequence,
  title = node.title,
  stateLabel = node.state.label,
  estimatedMinutes = node.estimatedMinutes,
  completed = node.state == RouteNodeState.COMPLETADO,
  locked = node.state == RouteNodeState.BLOQUEADO
 ) {
  if (node.state == RouteNodeState.BLOQUEADO) onBlockedNode() else onOpenNode(route, node)
 }
}

fun renderLessonBody(
 context: Context,
 scaffold: ScreenScaffold,
 state: LessonUiState,
 onBackToRoute: (LearningRouteSummary) -> Unit,
 onOpenExternalLink: (String) -> Unit,
 onSubmitAnswer: (activityId: String, questionId: String, optionId: String) -> Unit,
 onCompleteNode: () -> Unit,
 onOpenNextNode: (LearningRouteSummary, RouteNode) -> Unit,
 onOpenAiPractice: (StudyTopic) -> Unit,
 onOpenFlashcards: (StudyTopic) -> Unit
) {
 val route = state.route ?: return
 val node = state.node ?: return
 scaffold.rebuildBody(preserveScroll = true) {
  backButton("Volver a la ruta") { onBackToRoute(route) }
  val lesson = state.lesson ?: return@rebuildBody

  stickerCard {
   chip("Parada ${node.sequence}", RupiUi.SUN, RupiUi.FOREST, RupiUi.FOREST)
   chip(
    "${node.activityType.label} · ${route.title}",
    RupiUi.LEAF_SOFT,
    Color.rgb(148, 208, 161),
    RupiUi.LEAF_DARK
   )
   text(lesson.title.ifBlank { node.title }, 22)
   if (lesson.instructions != null) muted(lesson.instructions)
   listen("la lección") {
    (listOf(lesson.title) +
     listOfNotNull(lesson.instructions) +
     lesson.blocks.mapNotNull { it.text }).joinToString(". ")
   }
  }

  lesson.blocks.forEach { block ->
   val link = block.resourceUrl
   val body = block.text
   when {
    block.type == LessonBlockType.VIDEO && link != null -> card {
     text("Video de la lección", 20)
     if (body != null) muted(body)
     button("Ver el video", kind = RupiUi.Kind.SKY, iconRes = R.drawable.ic_play) { onOpenExternalLink(link) }
    }
    link != null || block.type == LessonBlockType.ADJUNTO -> card {
     chip("Material del profesor", Color.rgb(255, 248, 230), RupiUi.SUN, Color.rgb(107, 71, 5))
     text(body ?: "Material de clase", 18)
     block.accessibleText?.let { muted(it) }
     if (link != null) button("Abrir material", kind = RupiUi.Kind.QUIET) { onOpenExternalLink(link) }
    }
    block.type == LessonBlockType.DESTACADO && body != null -> feedback(body, NoticeTone.INFO)
    body != null -> card { body.split("\n\n").forEach { text(it, 17) } }
   }
  }

  if (lesson.questions.isNotEmpty()) {
   section("Preguntas", "Responde para comprobar lo que aprendiste. No cambia tus notas si te equivocas.")
  }

  lesson.questions.forEachIndexed { index, q ->
   card {
    text("${index + 1}. ${q.statement}", 20)
    listen("la pregunta ${index + 1}") { RupiVoice.question(q.statement, q.options.map { it.text }) }
    val answer = state.answers[q.id]
    val group = RadioGroup(context)
    val ids = mutableMapOf<Int, String>()
    q.options.forEachIndexed { j, option ->
     val radio = RadioButton(context).apply {
      id = j + 1
      text = if (answer != null && option.id == answer.correctOptionId) {
       iconLabel(R.drawable.ic_check, RupiUi.LEAF_DARK, option.text, iconDp = 16)
      } else {
       option.text
      }
      minWidth = dp(48)
      setSingleLine(false)
      setHorizontallyScrolling(false)
      setPadding(dp(12), dp(12), dp(12), dp(12))
      isEnabled = answer == null
      setTextColor(RupiUi.INK)
     }
     styleOption(radio)
     ids[j + 1] = option.id
     group.addView(radio)
    }
    addView(group)
    if (answer != null) {
     val explanation = answer.explanation.ifBlank { q.explanation }
     feedback(
      (if (answer.isCorrect) "¡Muy bien! " else "Sigamos aprendiendo. ") + explanation,
      if (answer.isCorrect) NoticeTone.OK else NoticeTone.RETRY
     )
     listen("la explicación ${index + 1}") { explanation }
    } else {
     val submit = button("Comprobar mi respuesta", enabled = false) {
      val selected = ids[group.checkedRadioButtonId] ?: return@button
      onSubmitAnswer(lesson.activityId, q.id, selected)
     }
     group.setOnCheckedChangeListener { _, checked -> submit.isEnabled = ids.containsKey(checked) }
    }
   }
  }

  val result = state.completionResult
  if (result != null) {
   val xp = result.experienceEarned
   val message = result.rupiMessage
   feedback(
    message + (if (xp > 0 && !message.contains("XP")) " +$xp XP" else ""),
    NoticeTone.OK
   )
   result.badgeName?.let { badge ->
    chip("Insignia: $badge", Color.rgb(255, 248, 230), RupiUi.SUN, Color.rgb(107, 71, 5), iconRes = R.drawable.ic_award)
   }
   if (result.isRouteCompleted) {
    val bonus = result.routeBonusExperience.takeIf { it > 0 }?.let { "+$it XP de bonificación." } ?: ""
    feedback("¡Terminaste toda la ruta! $bonus", NoticeTone.OK)
   }
   result.nextUnlockedNode?.let { next ->
    button("Ir a la siguiente parada: ${next.title}") {
     onOpenNextNode(route, next)
    }
   }
  } else {
   val answeredAll = lesson.questions.all { state.answers.containsKey(it.id) }
   val completed = lesson.state == "COMPLETADO" || node.state == RouteNodeState.COMPLETADO
   if (completed && !answeredAll) {
    feedback("Ya completaste esta parada. Puedes repasarla cuando quieras.", NoticeTone.OK)
   } else {
    button(
     label = if (answeredAll) "Terminar parada" else "Responde las ${lesson.questions.size} preguntas para terminar",
     enabled = answeredAll
    ) { onCompleteNode() }
   }
  }

  section("Más para repasar", "Recursos extra de esta parada: aparecen cuando tu profe los prepara o la IA está activa.")
  val topic = StudyTopic(
   routeId = route.versionRouteId,
   nodeId = node.id,
   title = node.title,
   routeTitle = route.title,
   route = route
  )
  button("Practicar con Rupi (IA)", kind = RupiUi.Kind.LEAF) { onOpenAiPractice(topic) }
  button("Repasar con tarjetas", kind = RupiUi.Kind.QUIET) { onOpenFlashcards(topic) }
 }
}
