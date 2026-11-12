package pe.rupi.mobile

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable

/**
 * Kit visual del estudiante (alineado con App.css, AuthViews.css, StudentDashboardView.css y DailyGoalRing.css):
 * paleta oficial RUPI, tipografía Nunito, superficies de cuaderno cálido, bordes sticker #173e30 y botones 3D táctiles.
 */
object RupiUi {
 val FOREST = Color.rgb(23, 62, 48)            // #173e30 (--ink, --rupi-border-sticker)
 val LEAF = Color.rgb(49, 114, 75)             // #31724b (--rupi-green-leaf)
 val LEAF_DARK = Color.rgb(33, 79, 52)         // #214f34 (--rupi-green-shadow)
 val LEAF_SOFT = Color.rgb(233, 243, 231)      // #e9f3e7
 val ORANGE = Color.rgb(249, 87, 56)           // #f95738 (--rupi-orange)
 val ORANGE_DARK = Color.rgb(204, 58, 29)      // #cc3a1d (--rupi-orange-shadow)
 val SUN = Color.rgb(255, 210, 106)            // #ffd26a (--rupi-yellow-sun)
 val SUN_DARK = Color.rgb(201, 142, 43)        // #c98e2b (--rupi-yellow-shadow)
 val GOLD_STROKE = Color.rgb(217, 154, 20)     // #d99a14
 val FLASHCARD_FRONT = Color.rgb(255, 245, 216)// #fff5d8
 val FLASHCARD_GOLD = Color.rgb(185, 134, 40)  // #b98628
 val SKY = Color.rgb(36, 106, 145)             // #246a91 (--rupi-blue-sky)
 val SKY_DARK = Color.rgb(24, 76, 104)         // #184c68 (--rupi-blue-shadow)
 val INK = FOREST
 val MUTED = Color.rgb(67, 97, 76)             // #43614c (--muted)
 val PAPER = Color.rgb(250, 248, 245)          // #faf8f5
 val CARD_SURFACE = Color.rgb(255, 253, 249)   // #fffdf9
 val PAPER_DEEP = Color.rgb(243, 239, 230)     // #f3efe6
 val LINE = Color.rgb(226, 215, 197)           // #e2d7c5
 val LINE_SHADOW = Color.rgb(208, 195, 174)    // #d0c3ae
 val SOFT_LINE = Color.rgb(219, 232, 210)      // #dbe8d2 (--line)
 val DANGER = Color.rgb(197, 48, 48)           // #c53030

 val TITLE: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
 val BOLD: Typeface = Typeface.create("sans-serif", Typeface.BOLD)

 private val fontCache = HashMap<Int, Typeface>(4)

 fun fontTitle(context: Context): Typeface = nunito(context, 900, TITLE)
 fun fontExtraBold(context: Context): Typeface = nunito(context, 800, TITLE)
 fun fontBold(context: Context): Typeface = nunito(context, 700, BOLD)
 fun fontBody(context: Context): Typeface = nunito(context, 600, Typeface.DEFAULT)

 @Synchronized
 private fun nunito(context: Context, weight: Int, fallback: Typeface): Typeface {
  fontCache[weight]?.let { return it }
  val loaded = try {
   Typeface.Builder(context.assets, "fonts/Nunito.ttf")
    .setFontVariationSettings("'wght' $weight")
    .build()
  } catch (_: Throwable) {
   fallback
  }
  fontCache[weight] = loaded
  return loaded
 }

 enum class Kind(val face: Int, val border: Int, val base: Int, val text: Int) {
  PRIMARY(ORANGE, FOREST, FOREST, Color.WHITE),
  LEAF(RupiUi.LEAF, LEAF_DARK, LEAF_DARK, Color.WHITE),
  SKY(RupiUi.SKY, SKY_DARK, SKY_DARK, Color.WHITE),
  SUN(RupiUi.SUN, FOREST, FOREST, FOREST),
  LISTEN(Color.rgb(255, 248, 230), RupiUi.SUN, Color.rgb(220, 163, 49), Color.rgb(122, 78, 5)),
  STICKER(Color.WHITE, FOREST, FOREST, FOREST),
  QUIET(CARD_SURFACE, Color.rgb(201, 190, 169), Color.rgb(179, 166, 144), FOREST),
  DANGER(Color.rgb(255, 240, 240), Color.rgb(254, 215, 215), Color.rgb(254, 178, 178), RupiUi.DANGER),
 }

