package pe.rupi.mobile.ui.components

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import pe.rupi.mobile.R
import pe.rupi.mobile.RupiUi
import pe.rupi.mobile.RupiVoice
import pe.rupi.mobile.ui.state.NoticeMessage
import pe.rupi.mobile.ui.state.NoticeTone

/**
 * Constructor de vistas nativas estilo portal estudiante de RUPI:
 * barra blanca con avatar circular de Rupi, tipografía Nunito, tarjetas cuaderno/sticker y botones 3D táctiles.
 */
class ScreenScaffold(
 private val activity: Activity,
 private val voice: RupiVoice?
) {
 var isBusy: Boolean = false
 private var noticeTone: RupiUi.Tone = RupiUi.Tone.INFO

 lateinit var rootScroll: ScrollView
  private set
 lateinit var contentContainer: LinearLayout
  private set
 private lateinit var noticeView: TextView
 private lateinit var currentLayout: LinearLayout

 fun dp(n: Int): Int = RupiUi.dp(activity, n)

 fun mountPage(title: String): LinearLayout {
  voice?.stop()
  val frame = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   background = RupiUi.Paper(activity)
  }
  val bar = header(title).apply {
   setPadding(dp(16), dp(10), dp(16), dp(14))
  }
  frame.addView(bar, LinearLayout.LayoutParams(-1, -2))

  val scroll = ScrollView(activity).apply {
   isFillViewport = true
   clipToPadding = false
  }
  rootScroll = scroll

  val outerLayout = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(dp(16), dp(8), dp(16), dp(24))
  }
  scroll.addView(outerLayout, FrameLayout.LayoutParams(-1, -2))
  frame.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
  activity.setContentView(frame)

  frame.setOnApplyWindowInsetsListener { _, insets ->
   @Suppress("DEPRECATION")
   val top = insets.systemWindowInsetTop
   @Suppress("DEPRECATION")
   val bottom = insets.systemWindowInsetBottom
   bar.setPadding(dp(16), top + dp(10), dp(16), dp(14))
   outerLayout.setPadding(dp(16), dp(8), dp(16), dp(24) + bottom)
   insets
  }

  noticeView = TextView(activity).apply {
   typeface = RupiUi.fontExtraBold(activity)
   textSize = 15f
   setPadding(dp(14), dp(12), dp(14), dp(12))
   visibility = View.GONE
   accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
   addTextChangedListener(object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) {
     paintNotice()
    }
   })
  }
  outerLayout.addView(
   noticeView,
   LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(4) }
  )

  contentContainer = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
  outerLayout.addView(contentContainer, LinearLayout.LayoutParams(-1, -2))
  currentLayout = contentContainer
  return contentContainer
 }

 fun rebuildBody(preserveScroll: Boolean = true, build: ScreenScaffold.() -> Unit) {
  val savedY = if (preserveScroll && ::rootScroll.isInitialized) rootScroll.scrollY else 0
  contentContainer.removeAllViews()
  into(contentContainer) { build() }
  if (savedY > 0) {
   rootScroll.post { rootScroll.scrollTo(0, savedY) }
  }
 }

 fun updateNotice(message: NoticeMessage?) {
  if (!::noticeView.isInitialized) return
  if (message == null || message.text.isEmpty()) {
   noticeView.text = ""
   paintNotice()
   return
  }
  noticeTone = mapTone(message.tone)
  noticeView.text = message.text
  paintNotice()
  if (message.scrollToTop) {
   rootScroll.post { rootScroll.smoothScrollTo(0, 0) }
  }
 }

 fun showLocalNotice(text: String, tone: NoticeTone = NoticeTone.INFO, scrollToTop: Boolean = true) {
  updateNotice(NoticeMessage(text = text, tone = tone, scrollToTop = scrollToTop))
 }

 private fun paintNotice() {
  noticeView.visibility = if (noticeView.text.isNullOrEmpty()) View.GONE else View.VISIBLE
  noticeView.background = RupiUi.rounded(noticeTone.back, dp(14).toFloat(), dp(2), noticeTone.border)
  noticeView.setTextColor(noticeTone.text)
 }

 private fun mapTone(tone: NoticeTone): RupiUi.Tone = when (tone) {
  NoticeTone.INFO -> RupiUi.Tone.INFO
  NoticeTone.ERROR -> RupiUi.Tone.ERROR
  NoticeTone.OK -> RupiUi.Tone.OK
  NoticeTone.RETRY -> RupiUi.Tone.RETRY
 }

 /** Barra blanca superior con el avatar circular de Rupi, marca "Rupi." y título actual (.sidebar de App.css). */
 private fun header(title: String): LinearLayout {
  val bar = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   background = RupiUi.topbar(activity)
  }

  val borderInset = dp(2)
  val shadowBottom = dp(3)
  val logoFrame = FrameLayout(activity).apply {
   background = RupiUi.brandLogoFrame(activity)
   setPadding(borderInset, borderInset, borderInset, borderInset + shadowBottom)
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  val avatar = ImageView(activity).apply {
   setImageResource(R.drawable.rupi_logo)
   scaleType = ImageView.ScaleType.CENTER_CROP
   clipToOutline = true
   outlineProvider = object : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
     outline.setOval(0, 0, view.width, view.height)
    }
   }
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  logoFrame.addView(avatar, FrameLayout.LayoutParams(-1, -1))
  bar.addView(logoFrame, LinearLayout.LayoutParams(dp(44), dp(47)))

  val brandText = SpannableString("Rupi.").apply {
   setSpan(ForegroundColorSpan(RupiUi.ORANGE), 4, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  }
  val names = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(dp(12), 0, 0, dp(2))
  }
  names.addView(TextView(activity).apply {
   text = brandText
   typeface = RupiUi.fontTitle(activity)
   textSize = 21f
   setTextColor(RupiUi.FOREST)
   includeFontPadding = false
  })
  names.addView(TextView(activity).apply {
   text = title
   typeface = RupiUi.fontExtraBold(activity)
   textSize = 13f
   setTextColor(RupiUi.MUTED)
   includeFontPadding = false
   if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
  })
  bar.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
  return bar
 }

 fun into(container: LinearLayout, build: ScreenScaffold.() -> Unit) {
  val outer = currentLayout
  currentLayout = container
  try {
   build()
  } finally {
   currentLayout = outer
  }
 }

 fun currentContainer(): LinearLayout = currentLayout

 fun addView(view: View, params: LinearLayout.LayoutParams? = null) {
  if (params != null) currentLayout.addView(view, params) else currentLayout.addView(view)
 }

 fun text(value: String, size: Int = 18) {
  currentLayout.addView(TextView(activity).apply {
   text = value
   textSize = size.toFloat()
   setPadding(0, dp(6), 0, dp(6))
   setLineSpacing(0f, 1.2f)
   if (size >= 20) {
    typeface = RupiUi.fontTitle(activity)
    setTextColor(RupiUi.FOREST)
    if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
   } else {
    typeface = RupiUi.fontBody(activity)
    setTextColor(RupiUi.INK)
   }
  })
 }

 fun card(build: ScreenScaffold.() -> Unit): LinearLayout =
  styledCard(RupiUi.card(activity), paddingLeftDp = 16, paddingBottomDp = 20, build = build)

 fun stickerCard(fill: Int = Color.WHITE, build: ScreenScaffold.() -> Unit): LinearLayout =
  styledCard(RupiUi.stickerCard(activity, fill), paddingLeftDp = 16, paddingBottomDp = 21, build = build)

 fun heroCard(build: ScreenScaffold.() -> Unit): LinearLayout =
  styledCard(RupiUi.heroCard(activity), paddingLeftDp = 18, paddingTopDp = 18, paddingBottomDp = 20, build = build)

 fun accentCard(stripeColor: Int, build: ScreenScaffold.() -> Unit): LinearLayout =
  styledCard(RupiUi.accentStripeCard(activity, stripeColor), paddingLeftDp = 22, paddingBottomDp = 20, build = build)

 fun goalCard(met: Boolean, build: ScreenScaffold.() -> Unit): LinearLayout =
  styledCard(
   if (met) RupiUi.goalCompletedCard(activity) else RupiUi.card(activity),
   paddingLeftDp = 16,
   paddingBottomDp = 20,
   build = build
  )

 fun readingCard(build: ScreenScaffold.() -> Unit): LinearLayout =
  styledCard(RupiUi.readingBox(activity), paddingLeftDp = 16, paddingBottomDp = 16, build = build)

 private fun styledCard(
  drawable: Drawable,
  paddingLeftDp: Int = 16,
  paddingTopDp: Int = 14,
  paddingBottomDp: Int = 18,
  build: ScreenScaffold.() -> Unit
 ): LinearLayout {
  val box = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   background = drawable
   setPadding(dp(paddingLeftDp), dp(paddingTopDp), dp(16), dp(paddingBottomDp))
  }
  currentLayout.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(6) })
  into(box, build)
  return box
 }

 /** Banner andino estilo panel de bienvenida de LoginView.tsx (cumbres verdes, sol y Rupi). */
 fun andeanWelcomeBanner(tag: String, heading: String, speech: String) {
  val frame = FrameLayout(activity).apply {
   background = GradientDrawable(
    GradientDrawable.Orientation.TL_BR,
    intArrayOf(Color.rgb(255, 251, 240), Color.rgb(244, 250, 246), Color.rgb(230, 243, 235))
   ).apply {
    cornerRadius = dp(22).toFloat()
    setStroke(dp(2), RupiUi.SOFT_LINE)
   }
   clipToOutline = true
  }

  val hills = ImageView(activity).apply {
   setImageResource(R.drawable.bg_andean_scene)
   scaleType = ImageView.ScaleType.FIT_XY
   alpha = 0.9f
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  frame.addView(hills, FrameLayout.LayoutParams(-1, dp(165), Gravity.BOTTOM))

  val content = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(dp(16), dp(14), dp(16), dp(12))
  }

  val badge = TextView(activity).apply {
   text = tag
   typeface = RupiUi.fontTitle(activity)
   textSize = 11f
   setTextColor(Color.rgb(158, 89, 45))
   background = RupiUi.rounded(Color.rgb(253, 246, 236), 999f, dp(2), RupiUi.SUN)
   setPadding(dp(12), dp(4), dp(12), dp(4))
  }
  content.addView(badge, LinearLayout.LayoutParams(-2, -2))

  val row = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
  }

  val balloon = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   background = RupiUi.speechBalloon(activity, RupiUi.LEAF, RupiUi.LEAF_DARK)
   setPadding(dp(14), dp(10), dp(14), dp(14))
  }
  balloon.addView(TextView(activity).apply {
   text = heading
   typeface = RupiUi.fontTitle(activity)
   textSize = 18f
   setTextColor(RupiUi.FOREST)
   if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
  })
  balloon.addView(TextView(activity).apply {
   text = speech
   typeface = RupiUi.fontBody(activity)
   textSize = 13.5f
   setTextColor(RupiUi.MUTED)
   setLineSpacing(0f, 1.15f)
   setPadding(0, dp(3), 0, 0)
  })
  row.addView(balloon, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })

  val mascot = ImageView(activity).apply {
   setImageResource(R.drawable.ic_rupi_mascot)
   scaleType = ImageView.ScaleType.FIT_CENTER
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  row.addView(mascot, LinearLayout.LayoutParams(dp(92), dp(98)))

  content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
  frame.addView(content, FrameLayout.LayoutParams(-1, -2))
  currentLayout.addView(frame, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6); bottomMargin = dp(6) })
 }

 /** Tarjeta compacta de mascota guía (.mascot-card de App.css). */
 fun mascotGuide(eyebrow: String, message: String) {
  val row = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   background = RupiUi.stickerCard(activity, Color.rgb(255, 253, 245))
   setPadding(dp(14), dp(12), dp(12), dp(16))
  }
  val copy = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
  copy.addView(TextView(activity).apply {
   text = eyebrow.uppercase()
   typeface = RupiUi.fontTitle(activity)
   textSize = 11f
   letterSpacing = 0.05f
   setTextColor(Color.rgb(152, 100, 63))
  })
  copy.addView(TextView(activity).apply {
   text = message
   typeface = RupiUi.fontExtraBold(activity)
   textSize = 14f
   setTextColor(RupiUi.FOREST)
   setLineSpacing(0f, 1.25f)
   setPadding(0, dp(4), 0, 0)
  })
  row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })
  row.addView(
   ImageView(activity).apply {
    setImageResource(R.drawable.ic_rupi_mascot)
    scaleType = ImageView.ScaleType.FIT_CENTER
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
   },
   LinearLayout.LayoutParams(dp(70), dp(74))
  )
  currentLayout.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(6) })
 }

 fun muted(value: String) {
  currentLayout.addView(TextView(activity).apply {
   text = value
   typeface = RupiUi.fontBody(activity)
   textSize = 14.5f
   setTextColor(RupiUi.MUTED)
   setPadding(0, dp(2), 0, dp(6))
   setLineSpacing(0f, 1.2f)
  })
 }

 fun section(title: String, subtitle: String? = null) {
  currentLayout.addView(TextView(activity).apply {
   text = title
   typeface = RupiUi.fontTitle(activity)
   textSize = 20f
   setTextColor(RupiUi.FOREST)
   setPadding(dp(2), dp(16), 0, dp(2))
   if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
  })
  if (subtitle != null) muted(subtitle)
 }

 fun chip(value: String, back: Int, border: Int, color: Int, iconRes: Int? = null, trailingValue: CharSequence? = null) {
  val label: CharSequence = if (trailingValue != null) {
   SpannableStringBuilder(iconLabel(iconRes, color, value, iconDp = 15)).append(trailingValue)
  } else {
   iconLabel(iconRes, color, value, iconDp = 15)
  }
  currentLayout.addView(
   TextView(activity).apply {
    text = label
    typeface = RupiUi.fontExtraBold(activity)
    textSize = 13f
    setTextColor(color)
    background = RupiUi.chip(activity, back, border)
    setPadding(dp(12), dp(5), dp(12), dp(5))
   },
   LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6); bottomMargin = dp(4) }
  )
 }

 fun iconLabel(iconRes: Int?, tint: Int, label: String, iconDp: Int = 18): CharSequence {
  if (iconRes == null) return label
  val drawable = activity.getDrawable(iconRes)?.mutate() ?: return label
  drawable.setTint(tint)
  val px = dp(iconDp)
  drawable.setBounds(0, 0, px, px)
  return SpannableStringBuilder("  $label").apply {
   setSpan(CenteredImageSpan(drawable), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  }
 }

 fun tile(
  title: String,
  detail: String?,
  completed: Boolean = false,
  locked: Boolean = false,
  action: () -> Unit
 ) {
  val row = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   isClickable = true
   isFocusable = true
   background = if (locked) {
    RupiUi.rounded(Color.rgb(236, 232, 224), dp(16).toFloat(), dp(2), RupiUi.LINE)
   } else {
    RupiUi.tactile(activity, RupiUi.Kind.QUIET, radiusDp = 16)
   }
   minimumHeight = dp(64)
   setPadding(dp(16), dp(12), dp(14), dp(16))
   contentDescription = if (detail == null) title else "$title. $detail"
   setOnClickListener { if (!isBusy) action() }
  }
  val texts = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
  if (detail != null) {
   texts.addView(TextView(activity).apply {
    text = detail
    textSize = 13f
    typeface = RupiUi.fontExtraBold(activity)
    setTextColor(RupiUi.MUTED)
   })
  }
  texts.addView(TextView(activity).apply {
   text = title
   textSize = 17f
   typeface = RupiUi.fontExtraBold(activity)
   setTextColor(RupiUi.FOREST)
  })
  row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
  row.addView(
   vectorIconView(
    iconRes = when {
     locked -> R.drawable.ic_lock
     completed -> R.drawable.ic_check
     else -> R.drawable.ic_chevron_right
    },
    tint = if (locked) RupiUi.MUTED else RupiUi.LEAF
   ),
   LinearLayout.LayoutParams(dp(22), dp(22)).apply { leftMargin = dp(8) }
  )
  currentLayout.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(2) })
 }

 /** Parada visual estilo "Mi Camino" (.trail-stop + .lesson-node + .node-label de App.css). */
 fun routeStopTile(
  sequence: Int,
  title: String,
  stateLabel: String,
  estimatedMinutes: Int,
  completed: Boolean,
  locked: Boolean,
  action: () -> Unit
 ) {
  val container = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   isClickable = true
   isFocusable = true
   val mins = if (estimatedMinutes > 0) " · $estimatedMinutes min" else ""
   contentDescription = "Parada $sequence: $title. $stateLabel$mins"
   setOnClickListener { if (!isBusy) action() }
  }

  val nodeCircle = FrameLayout(activity).apply {
   setPadding(0, 0, 0, dp(4))
   background = RupiUi.routeNodeCircle(activity, completed = completed, locked = locked)
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  nodeCircle.addView(
   vectorIconView(
    iconRes = when {
     completed -> R.drawable.ic_check
     locked -> R.drawable.ic_lock
     else -> R.drawable.ic_star
    },
    tint = when {
     completed -> Color.WHITE
     locked -> RupiUi.MUTED
     else -> Color.rgb(255, 240, 196)
    }
   ),
   FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
  )
  container.addView(nodeCircle, LinearLayout.LayoutParams(dp(50), dp(54)).apply { rightMargin = dp(12) })

  val cardKind = when {
   completed || !locked -> if (!completed && !locked) RupiUi.Kind.STICKER else RupiUi.Kind.QUIET
   else -> null
  }
  val labelBox = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   minimumHeight = dp(64)
   background = if (cardKind != null) {
    RupiUi.tactile(activity, cardKind, radiusDp = 16)
   } else {
    RupiUi.rounded(Color.rgb(236, 232, 224), dp(16).toFloat(), dp(2), RupiUi.LINE)
   }
   setPadding(dp(14), dp(10), dp(12), dp(14))
  }

  val texts = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
  val metaRow = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
  }
  val (pillBg, pillBorder, pillText) = when {
   completed -> Triple(RupiUi.LEAF_SOFT, Color.rgb(182, 211, 168), RupiUi.LEAF_DARK)
   locked -> Triple(RupiUi.PAPER_DEEP, RupiUi.LINE, RupiUi.MUTED)
   else -> Triple(RupiUi.SUN, RupiUi.FOREST, RupiUi.FOREST)
  }
  metaRow.addView(TextView(activity).apply {
   text = "Parada $sequence"
   typeface = RupiUi.fontTitle(activity)
   textSize = 11f
   setTextColor(pillText)
   background = RupiUi.pill(activity, pillBg, pillBorder)
   setPadding(dp(8), dp(2), dp(8), dp(2))
  })
  val mins = if (estimatedMinutes > 0) " · $estimatedMinutes min" else ""
  metaRow.addView(TextView(activity).apply {
   text = "$stateLabel$mins"
   typeface = RupiUi.fontBold(activity)
   textSize = 12f
   setTextColor(RupiUi.MUTED)
   setPadding(dp(8), 0, 0, 0)
  })
  texts.addView(metaRow)
  texts.addView(TextView(activity).apply {
   text = title
   typeface = RupiUi.fontExtraBold(activity)
   textSize = 16.5f
   setTextColor(RupiUi.FOREST)
   setPadding(0, dp(4), 0, 0)
  })
  labelBox.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
  if (!locked) {
   labelBox.addView(
    vectorIconView(
     iconRes = R.drawable.ic_chevron_right,
     tint = if (completed) RupiUi.LEAF else RupiUi.ORANGE
    ),
    LinearLayout.LayoutParams(dp(22), dp(22)).apply { leftMargin = dp(8) }
   )
  }
  container.addView(labelBox, LinearLayout.LayoutParams(0, -2, 1f))
  currentLayout.addView(container, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(4) })
 }

 /** Tarjeta interactiva de Flashcard (.memory-card en Flashcards.css). */
 fun flashcardButton(label: String, revealed: Boolean, action: () -> Unit): Button {
  val cardBtn = Button(activity).apply {
   text = label
   isAllCaps = false
   minHeight = dp(220)
   typeface = RupiUi.fontExtraBold(activity)
   textSize = 18f
   stateListAnimator = null
   elevation = 0f
   background = RupiUi.flashcard(activity, revealed)
   setTextColor(RupiUi.FOREST)
   gravity = Gravity.CENTER
   isSingleLine = false
   maxLines = Int.MAX_VALUE
   setHorizontallyScrolling(false)
   setPadding(dp(20), dp(20), dp(20), dp(25))
   setLineSpacing(0f, 1.3f)
   setOnClickListener { if (!isBusy) action() }
  }
  currentLayout.addView(cardBtn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(8) })
  return cardBtn
 }

 fun feedback(value: String, tone: NoticeTone, iconRes: Int? = null) {
  val uiTone = mapTone(tone)
  currentLayout.addView(
   TextView(activity).apply {
    text = iconLabel(iconRes, uiTone.text, value, iconDp = 17)
    textSize = 15.5f
    typeface = RupiUi.fontExtraBold(activity)
    setTextColor(uiTone.text)
    setLineSpacing(0f, 1.2f)
    background = RupiUi.rounded(uiTone.back, dp(14).toFloat(), dp(2), uiTone.border)
    setPadding(dp(14), dp(12), dp(14), dp(12))
   },
   LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(4) }
  )
 }

 fun styleOption(radio: RadioButton) {
  radio.background = RupiUi.option(activity)
  radio.buttonTintList = ColorStateList.valueOf(RupiUi.LEAF)
  radio.typeface = RupiUi.fontBold(activity)
  radio.textSize = 16.5f
  radio.minHeight = dp(52)
  radio.layoutParams = RadioGroup.LayoutParams(-1, -2).apply { topMargin = dp(6); bottomMargin = dp(2) }
 }

 fun listen(subject: String, value: () -> String) {
  button("Escuchar $subject", kind = RupiUi.Kind.LISTEN, iconRes = R.drawable.ic_volume_2) {
   if (voice?.speak(value()) != true) {
    showLocalNotice("Para escuchar, instala o activa una voz en español en los ajustes de Android (Texto a voz).")
   }
  }.contentDescription = "Escuchar $subject"
 }

 fun backButton(label: String, action: () -> Unit): Button =
  button(label, kind = RupiUi.Kind.STICKER, iconRes = R.drawable.ic_arrow_left, action = action)

 fun input(label: String, password: Boolean = false): EditText {
  currentLayout.addView(TextView(activity).apply {
   text = label
   typeface = RupiUi.fontExtraBold(activity)
   textSize = 15f
   setTextColor(RupiUi.FOREST)
   setPadding(0, dp(12), 0, dp(6))
  })
  val field = EditText(activity).apply {
   hint = label
   contentDescription = label
   minHeight = dp(52)
   isSaveEnabled = false
   textSize = 16.5f
   setTextColor(RupiUi.INK)
   setHintTextColor(Color.rgb(112, 130, 118))
   background = RupiUi.field(activity)
   setPadding(dp(14), dp(12), dp(14), dp(12))
   inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
   typeface = RupiUi.fontBody(activity)
  }
  currentLayout.addView(field, LinearLayout.LayoutParams(-1, -2))
  return field
 }

 fun button(
  label: String,
  enabled: Boolean = true,
  kind: RupiUi.Kind = RupiUi.Kind.PRIMARY,
  iconRes: Int? = null,
  action: () -> Unit
 ): Button {
  val button = Button(activity).apply {
   text = iconLabel(iconRes, kind.text, label, iconDp = 18)
   contentDescription = label
   isAllCaps = false
   minHeight = dp(52)
   isEnabled = enabled
   typeface = RupiUi.fontExtraBold(activity)
   textSize = 16f
   stateListAnimator = null
   elevation = 0f
   background = RupiUi.tactile(activity, kind)
   setTextColor(RupiUi.tactileText(kind))
   setOnClickListener { if (!isBusy) action() }
   isSingleLine = false
   maxLines = Int.MAX_VALUE
   setHorizontallyScrolling(false)
   minWidth = dp(48)
   setPadding(dp(16), dp(12), dp(16), dp(16))
  }
  currentLayout.addView(button, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(6) })
  return button
 }

 private fun vectorIconView(iconRes: Int, tint: Int): ImageView =
  ImageView(activity).apply {
   val d = activity.getDrawable(iconRes)?.mutate()
   d?.setTint(tint)
   setImageDrawable(d)
   scaleType = ImageView.ScaleType.FIT_CENTER
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }

 private class CenteredImageSpan(drawable: Drawable) : ImageSpan(drawable) {
  override fun draw(
   canvas: Canvas,
   text: CharSequence,
   start: Int,
   end: Int,
   x: Float,
   top: Int,
   y: Int,
   bottom: Int,
   paint: Paint
  ) {
   val d = drawable
   val fm = paint.fontMetricsInt
   val transY = y + (fm.descent + fm.ascent) / 2 - d.bounds.height() / 2
   canvas.save()
   canvas.translate(x, transY.toFloat())
   d.draw(canvas)
   canvas.restore()
  }
 }
}
