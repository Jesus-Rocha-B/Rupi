package pe.rupi.mobile.ui.screens

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.text.InputFilter
import android.view.MotionEvent
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import pe.rupi.mobile.RupiUi
import pe.rupi.mobile.RupiVoice
import pe.rupi.mobile.data.model.QuestionHint
import pe.rupi.mobile.data.model.SavedVisualSummary
import pe.rupi.mobile.data.model.StudyTopic
import pe.rupi.mobile.data.model.VisualSummaryResource
import pe.rupi.mobile.ui.components.ScreenScaffold
import pe.rupi.mobile.ui.state.AiPracticeUiState
import pe.rupi.mobile.ui.state.FlashcardsUiState
import pe.rupi.mobile.ui.state.NoticeTone
import pe.rupi.mobile.ui.state.SavedSummariesUiState
import pe.rupi.mobile.ui.state.VisualSummaryUiState
import kotlin.math.abs

fun renderAiPracticeBody(
 context: Context,
 scaffold: ScreenScaffold,
 state: AiPracticeUiState,
 onBackToLesson: (StudyTopic) -> Unit,
 onRefresh: (StudyTopic) -> Unit,
 onOpenFlashcards: (StudyTopic) -> Unit,
 onStartPractice: (StudyTopic) -> Unit,
 onSubmitAnswer: (StudyTopic, practiceId: String, questionId: String, optionId: String) -> Unit,
 onRequestHint: (practiceId: String, questionId: String) -> Unit,
 onReportQuestion: (StudyTopic, practiceId: String, questionId: String, reason: String) -> Unit
) {
 val topic = state.topic ?: return
 scaffold.rebuildBody(preserveScroll = true) {
  backButton("Volver a la lección") { onBackToLesson(topic) }
  button("Consultar mi práctica", kind = RupiUi.Kind.QUIET) { onRefresh(topic) }
  button("Repasar con tarjetas", kind = RupiUi.Kind.LEAF) { onOpenFlashcards(topic) }

  val status = state.status ?: return@rebuildBody
  val practice = status.practice
  text("Práctica formativa: no cambia tus calificaciones.")
  if (!status.configured) {
   text("La generación con IA todavía no está habilitada. Podrás usar cuestionarios revisados cuando estén disponibles.")
  }
  if (practice != null) {
   when (practice.status) {
    "GENERANDO" -> {
     text("Preparando preguntas. Consulta tu práctica en unos segundos.")
     return@rebuildBody
    }
    "FALLIDO" -> {
     text("No pudimos preparar el cuestionario. Puedes seguir estudiando tu lección.")
    }
    "RETIRADO" -> {
     text("Cuestionario retirado para revisión docente.")
    }
    else -> {
     text(
      if (practice.source == "REUTILIZADO") {
       "Cuestionario existente revisado por docente."
      } else {
       "Cuestionario asistido por IA · Puede contener errores."
      }
     )
     text(
      if (practice.reviewState == "REVISADO_DOCENTE") {
       "Revisión docente realizada."
      } else {
       "Validación automática; no sustituye la revisión docente."
      }
     )
     practice.questions.forEach { q ->
      card {
       text(q.text, 20)
       val group = RadioGroup(context)
       listen("la pregunta") { RupiVoice.question(q.text, q.options.map { it.text }) }
       val ids = mutableMapOf<Int, String>()
       val answered = q.selectedOptionId != null
       q.options.forEachIndexed { j, option ->
        val radio = RadioButton(context).apply {
         id = j + 1
         text = option.text
         minHeight = dp(48)
         minWidth = dp(48)
         setSingleLine(false)
         setHorizontallyScrolling(false)
         setPadding(dp(12), dp(12), dp(12), dp(12))
         isEnabled = !answered
         setTextColor(RupiUi.INK)
        }
        styleOption(radio)
        ids[j + 1] = option.id
        group.addView(radio)
        if (answered && option.id == q.selectedOptionId) group.check(j + 1)
       }
       addView(group)
       if (answered) {
        feedback(
         (if (q.isCorrect) "¡Buen trabajo! " else "Sigamos aprendiendo. ") + q.explanation,
         if (q.isCorrect) NoticeTone.OK else NoticeTone.RETRY
        )
        listen("la explicación") { q.explanation }
        if (!q.isCorrect) {
         button("¿Por qué me equivoqué?", kind = RupiUi.Kind.SKY) {
          onRequestHint(practice.practiceId, q.id)
         }
        }
       } else {
        val submit = button("Comprobar mi respuesta", enabled = false) {
         val selected = ids[group.checkedRadioButtonId] ?: return@button
         onSubmitAnswer(topic, practice.practiceId, q.id, selected)
        }
        group.setOnCheckedChangeListener { _, checked -> submit.isEnabled = ids.containsKey(checked) }
       }
       button("Reportar esta pregunta", kind = RupiUi.Kind.DANGER) {
        showReportDialog(context, title = "Revisar pregunta") { reason ->
         onReportQuestion(topic, practice.practiceId, q.id, reason)
        }
       }
      }
     }
     if (practice.status == "COMPLETADO") {
      feedback(
       "¡Terminaste! ${practice.correctCount} de ${practice.questions.size} aciertos. Revisa las explicaciones.",
       NoticeTone.OK
      )
     }
    }
   }
  }
  if (practice == null || practice.status !in setOf("LISTO", "GENERANDO")) {
   button("Practicar con Rupi", enabled = status.configured || status.reviewedAvailable) {
    onStartPractice(topic)
   }
  }
 }
}

