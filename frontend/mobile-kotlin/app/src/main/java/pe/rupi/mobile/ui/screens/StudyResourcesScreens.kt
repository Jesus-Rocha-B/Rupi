package pe.rupi.mobile.ui.screens

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputFilter
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import pe.rupi.mobile.R
import pe.rupi.mobile.RupiUi
import pe.rupi.mobile.RupiVoice
import pe.rupi.mobile.data.model.FlashcardItem
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

data class DuolingoFlashcard(
 val id: String,
 val question: String,
 val answer: String,
 val explanation: String,
 val options: List<String>,
 val correctIndex: Int,
 val source: String,
 val badge: String = "CONCEPTO CLAVE · AYACUCHO"
)

private class DuolingoFlashcardSession(
 val topicKey: String,
 val cards: List<DuolingoFlashcard>
) {
 var index: Int = 0
 var selectedOptionIndex: Int? = null
 var isRevealed: Boolean = false
 var isChecked: Boolean = false
 var isCorrect: Boolean = false
 var correctCount: Int = 0
 var totalXp: Int = 0
 var isCompleted: Boolean = false
}

private var currentFlashcardSession: DuolingoFlashcardSession? = null

private fun buildCurriculumFlashcards(topic: StudyTopic): List<DuolingoFlashcard> {
 val t = (topic.title + " " + topic.routeTitle).lowercase()
 return when {
  t.contains("ecosistema") || t.contains("biodiversidad") || t.contains("ambiente") || t.contains("ciencia") -> listOf(
   DuolingoFlashcard(
    id = "eco-1",
    question = "¿Cuál es el ave nacional del Perú que habita en los bosques de neblina andinos?",
    answer = "El Gallito de las Rocas (Tunki)",
    explanation = "El Tunki es el ave nacional del Perú y protege los bosques de neblina andinos.",
    options = listOf("El Gallito de las Rocas (Tunki)", "El Pingüino de Humboldt", "La Paloma torcaza"),
    correctIndex = 0,
    source = "Ciencia y Tecnología · MINEDU",
    badge = "BIODIVERSIDAD · AYACUCHO"
   ),
   DuolingoFlashcard(
    id = "eco-2",
    question = "¿Qué región natural se caracteriza por la cordillera de los Andes y valles interandinos?",
    answer = "La Región Sierra o Andina",
    explanation = "La Sierra comprende la cordillera de los Andes y sus valles fértiles como Huamanga.",
    options = listOf("El Mar de Grau", "La Región Sierra o Andina", "La Selva Baja"),
    correctIndex = 1,
    source = "Geografía Peruana · 3.° Primaria",
    badge = "GEOGRAFÍA ANDINA"
   ),
   DuolingoFlashcard(
    id = "eco-3",
    question = "¿Por qué debemos conservar la flora y fauna nativa de nuestra comunidad?",
    answer = "Para mantener el equilibrio de la naturaleza y la vida",
    explanation = "Los ecosistemas equilibrados garantizan agua limpia y aire puro para todas las familias.",
    options = listOf("Para explotar todos los recursos", "Para mantener el equilibrio de la naturaleza y la vida", "No tiene importancia"),
    correctIndex = 1,
    source = "Cuidado Ambiental · Perú",
    badge = "ECOLOGÍA ESCOLAR"
   )
  )
  t.contains("fracci") || t.contains("medida") || t.contains("número") || t.contains("matemát") -> listOf(
   DuolingoFlashcard(
    id = "mat-1",
    question = "Si en el mercado de Huamanga divides un queso en 4 partes iguales y tomas 1, ¿qué fracción representa?",
    answer = "1/4 (un cuarto)",
    explanation = "El numerador 1 indica la parte tomada y el 4 las partes iguales de la unidad.",
    options = listOf("1/2 (un medio)", "1/4 (un cuarto)", "3/4 (tres cuartos)"),
    correctIndex = 1,
    source = "Matemática Vivencial · 3.° Primaria",
    badge = "FRACCIONES ANDINAS"
   ),
   DuolingoFlashcard(
    id = "mat-2",
    question = "¿Cuál es la unidad oficial para medir el peso de la quinua o papa en el mercado?",
    answer = "El kilogramo (kg)",
    explanation = "El kilogramo es la unidad estándar de masa utilizada en los mercados peruanos.",
    options = listOf("El metro (m)", "El litro (l)", "El kilogramo (kg)"),
    correctIndex = 2,
    source = "Medidas y Magnitudes · MINEDU",
    badge = "MEDIDAS DE MERCADO"
   ),
   DuolingoFlashcard(
    id = "mat-3",
    question = "¿Cuántos cuartos de kilo (1/4 kg) equivalen exactamente a 1 kilogramo completo?",
    answer = "4 cuartos de kilo",
    explanation = "Cuatro cuartos forman 4/4, lo cual equivale a la unidad entera (1 kg).",
    options = listOf("2 cuartos de kilo", "4 cuartos de kilo", "8 cuartos de kilo"),
    correctIndex = 1,
    source = "Equivalencias Matemáticas",
    badge = "EQUIVALENCIAS"
   )
  )
  else -> listOf(
   DuolingoFlashcard(
    id = "com-1",
    question = "¿Cuál es el propósito principal de la idea principal en un texto escolar?",
    answer = "Transmitir el mensaje central del autor",
    explanation = "La idea principal expresa la afirmación indispensable de la lectura.",
    options = listOf("Transmitir el mensaje central del autor", "Ser la oración más larga", "Incluir dibujos y colores"),
    correctIndex = 0,
    source = "Comunicación Integral · 3.° Primaria",
    badge = "COMPRENSIÓN LECTORA"
   ),
   DuolingoFlashcard(
    id = "com-2",
    question = "¿Qué signos de puntuación se emplean al inicio y al final de una pregunta en español?",
    answer = "Signos de interrogación (¿ ?)",
    explanation = "En nuestro idioma el signo ¿ abre la pregunta y el signo ? la cierra.",
    options = listOf("Signos de admiración (¡ !)", "Signos de interrogación (¿ ?)", "Puntos suspensivos (...)"),
    correctIndex = 1,
    source = "Ortografía Escolar",
    badge = "SIGNOS DE PUNTUACIÓN"
   ),
   DuolingoFlashcard(
    id = "com-3",
    question = "¿Cómo se clasifica un sustantivo que nombra a un lugar o persona única, como Ayacucho o Mateo?",
    answer = "Sustantivo propio",
    explanation = "Los sustantivos propios distinguen a un ser o lugar de los demás y se escriben con mayúscula.",
    options = listOf("Sustantivo propio", "Sustantivo común", "Verbo conjugado"),
    correctIndex = 0,
    source = "Gramática Escolar · Primaria",
    badge = "GRAMÁTICA ANDINA"
   )
  )
 }
}

