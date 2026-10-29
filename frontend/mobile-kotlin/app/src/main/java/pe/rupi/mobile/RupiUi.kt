package pe.rupi.mobile

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable

/**
 * Kit visual de la app con el mismo lenguaje que la consola de administración (AdminConsole.css):
 * cuaderno escolar con renglones, tinta de bosque, sellos de color y botones con volumen que se hunden al tocarlos.
 */
object RupiUi {
 // Tokens de AdminConsole.css (--adm-*).
 val FOREST = Color.rgb(23,62,48)
 val LEAF = Color.rgb(49,114,75)
 val LEAF_DARK = Color.rgb(31,79,51)
 val ORANGE = Color.rgb(249,87,56)
 val ORANGE_DARK = Color.rgb(184,56,28)
 val SUN = Color.rgb(255,210,106)
 val SKY = Color.rgb(36,106,145)
 val SKY_DARK = Color.rgb(21,69,98)
 val INK = Color.rgb(26,46,34)
 val MUTED = Color.rgb(67,97,76)
 val PAPER = Color.rgb(250,248,245)
 val PAPER_DEEP = Color.rgb(243,239,230)
 val LINE = Color.rgb(217,210,195)
 val DANGER = Color.rgb(168,50,27)

 /** Tipografía de títulos: la más gruesa del sistema, como Nunito 900 en la web. */
 val TITLE: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
 val BOLD: Typeface = Typeface.create("sans-serif", Typeface.BOLD)

 /** Variantes de botón de la consola: principal naranja, hoja, cielo, secundario y peligro. */
 enum class Kind(val face: Int, val border: Int, val base: Int, val text: Int) {
  PRIMARY(ORANGE, Color.rgb(212,56,24), Color.rgb(179,42,16), Color.WHITE),
  LEAF(RupiUi.LEAF, Color.rgb(35,88,56), Color.rgb(25,64,40), Color.WHITE),
  SKY(RupiUi.SKY, Color.rgb(26,82,114), Color.rgb(19,58,81), Color.WHITE),
  QUIET(Color.WHITE, LINE, LINE, FOREST),
  DANGER(Color.WHITE, RupiUi.DANGER, Color.rgb(232,196,187), RupiUi.DANGER),
 }

 enum class Tone(val back: Int, val border: Int, val text: Int) {
  INFO(Color.rgb(238,246,251), Color.rgb(169,203,224), SKY_DARK),
  ERROR(Color.rgb(255,241,237), Color.rgb(227,165,149), Color.rgb(127,36,16)),
  OK(Color.rgb(238,245,232), Color.rgb(182,211,168), LEAF_DARK),
 }

 fun dp(context: Context, n: Int) = (n * context.resources.displayMetrics.density).toInt()

 fun rounded(fill: Int, radius: Float, stroke: Int = 0, strokeColor: Int = Color.TRANSPARENT) =
  GradientDrawable().apply { setColor(fill); cornerRadius=radius; if(stroke>0) setStroke(stroke,strokeColor) }

 /**
  * Botón con base sólida: la cara se apoya sobre una franja más oscura y al presionar baja hasta casi tocarla
  * (equivale a `box-shadow: 0 4px 0` y `:active { translateY(3px) }` de la web). Deshabilitado queda plano, color papel.
  */
 fun tactile(context: Context, kind: Kind): StateListDrawable {
  val r = dp(context,14).toFloat(); val depth = dp(context,4); val pressed = dp(context,3); val stroke = dp(context,2)
  fun layered(offsetTop: Int, offsetBottom: Int) = LayerDrawable(arrayOf(
   rounded(kind.base, r),
   rounded(kind.face, r, stroke, kind.border),
  )).apply { setLayerInset(0,0,depth,0,0); setLayerInset(1,0,offsetTop,0,offsetBottom) }
  return StateListDrawable().apply {
   addState(intArrayOf(-android.R.attr.state_enabled), LayerDrawable(arrayOf(rounded(Color.rgb(226,221,210), r, stroke, LINE)))
    .apply { setLayerInset(0,0,depth,0,0) })
   addState(intArrayOf(android.R.attr.state_pressed), layered(pressed, depth-pressed))
   addState(intArrayOf(), layered(0, depth))
  }
 }

