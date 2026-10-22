package com.zyon.prototype

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.sin

/** Synthetic overlay for testing on-screen rendering. No game data is accessed. */
class DemoOverlayView(context: Context) : View(context) {
    private val prefs = context.getSharedPreferences("zyon", Context.MODE_PRIVATE)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var start = System.nanoTime()

    init { setBackgroundColor(Color.TRANSPARENT) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!prefs.getBoolean("demoOverlay", false)) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = (System.nanoTime() - start) / 1_000_000_000f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(105, 230, 232)
        paint.textSize = 28f
        canvas.drawText("ZYON • DEMO / DADOS SIMULADOS", 24f, 54f, paint)

        val targets = listOf(Triple(.30f, .42f, 75), Triple(.72f, .56f, 48))
        targets.forEachIndexed { i, target ->
            val x = w * target.first + sin(t * (0.55f + i * .15f)) * 35f
            val y = h * target.second
            val boxW = (w * .13f).coerceAtMost(130f)
            val boxH = (h * .21f).coerceAtMost(230f)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.CYAN
            if (prefs.getBoolean("box", true)) canvas.drawRect(x - boxW / 2, y - boxH / 2, x + boxW / 2, y + boxH / 2, paint)
            if (prefs.getBoolean("health", true)) {
                paint.strokeWidth = 7f
                paint.color = Color.DKGRAY
                canvas.drawLine(x - boxW / 2 - 12f, y - boxH / 2, x - boxW / 2 - 12f, y + boxH / 2, paint)
                paint.color = Color.GREEN
                val ratio = target.third / 100f
                canvas.drawLine(x - boxW / 2 - 12f, y + boxH / 2, x - boxW / 2 - 12f, y + boxH / 2 - boxH * ratio, paint)
            }
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.textSize = 26f
            canvas.drawText("ALVO TESTE ${i + 1}", x - boxW / 2, y - boxH / 2 - 18f, paint)
            if (prefs.getBoolean("distance", true)) canvas.drawText("${35 + i * 48}m (simulado)", x - boxW / 2, y + boxH / 2 + 32f, paint)
        }
        if (prefs.getBoolean("aimEnabled", false)) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.YELLOW
            paint.strokeWidth = 2f
            canvas.drawCircle(w / 2f, h / 2f, prefs.getInt("fov", 110).toFloat(), paint)
        }
        postInvalidateDelayed(33L)
    }
}