private fun mapBackendCards(cards: List<FlashcardItem>, topic: StudyTopic): List<DuolingoFlashcard> {
 return cards.mapIndexed { idx, card ->
  val distractor1 = "Detalle complementario de la lección"
  val distractor2 = "Concepto secundario del tema"
  val opts = listOf(card.answer, distractor1, distractor2)
  DuolingoFlashcard(
   id = card.id,
   question = card.question,
   answer = card.answer,
   explanation = "Respuesta correcta indicada por tu docente: ${card.answer}",
   options = opts,
   correctIndex = 0,
   source = card.source.ifBlank { "Docente de aula" },
   badge = "TARJETA ${idx + 1} · AYACUCHO"
  )
 }
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
 val context = scaffold.context
 val dp = { n: Int -> scaffold.dp(n) }

 val topicKey = "${topic.routeId}_${topic.nodeId}"
 var session = currentFlashcardSession
 if (session == null || session.topicKey != topicKey) {
  val deck = state.deck
  val items = if (deck != null && deck.cards.isNotEmpty()) {
   mapBackendCards(deck.cards, topic)
  } else {
   buildCurriculumFlashcards(topic)
  }
  session = DuolingoFlashcardSession(topicKey, items)
  currentFlashcardSession = session
 }

 fun refreshScreen() {
  renderFlashcardsBody(scaffold, state, onBackToTopic, onRefresh, onAction)
 }

 scaffold.rebuildBody(preserveScroll = false) {
  if (session.isCompleted) {
   // PANTALLA DE VICTORIA (IMAGEN 3 DUOLINGO)
   val victoryBox = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER_HORIZONTAL
    setPadding(dp(16), dp(24), dp(16), dp(24))
   }

   victoryBox.addView(ImageView(context).apply {
    setImageResource(R.drawable.ic_rupi_mascot)
    scaleType = ImageView.ScaleType.FIT_CENTER
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
   }, LinearLayout.LayoutParams(dp(110), dp(115)))

   victoryBox.addView(TextView(context).apply {
    text = "¡Lección Completada!"
    typeface = RupiUi.fontTitle(context)
    textSize = 24f
    setTextColor(Color.rgb(217, 119, 6))
    gravity = Gravity.CENTER
    setPadding(0, dp(12), 0, dp(4))
   })

   victoryBox.addView(TextView(context).apply {
    text = "¡Excelente trabajo repasando tus conceptos escolares en Ayacucho!"
    typeface = RupiUi.fontBold(context)
    textSize = 14.5f
    setTextColor(RupiUi.FOREST)
    gravity = Gravity.CENTER
    setPadding(0, 0, 0, dp(24))
   })

   // Tarjetas dobles de estadística: TOTAL XP y PRECISIÓN
   val statsRow = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER
   }

   // Tarjeta 1: TOTAL XP (Oro / Amarillo)
   val xpCard = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER
    background = RupiUi.duolingoStatCard(context, isGold = true)
    setPadding(dp(12), dp(10), dp(12), dp(14))
   }
   xpCard.addView(TextView(context).apply {
    text = "TOTAL XP"
    typeface = RupiUi.fontExtraBold(context)
    textSize = 11.5f
    setTextColor(Color.rgb(180, 83, 9))
    gravity = Gravity.CENTER
   })
   val xpContent = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER
    setPadding(0, dp(6), 0, 0)
    addView(ImageView(context).apply {
     setImageResource(R.drawable.ic_lightning)
     val d = drawable.mutate()
     d.setTint(Color.rgb(234, 179, 8))
     setImageDrawable(d)
    }, LinearLayout.LayoutParams(dp(22), dp(22)))
    addView(TextView(context).apply {
     text = " +${session.totalXp.coerceAtLeast(15)} XP"
     typeface = RupiUi.fontExtraBold(context)
     textSize = 20f
     setTextColor(Color.rgb(180, 83, 9))
    })
   }
   xpCard.addView(xpContent)
   statsRow.addView(xpCard, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })

   // Tarjeta 2: PRECISIÓN (Verde)
   val accCard = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER
    background = RupiUi.duolingoStatCard(context, isGold = false)
    setPadding(dp(12), dp(10), dp(12), dp(14))
   }
   accCard.addView(TextView(context).apply {
    text = "PRECISIÓN"
    typeface = RupiUi.fontExtraBold(context)
    textSize = 11.5f
    setTextColor(Color.rgb(21, 128, 61))
    gravity = Gravity.CENTER
   })
   val pct = ((session.correctCount.toFloat() / session.cards.size.coerceAtLeast(1)) * 100).toInt()
   val accContent = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER
    setPadding(0, dp(6), 0, 0)
    addView(ImageView(context).apply {
     setImageResource(R.drawable.ic_target)
     val d = drawable.mutate()
     d.setTint(Color.rgb(22, 163, 74))
     setImageDrawable(d)
    }, LinearLayout.LayoutParams(dp(22), dp(22)))
    addView(TextView(context).apply {
     text = " $pct%"
     typeface = RupiUi.fontExtraBold(context)
     textSize = 20f
     setTextColor(Color.rgb(21, 128, 61))
    })
   }
   accCard.addView(accContent)
   statsRow.addView(accCard, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })

   victoryBox.addView(statsRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(28) })

   addView(victoryBox, LinearLayout.LayoutParams(-1, -2))

   button("CONTINUAR", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_check) {
    onBackToTopic(topic)
   }

   button("VOLVER A REPASAR", kind = RupiUi.Kind.QUIET) {
    session.index = 0
    session.selectedOptionIndex = null
    session.isChecked = false
    session.isRevealed = false
    session.isCompleted = false
    session.correctCount = 0
    session.totalXp = 0
    val deck = state.deck
    if (deck != null && deck.cards.isNotEmpty()) {
     onAction(topic, deck.revision, "RESTART", deck.cards.first().id)
    }
    refreshScreen()
   }
   return@rebuildBody
  }

  // FLUJO DE PREGUNTAS Y OPCIONES (IMÁGENES 1 Y 2 DUOLINGO)
  val currentCard = session.cards[session.index]

  // BARRA SUPERIOR DUOLINGO: [✕] [========      ] [❤ 5]
  val topBar = LinearLayout(context).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   setPadding(0, dp(4), 0, dp(12))
  }

  val closeBtn = FrameLayout(context).apply {
   minimumWidth = dp(48)
   minimumHeight = dp(48)
   isClickable = true
   isFocusable = true
   contentDescription = "Cerrar repaso y volver"
   setOnClickListener { onBackToTopic(topic) }
   addView(ImageView(context).apply {
    setImageResource(R.drawable.ic_close)
    scaleType = ImageView.ScaleType.FIT_CENTER
   }, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
  }
  topBar.addView(closeBtn, LinearLayout.LayoutParams(-2, -2))

  val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
   max = session.cards.size
   progress = session.index + 1
   progressTintList = ColorStateList.valueOf(RupiUi.LEAF)
   progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(229, 229, 229))
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  topBar.addView(progressBar, LinearLayout.LayoutParams(0, dp(12), 1f).apply {
   leftMargin = dp(12)
   rightMargin = dp(12)
  })

  val heartsBadge = LinearLayout(context).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   setPadding(dp(8), dp(4), dp(8), dp(4))
   addView(ImageView(context).apply {
    setImageResource(R.drawable.ic_heart)
    val d = drawable.mutate()
    d.setTint(Color.rgb(234, 43, 43))
    setImageDrawable(d)
   }, LinearLayout.LayoutParams(dp(20), dp(20)))
   addView(TextView(context).apply {
    text = " 5"
    typeface = RupiUi.fontExtraBold(context)
    textSize = 15f
    setTextColor(Color.rgb(234, 43, 43))
   })
  }
  topBar.addView(heartsBadge, LinearLayout.LayoutParams(-2, -2))

  addView(topBar, LinearLayout.LayoutParams(-1, -2))

  // CHIP DE TEMA / BADGE (IMAGEN 1 DUOLINGO)
  val badgeView = LinearLayout(context).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   background = RupiUi.rounded(Color.rgb(243, 232, 255), dp(10).toFloat(), dp(1), Color.rgb(216, 180, 254))
   setPadding(dp(10), dp(4), dp(10), dp(4))
   addView(ImageView(context).apply {
    setImageResource(R.drawable.ic_gem)
    val d = drawable.mutate()
    d.setTint(Color.rgb(126, 34, 206))
    setImageDrawable(d)
   }, LinearLayout.LayoutParams(dp(15), dp(15)))
   addView(TextView(context).apply {
    text = "  ${currentCard.badge}"
    typeface = RupiUi.fontExtraBold(context)
    textSize = 11.5f
    setTextColor(Color.rgb(126, 34, 206))
   })
  }
  addView(badgeView, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(10) })

  // TÍTULO DE LA PREGUNTA
  val questionTitle = TextView(context).apply {
   text = currentCard.question
   typeface = RupiUi.fontTitle(context)
   textSize = 20.5f
   setTextColor(RupiUi.FOREST)
   setLineSpacing(0f, 1.25f)
   setPadding(0, 0, 0, dp(12))
  }
  addView(questionTitle, LinearLayout.LayoutParams(-1, -2))

  listen("la pregunta") { currentCard.question }

  // TARJETA TÁCTIL 3D CON VOLTEO (INTERACTIVE FLIP CARD)
  flashcardButton(
   label = if (session.isRevealed) {
    "Respuesta:\n\n${currentCard.answer}\n\n${currentCard.explanation}\n\n(Toca para volver a la pregunta)"
   } else {
    "Pregunta:\n\n${currentCard.question}\n\n(Toca la tarjeta para voltear y ver la respuesta)"
   },
   revealed = session.isRevealed
  ) {
   session.isRevealed = !session.isRevealed
   val deck = state.deck
   if (deck != null && deck.cards.isNotEmpty()) {
    onAction(topic, deck.revision, if (session.isRevealed) "REVEAL" else "NAVIGATE", currentCard.id)
   }
   refreshScreen()
  }

  // TARJETAS DE OPCIÓN 3D ESTILO DUOLINGO (IMAGEN 1)
  val optionsLabel = TextView(context).apply {
   text = "O ELIGE LA RESPUESTA CORRECTA:"
   typeface = RupiUi.fontExtraBold(context)
   textSize = 11.5f
   setTextColor(RupiUi.MUTED)
   setPadding(0, dp(14), 0, dp(4))
  }
  addView(optionsLabel, LinearLayout.LayoutParams(-1, -2))

  val optionLetters = listOf("A", "B", "C", "D")
  currentCard.options.forEachIndexed { optIdx, optText ->
   val isSelected = (session.selectedOptionIndex == optIdx)
   val optContainer = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    background = RupiUi.duolingoOptionCard(context, isSelected = isSelected)
    minimumHeight = dp(58)
    isClickable = !session.isChecked
    isFocusable = !session.isChecked
    setPadding(dp(16), dp(12), dp(16), dp(12))
    setOnClickListener {
     if (!session.isChecked) {
      session.selectedOptionIndex = optIdx
      refreshScreen()
     }
    }
   }

   val letterBadge = TextView(context).apply {
    text = optionLetters.getOrElse(optIdx) { "${optIdx + 1}" }
    typeface = RupiUi.fontExtraBold(context)
    textSize = 13f
    setTextColor(if (isSelected) Color.WHITE else Color.rgb(107, 114, 128))
    background = RupiUi.rounded(
     if (isSelected) Color.rgb(28, 176, 246) else Color.rgb(243, 244, 246),
     dp(8).toFloat()
    )
    gravity = Gravity.CENTER
    minimumWidth = dp(28)
    minimumHeight = dp(28)
   }
   optContainer.addView(letterBadge, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(12) })

   val optTextView = TextView(context).apply {
    text = optText
    typeface = RupiUi.fontExtraBold(context)
    textSize = 15.5f
    setTextColor(if (isSelected) Color.rgb(24, 153, 214) else RupiUi.FOREST)
    setLineSpacing(0f, 1.2f)
   }
   optContainer.addView(optTextView, LinearLayout.LayoutParams(0, -2, 1f))

   addView(optContainer, LinearLayout.LayoutParams(-1, -2).apply {
    topMargin = dp(6)
    bottomMargin = dp(4)
   })
  }

  // ZONA INFERIOR DE ACCIÓN (ESTADOS: COMPROBAR O FEEDBACK CONTINUAR - IMAGEN 2)
  if (!session.isChecked) {
   val hasSelection = session.selectedOptionIndex != null || session.isRevealed
   button(
    label = "COMPROBAR",
    kind = if (hasSelection) RupiUi.Kind.LEAF else RupiUi.Kind.QUIET,
    enabled = hasSelection,
    iconRes = if (hasSelection) R.drawable.ic_check else null
   ) {
    session.isChecked = true
    if (session.selectedOptionIndex != null) {
     session.isCorrect = (session.selectedOptionIndex == currentCard.correctIndex)
    } else {
     session.isCorrect = true
    }
    if (session.isCorrect) {
     session.correctCount++
     session.totalXp += 15
     val deck = state.deck
     if (deck != null && deck.cards.isNotEmpty()) {
      onAction(topic, deck.revision, "REMEMBER", currentCard.id)
     }
    } else {
     val deck = state.deck
     if (deck != null && deck.cards.isNotEmpty()) {
      onAction(topic, deck.revision, "PRACTICE", currentCard.id)
     }
    }
    refreshScreen()
   }
  } else {
   // BANNER INFERIOR DE FEEDBACK (IMAGEN 2 DUOLINGO)
   val feedbackBanner = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    if (session.isCorrect) {
     background = RupiUi.rounded(Color.rgb(215, 255, 184), dp(16).toFloat(), dp(2), Color.rgb(88, 204, 2))
    } else {
     background = RupiUi.rounded(Color.rgb(255, 220, 216), dp(16).toFloat(), dp(2), Color.rgb(234, 43, 43))
    }
    setPadding(dp(16), dp(16), dp(16), dp(16))
   }

   val bannerHeader = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
   }

   if (session.isCorrect) {
    bannerHeader.addView(ImageView(context).apply {
     setImageResource(R.drawable.ic_check)
     val d = drawable.mutate()
     d.setTint(Color.rgb(43, 122, 11))
     setImageDrawable(d)
    }, LinearLayout.LayoutParams(dp(24), dp(24)))

    bannerHeader.addView(TextView(context).apply {
     text = " ¡Excelente! +15 XP"
     typeface = RupiUi.fontTitle(context)
     textSize = 19f
     setTextColor(Color.rgb(43, 122, 11))
    }, LinearLayout.LayoutParams(0, -2, 1f))
   } else {
    bannerHeader.addView(TextView(context).apply {
     text = "Solución correcta:"
     typeface = RupiUi.fontTitle(context)
     textSize = 18f
     setTextColor(Color.rgb(234, 43, 43))
    }, LinearLayout.LayoutParams(0, -2, 1f))
   }

   feedbackBanner.addView(bannerHeader, LinearLayout.LayoutParams(-1, -2))

   val explanationText = if (session.isCorrect) {
    currentCard.explanation
   } else {
    "La respuesta correcta es: ${currentCard.answer}. ${currentCard.explanation}"
   }
   feedbackBanner.addView(TextView(context).apply {
    text = explanationText
    typeface = RupiUi.fontBold(context)
    textSize = 14f
    setTextColor(if (session.isCorrect) Color.rgb(23, 62, 48) else Color.rgb(185, 28, 28))
    setLineSpacing(0f, 1.2f)
    setPadding(0, dp(6), 0, 0)
   })

   addView(feedbackBanner, LinearLayout.LayoutParams(-1, -2).apply {
    topMargin = dp(14)
    bottomMargin = dp(10)
   })

   button(
    label = "CONTINUAR",
    kind = if (session.isCorrect) RupiUi.Kind.LEAF else RupiUi.Kind.PRIMARY,
    iconRes = R.drawable.ic_check
   ) {
    if (session.index < session.cards.size - 1) {
     session.index++
     session.selectedOptionIndex = null
     session.isChecked = false
     session.isRevealed = false
     val deck = state.deck
     if (deck != null && deck.cards.isNotEmpty()) {
      onAction(topic, deck.revision, "NAVIGATE", session.cards[session.index].id)
     }
     refreshScreen()
    } else {
     session.isCompleted = true
     refreshScreen()
    }
   }
  }

  muted("Fuente pedagógica: ${currentCard.source}")
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
