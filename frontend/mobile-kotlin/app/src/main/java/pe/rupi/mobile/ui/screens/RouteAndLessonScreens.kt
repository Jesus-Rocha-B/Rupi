package pe.rupi.mobile.ui.screens

import android.content.Context
import android.content.res.ColorStateList
import android.animation.ObjectAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.graphics.drawable.StateListDrawable
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

private fun animatePopIn(view: View, celebratory: Boolean = false) {
 view.alpha = 0f
 view.scaleX = if (celebratory) 0.78f else 0.96f
 view.scaleY = if (celebratory) 0.78f else 0.96f
 view.translationY = RupiUi.dp(view.context, if (celebratory) 18 else 8).toFloat()
 view.animate()
  .alpha(1f)
  .scaleX(1f)
  .scaleY(1f)
  .translationY(0f)
  .setDuration(if (celebratory) 520 else 260)
  .setInterpolator(if (celebratory) OvershootInterpolator(1.15f) else DecelerateInterpolator())
  .start()
}

private fun pulseWhenVisible(view: View, scale: Float = 1.06f, durationMs: Long = 850L) {
 val x = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, scale).apply {
  duration = durationMs
  repeatCount = ObjectAnimator.INFINITE
  repeatMode = ObjectAnimator.REVERSE
 }
 val y = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, scale).apply {
  duration = durationMs
  repeatCount = ObjectAnimator.INFINITE
  repeatMode = ObjectAnimator.REVERSE
 }
 val listener = object : View.OnAttachStateChangeListener {
  override fun onViewAttachedToWindow(v: View) { x.start(); y.start() }
  override fun onViewDetachedFromWindow(v: View) { x.cancel(); y.cancel() }
 }
 view.addOnAttachStateChangeListener(listener)
 if (view.isAttachedToWindow) { x.start(); y.start() }
}

private fun floatWhenVisible(view: View, heightDp: Int = 4) {
 val float = ObjectAnimator.ofFloat(
  view,
  View.TRANSLATION_Y,
  0f,
  -RupiUi.dp(view.context, heightDp).toFloat(),
  0f
 ).apply {
  duration = 1700L
  repeatCount = ObjectAnimator.INFINITE
 }
 val listener = object : View.OnAttachStateChangeListener {
  override fun onViewAttachedToWindow(v: View) { float.start() }
  override fun onViewDetachedFromWindow(v: View) { float.cancel() }
 }
 view.addOnAttachStateChangeListener(listener)
 if (view.isAttachedToWindow) float.start()
}

private fun lessonChoiceBackground(context: Context): StateListDrawable {
 val radius = RupiUi.dp(context, 14).toFloat()
 val stroke = RupiUi.dp(context, 2)
 return StateListDrawable().apply {
  addState(
   intArrayOf(android.R.attr.state_checked, -android.R.attr.state_enabled),
   RupiUi.rounded(RupiUi.LEAF_SOFT, radius, stroke, RupiUi.LEAF)
  )
  addState(
   intArrayOf(android.R.attr.state_checked),
   RupiUi.rounded(Color.rgb(255, 242, 190), radius, stroke, RupiUi.GOLD_STROKE)
  )
  addState(
   intArrayOf(-android.R.attr.state_enabled),
   RupiUi.rounded(Color.rgb(250, 248, 242), radius, stroke, RupiUi.LINE)
  )
  addState(intArrayOf(), RupiUi.rounded(Color.WHITE, radius, stroke, RupiUi.LINE))
 }
}

