package pe.rupi.mobile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Circular indicator of today's goal (HU-35). Mirrors DailyGoalRing.css: track #ebe4d8, progress arc and center count. */
class DailyGoalRing(
 context: Context,
 private val completed: Int,
 private val target: Int,
 private val met: Boolean
) : View(context) {
 private val density = resources.displayMetrics.density
 private val stroke = 13 * density
 private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE
  strokeWidth = stroke
  color = Color.rgb(235, 228, 216) // #ebe4d8
 }
 private val progress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE
  strokeWidth = stroke
  strokeCap = Paint.Cap.ROUND
  color = if (met) RupiUi.GOLD_STROKE else RupiUi.LEAF
 }
 private val count = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  color = RupiUi.FOREST
  textAlign = Paint.Align.CENTER
  textSize = 32 * density
  typeface = RupiUi.fontTitle(context)
 }
 private val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  color = if (met) Color.rgb(140, 99, 0) else RupiUi.MUTED
  textAlign = Paint.Align.CENTER
  textSize = 15 * density
  typeface = RupiUi.fontExtraBold(context)
 }
 private val bounds = RectF()
 val fraction: Float get() = if (target <= 0) 0f else (completed.toFloat() / target).coerceIn(0f, 1f)

 init {
  contentDescription = if (met) {
   "Meta de hoy lograda: $completed de $target lecciones"
  } else {
   "Meta de hoy: $completed de $target lecciones completadas"
  }
  isFocusable = true
 }

 override fun onMeasure(widthSpec: Int, heightSpec: Int) {
  val size = (168 * density).toInt()
  setMeasuredDimension(resolveSize(size, widthSpec), size)
 }

 override fun onDraw(canvas: Canvas) {
  val side = minOf(width, height).toFloat() - stroke
  val left = (width - side) / 2f
  val top = (height - side) / 2f
  bounds.set(left, top, left + side, top + side)
  canvas.drawArc(bounds, 0f, 360f, false, track)
  if (fraction > 0f) canvas.drawArc(bounds, -90f, 360f * fraction, false, progress)
  val cy = height / 2f
  canvas.drawText("$completed/$target", width / 2f, cy + 6 * density, count)
  canvas.drawText(if (met) "¡Lograda!" else "${(fraction * 100).toInt()}%", width / 2f, cy + 30 * density, caption)
 }
}
