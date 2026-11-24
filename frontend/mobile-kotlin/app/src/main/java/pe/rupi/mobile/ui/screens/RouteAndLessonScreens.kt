package pe.rupi.mobile.ui.screens

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
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

class RoadmapTrailLayout(context: Context) : LinearLayout(context) {
 init {
  orientation = VERTICAL
  setWillNotDraw(false)
  val r = RupiUi.dp(context, 20).toFloat()
  background = GradientDrawable(
   GradientDrawable.Orientation.TOP_BOTTOM,
   intArrayOf(
    Color.rgb(234, 246, 230), // Verde pradera andina de Ayacucho
    Color.rgb(244, 250, 240),
    Color.rgb(234, 246, 230)
   )
  ).apply {
   cornerRadius = r
   setStroke(RupiUi.dp(context, 2), Color.rgb(195, 222, 188))
  }
  setPadding(0, RupiUi.dp(context, 20), 0, RupiUi.dp(context, 24))
 }

 private val roadShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE
  strokeWidth = RupiUi.dp(context, 68).toFloat()
  color = Color.argb(32, 25, 45, 20)
  strokeCap = Paint.Cap.ROUND
  strokeJoin = Paint.Join.ROUND
 }

 private val roadBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE
  strokeWidth = RupiUi.dp(context, 58).toFloat()
  color = Color.rgb(208, 182, 138) // Borde del camino de tierra/piedra
  strokeCap = Paint.Cap.ROUND
  strokeJoin = Paint.Join.ROUND
 }

 private val roadSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE
  strokeWidth = RupiUi.dp(context, 50).toFloat()
  color = Color.rgb(250, 241, 222) // Sendero andino claro
  strokeCap = Paint.Cap.ROUND
  strokeJoin = Paint.Join.ROUND
 }

 private val roadDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE
  strokeWidth = RupiUi.dpF(context, 3.5f)
  color = Color.rgb(195, 165, 118) // Línea discontinua central del camino
  strokeCap = Paint.Cap.ROUND
  strokeJoin = Paint.Join.ROUND
  pathEffect = DashPathEffect(
   floatArrayOf(RupiUi.dp(context, 10).toFloat(), RupiUi.dp(context, 8).toFloat()),
   0f
  )
 }

 private val path = Path()

 override fun dispatchDraw(canvas: Canvas) {
  if (childCount >= 2) {
   path.reset()
   val points = mutableListOf<Pair<Float, Float>>()
   for (i in 0 until childCount) {
    val child = getChildAt(i)
    val stoneView = child.findViewWithTag<View>("stone") ?: child
    val cx = child.left + child.width / 2f + stoneView.translationX
    val cy = child.top + stoneView.top + stoneView.height / 2f
    points.add(Pair(cx, cy))
   }

   if (points.isNotEmpty()) {
    path.moveTo(points[0].first, points[0].second)
    for (i in 0 until points.size - 1) {
     val p0 = points[i]
     val p1 = points[i + 1]
     val midY = (p0.second + p1.second) / 2f
     path.cubicTo(p0.first, midY, p1.first, midY, p1.first, p1.second)
    }

    canvas.drawPath(path, roadShadowPaint)
    canvas.drawPath(path, roadBorderPaint)
    canvas.drawPath(path, roadSurfacePaint)
    canvas.drawPath(path, roadDashPaint)
   }
  }
  super.dispatchDraw(canvas)
 }
}

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

  // Banner superior estilo Duolingo con ambientación de Ayacucho
  val unitBanner = LinearLayout(context).apply {
   orientation = LinearLayout.VERTICAL
   background = RupiUi.rounded(RupiUi.LEAF, dp(18).toFloat(), dp(2), RupiUi.LEAF_DARK)
   setPadding(dp(16), dp(14), dp(16), dp(16))
  }
  val areaGrade = listOf(summary.areaName, summary.gradeName).filter { it.isNotBlank() }.joinToString(" · ")
  unitBanner.addView(TextView(context).apply {
   text = if (areaGrade.isNotBlank()) "SECCIÓN 1 · $areaGrade" else "SECCIÓN 1 · AYACUCHO"
   typeface = RupiUi.fontExtraBold(context)
   textSize = 12f
   setTextColor(Color.rgb(220, 255, 200))
   letterSpacing = 0.05f
  })
  unitBanner.addView(TextView(context).apply {
   text = summary.title
   typeface = RupiUi.fontTitle(context)
   textSize = 20.5f
   setTextColor(Color.WHITE)
   setPadding(0, dp(4), 0, dp(8))
  })
  val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
   max = summary.totalNodes.coerceAtLeast(1)
   progress = summary.completedNodes
   progressTintList = ColorStateList.valueOf(RupiUi.SUN)
   progressBackgroundTintList = ColorStateList.valueOf(Color.argb(70, 255, 255, 255))
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  unitBanner.addView(bar, LinearLayout.LayoutParams(-1, dp(12)).apply { bottomMargin = dp(4) })
  val pct = if (summary.totalNodes > 0) ((summary.completedNodes.toFloat() / summary.totalNodes) * 100).toInt() else 0
  unitBanner.addView(TextView(context).apply {
   text = "${summary.completedNodes} de ${summary.totalNodes} paradas completadas ($pct%)"
   typeface = RupiUi.fontBold(context)
   textSize = 12.5f
   setTextColor(Color.rgb(230, 255, 220))
  })
  addView(unitBanner, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

  // Sendero continuo y nodos en fondo paisajístico andino (RoadmapTrailLayout)
  val trailLayout = RoadmapTrailLayout(context)

  var activeFound = false
  var overallIndex = 0

  detail.unitGroups.forEach { group ->
   group.nodes.forEach { node ->
    val isActive = !activeFound && node.state != RouteNodeState.COMPLETADO && node.state != RouteNodeState.BLOQUEADO
    if (isActive) activeFound = true
    renderWindingStopNode(context, scaffold, trailLayout, route, node, overallIndex, isActive, onOpenNode, onBlockedNode)
    overallIndex++
   }
  }
  if (detail.ungroupedNodes.isNotEmpty()) {
   detail.ungroupedNodes.forEach { node ->
    val isActive = !activeFound && node.state != RouteNodeState.COMPLETADO && node.state != RouteNodeState.BLOQUEADO
    if (isActive) activeFound = true
    renderWindingStopNode(context, scaffold, trailLayout, route, node, overallIndex, isActive, onOpenNode, onBlockedNode)
    overallIndex++
   }
  }

  addView(trailLayout, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) })
 }
}