fun showHintDialog(
 context: Context,
 hint: QuestionHint,
 voice: RupiVoice?,
 onTtsUnavailable: () -> Unit
) {
 val level = when (hint.level) {
  "INICIAL" -> "1.° y 2.° grado"
  "INTERMEDIO" -> "3.° y 4.° grado"
  else -> "5.° y 6.° grado"
 }
 val body = StringBuilder(hint.hint).append("\n")
 hint.steps.forEachIndexed { i, step -> body.append("\n").append(i + 1).append(". ").append(step) }
 hint.realLifeExample?.let { body.append("\n\nEjemplo de la vida real:\n").append(it) }
 body.append("\n\n").append(
  if (hint.source == "IA") "Pista asistida por IA · revisada automáticamente."
  else "Basada en la explicación revisada de la pregunta."
 )
 AlertDialog.Builder(context)
  .setTitle("Rupi te explica · $level" + if (hint.reinforced) " · paso a paso" else "")
  .setMessage(body.toString())
  .setPositiveButton("Entendido", null)
  .setNeutralButton("Escuchar") { _, _ ->
   if (voice?.speak(body.toString()) != true) onTtsUnavailable()
  }
  .show()
}

@SuppressLint("ClickableViewAccessibility")
fun renderFlashcardsBody(
 scaffold: ScreenScaffold,
 state: FlashcardsUiState,
 onBackToTopic: (StudyTopic) -> Unit,
 onRefresh: (StudyTopic) -> Unit,
 onAction: (StudyTopic, revision: Long, action: String, cardId: String) -> Unit
) {
 val topic = state.topic ?: return
 scaffold.rebuildBody(preserveScroll = true) {
  backButton("Volver al tema") { onBackToTopic(topic) }
  button("Recuperar mi repaso", kind = RupiUi.Kind.QUIET) { onRefresh(topic) }

  val deck = state.deck ?: return@rebuildBody
  card {
   text("Tarjetas de estudio", 20)
   muted("Piensa en la respuesta y voltea la tarjeta. Tu autoevaluación no cambia tus notas.")
   val cards = deck.cards
   if (cards.isEmpty()) {
    muted("Todavía no hay tarjetas revisadas para esta actividad.")
    return@card
   }
   val index = cards.indexOfFirst { it.id == deck.currentCardId }.let { if (it >= 0) it else 0 }
   val card = cards[index]
   val revealed = deck.revealed
   fun move(delta: Int) {
    val next = index + delta
    if (next in cards.indices) {
     onAction(topic, deck.revision, "NAVIGATE", cards[next].id)
    }
   }

   val stateLabel = when (card.state) {
    "PRACTICAR" -> "Necesito practicar"
    "RECUERDO" -> "Lo recuerdo"
    else -> "Por descubrir"
   }
   chip("Tarjeta ${index + 1} de ${cards.size} · $stateLabel", RupiUi.SUN, RupiUi.FOREST, RupiUi.FOREST)

   val face = flashcardButton(
    label = (if (revealed) "Respuesta:\n\n" else "Pregunta:\n\n") + (if (revealed) card.answer else card.question),
    revealed = revealed
   ) {
    onAction(topic, deck.revision, if (revealed) "NAVIGATE" else "REVEAL", card.id)
   }

   var startX = 0f
   var startY = 0f
   var dragged = false
   face.setOnTouchListener { _, event ->
    when (event.actionMasked) {
     MotionEvent.ACTION_DOWN -> {
      startX = event.x
      startY = event.y
      dragged = false
      false
     }
     MotionEvent.ACTION_MOVE -> {
      val dx = event.x - startX
      val dy = event.y - startY
      if (abs(dx) > dp(10) || abs(dy) > dp(10)) dragged = true
      if (abs(dx) > abs(dy) * 1.5 && abs(dx) > dp(10)) {
       face.parent.requestDisallowInterceptTouchEvent(true)
      }
      dragged
     }
     MotionEvent.ACTION_UP -> {
      val dx = event.x - startX
      val dy = event.y - startY
      face.parent.requestDisallowInterceptTouchEvent(false)
      face.isPressed = false
      if (!scaffold.isBusy && abs(dx) > dp(60) && abs(dx) > abs(dy) * 1.5) {
       move(if (dx < 0) 1 else -1)
       true
      } else {
       if (!dragged) face.performClick()
       true
      }
     }
     MotionEvent.ACTION_CANCEL -> {
      dragged = true
      face.isPressed = false
      face.parent.requestDisallowInterceptTouchEvent(false)
      true
     }
     else -> false
    }
   }

   listen(if (revealed) "la respuesta" else "la pregunta") { if (revealed) card.answer else card.question }
   button(if (revealed) "Ver pregunta" else "Ver respuesta", kind = RupiUi.Kind.STICKER) {
    onAction(topic, deck.revision, if (revealed) "NAVIGATE" else "REVEAL", card.id)
   }
   button("Anterior", index > 0, kind = RupiUi.Kind.QUIET) { move(-1) }
   button("Siguiente", index < cards.size - 1, kind = RupiUi.Kind.QUIET) { move(1) }
   if (revealed) {
    button("Lo recuerdo", kind = RupiUi.Kind.LEAF) { onAction(topic, deck.revision, "REMEMBER", card.id) }
    button("Necesito practicar", kind = RupiUi.Kind.SUN) { onAction(topic, deck.revision, "PRACTICE", card.id) }
   }
   muted("Fuente indicada por tu docente: ${card.source}")
   muted("Primero repasamos lo que necesita práctica; después las tarjetas nuevas y lo que recuerdas.")
   button("Volver a repasar", kind = RupiUi.Kind.QUIET) { onAction(topic, deck.revision, "RESTART", card.id) }
  }
 }
}

