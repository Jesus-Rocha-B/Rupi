package pe.rupi.mobile.ui.components

import android.app.Activity
import android.content.Context
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
import android.view.inputmethod.EditorInfo
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
 val context: Context get() = activity
 var isBusy: Boolean = false
 private var noticeTone: RupiUi.Tone = RupiUi.Tone.INFO

 lateinit var rootScroll: ScrollView
  private set
 lateinit var contentContainer: LinearLayout
  private set
 private lateinit var noticeContainer: LinearLayout
 private lateinit var noticeView: TextView
 private lateinit var noticeCloseBtn: TextView
 private lateinit var currentLayout: LinearLayout

 fun dp(n: Int): Int = RupiUi.dp(activity, n)

 fun mountLoginPage(): LinearLayout {
  voice?.stop()
  val frame = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   background = RupiUi.Paper(activity)
  }
  val scroll = ScrollView(activity).apply {
   isFillViewport = true
   clipToPadding = false
  }
  rootScroll = scroll

  val outerLayout = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(dp(16), dp(12), dp(16), dp(24))
  }
  scroll.addView(outerLayout, FrameLayout.LayoutParams(-1, -2))
  frame.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
  activity.setContentView(frame)

  frame.setOnApplyWindowInsetsListener { _, insets ->
   @Suppress("DEPRECATION")
   val top = insets.systemWindowInsetTop
   @Suppress("DEPRECATION")
   val bottom = insets.systemWindowInsetBottom
   outerLayout.setPadding(dp(16), top + dp(12), dp(16), dp(24) + bottom)
   insets
  }

  setupNoticeView(outerLayout)
  contentContainer = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
  outerLayout.addView(contentContainer, LinearLayout.LayoutParams(-1, -2))
  currentLayout = contentContainer
  return contentContainer
 }

 fun mountDuolingoShell(
  streakDays: Int = 3,
  xpPoints: Int = 120,
  hearts: Int = 5,
  activeTab: Int = 0,
  onTabSelected: (Int) -> Unit
 ): LinearLayout {
  voice?.stop()
  val frame = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   background = RupiUi.Paper(activity)
  }

  // TOP STATS BAR (Estilo Duolingo)
  val topBar = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   background = RupiUi.rounded(RupiUi.CARD_SURFACE, dp(18).toFloat(), dp(1), RupiUi.SOFT_LINE)
   setPadding(dp(14), dp(8), dp(14), dp(8))
  }

  // Bandera de Perú / Grado a la izquierda
  val flagBadge = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   background = RupiUi.rounded(RupiUi.LEAF_SOFT, dp(18).toFloat(), dp(1), RupiUi.SOFT_LINE)
   minimumHeight = dp(40)
   setPadding(dp(8), dp(4), dp(10), dp(4))
   addView(ImageView(activity).apply {
    setImageResource(R.drawable.ic_flag_pe)
    scaleType = ImageView.ScaleType.FIT_CENTER
   }, LinearLayout.LayoutParams(dp(24), dp(18)))
   addView(TextView(activity).apply {
    text = "Primaria · 3.°"
    typeface = RupiUi.fontExtraBold(activity)
    textSize = 12.5f
    setTextColor(RupiUi.FOREST)
    setPadding(dp(6), 0, 0, 0)
   })
  }
  topBar.addView(flagBadge, LinearLayout.LayoutParams(0, -2, 1f))

  fun statItem(iconRes: Int, value: String, tintColor: Int): LinearLayout {
   return LinearLayout(activity).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    background = RupiUi.rounded(Color.WHITE, dp(16).toFloat(), dp(1), RupiUi.LINE)
    minimumHeight = dp(38)
    setPadding(dp(7), dp(3), dp(7), dp(3))
    contentDescription = "$value ${when (iconRes) {
     R.drawable.ic_flame -> "días de racha"
     R.drawable.ic_gem -> "puntos de experiencia"
     else -> "vidas"
    }}"
    addView(ImageView(activity).apply {
     setImageResource(iconRes)
     val d = drawable.mutate()
     d.setTint(tintColor)
     setImageDrawable(d)
    }, LinearLayout.LayoutParams(dp(17), dp(17)))
    addView(TextView(activity).apply {
     text = value
     typeface = RupiUi.fontExtraBold(activity)
     textSize = 13f
     setTextColor(RupiUi.FOREST)
     setPadding(dp(3), 0, 0, 0)
    })
   }
  }

  fun statParams() = LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(5) }
  topBar.addView(statItem(R.drawable.ic_flame, "$streakDays", Color.rgb(235, 131, 36)), statParams())
  topBar.addView(statItem(R.drawable.ic_gem, "$xpPoints", Color.rgb(0, 145, 176)), statParams())
  topBar.addView(statItem(R.drawable.ic_heart, "$hearts", Color.rgb(222, 118, 24)), statParams())
  frame.addView(topBar, LinearLayout.LayoutParams(-1, -2))

  // SCROLLVIEW (Contenido en medio)
  val scroll = ScrollView(activity).apply {
   isFillViewport = true
   clipToPadding = false
  }
  rootScroll = scroll

  val outerLayout = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(dp(14), dp(8), dp(14), dp(16))
  }
  scroll.addView(outerLayout, FrameLayout.LayoutParams(-1, -2))
  frame.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

  // FIXED BOTTOM NAVBAR (Barra de navegación fija estilo Duolingo sin recuadros toscos)
  val bottomNav = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   background = RupiUi.rounded(RupiUi.CARD_SURFACE, dp(20).toFloat(), dp(1), RupiUi.SOFT_LINE)
   setPadding(dp(5), dp(6), dp(5), dp(8))
   elevation = dp(8).toFloat()
  }

  val tabThemes = IntArray(5) { RupiUi.LEAF }

  val tabButtons = mutableListOf<LinearLayout>()

  fun navTab(iconRes: Int, label: String, index: Int, isSelected: Boolean): LinearLayout {
   val themeColor = tabThemes[index % tabThemes.size]
   val tabBox = LinearLayout(activity).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER
    isClickable = true
    isFocusable = true
    minimumHeight = dp(58)
    background = if (isSelected) RupiUi.tactile(activity, RupiUi.Kind.LEAF, radiusDp = 15)
     else RupiUi.rounded(Color.WHITE, dp(15).toFloat(), dp(1), RupiUi.SOFT_LINE)
    setPadding(dp(3), dp(5), dp(3), dp(5))
    contentDescription = if (isSelected) "$label, seleccionado" else label
    this.isSelected = isSelected
   }

   val iconView = ImageView(activity).apply {
    setImageResource(iconRes)
    val d = drawable.mutate()
    d.setTint(if (isSelected) Color.WHITE else RupiUi.FOREST)
    setImageDrawable(d)
    if (isSelected) {
     scaleX = 1.1f
     scaleY = 1.1f
    }
   }
   tabBox.addView(iconView, LinearLayout.LayoutParams(dp(22), dp(22)))

   val textView = TextView(activity).apply {
    text = label
    typeface = if (isSelected) RupiUi.fontExtraBold(activity) else RupiUi.fontBody(activity)
    textSize = 9.5f
    setTextColor(if (isSelected) Color.WHITE else RupiUi.MUTED)
    gravity = Gravity.CENTER
    setPadding(0, dp(3), 0, 0)
   }
   tabBox.addView(textView)

   val dotIndicator = View(activity).apply {
    background = RupiUi.rounded(if (isSelected) themeColor else Color.TRANSPARENT, dp(2).toFloat(), 0, 0)
   }
   tabBox.addView(dotIndicator, LinearLayout.LayoutParams(dp(16), dp(3)).apply { topMargin = dp(2) })

   tabBox.setOnClickListener {
    onTabSelected(index)
    // Tactile bounce animation on tap
    tabBox.animate().scaleX(1.08f).scaleY(1.08f).setDuration(100).withEndAction {
     tabBox.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
    }.start()

    tabButtons.forEachIndexed { i, box ->
     val active = (i == index)
     val color = tabThemes[i % tabThemes.size]
     val iv = box.getChildAt(0) as? ImageView
     val tv = box.getChildAt(1) as? TextView
     val dot = box.getChildAt(2)

     val tint = if (active) Color.WHITE else RupiUi.FOREST
     iv?.drawable?.mutate()?.setTint(tint)
     iv?.scaleX = if (active) 1.1f else 1.0f
     iv?.scaleY = if (active) 1.1f else 1.0f
     tv?.typeface = if (active) RupiUi.fontExtraBold(activity) else RupiUi.fontBody(activity)
     tv?.setTextColor(tint)
     box.background = if (active) RupiUi.tactile(activity, RupiUi.Kind.LEAF, radiusDp = 15)
      else RupiUi.rounded(Color.WHITE, dp(15).toFloat(), dp(1), RupiUi.SOFT_LINE)
     box.isSelected = active
     box.contentDescription = if (active) "${tv?.text}, seleccionado" else tv?.text?.toString()
     dot?.background = RupiUi.rounded(if (active) color else Color.TRANSPARENT, dp(2).toFloat(), 0, 0)
    }
   }
   return tabBox
  }

  val tabParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
   setMargins(dp(2), 0, dp(2), 0)
  }

  val tabs = listOf(
   Triple(R.drawable.ic_home, "Camino", 0),
   Triple(R.drawable.ic_message_circle, "Tutor IA", 1),
   Triple(R.drawable.ic_chest, "Misiones", 2),
   Triple(R.drawable.ic_award, "Logros", 3),
   Triple(R.drawable.ic_user, "Perfil", 4)
  )

  tabs.forEach { (icon, title, idx) ->
   val tView = navTab(icon, title, idx, idx == activeTab)
   tabButtons.add(tView)
   bottomNav.addView(tView, tabParams)
  }

  frame.addView(bottomNav, LinearLayout.LayoutParams(-1, -2))
  activity.setContentView(frame)

  frame.setOnApplyWindowInsetsListener { _, insets ->
   @Suppress("DEPRECATION")
   val top = insets.systemWindowInsetTop
   @Suppress("DEPRECATION")
   val bottom = insets.systemWindowInsetBottom
   topBar.setPadding(dp(14), top + dp(8), dp(14), dp(8))
   bottomNav.setPadding(dp(6), dp(4), dp(6), dp(6) + bottom)
   insets
  }

  setupNoticeView(outerLayout)
  contentContainer = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
  outerLayout.addView(contentContainer, LinearLayout.LayoutParams(-1, -2))
  currentLayout = contentContainer
  return contentContainer
 }

 fun mountPage(title: String): LinearLayout {
  voice?.stop()
  val frame = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   background = RupiUi.Paper(activity)
  }

  var bar: LinearLayout? = null
  if (title.isNotBlank()) {
   bar = LinearLayout(activity).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    background = RupiUi.rounded(Color.WHITE, 0f, dp(1), RupiUi.LINE)
    setPadding(dp(12), dp(10), dp(16), dp(10))
   }

   val backBtn = ImageView(activity).apply {
    setImageResource(R.drawable.ic_arrow_left)
    val d = drawable.mutate()
    d.setTint(RupiUi.FOREST)
    setImageDrawable(d)
    isClickable = true
    isFocusable = true
    background = RupiUi.rounded(RupiUi.LEAF_SOFT, dp(10).toFloat(), dp(1), RupiUi.LEAF)
    setPadding(dp(6), dp(6), dp(6), dp(6))
    setOnClickListener {
     (activity as? androidx.activity.ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() ?: run {
      @Suppress("DEPRECATION")
      activity.onBackPressed()
     }
    }
   }
   bar.addView(backBtn, LinearLayout.LayoutParams(dp(36), dp(36)).apply {
    rightMargin = dp(10)
   })

   bar.addView(TextView(activity).apply {
    text = title
    typeface = RupiUi.fontTitle(activity)
    textSize = 17f
    setTextColor(RupiUi.FOREST)
    maxLines = 1
    ellipsize = android.text.TextUtils.TruncateAt.END
   }, LinearLayout.LayoutParams(0, -2, 1f))
   frame.addView(bar, LinearLayout.LayoutParams(-1, -2))
  }

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
   if (bar != null) {
    bar.setPadding(dp(12), top + dp(6), dp(16), dp(10))
    outerLayout.setPadding(dp(16), dp(8), dp(16), dp(24) + bottom)
   } else {
    outerLayout.setPadding(dp(16), top + dp(8), dp(16), dp(24) + bottom)
   }
   insets
  }

  setupNoticeView(outerLayout)
  contentContainer = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
  outerLayout.addView(contentContainer, LinearLayout.LayoutParams(-1, -2))
  currentLayout = contentContainer
  return contentContainer
 }

 private fun setupNoticeView(outerLayout: LinearLayout) {
  noticeContainer = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   visibility = View.GONE
   setPadding(dp(14), dp(10), dp(8), dp(10))
  }

  noticeView = TextView(activity).apply {
   typeface = RupiUi.fontExtraBold(activity)
   textSize = 14f
   accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
   addTextChangedListener(object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) {
     paintNotice()
    }
   })
  }
  noticeContainer.addView(noticeView, LinearLayout.LayoutParams(0, -2, 1f))

  noticeCloseBtn = TextView(activity).apply {
   text = "✕"
   textSize = 15f
   typeface = RupiUi.fontExtraBold(activity)
   gravity = Gravity.CENTER
   minimumWidth = dp(36)
   minimumHeight = dp(36)
   isClickable = true
   isFocusable = true
   contentDescription = "Cerrar aviso"
   setPadding(dp(6), dp(4), dp(6), dp(4))
   setOnClickListener {
    noticeView.text = ""
    noticeContainer.visibility = View.GONE
   }
  }
  noticeContainer.addView(noticeCloseBtn, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(6) })

  outerLayout.addView(
   noticeContainer,
   LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(4) }
  )
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
  val hasText = !noticeView.text.isNullOrEmpty()
  if (::noticeContainer.isInitialized) {
   noticeContainer.visibility = if (hasText) View.VISIBLE else View.GONE
   if (hasText) {
    noticeContainer.background = RupiUi.rounded(noticeTone.back, dp(14).toFloat(), dp(2), noticeTone.border)
    noticeView.setTextColor(noticeTone.text)
    if (::noticeCloseBtn.isInitialized) {
     noticeCloseBtn.setTextColor(noticeTone.text)
    }
   }
  }
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
 fun andeanWelcomeBanner(tag: String, heading: String, speech: String, compact: Boolean = false) {
  val frame = FrameLayout(activity).apply {
   background = GradientDrawable(
    GradientDrawable.Orientation.TL_BR,
    intArrayOf(Color.rgb(255, 251, 240), Color.rgb(244, 250, 246), Color.rgb(230, 243, 235))
   ).apply {
    cornerRadius = dp(if (compact) 16 else 22).toFloat()
    setStroke(dp(2), RupiUi.SOFT_LINE)
   }
   clipToOutline = true
  }

  val hillsHeight = if (compact) 100 else 165
  val hills = ImageView(activity).apply {
   setImageResource(R.drawable.bg_andean_scene)
   scaleType = ImageView.ScaleType.FIT_XY
   alpha = 0.9f
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  frame.addView(hills, FrameLayout.LayoutParams(-1, dp(hillsHeight), Gravity.BOTTOM))

  val content = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   val padH = if (compact) 12 else 16
   val padTop = if (compact) 10 else 14
   val padBottom = if (compact) 8 else 12
   setPadding(dp(padH), dp(padTop), dp(padH), dp(padBottom))
  }

  val badge = TextView(activity).apply {
   text = tag
   typeface = RupiUi.fontTitle(activity)
   textSize = if (compact) 10.5f else 11f
   setTextColor(Color.rgb(158, 89, 45))
   background = RupiUi.rounded(Color.rgb(253, 246, 236), 999f, dp(2), RupiUi.SUN)
   setPadding(dp(if (compact) 10 else 12), dp(if (compact) 3 else 4), dp(if (compact) 10 else 12), dp(if (compact) 3 else 4))
  }
  content.addView(badge, LinearLayout.LayoutParams(-2, -2))

  val row = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
  }

  val balloon = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   background = RupiUi.speechBalloon(activity, RupiUi.LEAF, RupiUi.LEAF_DARK)
   val bPad = if (compact) 10 else 14
   val bPadV = if (compact) 8 else 10
   setPadding(dp(bPad), dp(bPadV), dp(bPad), dp(bPad))
  }
  balloon.addView(TextView(activity).apply {
   text = heading
   typeface = RupiUi.fontTitle(activity)
   textSize = if (compact) 16f else 18f
   setTextColor(RupiUi.FOREST)
   if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
  })
  balloon.addView(TextView(activity).apply {
   text = speech
   typeface = RupiUi.fontBody(activity)
   textSize = if (compact) 12.5f else 13.5f
   setTextColor(RupiUi.MUTED)
   setLineSpacing(0f, 1.15f)
   setPadding(0, dp(2), 0, 0)
  })
  row.addView(balloon, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })

  val mascotW = if (compact) 68 else 92
  val mascotH = if (compact) 74 else 98
  val mascot = ImageView(activity).apply {
   setImageResource(R.drawable.ic_rupi_mascot)
   scaleType = ImageView.ScaleType.FIT_CENTER
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  row.addView(mascot, LinearLayout.LayoutParams(dp(mascotW), dp(mascotH)))

  content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(if (compact) 6 else 10) })
  frame.addView(content, FrameLayout.LayoutParams(-1, -2))
  currentLayout.addView(frame, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4); bottomMargin = dp(6) })
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
   minimumHeight = dp(54)
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
  radio.minWidth = dp(48)
  radio.isFocusable = true
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
  button(label, kind = RupiUi.Kind.STICKER, iconRes = R.drawable.ic_arrow_left, action = action).apply {
   contentDescription = "Volver: $label"
  }

 fun input(
  label: String,
  password: Boolean = false,
  hintText: String? = null,
  iconRes: Int? = null,
  imeAction: Int = if (password) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
 ): EditText {
  if (label.isNotBlank()) {
   currentLayout.addView(TextView(activity).apply {
    text = label
    typeface = RupiUi.fontExtraBold(activity)
    textSize = 14f
    setTextColor(RupiUi.FOREST)
    setPadding(0, dp(8), 0, dp(4))
   })
  }
  val field = EditText(activity).apply {
   hint = hintText ?: label
   contentDescription = label.ifBlank { hintText ?: "Campo de texto" }
   minimumHeight = dp(54)
   isSaveEnabled = false
   textSize = 15.5f
   setTextColor(RupiUi.INK)
   setHintTextColor(Color.rgb(150, 165, 155))
   background = RupiUi.field(activity)
   setPadding(dp(16), dp(12), dp(16), dp(12))
   inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
   imeOptions = imeAction
   typeface = RupiUi.fontBody(activity)

   if (iconRes != null) {
    val d = androidx.core.content.ContextCompat.getDrawable(activity, iconRes)?.mutate()
    d?.setTint(RupiUi.LEAF)
    setCompoundDrawablesRelativeWithIntrinsicBounds(d, null, null, null)
    compoundDrawablePadding = dp(12)
   }
  }
  currentLayout.addView(field, LinearLayout.LayoutParams(-1, -2).apply {
   bottomMargin = dp(6)
  })
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