 enum class Tone(val back: Int, val border: Int, val text: Int) {
  INFO(Color.rgb(240, 247, 251), Color.rgb(156, 195, 218), SKY_DARK),
  ERROR(Color.rgb(255, 241, 237), Color.rgb(227, 165, 149), Color.rgb(138, 42, 18)),
  OK(LEAF_SOFT, Color.rgb(148, 208, 161), LEAF_DARK),
  /** Respuesta para seguir aprendiendo: ámbar cálido, nunca rojo de castigo. */
  RETRY(Color.rgb(255, 246, 220), GOLD_STROKE, Color.rgb(107, 71, 5)),
 }

 fun dp(context: Context, n: Int) = (n * context.resources.displayMetrics.density).toInt()

 fun dpF(context: Context, n: Float) = n * context.resources.displayMetrics.density

 fun rounded(fill: Int, radius: Float, stroke: Int = 0, strokeColor: Int = Color.TRANSPARENT) =
  GradientDrawable().apply {
   setColor(fill)
   cornerRadius = radius
   if (stroke > 0) setStroke(stroke, strokeColor)
  }

 fun pill(context: Context, fill: Int, border: Int): GradientDrawable =
  rounded(fill, 999f, dp(context, 2), border)

 fun chip(context: Context, back: Int, border: Int) = pill(context, back, border)

 /**
  * Botón 3D con volumen sólido y hundimiento físico al tocarlo (.start-button / .challenge-btn / .admin-btn).
  */
 fun tactile(context: Context, kind: Kind, radiusDp: Int = 14): StateListDrawable {
  val r = dp(context, radiusDp).toFloat()
  val depth = dp(context, 4)
  val pressed = dp(context, 3)
  val stroke = dp(context, 2)
  fun layered(offsetTop: Int, offsetBottom: Int) = LayerDrawable(arrayOf(
   rounded(kind.base, r),
   rounded(kind.face, r, stroke, kind.border),
  )).apply {
   setLayerInset(0, 0, depth, 0, 0)
   setLayerInset(1, 0, offsetTop, 0, offsetBottom)
  }
  return StateListDrawable().apply {
   addState(
    intArrayOf(-android.R.attr.state_enabled),
    LayerDrawable(arrayOf(rounded(Color.rgb(232, 227, 217), r, stroke, LINE)))
     .apply { setLayerInset(0, 0, depth, 0, 0) }
   )
   addState(intArrayOf(android.R.attr.state_pressed), layered(pressed, depth - pressed))
   addState(intArrayOf(), layered(0, depth))
  }
 }

 fun tactileText(kind: Kind) = ColorStateList(
  arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
  intArrayOf(Color.rgb(110, 102, 88), kind.text)
 )

 /** Tarjeta de cuaderno escolar estudiante (#fffdf9, borde #e2d7c5, sombra sólida #d0c3ae). */
 fun card(context: Context) = LayerDrawable(arrayOf(
  rounded(LINE_SHADOW, dp(context, 20).toFloat()),
  rounded(CARD_SURFACE, dp(context, 20).toFloat(), dp(context, 2), LINE),
 )).apply {
  val shadow = dp(context, 4)
  setLayerInset(0, 0, shadow, 0, 0)
  setLayerInset(1, 0, 0, 0, shadow)
 }