fun renderVisualSummaryBody(
 context: Context,
 scaffold: ScreenScaffold,
 state: VisualSummaryUiState,
 onBackToTopic: (StudyTopic) -> Unit,
 onRefresh: (StudyTopic) -> Unit,
 onGenerate: (StudyTopic) -> Unit,
 onSaveOffline: (StudyTopic, resourceId: String) -> Unit,
 onReport: (StudyTopic, resourceId: String, reason: String) -> Unit
) {
 val topic = state.topic ?: return
 scaffold.rebuildBody(preserveScroll = true) {
  backButton("Volver al tema") { onBackToTopic(topic) }
  button("Comprobar versión y revisión", kind = RupiUi.Kind.QUIET) { onRefresh(topic) }
  val lookup = state.lookup ?: return@rebuildBody
  val resource = lookup.resource
  if (resource != null) {
   if (lookup.invalidatedPreviousCopy) {
    text("La copia anterior cambió o fue retirada. Comprueba este recurso antes de guardarlo nuevamente.")
   }
   renderVisualSummaryResource(context, scaffold, topic, resource, lookup.offline, onSaveOffline, onReport)
  } else {
   text("No hay un resumen disponible para esta versión del tema.")
  }
  if (!lookup.offline && lookup.configured && (resource == null || resource.state == "FALLIDO")) {
   button("Preparar resumen visual") { onGenerate(topic) }
  }
  if (!lookup.configured && resource == null) {
   text("La IA todavía no está configurada. Puedes continuar tu lección.")
  }
 }
}

fun renderSavedSummariesBody(
 scaffold: ScreenScaffold,
 state: SavedSummariesUiState,
 onBack: () -> Unit,
 onOpenSaved: (SavedVisualSummary) -> Unit
) {
 scaffold.rebuildBody(preserveScroll = false) {
  muted("Copias guardadas en este dispositivo. Pueden estar desactualizadas; vuelve al tema con conexión para comprobar su vigencia.")
  backButton("Volver") { onBack() }
  val items = state.result.items
  if (items.isEmpty() && state.result.unreadableCount == 0) {
   text("Todavía no guardaste resúmenes.")
  }
  items.forEach { saved ->
   button(saved.topic.title, kind = RupiUi.Kind.LEAF) { onOpenSaved(saved) }
  }
  repeat(state.result.unreadableCount) {
   text("No pudimos leer una descarga.")
  }
 }
}

