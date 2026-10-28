package pe.rupi.mobile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Circular indicator of today's goal (HU-35). Mirrors the web ring: track, progress arc and center count. */
class DailyGoalRing(context: Context, private val completed: Int, private val target: Int, private val met: Boolean) : View(context) {
 private val density = resources.displayMetrics.density
 private val stroke = 14 * density
 private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; color = Color.rgb(233,243,231) }
 private val progress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND
  color = if(met) Color.rgb(196,140,20) else Color.rgb(49,114,75)
 }
 private val count = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(23,62,48); textAlign = Paint.Align.CENTER; textSize = 34 * density; isFakeBoldText = true }
 private val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(23,62,48); textAlign = Paint.Align.CENTER; textSize = 15 * density }
 private val bounds = RectF()
 val fraction: Float get() = if(target <= 0) 0f else (completed.toFloat() / target).coerceIn(0f, 1f)

 init {
  contentDescription = if(met) "Meta de hoy lograda: $completed de $target lecciones" else "Meta de hoy: $completed de $target lecciones completadas"
  isFocusable = true
 }

 override fun onMeasure(widthSpec: Int, heightSpec: Int) {
  val size = (180 * density).toInt()
  setMeasuredDimension(resolveSize(size, widthSpec), size)
 }

 override fun onDraw(canvas: Canvas) {
  val side = minOf(width, height).toFloat() - stroke
  val left = (width - side) / 2f; val top = (height - side) / 2f
  bounds.set(left, top, left + side, top + side)
  canvas.drawArc(bounds, 0f, 360f, false, track)
  if(fraction > 0f) canvas.drawArc(bounds, -90f, 360f * fraction, false, progress)
  val cy = height / 2f
  canvas.drawText("$completed/$target", width / 2f, cy + 8 * density, count)
  canvas.drawText(if(met) "¡Lograda!" else "${(fraction * 100).toInt()}%", width / 2f, cy + 32 * density, caption)
 }
}