 /** Tarjeta sticker estilo cabecera de lección / guía de Rupi (borde #173e30 y sombra sólida). */
 fun stickerCard(context: Context, fill: Int = Color.WHITE) = LayerDrawable(arrayOf(
  rounded(FOREST, dp(context, 22).toFloat()),
  rounded(fill, dp(context, 22).toFloat(), dp(context, 2), FOREST),
 )).apply {
  val shadow = dp(context, 5)
  setLayerInset(0, 0, shadow, 0, 0)
  setLayerInset(1, 0, 0, 0, shadow)
 }

 /** Globo de diálogo de Rupi (.rupi-speech-balloon). */
 fun speechBalloon(context: Context, borderColor: Int = LEAF, shadowColor: Int = LEAF_DARK) = LayerDrawable(arrayOf(
  rounded(shadowColor, dp(context, 18).toFloat()),
  rounded(Color.WHITE, dp(context, 18).toFloat(), dp(context, 2), borderColor),
 )).apply {
  val shadow = dp(context, 4)
  setLayerInset(0, 0, shadow, 0, 0)
  setLayerInset(1, 0, 0, 0, shadow)
 }

 /** Tarjeta de meta cumplida en oro cálido (DailyGoalRing.css .goal-completed-card). */
 fun goalCompletedCard(context: Context) = LayerDrawable(arrayOf(
  rounded(Color.rgb(214, 171, 56), dp(context, 20).toFloat()),
  rounded(Color.rgb(255, 251, 240), dp(context, 20).toFloat(), dp(context, 2), SUN),
 )).apply {
  val shadow = dp(context, 4)
  setLayerInset(0, 0, shadow, 0, 0)
  setLayerInset(1, 0, 0, 0, shadow)
 }

 /** Caja de lectura escolar (#f4f9fc con borde discontinuo azul suave #9cc3da, .challenge-reading). */
 fun readingBox(context: Context) = GradientDrawable().apply {
  setColor(Color.rgb(244, 249, 252))
  cornerRadius = dp(context, 14).toFloat()
  setStroke(dp(context, 2), Color.rgb(156, 195, 218), dp(context, 8).toFloat(), dp(context, 5).toFloat())
 }

 /** Tarjeta de memoria (.flashcards .memory-card en Flashcards.css). */
 fun flashcard(context: Context, revealed: Boolean): StateListDrawable {
  val r = dp(context, 18).toFloat()
  val depth = dp(context, 5)
  val pressed = dp(context, 3)
  val stroke = dp(context, 2)
  val face = if (revealed) LEAF_SOFT else FLASHCARD_FRONT
  val accent = if (revealed) LEAF else FLASHCARD_GOLD
  fun layered(offsetTop: Int, offsetBottom: Int) = LayerDrawable(arrayOf(
   rounded(accent, r),
   rounded(face, r, stroke, accent),
  )).apply {
   setLayerInset(0, 0, depth, 0, 0)
   setLayerInset(1, 0, offsetTop, 0, offsetBottom)
  }
  return StateListDrawable().apply {
   addState(intArrayOf(android.R.attr.state_pressed), layered(pressed, depth - pressed))
   addState(intArrayOf(), layered(0, depth))
  }
 }

 /** Tarjeta hero de bienvenida con banda superior andina (StudentDashboardView.css .dashboard-hero::before). */
 fun heroCard(context: Context): Drawable = HeroCardDrawable(context)

 /** Tarjeta con banda lateral de color (DailyChallengeCard.css .challenge-card). */
 fun accentStripeCard(context: Context, stripeColor: Int): Drawable = StripeCardDrawable(context, stripeColor)