class RoadmapTrailLayout(context: Context) : LinearLayout(context) {
 private val bgDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_ayacucho_plaza)
 private val overlayPaint = Paint().apply {
  // Mantiene legibles las paradas sin ocultar el paisaje andino de la ruta.
  color = Color.argb(172, 250, 248, 238)
 }

 init {
  orientation = VERTICAL
  setWillNotDraw(false)
  val r = RupiUi.dp(context, 20).toFloat()
  background = GradientDrawable(
   GradientDrawable.Orientation.TOP_BOTTOM,
   intArrayOf(
    Color.rgb(234, 246, 230),
    Color.rgb(244, 250, 240),
    Color.rgb(234, 246, 230)
   )
  ).apply {
   cornerRadius = r
   setStroke(RupiUi.dp(context, 2), Color.rgb(195, 222, 188))
  }
  setPadding(0, RupiUi.dp(context, 20), 0, RupiUi.dp(context, 24))
 }

 override fun onDraw(canvas: Canvas) {
  bgDrawable?.let { d ->
   d.setBounds(0, 0, width, height)
   d.draw(canvas)
   canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
  }
  super.onDraw(canvas)
 }

 private val roadShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE
  strokeWidth = RupiUi.dp(context, 48).toFloat()
  color = Color.argb(30, 23, 62, 48)
  strokeCap = Paint.Cap.ROUND
  strokeJoin = Paint.Join.ROUND
 }

 private val roadBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE
  strokeWidth = RupiUi.dp(context, 40).toFloat()
  color = Color.rgb(169, 192, 130) // Camino verde suave del mapa escolar
  strokeCap = Paint.Cap.ROUND
  strokeJoin = Paint.Join.ROUND
 }

 private val roadSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE
  strokeWidth = RupiUi.dp(context, 33).toFloat()
  color = Color.rgb(255, 246, 218) // Interior de pergamino cálido
  strokeCap = Paint.Cap.ROUND
  strokeJoin = Paint.Join.ROUND
 }

 private val roadDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE
  strokeWidth = RupiUi.dpF(context, 3f)
  color = Color.rgb(132, 163, 96) // Huellas para seguir el recorrido
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
  val detail = state.detail ?: return@rebuildBody
  val route = state.route ?: detail.route
  val summary = detail.route

  // Resumen de ruta con materialidad de cuaderno, el avance real y la mascota original.
  val unitBanner = LinearLayout(context).apply {
   orientation = LinearLayout.VERTICAL
   background = RupiUi.rounded(RupiUi.CARD_SURFACE, dp(20).toFloat(), dp(2), RupiUi.LEAF)
   setPadding(dp(14), dp(12), dp(14), dp(14))
   elevation = dp(3).toFloat()
  }
  val areaGrade = listOf(summary.areaName, summary.gradeName).filter { it.isNotBlank() }.joinToString(" · ")
  val headingRow = LinearLayout(context).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
  }
  val headingCopy = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
  headingCopy.addView(TextView(context).apply {
   text = summary.areaName.ifBlank { areaGrade.ifBlank { "Camino de aprendizaje" } }.uppercase()
   typeface = RupiUi.fontExtraBold(context)
   textSize = 10.5f
   setTextColor(RupiUi.SUN_DARK)
   maxLines = 1
   ellipsize = android.text.TextUtils.TruncateAt.END
   background = RupiUi.rounded(Color.rgb(255, 246, 218), dp(10).toFloat(), dp(1), RupiUi.SUN)
   setPadding(dp(8), dp(4), dp(8), dp(4))
  })
  headingCopy.addView(TextView(context).apply {
   text = summary.title
   typeface = RupiUi.fontTitle(context)
   textSize = 19f
   setTextColor(RupiUi.FOREST)
   maxLines = 3
   ellipsize = android.text.TextUtils.TruncateAt.END
   setLineSpacing(0f, 1.08f)
   setPadding(0, dp(6), dp(4), dp(4))
  })
  headingRow.addView(headingCopy, LinearLayout.LayoutParams(0, -2, 1f))
  val mascotMedallion = android.widget.FrameLayout(context).apply {
   background = RupiUi.rounded(RupiUi.SUN, dp(34).toFloat(), dp(2), RupiUi.GOLD_STROKE)
   elevation = dp(2).toFloat()
   contentDescription = "Rupi te acompaña en esta ruta"
  }
  mascotMedallion.addView(ImageView(context).apply {
   setImageResource(R.drawable.ic_rupi_mascot)
   scaleType = ImageView.ScaleType.FIT_CENTER
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }, android.widget.FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER))
  headingRow.addView(mascotMedallion, LinearLayout.LayoutParams(dp(62), dp(62)).apply { leftMargin = dp(6) })
  unitBanner.addView(headingRow)
  val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
   max = summary.totalNodes.coerceAtLeast(1)
   progress = summary.completedNodes
   progressTintList = ColorStateList.valueOf(RupiUi.LEAF)
   progressBackgroundTintList = ColorStateList.valueOf(RupiUi.PAPER_DEEP)
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  unitBanner.addView(bar, LinearLayout.LayoutParams(-1, dp(10)).apply { topMargin = dp(8); bottomMargin = dp(5) })
  val pct = if (summary.totalNodes > 0) ((summary.completedNodes.toFloat() / summary.totalNodes) * 100).toInt() else 0
  val progressCopy = LinearLayout(context).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
  }
  progressCopy.addView(TextView(context).apply {
   text = "${summary.completedNodes} de ${summary.totalNodes} paradas completadas"
   typeface = RupiUi.fontBold(context)
   textSize = 12f
   setTextColor(RupiUi.MUTED)
  }, LinearLayout.LayoutParams(0, -2, 1f))
  progressCopy.addView(TextView(context).apply {
   text = "$pct%"
   typeface = RupiUi.fontExtraBold(context)
   textSize = 12f
   setTextColor(RupiUi.LEAF_DARK)
   background = RupiUi.rounded(RupiUi.LEAF_SOFT, dp(12).toFloat(), dp(1), RupiUi.SOFT_LINE)
   setPadding(dp(9), dp(4), dp(9), dp(4))
  })
  unitBanner.addView(progressCopy)
  addView(unitBanner, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

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
  setPadding(0, dp(12), 0, dp(16))
  minimumHeight = dp(48)
  isClickable = true
  isFocusable = true
  contentDescription = "Parada ${node.sequence}: ${node.title}. ${node.state.label}"
  setOnClickListener {
   if (locked) onBlockedNode() else onOpenNode(route, node)
  }
 }

 // Ondulación suave y orgánica que mantiene los elementos perfectamente dentro de la pantalla
 val offsetPattern = intArrayOf(0, 44, 22, -22, -44, -22)
 val shiftX = dp(offsetPattern[index % offsetPattern.size])

 if (isActive) {
  val startBubble = TextView(context).apply {
   text = "SIGUIENTE PARADA"
   typeface = RupiUi.fontExtraBold(context)
   textSize = 11.5f
   setTextColor(RupiUi.FOREST)
   background = RupiUi.rounded(RupiUi.SUN, dp(12).toFloat(), dp(1), RupiUi.GOLD_STROKE)
   setPadding(dp(12), dp(5), dp(12), dp(5))
   gravity = Gravity.CENTER
   translationX = shiftX.toFloat()
  }
  android.animation.ObjectAnimator.ofFloat(startBubble, "translationY", 0f, -dp(5).toFloat(), 0f).apply {
   duration = 1000
   repeatCount = android.animation.ValueAnimator.INFINITE
   repeatMode = android.animation.ValueAnimator.REVERSE
   start()
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
   stoneRow.addView(mascotIcon, LinearLayout.LayoutParams(dp(54), dp(58)).apply { rightMargin = dp(4) })
 }

 val stoneFrame = android.widget.FrameLayout(context).apply {
  background = RupiUi.routeNodeCircle(context, completed = completed, locked = locked)
   val nodeSize = if (isActive) 76 else if (completed) 68 else 64
   minimumWidth = dp(nodeSize)
   minimumHeight = dp(nodeSize)
   setPadding(dp(3), dp(2), dp(3), dp(7))
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
 }
 val stoneIcon = ImageView(context).apply {
  val iconRes = when {
   completed -> R.drawable.ic_check
   locked -> R.drawable.ic_lock
   else -> R.drawable.ic_star
  }
  setImageResource(iconRes)
  val d = drawable.mutate()
  d.setTint(if (completed) Color.WHITE else if (locked) RupiUi.MUTED else RupiUi.FOREST)
  setImageDrawable(d)
  importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
 }
  val iconSize = if (isActive) 32 else 28
  stoneFrame.addView(stoneIcon, android.widget.FrameLayout.LayoutParams(dp(iconSize), dp(iconSize), Gravity.CENTER))
  val nodeSize = if (isActive) 76 else if (completed) 68 else 64
  stoneRow.addView(stoneFrame, LinearLayout.LayoutParams(dp(nodeSize), dp(nodeSize + 4)))

 if (node.sequence % 4 == 0) {
  val chestIcon = ImageView(context).apply {
   setImageResource(R.drawable.ic_chest)
   val d = drawable.mutate()
   d.setTint(RupiUi.SUN_DARK)
   setImageDrawable(d)
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  stoneRow.addView(chestIcon, LinearLayout.LayoutParams(dp(36), dp(36)).apply { leftMargin = dp(6) })
 }

 stopWrapper.addView(stoneRow, LinearLayout.LayoutParams(-1, -2))

 // Tarjeta blanca nítida y legible con sombra suave para el título de la parada
 val titleBadge = LinearLayout(context).apply {
  orientation = LinearLayout.VERTICAL
  gravity = Gravity.CENTER_HORIZONTAL
  translationX = shiftX.toFloat()
  background = RupiUi.rounded(
   when { locked -> RupiUi.PAPER_DEEP; isActive -> Color.rgb(255, 246, 218); else -> Color.WHITE },
   dp(14).toFloat(),
   dp(if (isActive) 2 else 1),
   when { locked -> RupiUi.LINE; isActive -> RupiUi.GOLD_STROKE; else -> RupiUi.SOFT_LINE }
  )
  minimumHeight = dp(44)
  setPadding(dp(14), dp(7), dp(14), dp(7))
  elevation = dp(if (isActive) 3 else 1).toFloat()
 }

 val labelView = TextView(context).apply {
  text = "${node.sequence}. ${node.title}"
  typeface = RupiUi.fontExtraBold(context)
  textSize = 12.5f
  setTextColor(if (locked) RupiUi.MUTED else if (isActive) RupiUi.SUN_DARK else RupiUi.FOREST)
  gravity = Gravity.CENTER_HORIZONTAL
  maxLines = 2
  ellipsize = android.text.TextUtils.TruncateAt.END
 }
 titleBadge.addView(labelView)

 val stateView = TextView(context).apply {
  val mins = if (node.estimatedMinutes > 0) "${node.estimatedMinutes} min · " else ""
  text = "$mins${node.state.label}"
  typeface = RupiUi.fontBody(context)
  textSize = 11f
  setTextColor(if (completed) RupiUi.LEAF_DARK else RupiUi.MUTED)
  gravity = Gravity.CENTER_HORIZONTAL
  setPadding(0, dp(1), 0, 0)
 }
 titleBadge.addView(stateView)

 val badgeParams = LinearLayout.LayoutParams(-2, -2).apply {
  topMargin = dp(6)
 }
 stopWrapper.addView(titleBadge, badgeParams)

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
		val lesson = state.lesson ?: return@rebuildBody

		val totalQ = lesson.questions.size
		val answeredQ = lesson.questions.count { state.answers.containsKey(it.id) }
		val totalSteps = totalQ + 1
		val currentStep = (answeredQ + 1).coerceAtMost(totalSteps)
		val hasVideo = lesson.blocks.any { it.type == LessonBlockType.VIDEO && it.resourceUrl != null }
		val lessonSearchText = listOf(route.title, route.areaName, node.title, lesson.title)
			.joinToString(" ").lowercase()
		val isHeatMaterialsLesson = lessonSearchText.contains("calor") &&
			(lessonSearchText.contains("conductor") || lessonSearchText.contains("aislante"))

		val lessonHero = stickerCard(fill = Color.rgb(255, 251, 240)) {
			val paradaLabel = listOf("Parada ${node.sequence}", route.areaName.ifBlank { route.title })
				.filter { it.isNotBlank() }
				.joinToString(" · ")
			chip(
				paradaLabel,
				RupiUi.LEAF_SOFT,
				RupiUi.SOFT_LINE,
				RupiUi.LEAF_DARK
			)
			text(lesson.title.ifBlank { node.title }, 20)

			if (totalQ > 0) {
				val stepLabel = TextView(context).apply {
					text = "PASO $currentStep DE $totalSteps"
					typeface = RupiUi.fontExtraBold(context)
					textSize = 13f
					gravity = Gravity.CENTER
					setTextColor(RupiUi.FOREST)
					background = RupiUi.rounded(RupiUi.LEAF_SOFT, dp(22).toFloat())
					setPadding(dp(16), dp(5), dp(16), dp(5))
				}
				addView(stepLabel, LinearLayout.LayoutParams(-2, -2).apply {
					gravity = Gravity.CENTER_HORIZONTAL
					topMargin = dp(2)
					bottomMargin = dp(5)
				})

				val progressStrip = LinearLayout(context).apply {
					orientation = LinearLayout.HORIZONTAL
					gravity = Gravity.CENTER_VERTICAL
					setPadding(dp(4), dp(2), dp(4), dp(2))
				}
				for (stepIndex in 0 until totalSteps) {
					if (stepIndex > 0) {
						progressStrip.addView(View(context).apply {
							background = RupiUi.rounded(
								if (stepIndex <= answeredQ) RupiUi.LEAF else RupiUi.SOFT_LINE,
								dp(6).toFloat()
							)
						}, LinearLayout.LayoutParams(0, dp(4), 1f).apply {
							leftMargin = dp(2)
							rightMargin = dp(2)
						})
					}
					val isVideoStep = stepIndex == 0
					val question = lesson.questions.getOrNull(stepIndex - 1)
					val savedAnswer = question?.let { state.answers[it.id] }
					val activeStep = stepIndex == currentStep - 1
					val doneStep = if (isVideoStep) answeredQ > 0 else savedAnswer != null
					val marker = FrameLayout(context).apply {
						background = RupiUi.rounded(
							when {
								doneStep && (isVideoStep || savedAnswer?.isCorrect == true) -> RupiUi.LEAF
								activeStep -> RupiUi.SUN
								else -> Color.WHITE
							},
							dp(22).toFloat(),
							dp(2),
							when {
								doneStep && (isVideoStep || savedAnswer?.isCorrect == true) -> RupiUi.LEAF_DARK
								activeStep -> RupiUi.SUN_DARK
								else -> RupiUi.LINE
							}
						)
						contentDescription = if (isVideoStep) "Paso de la lección" else "Pregunta $stepIndex"
						val markerText = when {
							doneStep && (isVideoStep || savedAnswer?.isCorrect == true) -> "✓"
							isVideoStep -> "▶"
							else -> "$stepIndex"
						}
						addView(TextView(context).apply {
							text = markerText
							typeface = RupiUi.fontExtraBold(context)
							textSize = if (isVideoStep && !doneStep) 11f else 13f
							gravity = Gravity.CENTER
							setTextColor(
								if (doneStep && (isVideoStep || savedAnswer?.isCorrect == true)) Color.WHITE
								else RupiUi.FOREST
							)
						}, FrameLayout.LayoutParams(-1, -1))
						if (activeStep && !doneStep) pulseWhenVisible(this, 1.06f, 1050L)
					}
					progressStrip.addView(marker, LinearLayout.LayoutParams(dp(31), dp(31)))
				}
				val progressScroll = HorizontalScrollView(context).apply {
					isHorizontalScrollBarEnabled = false
					addView(progressStrip, ViewGroup.LayoutParams(-2, dp(35)))
				}
				addView(progressScroll, LinearLayout.LayoutParams(-1, dp(35)).apply {
					bottomMargin = dp(3)
				})
			}

			val guideRow = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				setPadding(0, dp(1), 0, dp(4))
			}
			val guideMascot = ImageView(context).apply {
				setImageResource(R.drawable.rupi_explorer)
				scaleType = ImageView.ScaleType.FIT_CENTER
				contentDescription = "Rupi, el gallito de las rocas explorador"
			}
			guideRow.addView(guideMascot, LinearLayout.LayoutParams(dp(68), dp(70)).apply {
				rightMargin = dp(8)
			})
			floatWhenVisible(guideMascot, 3)
			val guideBubble = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				gravity = Gravity.CENTER_VERTICAL
				background = RupiUi.speechBalloon(context, RupiUi.LEAF, RupiUi.LEAF_DARK)
				setPadding(dp(12), dp(8), dp(12), dp(10))
			}
			guideBubble.addView(TextView(context).apply {
				text = if (hasVideo && answeredQ == 0) "¡Mira con atención!" else "¡Rupi te acompaña!"
				typeface = RupiUi.fontExtraBold(context)
				textSize = 14f
				setTextColor(RupiUi.FOREST)
			})
			lesson.instructions?.takeIf { it.isNotBlank() }?.let { instructions ->
				guideBubble.addView(TextView(context).apply {
					text = instructions
					typeface = RupiUi.fontBody(context)
					textSize = 11.5f
					maxLines = 2
					ellipsize = android.text.TextUtils.TruncateAt.END
					setTextColor(RupiUi.MUTED)
					setPadding(0, dp(2), 0, 0)
				})
			}
			guideRow.addView(guideBubble, LinearLayout.LayoutParams(0, -2, 1f))
			addView(guideRow, LinearLayout.LayoutParams(-1, -2))

			val audioButton = listen("la lección", kind = RupiUi.Kind.TANGERINE) {
				(listOf(lesson.title) +
					listOfNotNull(lesson.instructions) +
					lesson.blocks.mapNotNull { it.text }).joinToString(". ")
			}
			audioButton.apply {
				minimumHeight = dp(46)
				textSize = 14.5f
				setPadding(dp(10), dp(7), dp(10), dp(10))
				(layoutParams as? LinearLayout.LayoutParams)?.let { params ->
					params.topMargin = dp(4)
					params.bottomMargin = dp(2)
					layoutParams = params
				}
			}
			pulseWhenVisible(audioButton, 1.012f, 1250L)
		}
		animatePopIn(lessonHero)

		// Destaca el video antes de la lectura, manteniendo el orden original del resto de materiales.
		lesson.blocks.sortedBy { if (it.type == LessonBlockType.VIDEO) 0 else 1 }.forEach { block ->
			val link = block.resourceUrl
			val body = block.text
			when {
				block.type == LessonBlockType.VIDEO && link != null -> stickerCard(fill = Color.rgb(255, 251, 240)) {
					val videoHeading = LinearLayout(context).apply {
						orientation = LinearLayout.HORIZONTAL
						gravity = Gravity.CENTER
						setPadding(0, dp(2), 0, dp(8))
					}
					listOf(true, false, true).forEachIndexed { index, _ ->
						if (index == 1) {
							videoHeading.addView(TextView(context).apply {
								text = "Video de la lección"
								typeface = RupiUi.fontTitle(context)
								textSize = 21f
								gravity = Gravity.CENTER
								setTextColor(RupiUi.FOREST)
							}, LinearLayout.LayoutParams(0, -2, 1f))
						} else {
							videoHeading.addView(ImageView(context).apply {
								setImageResource(R.drawable.ic_star)
								setColorFilter(RupiUi.SUN)
								scaleType = ImageView.ScaleType.FIT_CENTER
							}, LinearLayout.LayoutParams(dp(18), dp(18)).apply {
								if (index == 0) rightMargin = dp(8) else leftMargin = dp(8)
							})
						}
					}
					addView(videoHeading, LinearLayout.LayoutParams(-1, -2))
					val videoCaption = if (isHeatMaterialsLesson) {
						"Descubre qué materiales dejan pasar el calor."
					} else {
						"Mira el video para descubrir el tema de esta parada."
					}
					muted(videoCaption)

					// Portada reutilizable: usa la ruta real, el paisaje ilustrado y la mascota de Rupi.
					// La portada y el botón CTA abren el mismo enlace del recurso asignado.
					val preview = FrameLayout(context).apply {
						background = RupiUi.rounded(RupiUi.LEAF_SOFT, dp(18).toFloat(), dp(2), RupiUi.LEAF)
						clipToOutline = true
						minimumHeight = dp(164)
						contentDescription = block.accessibleText
					}
					preview.addView(ImageView(context).apply {
						setImageResource(if (isHeatMaterialsLesson) R.drawable.video_heat_materials else R.drawable.bg_andean_scene)
						scaleType = ImageView.ScaleType.CENTER_CROP
						alpha = 1f
						importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
					}, FrameLayout.LayoutParams(-1, -1))
					val playBadge = FrameLayout(context).apply {
						background = RupiUi.rounded(RupiUi.LEAF, dp(50).toFloat(), dp(3), Color.WHITE)
						elevation = dp(3).toFloat()
						isClickable = true
						isFocusable = true
						contentDescription = "Reproducir video de la lección"
						setOnClickListener { onOpenExternalLink(link) }
						addView(ImageView(context).apply {
							setImageResource(R.drawable.ic_play)
							setColorFilter(Color.WHITE)
							scaleType = ImageView.ScaleType.FIT_CENTER
						}, FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER))
					}
					preview.addView(playBadge, FrameLayout.LayoutParams(dp(68), dp(68), Gravity.CENTER))
					pulseWhenVisible(playBadge, 1.08f, 900L)
					addView(preview, LinearLayout.LayoutParams(-1, dp(174)).apply {
						topMargin = dp(6)
						bottomMargin = dp(4)
					})
					button("Ver el video", kind = RupiUi.Kind.TANGERINE, iconRes = R.drawable.ic_play) { onOpenExternalLink(link) }
					if (totalQ > 0) {
						val afterVideo = LinearLayout(context).apply {
							orientation = LinearLayout.HORIZONTAL
							gravity = Gravity.CENTER
							background = RupiUi.rounded(RupiUi.LEAF_SOFT, dp(24).toFloat())
							setPadding(dp(12), dp(8), dp(12), dp(8))
						}
							afterVideo.addView(ImageView(context).apply {
								setImageResource(R.drawable.ic_message_circle)
							setColorFilter(RupiUi.LEAF_DARK)
						}, LinearLayout.LayoutParams(dp(19), dp(19)).apply { rightMargin = dp(8) })
						afterVideo.addView(TextView(context).apply {
							text = "Después, responde $totalQ ${if (totalQ == 1) "pregunta" else "preguntas"}."
							typeface = RupiUi.fontBold(context)
							textSize = 14f
							setTextColor(RupiUi.LEAF_DARK)
						})
						addView(afterVideo, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
					}
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
			val answer = state.answers[q.id]
			val questionCard = stickerCard(fill = when {
				answer?.isCorrect == true -> Color.rgb(248, 253, 246)
				answer != null -> Color.rgb(255, 251, 240)
				else -> Color.WHITE
			}) {
				chip(
					"PREGUNTA ${index + 1} DE $totalQ",
					if (answer?.isCorrect == true) RupiUi.LEAF_SOFT else Color.rgb(255, 246, 218),
					if (answer?.isCorrect == true) RupiUi.LEAF else RupiUi.GOLD_STROKE,
					if (answer?.isCorrect == true) RupiUi.LEAF_DARK else RupiUi.SUN_DARK
				)
				text(q.statement, 20)
				listen("la pregunta ${index + 1}") { RupiVoice.question(q.statement, q.options.map { it.text }) }
				val group = RadioGroup(context)
				val ids = mutableMapOf<Int, String>()
				q.options.forEachIndexed { j, option ->
					val radio = RadioButton(context).apply {
						id = j + 1
						text = option.text
						minWidth = dp(48)
						minHeight = dp(62)
						setSingleLine(false)
						setHorizontallyScrolling(false)
						setPadding(dp(14), dp(12), dp(14), dp(12))
						isEnabled = answer == null
						setTextColor(RupiUi.INK)
					}
					styleOption(radio)
					radio.background = lessonChoiceBackground(context)
					ids[j + 1] = option.id
					group.addView(radio)
				}
				if (answer != null) {
					val correctIndex = q.options.indexOfFirst { it.id == answer.correctOptionId }
					if (correctIndex >= 0) group.check(correctIndex + 1)
				}
				addView(group)
				if (answer != null) {
					val explanation = answer.explanation.ifBlank { q.explanation }
					feedback(
						(if (answer.isCorrect) "¡Muy bien! " else "¡Buen intento! La alternativa correcta está en verde. ") + explanation,
						if (answer.isCorrect) NoticeTone.OK else NoticeTone.RETRY,
						iconRes = if (answer.isCorrect) R.drawable.ic_check else R.drawable.ic_arrow_left
					)
					listen("la explicación ${index + 1}") { explanation }
				} else {
					val submit = button("Comprobar respuesta", kind = RupiUi.Kind.TANGERINE, enabled = false) {
						val selected = ids[group.checkedRadioButtonId] ?: return@button
						onSubmitAnswer(lesson.activityId, q.id, selected)
					}
					group.setOnCheckedChangeListener { _, checked ->
						submit.isEnabled = ids.containsKey(checked)
						group.findViewById<RadioButton>(checked)?.let { selected ->
							selected.animate().cancel()
							selected.scaleX = 1f
							selected.scaleY = 1f
							selected.animate().scaleX(1.035f).scaleY(1.035f).setDuration(100)
								.withEndAction {
									selected.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
								}.start()
						}
					}
				}
			}
			if (answer != null) animatePopIn(questionCard)
		}

		val result = state.completionResult
		if (result != null) {
			val xp = result.experienceEarned
			val message = result.rupiMessage

			// La API entrega premios, mensaje y siguiente parada; la vista solo los presenta con el diseño Rupi.
			val completionCard = heroCard {
				chip("¡PARADA COMPLETADA!", Color.rgb(255, 246, 218), RupiUi.GOLD_STROKE, RupiUi.SUN_DARK, iconRes = R.drawable.ic_award)
				val mascotBox = LinearLayout(context).apply {
					orientation = LinearLayout.VERTICAL
					gravity = Gravity.CENTER_HORIZONTAL
					setPadding(0, dp(8), 0, dp(10))
				}
				val completionMascot = ImageView(context).apply {
						setImageResource(R.drawable.ic_rupi_mascot)
						scaleType = ImageView.ScaleType.FIT_CENTER
						importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
					}
				mascotBox.addView(completionMascot, LinearLayout.LayoutParams(dp(80), dp(84)))
				animatePopIn(completionMascot, celebratory = true)
				mascotBox.addView(
					TextView(context).apply {
						text = "¡Parada completada!"
						typeface = RupiUi.fontTitle(context)
						textSize = 24f
						setTextColor(RupiUi.FOREST)
						setPadding(0, dp(6), 0, 0)
					}
				)
				addView(mascotBox, LinearLayout.LayoutParams(-1, -2))
				if (totalQ > 0) {
					muted("$answeredQ de $totalQ preguntas respondidas")
				}

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
			animatePopIn(completionCard, celebratory = true)

			result.nextUnlockedNode?.let { next ->
				button("Continuar a la siguiente parada", kind = RupiUi.Kind.TANGERINE, iconRes = R.drawable.ic_play) {
					onOpenNextNode(route, next)
				}
			} ?: run {
				button("Volver a la ruta", kind = RupiUi.Kind.LEAF, iconRes = R.drawable.ic_check) {
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
					answeredAll -> "¡Completar parada y ganar XP!"
					lesson.questions.size == 1 -> "Responde la pregunta pendiente para terminar"
					else -> "Responde las ${lesson.questions.size} preguntas para terminar"
				}
				button(
					label = buttonLabel,
					kind = if (answeredAll) RupiUi.Kind.TANGERINE else RupiUi.Kind.QUIET,
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
		button("Repasar con tarjetas", kind = RupiUi.Kind.SUN, iconRes = R.drawable.ic_book) { onOpenFlashcards(topic) }
	}
}