fun renderSavedSummaryDetailBody(
 context: Context,
 scaffold: ScreenScaffold,
 saved: SavedVisualSummary,
 isAuthenticated: Boolean,
 onBackToSavedList: () -> Unit,
 onVerifyOnline: (StudyTopic) -> Unit
) {
 scaffold.rebuildBody(preserveScroll = false) {
  backButton("Volver a mis descargas") { onBackToSavedList() }
  if (isAuthenticated) {
   button("Comprobar vigencia", kind = RupiUi.Kind.QUIET) { onVerifyOnline(saved.topic) }
  }
  renderVisualSummaryResource(
   context = context,
   scaffold = scaffold,
   topic = saved.topic,
   resource = saved.resource,
   offline = true,
   onSaveOffline = { _, _ -> },
   onReport = { _, _, _ -> }
  )
 }
}

private fun renderVisualSummaryResource(
 context: Context,
 scaffold: ScreenScaffold,
 topic: StudyTopic,
 resource: VisualSummaryResource,
 offline: Boolean,
 onSaveOffline: (StudyTopic, resourceId: String) -> Unit,
 onReport: (StudyTopic, resourceId: String, reason: String) -> Unit
) {
 if (offline) {
  scaffold.feedback("Copia descargada. No pudimos comprobar su vigencia. Puede haber una versión nueva o estar retirada.", NoticeTone.INFO)
 }
 val content = resource.content
 if (content == null) {
  scaffold.card {
   muted(
    when (resource.state) {
     "GENERANDO" -> "Preparando el resumen. Consulta nuevamente en unos segundos."
     "FALLIDO" -> "No pudimos preparar el resumen. Continúa tu lección."
     else -> "Resumen retirado o pendiente de revisión."
    }
   )
  }
  return
 }
 scaffold.heroCard {
  chip("Contenido asistido por IA · ${resource.grade}.º de primaria", RupiUi.LEAF_SOFT, RupiUi.SOFT_LINE, RupiUi.FOREST)
  muted(
   if (resource.state == "REVISADO_DOCENTE") "Revisado por docente." else "Validación automática; puede contener errores."
  )
  text(content.title, 24)
  listen("el resumen") {
   val parts = mutableListOf(content.title)
   content.concepts.forEach { parts.add("${it.title}: ${it.text}") }
   content.steps.forEachIndexed { i, step -> parts.add("Paso ${i + 1}: $step") }
   parts.add("Ejemplo explicado: ${content.example}")
   parts.add("Para recordar: ${content.remember}")
   parts.joinToString(" ")
  }
 }
 content.concepts.forEachIndexed { i, concept ->
  scaffold.card {
   text("${i + 1}. ${concept.title}", 20)
   text(concept.text, 17)
  }
 }
 scaffold.card {
  text("Paso a paso", 20)
  content.steps.forEachIndexed { i, step -> text("${i + 1}. $step", 17) }
 }
 scaffold.accentCard(RupiUi.SKY) {
  text("Ejemplo explicado", 20)
  text(content.example, 17)
 }
 scaffold.accentCard(RupiUi.SUN) {
  text("Para recordar", 20)
  text(content.remember, 17)
  muted("Fuente: ${resource.source}")
 }
 if (!offline) {
  scaffold.button("Guardar para consultar sin conexión", kind = RupiUi.Kind.LEAF) {
   onSaveOffline(topic, resource.id)
  }
  scaffold.button("Reportar contenido incorrecto", kind = RupiUi.Kind.DANGER) {
   showReportDialog(context, title = "Revisar resumen") { reason ->
    onReport(topic, resource.id, reason)
   }
  }
 }
}

private fun showReportDialog(
 context: Context,
 title: String,
 onSubmit: (String) -> Unit
) {
 val reason = EditText(context).apply {
  hint = "¿Qué debemos revisar?"
  minHeight = RupiUi.dp(context, 48)
  filters = arrayOf(InputFilter.LengthFilter(500))
 }
 AlertDialog.Builder(context)
  .setTitle(title)
  .setView(reason)
  .setNegativeButton("Cancelar", null)
  .setPositiveButton("Enviar") { _, _ ->
   val text = reason.text.toString()
   if (text.isNotBlank()) onSubmit(text)
  }
  .show()
}
