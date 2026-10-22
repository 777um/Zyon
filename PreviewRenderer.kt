package com.zyon.prototype

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.min

/** Standalone simulated scene. No game process interaction. */
class PreviewRenderer(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targets = listOf(
        Target(.22f, .38f, .12f, .29f, .84f, 32),
        Target(.63f, .31f, .10f, .36f, .43f, 58),
        Target(.81f, .53f, .08f, .22f, .96f, 91)
    )
    private val prefs = context.getSharedPreferences("zyon_settings", Context.MODE_PRIVATE)
    private data class Target(val x: Float, val y: Float, val w: Float, val h: Float, val hp: Float, val meters: Int)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(Color.rgb(13, 20, 29))
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(98, 221, 226)
        paint.textSize = 16f * resources.displayMetrics.scaledDensity
        canvas.drawText("ZYON · SIMULATED TARGETS", 18f, 42f, paint)
        val fov = prefs.getInt("fov", 110)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.argb(130, 90, 215, 225)
        canvas.drawCircle(w / 2f, h / 2f, min(w, h) * fov / 650f, paint)
        for (target in targets) {
            if (target.meters > prefs.getInt("espRange", 100)) continue
            val left = w * target.x
            val top = h * target.y
            val right = left + w * target.w
            val bottom = top + h * target.h
            if (prefs.getBoolean("box", true)) {
                paint.color = Color.rgb(91, 225, 228)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                canvas.drawRect(left, top, right, bottom, paint)
            }
            if (prefs.getBoolean("health", true)) {
                val barX = left - 11f
                paint.style = Paint.Style.FILL
                paint.color = Color.DKGRAY
                canvas.drawRect(barX, top, barX + 5f, bottom, paint)
                paint.color = when {
                    target.hp > .65f -> Color.GREEN
                    target.hp > .3f -> Color.YELLOW
                    else -> Color.RED
                }
                canvas.drawRect(barX, bottom - (bottom - top) * target.hp, barX + 5f, bottom, paint)
            }
            if (prefs.getBoolean("skeleton", false)) {
                paint.style = Paint.Style.STROKE
                paint.color = Color.WHITE
                paint.strokeWidth = 2f
                val cx = (left + right) / 2f
                canvas.drawLine(cx, top + 12f, cx, bottom - 15f, paint)
                canvas.drawLine(left + 8f, top + (bottom-top)*.35f, right - 8f, top + (bottom-top)*.35f, paint)
                canvas.drawLine(cx, bottom - 15f, left + 8f, bottom, paint)
                canvas.drawLine(cx, bottom - 15f, right - 8f, bottom, paint)
            }
            if (prefs.getBoolean("distance", true)) {
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                paint.textSize = 13f * resources.displayMetrics.scaledDensity
                canvas.drawText("${target.meters}m", left, bottom + 20f, paint)
            }
        }
    }
}
