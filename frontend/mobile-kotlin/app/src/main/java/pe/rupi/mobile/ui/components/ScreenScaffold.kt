package pe.rupi.mobile.ui.components

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import pe.rupi.mobile.RupiUi
import pe.rupi.mobile.RupiVoice
import pe.rupi.mobile.ui.state.NoticeMessage
import pe.rupi.mobile.ui.state.NoticeTone

/**
 * Constructor imperativo de pantallas nativas reutilizable para la capa View (sin lógica de red ni negocio).
 * Separa el contenedor del mensaje superior (notice) del cuerpo para evitar perder scroll o campos editados.
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
   setPadding(dp(16), dp(12), dp(16), dp(16))
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
   bar.setPadding(dp(16), top + dp(12), dp(16), dp(16))
   outerLayout.setPadding(dp(16), dp(8), dp(16), dp(24) + bottom)
   insets
  }

  noticeView = TextView(activity).apply {
   typeface = RupiUi.BOLD
   textSize = 16f
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

 private fun header(title: String): LinearLayout {
  val bar = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   background = RupiUi.topbar(activity)
  }
  bar.addView(
   TextView(activity).apply {
    text = "R"
    typeface = RupiUi.TITLE
    textSize = 22f
    setTextColor(Color.WHITE)
    gravity = Gravity.CENTER
    background = RupiUi.brandMark(activity)
    setPadding(0, 0, 0, dp(4))
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
   },
   LinearLayout.LayoutParams(dp(44), dp(48))
  )
  val names = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(dp(12), 0, 0, 0)
  }
  names.addView(TextView(activity).apply {
   text = "Rupi"
   typeface = RupiUi.TITLE
   textSize = 20f
   setTextColor(Color.WHITE)
  })
  names.addView(TextView(activity).apply {
   text = title
   typeface = RupiUi.BOLD
   textSize = 14f
   setTextColor(Color.rgb(214, 232, 220))
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
   setPadding(0, dp(8), 0, dp(8))
   setLineSpacing(0f, 1.15f)
   if (size >= 20) {
    typeface = RupiUi.TITLE
    setTextColor(RupiUi.FOREST)
    if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
   } else {
    setTextColor(RupiUi.INK)
   }
  })
 }

 fun card(build: ScreenScaffold.() -> Unit): LinearLayout {
  val box = LinearLayout(activity).apply {
   orientation = LinearLayout.VERTICAL
   background = RupiUi.card(activity)
   setPadding(dp(16), dp(12), dp(16), dp(20))
  }
  currentLayout.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(6) })
  into(box, build)
  return box
 }

 fun muted(value: String) {
  currentLayout.addView(TextView(activity).apply {
   text = value
   textSize = 15f
   setTextColor(RupiUi.MUTED)
   setPadding(0, dp(2), 0, dp(6))
   setLineSpacing(0f, 1.15f)
  })
 }

 fun section(title: String, subtitle: String? = null) {
  currentLayout.addView(TextView(activity).apply {
   text = title
   typeface = RupiUi.TITLE
   textSize = 20f
   setTextColor(RupiUi.FOREST)
   setPadding(dp(2), dp(18), 0, dp(2))
   if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
  })
  if (subtitle != null) muted(subtitle)
 }

 fun chip(value: String, back: Int, border: Int, color: Int) {
  currentLayout.addView(
   TextView(activity).apply {
    text = value
    typeface = RupiUi.BOLD
    textSize = 13f
    setTextColor(color)
    background = RupiUi.chip(activity, back, border)
    setPadding(dp(12), dp(5), dp(12), dp(5))
   },
   LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6); bottomMargin = dp(4) }
  )
 }

 fun tile(title: String, detail: String?, mark: String = "›", locked: Boolean = false, action: () -> Unit) {
  val row = LinearLayout(activity).apply {
   orientation = LinearLayout.HORIZONTAL
   gravity = Gravity.CENTER_VERTICAL
   isClickable = true
   isFocusable = true
   background = if (locked) {
    RupiUi.rounded(Color.rgb(236, 232, 224), dp(14).toFloat(), dp(2), RupiUi.LINE)
   } else {
    RupiUi.tactile(activity, RupiUi.Kind.QUIET)
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
    typeface = RupiUi.BOLD
    setTextColor(RupiUi.MUTED)
   })
  }
  texts.addView(TextView(activity).apply {
   text = title
   textSize = 17f
   typeface = RupiUi.BOLD
   setTextColor(RupiUi.FOREST)
  })
  row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
  row.addView(TextView(activity).apply {
   text = mark
   textSize = if (mark == "›") 28f else 22f
   setTextColor(if (locked) RupiUi.MUTED else RupiUi.LEAF)
   setPadding(dp(8), 0, 0, 0)
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  })
  currentLayout.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(2) })
 }

 fun feedback(value: String, tone: NoticeTone) {
  val uiTone = mapTone(tone)
  currentLayout.addView(
   TextView(activity).apply {
    text = value
    textSize = 16f
    typeface = RupiUi.BOLD
    setTextColor(uiTone.text)
    setLineSpacing(0f, 1.15f)
    background = RupiUi.rounded(uiTone.back, dp(14).toFloat(), dp(2), uiTone.border)
    setPadding(dp(14), dp(12), dp(14), dp(12))
   },
   LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(4) }
  )
 }

 fun styleOption(radio: RadioButton) {
  radio.background = RupiUi.option(activity)
  radio.buttonTintList = ColorStateList.valueOf(RupiUi.SKY)
  radio.textSize = 17f
  radio.minHeight = dp(52)
  radio.layoutParams = RadioGroup.LayoutParams(-1, -2).apply { topMargin = dp(6); bottomMargin = dp(2) }
 }

 fun listen(subject: String, value: () -> String) {
  button("🔊 Escuchar $subject", kind = RupiUi.Kind.SKY) {
   if (voice?.speak(value()) != true) {
    showLocalNotice("Para escuchar, instala o activa una voz en español en los ajustes de Android (Texto a voz).")
   }
  }.contentDescription = "Escuchar $subject"
 }

 fun input(label: String, password: Boolean = false): EditText {
  currentLayout.addView(TextView(activity).apply {
   text = label
   typeface = RupiUi.BOLD
   textSize = 15f
   setTextColor(RupiUi.FOREST)
   setPadding(0, dp(12), 0, dp(6))
  })
  val field = EditText(activity).apply {
   hint = label
   contentDescription = label
   minHeight = dp(52)
   isSaveEnabled = false
   textSize = 17f
   setTextColor(RupiUi.INK)
   setHintTextColor(Color.rgb(122, 135, 124))
   background = RupiUi.field(activity)
   setPadding(dp(14), dp(12), dp(14), dp(12))
   inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
   typeface = Typeface.DEFAULT
  }
  currentLayout.addView(field, LinearLayout.LayoutParams(-1, -2))
  return field
 }

 fun button(
  label: String,
  enabled: Boolean = true,
  kind: RupiUi.Kind = RupiUi.Kind.PRIMARY,
  action: () -> Unit
 ): Button {
  val button = Button(activity).apply {
   text = label
   isAllCaps = false
   minHeight = dp(52)
   isEnabled = enabled
   typeface = RupiUi.BOLD
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
}