private fun renderWindingStopNode(
 context: Context,
 scaffold: ScreenScaffold,
 parentContainer: LinearLayout,
 route: LearningRouteSummary,
 node: RouteNode,
 index: Int,
 isActive: Boolean,
 onOpenNode: (LearningRouteSummary, RouteNode) -> Unit,
 onBlockedNode: () -> Unit
) {
 val dp = { n: Int -> scaffold.dp(n) }
 val completed = node.state == RouteNodeState.COMPLETADO
 val locked = node.state == RouteNodeState.BLOQUEADO

 val stopWrapper = LinearLayout(context).apply {
  orientation = LinearLayout.VERTICAL
  gravity = Gravity.CENTER_HORIZONTAL
  setPadding(0, dp(10), 0, dp(14))
  isClickable = true
  isFocusable = true
  contentDescription = "Parada ${node.sequence}: ${node.title}. ${node.state.label}"
  setOnClickListener {
   if (locked) onBlockedNode() else onOpenNode(route, node)
  }
 }

 val offsetPattern = intArrayOf(0, -68, 0, 68)
 val shiftX = dp(offsetPattern[index % 4])

 if (isActive) {
  val startBubble = TextView(context).apply {
   text = "EMPEZAR"
   typeface = RupiUi.fontExtraBold(context)
   textSize = 11.5f
   setTextColor(Color.WHITE)
   background = RupiUi.rounded(RupiUi.LEAF, dp(10).toFloat(), dp(2), RupiUi.LEAF_DARK)
   setPadding(dp(14), dp(4), dp(14), dp(4))
   gravity = Gravity.CENTER
   translationX = shiftX.toFloat()
  }
  stopWrapper.addView(startBubble, LinearLayout.LayoutParams(-2, -2).apply {
   bottomMargin = dp(6)
  })
 }

 val stoneRow = LinearLayout(context).apply {
  tag = "stone"
  orientation = LinearLayout.HORIZONTAL
  gravity = Gravity.CENTER
  translationX = shiftX.toFloat()
 }

 if (isActive) {
  val mascotIcon = ImageView(context).apply {
   setImageResource(R.drawable.ic_rupi_mascot)
   scaleType = ImageView.ScaleType.FIT_CENTER
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  stoneRow.addView(mascotIcon, LinearLayout.LayoutParams(dp(48), dp(52)).apply { rightMargin = dp(8) })
 }

 val stoneFrame = android.widget.FrameLayout(context).apply {
  background = RupiUi.routeNodeCircle(context, completed = completed, locked = locked)
  setPadding(0, 0, 0, dp(4))
 }
 val stoneIcon = ImageView(context).apply {
  val iconRes = when {
   completed -> R.drawable.ic_check
   locked -> R.drawable.ic_lock
   else -> R.drawable.ic_star
  }
  setImageResource(iconRes)
  val d = drawable.mutate()
  d.setTint(if (completed) Color.WHITE else if (locked) RupiUi.MUTED else Color.WHITE)
  setImageDrawable(d)
  importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
 }
 stoneFrame.addView(stoneIcon, android.widget.FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
 stoneRow.addView(stoneFrame, LinearLayout.LayoutParams(dp(62), dp(66)))

 if (node.sequence % 4 == 0) {
  val chestIcon = ImageView(context).apply {
   setImageResource(R.drawable.ic_award)
   val d = drawable.mutate()
   d.setTint(RupiUi.SUN_DARK)
   setImageDrawable(d)
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  stoneRow.addView(chestIcon, LinearLayout.LayoutParams(dp(30), dp(30)).apply { leftMargin = dp(8) })
 }

 stopWrapper.addView(stoneRow, LinearLayout.LayoutParams(-1, -2))

 val labelView = TextView(context).apply {
  text = "Parada ${node.sequence}: ${node.title}"
  typeface = RupiUi.fontExtraBold(context)
  textSize = 13.5f
  setTextColor(if (locked) RupiUi.MUTED else RupiUi.FOREST)
  gravity = Gravity.CENTER_HORIZONTAL
  translationX = shiftX.toFloat()
  setPadding(dp(8), dp(4), dp(8), 0)
 }
 stopWrapper.addView(labelView, LinearLayout.LayoutParams(-2, -2))

 val stateView = TextView(context).apply {
  val mins = if (node.estimatedMinutes > 0) "${node.estimatedMinutes} min · " else ""
  text = "$mins${node.state.label}"
  typeface = RupiUi.fontBody(context)
  textSize = 12f
  setTextColor(RupiUi.MUTED)
  gravity = Gravity.CENTER_HORIZONTAL
  translationX = shiftX.toFloat()
 }
 stopWrapper.addView(stateView, LinearLayout.LayoutParams(-2, -2))

 parentContainer.addView(stopWrapper, LinearLayout.LayoutParams(-1, -2))
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
	val dp = { n: Int -> scaffold.dp(n) }

	scaffold.rebuildBody(preserveScroll = true) {
		backButton("Volver a la ruta") { onBackToRoute(route) }
		val lesson = state.lesson ?: return@rebuildBody

		val totalQ = lesson.questions.size
		val answeredQ = lesson.questions.count { state.answers.containsKey(it.id) }

		// Barra de progreso de preguntas estilo lección de Duolingo
		if (totalQ > 0) {
			val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
				max = totalQ
				progress = answeredQ
				progressTintList = ColorStateList.valueOf(RupiUi.LEAF)
				progressBackgroundTintList = ColorStateList.valueOf(RupiUi.PAPER_DEEP)
				importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			}
			addView(bar, LinearLayout.LayoutParams(-1, dp(14)).apply {
				topMargin = dp(4)
				bottomMargin = dp(8)
			})
		}

		// Prompt con Rupi y bocadillo de diálogo (Estilo Duolingo)
		val promptRow = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding(0, dp(4), 0, dp(8))
		}
		promptRow.addView(
			ImageView(context).apply {
				setImageResource(R.drawable.ic_rupi_mascot)
				scaleType = ImageView.ScaleType.FIT_CENTER
				importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
			},
			LinearLayout.LayoutParams(dp(54), dp(58)).apply { rightMargin = dp(10) }
		)
		val bubble = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			background = RupiUi.rounded(Color.WHITE, dp(16).toFloat(), dp(2), RupiUi.LINE)
			setPadding(dp(14), dp(10), dp(14), dp(10))
		}
		val promptText = lesson.instructions?.ifBlank { "¡Hola! Lee con atencion y responde las preguntas para ganar puntos de experiencia." }
			?: "¡Hola! Lee con atencion y responde las preguntas para ganar puntos de experiencia."
		bubble.addView(TextView(context).apply {
			text = promptText
			typeface = RupiUi.fontExtraBold(context)
			textSize = 13.5f
			setTextColor(RupiUi.FOREST)
		})
		promptRow.addView(bubble, LinearLayout.LayoutParams(0, -2, 1f))
		addView(promptRow, LinearLayout.LayoutParams(-1, -2))

		stickerCard {
			chip("Parada ${node.sequence}", RupiUi.SUN, RupiUi.FOREST, RupiUi.FOREST)
			chip(
				"${node.activityType.label} · ${route.title}",
				RupiUi.LEAF_SOFT,
				Color.rgb(148, 208, 161),
				RupiUi.LEAF_DARK
			)
			text(lesson.title.ifBlank { node.title }, 22)
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
						(if (answer.isCorrect) "¡Excelente! " else "Sigamos aprendiendo. ") + explanation,
						if (answer.isCorrect) NoticeTone.OK else NoticeTone.RETRY,
						iconRes = if (answer.isCorrect) R.drawable.ic_check else R.drawable.ic_arrow_left
					)
					listen("la explicación ${index + 1}") { explanation }
				} else {
					val submit = button("COMPROBAR RESPUESTA", kind = RupiUi.Kind.PRIMARY, enabled = false) {
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

			// Tarjeta de Victoria estilo Duolingo
			heroCard {
				val mascotBox = LinearLayout(context).apply {
					orientation = LinearLayout.VERTICAL
					gravity = Gravity.CENTER_HORIZONTAL
					setPadding(0, dp(8), 0, dp(10))
				}
				mascotBox.addView(
					ImageView(context).apply {
						setImageResource(R.drawable.ic_rupi_mascot)
						scaleType = ImageView.ScaleType.FIT_CENTER
						importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
					},
					LinearLayout.LayoutParams(dp(80), dp(84))
				)
				mascotBox.addView(
					TextView(context).apply {
						text = "¡Lección Completada!"
						typeface = RupiUi.fontTitle(context)
						textSize = 24f
						setTextColor(RupiUi.FOREST)
						setPadding(0, dp(6), 0, 0)
					}
				)
				addView(mascotBox, LinearLayout.LayoutParams(-1, -2))

				if (xp > 0) {
					chip("+$xp XP Ganados", Color.rgb(255, 248, 230), RupiUi.SUN, Color.rgb(107, 71, 5), iconRes = R.drawable.ic_gem)
				}
				muted(message)
				result.badgeName?.let { badge ->
					chip("Insignia ganada: $badge", RupiUi.LEAF_SOFT, Color.rgb(148, 208, 161), RupiUi.LEAF_DARK, iconRes = R.drawable.ic_award)
				}
				if (result.isRouteCompleted) {
					val bonus = result.routeBonusExperience.takeIf { it > 0 }?.let { "+$it XP extra" } ?: ""
					chip("¡Completaste toda la ruta escolar! $bonus", Color.rgb(227, 240, 248), Color.rgb(147, 196, 223), RupiUi.SKY_DARK, iconRes = R.drawable.ic_award)
				}
			}

			result.nextUnlockedNode?.let { next ->
				button("CONTINUAR A LA SIGUIENTE PARADA", kind = RupiUi.Kind.PRIMARY, iconRes = R.drawable.ic_play) {
					onOpenNextNode(route, next)
				}
			} ?: run {
				button("VOLVER A LA RUTA", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_check) {
					onBackToRoute(route)
				}
			}
		} else {
			val answeredAll = lesson.questions.all { state.answers.containsKey(it.id) }
			val completed = lesson.state == "COMPLETADO" || node.state == RouteNodeState.COMPLETADO
			if (completed && !answeredAll) {
				feedback("Ya completaste esta parada. Puedes repasarla cuando quieras.", NoticeTone.OK)
			} else {
				val buttonLabel = when {
					answeredAll -> "COMPLETAR PARADA Y GANAR XP"
					lesson.questions.size == 1 -> "Responde la pregunta pendiente para terminar"
					else -> "Responde las ${lesson.questions.size} preguntas para terminar"
				}
				button(
					label = buttonLabel,
					kind = if (answeredAll) RupiUi.Kind.PRIMARY else RupiUi.Kind.QUIET,
					iconRes = if (answeredAll) R.drawable.ic_check else null,
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
		button("Practicar con Rupi (IA)", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_message_circle) { onOpenAiPractice(topic) }
		button("Repasar con tarjetas", kind = RupiUi.Kind.QUIET, iconRes = R.drawable.ic_book) { onOpenFlashcards(topic) }
	}
}