 /** Nodo circular de parada en "Mi Camino" (.lesson-node). */
 fun routeNodeCircle(context: Context, completed: Boolean, locked: Boolean): Drawable {
  val shadow = dp(context, 4)
  val stroke = dp(context, 3)
  val (fill, base, rim) = when {
   completed -> Triple(LEAF, LEAF_DARK, Color.WHITE)
   locked -> Triple(Color.rgb(199, 211, 188), Color.rgb(133, 152, 125), Color.WHITE)
   else -> Triple(ORANGE, ORANGE_DARK, SUN)
  }
  return LayerDrawable(arrayOf(
   GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(base) },
   GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(fill); setStroke(stroke, rim) },
  )).apply {
   setLayerInset(0, 0, shadow, 0, 0)
   setLayerInset(1, 0, 0, 0, shadow)
  }
 }

 /** Marco circular con sombra para el avatar de Rupi en cabecera (.brand-logo en App.css). */
 fun brandLogoFrame(context: Context): LayerDrawable {
  val stroke = dpF(context, 2.5f).toInt().coerceAtLeast(2)
  val shadow = dp(context, 3)
  return LayerDrawable(arrayOf(
   GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(FOREST) },
   GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE); setStroke(stroke, FOREST) },
  )).apply {
   setLayerInset(0, 0, shadow, 0, 0)
   setLayerInset(1, 0, 0, 0, shadow)
  }
 }

 /** Barra superior blanca estilo portal estudiante (.sidebar en App.css). */
 fun topbar(context: Context): LayerDrawable {
  val softShadow = dp(context, 2)
  val border = dpF(context, 2.5f).toInt().coerceAtLeast(2)
  return LayerDrawable(arrayOf(
   ColorDrawableCompat(Color.argb(20, 23, 62, 48)),
   ColorDrawableCompat(FOREST),
   ColorDrawableCompat(Color.WHITE),
  )).apply {
   setLayerInset(1, 0, 0, 0, softShadow)
   setLayerInset(2, 0, 0, 0, softShadow + border)
  }
 }

 /** Marca «R» auxiliar cuando se requiera insignia tipográfica. */
 fun brandMark(context: Context): LayerDrawable {
  val big = dp(context, 14).toFloat()
  val small = dp(context, 4).toFloat()
  fun shape(color: Int) = GradientDrawable().apply {
   setColor(color)
   cornerRadii = floatArrayOf(big, big, big, big, big, big, small, small)
  }
  return LayerDrawable(arrayOf(shape(ORANGE_DARK), shape(ORANGE))).apply {
   setLayerInset(0, 0, dp(context, 4), 0, 0)
   setLayerInset(1, 0, 0, 0, dp(context, 4))
  }
 }

 /** Campo de texto blanco limpio con borde verde suave y foco azul cielo. */
 fun field(context: Context) = StateListDrawable().apply {
  val r = dp(context, 14).toFloat()
  val s = dp(context, 2)
  addState(intArrayOf(android.R.attr.state_focused), rounded(Color.WHITE, r, s, SKY))
  addState(intArrayOf(), rounded(Color.WHITE, r, s, SOFT_LINE))
 }

 /** Alternativa de una pregunta (.challenge-option): se marca en verde hoja suave al elegirla. */
 fun option(context: Context) = StateListDrawable().apply {
  val r = dp(context, 14).toFloat()
  val s = dp(context, 2)
  addState(intArrayOf(android.R.attr.state_checked), rounded(LEAF_SOFT, r, s, LEAF))
  addState(intArrayOf(-android.R.attr.state_enabled), rounded(PAPER, r, s, LINE))
  addState(intArrayOf(), rounded(Color.WHITE, r, s, LINE))
 }

 private class ColorDrawableCompat(color: Int) : GradientDrawable() {
  init { setColor(color) }
 }

 /** Fondo limpio cálido de cuaderno estudiante (#faf8f5 con suavidad superior). */
 class Paper(context: Context) : Drawable() {
  private val density = context.resources.displayMetrics.density
  private val paint = Paint()
  private var lastHeight = -1

  override fun draw(canvas: Canvas) {
   val h = bounds.height()
   val w = bounds.width()
   if (h <= 0 || w <= 0) return
   if (h != lastHeight) {
    lastHeight = h
    paint.shader = LinearGradient(
     0f,
     0f,
     0f,
     minOf(h.toFloat(), 260f * density),
     Color.rgb(255, 253, 249),
     PAPER,
     Shader.TileMode.CLAMP
    )
   }
   canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
  }

  override fun setAlpha(alpha: Int) {}
  override fun setColorFilter(colorFilter: ColorFilter?) {}
  @Deprecated("Requerido por Drawable") override fun getOpacity() = PixelFormat.OPAQUE
 }

 private class HeroCardDrawable(context: Context) : Drawable() {
  private val radius = dp(context, 20).toFloat()
  private val shadowDepth = dp(context, 4).toFloat()
  private val borderWidth = dp(context, 2).toFloat()
  private val stripeHeight = dp(context, 6).toFloat()

  private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LINE_SHADOW }
  private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD_SURFACE }
  private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
   style = Paint.Style.STROKE
   strokeWidth = borderWidth
   color = LINE
  }
  private val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val rect = RectF()
  private val shadowRect = RectF()
  private val clipPath = Path()
  private var lastWidth = -1f

  override fun draw(canvas: Canvas) {
   val w = bounds.width().toFloat()
   val h = bounds.height().toFloat()
   if (w <= 0f || h <= shadowDepth) return

   val halfStroke = borderWidth / 2f
   shadowRect.set(0f, shadowDepth, w, h)
   canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint)

   rect.set(halfStroke, halfStroke, w - halfStroke, h - shadowDepth - halfStroke)
   canvas.drawRoundRect(rect, radius, radius, fillPaint)

   if (w != lastWidth) {
    lastWidth = w
    stripePaint.shader = LinearGradient(
     0f,
     0f,
     w,
     0f,
     intArrayOf(FOREST, LEAF, SUN, ORANGE),
     floatArrayOf(0f, 0.35f, 0.70f, 1f),
     Shader.TileMode.CLAMP
    )
   }
   clipPath.reset()
   clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
   canvas.save()
   canvas.clipPath(clipPath)
   canvas.drawRect(0f, 0f, w, stripeHeight, stripePaint)
   canvas.restore()

   canvas.drawRoundRect(rect, radius, radius, strokePaint)
  }

  override fun setAlpha(alpha: Int) {}
  override fun setColorFilter(colorFilter: ColorFilter?) {}
  @Deprecated("Requerido por Drawable") override fun getOpacity() = PixelFormat.TRANSLUCENT
 }

 private class StripeCardDrawable(context: Context, stripeColor: Int) : Drawable() {
  private val radius = dp(context, 18).toFloat()
  private val shadowDepth = dp(context, 4).toFloat()
  private val borderWidth = dp(context, 2).toFloat()
  private val stripeWidth = dp(context, 8).toFloat()

  private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LINE_SHADOW }
  private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD_SURFACE }
  private val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = stripeColor }
  private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
   style = Paint.Style.STROKE
   strokeWidth = borderWidth
   color = LINE
  }
  private val rect = RectF()
  private val shadowRect = RectF()
  private val clipPath = Path()

  override fun draw(canvas: Canvas) {
   val w = bounds.width().toFloat()
   val h = bounds.height().toFloat()
   if (w <= 0f || h <= shadowDepth) return

   val halfStroke = borderWidth / 2f
   shadowRect.set(0f, shadowDepth, w, h)
   canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint)

   rect.set(halfStroke, halfStroke, w - halfStroke, h - shadowDepth - halfStroke)
   canvas.drawRoundRect(rect, radius, radius, fillPaint)

   clipPath.reset()
   clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
   canvas.save()
   canvas.clipPath(clipPath)
   canvas.drawRect(0f, 0f, stripeWidth, h, stripePaint)
   canvas.restore()

   canvas.drawRoundRect(rect, radius, radius, strokePaint)
  }

  override fun setAlpha(alpha: Int) {}
  override fun setColorFilter(colorFilter: ColorFilter?) {}
  @Deprecated("Requerido por Drawable") override fun getOpacity() = PixelFormat.TRANSLUCENT
 }
}