 fun tactileText(kind: Kind) = ColorStateList(
  arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(Color.rgb(92,84,72), kind.text))

 /** Tarjeta blanca de la consola: borde de 2 dp, esquinas de 20 dp y sombra sólida corta. */
 fun card(context: Context) = LayerDrawable(arrayOf(
  rounded(Color.argb(20,23,62,48), dp(context,20).toFloat()),
  rounded(Color.WHITE, dp(context,20).toFloat(), dp(context,2), LINE),
 )).apply { setLayerInset(0,0,dp(context,5),0,0); setLayerInset(1,0,0,0,dp(context,5)) }

 /** Campo de texto: blanco con borde de cuaderno; con foco, borde cielo como en la consola. */
 fun field(context: Context) = StateListDrawable().apply {
  addState(intArrayOf(android.R.attr.state_focused), rounded(Color.WHITE, dp(context,12).toFloat(), dp(context,2), SKY))
  addState(intArrayOf(), rounded(Color.WHITE, dp(context,12).toFloat(), dp(context,2), LINE))
 }

 /** Alternativa de una pregunta: fila táctil que se marca en cielo al elegirla. */
 fun option(context: Context) = StateListDrawable().apply {
  val r = dp(context,12).toFloat(); val s = dp(context,2)
  addState(intArrayOf(android.R.attr.state_checked), rounded(Color.rgb(244,249,252), r, s, SKY))
  addState(intArrayOf(-android.R.attr.state_enabled), rounded(PAPER, r, s, LINE))
  addState(intArrayOf(), rounded(Color.WHITE, r, s, LINE))
 }

 fun chip(context: Context, back: Int, border: Int) = rounded(back, 999f, dp(context,2), border)

 /** Marca «R» de Rupi: naranja con la esquina inferior izquierda recortada y base sólida. */
 fun brandMark(context: Context): LayerDrawable {
  val big = dp(context,14).toFloat(); val small = dp(context,4).toFloat()
  fun shape(color: Int) = GradientDrawable().apply { setColor(color); cornerRadii=floatArrayOf(big,big,big,big,big,big,small,small) }
  return LayerDrawable(arrayOf(shape(ORANGE_DARK), shape(ORANGE))).apply { setLayerInset(0,0,dp(context,4),0,0); setLayerInset(1,0,0,0,dp(context,4)) }
 }

 /** Barra superior: tinta de bosque con el borde amarillo maíz de la consola. */
 fun topbar(context: Context) = LayerDrawable(arrayOf(ColorDrawableCompat(SUN), ColorDrawableCompat(FOREST)))
  .apply { setLayerInset(1,0,0,0,dp(context,4)) }

 private class ColorDrawableCompat(color: Int) : GradientDrawable() { init { setColor(color) } }

 /** Papel de cuaderno con renglones cada 32 dp, como el fondo de la consola. */
 class Paper(context: Context) : Drawable() {
  private val step = dp(context,32).toFloat()
  private val line = Paint().apply { color = Color.argb(13,36,106,145); strokeWidth = dp(context,1).toFloat() }
  override fun draw(canvas: Canvas) {
   canvas.drawColor(PAPER)
   var y = step
   while(y < bounds.height()) { canvas.drawLine(0f,y,bounds.width().toFloat(),y,line); y += step }
  }
  override fun setAlpha(alpha: Int) {}
  override fun setColorFilter(colorFilter: ColorFilter?) {}
  @Deprecated("Requerido por Drawable") override fun getOpacity() = PixelFormat.OPAQUE
 }
}
