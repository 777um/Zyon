package com.zyon.prototype

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import kotlin.math.roundToInt

class FloatingMenuService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var params: WindowManager.LayoutParams
    private val prefs by lazy { getSharedPreferences("zyon_settings", MODE_PRIVATE) }
    private val cyan = Color.rgb(94, 226, 230)
    private var activeTab = "ESP"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xee111824.toInt())
                cornerRadius = 26f
                setStroke(2, cyan)
            }
            setPadding(18, 12, 18, 18)
        }
        val header = TextView(this).apply {
            text = "✦ ZYON  ·  PREVIEW   ⌄"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(cyan)
            setPadding(12, 10, 12, 16)
        }
        root.addView(header)
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("ESP", "Aim", "Loot", "Visual", "Config").forEach { name ->
            tabs.addView(TextView(this).apply {
                text = name
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(9, 12, 9, 12)
                setOnClickListener { activeTab = name; renderTab() }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(tabs)
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(body)
        params = WindowManager.LayoutParams(
            dp(330), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(18); y = dp(110) }
        var startX = 0; var startY = 0; var downX = 0f; var downY = 0f
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    downX = event.rawX; downY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downX).roundToInt()
                    params.y = startY + (event.rawY - downY).roundToInt()
                    wm.updateViewLayout(root, params)
                    true
                }
                else -> true
            }
        }
        wm.addView(root, params)
        renderTab()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private fun label(text: String) {
        body.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(8, 12, 8, 4)
        })
    }

    private fun toggle(key: String, text: String, default: Boolean = false) {
        body.addView(Switch(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean(key, default)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
            }
        })
    }

    private fun slider(key: String, text: String, min: Int, max: Int, default: Int) {
        val value = prefs.getInt(key, default).coerceIn(min, max)
        val title = TextView(this).apply {
            setTextColor(cyan); textSize = 12f
            this.text = "$text: $value"
        }
        body.addView(title)
        body.addView(SeekBar(this).apply {
            this.max = max - min
            progress = value - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val v = min + progress
                    title.text = "$text: $v"
                    if (fromUser) prefs.edit().putInt(key, v).apply()
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        })
    }

    private fun renderTab() {
        body.removeAllViews()
        when (activeTab) {
            "ESP" -> {
                label("Elementos demonstrativos")
                toggle("box", "Box 2D", true)
                toggle("health", "Barra de vida lateral", true)
                toggle("skeleton", "Skeleton")
                toggle("distance", "Distância", true)
                slider("espRange", "Alcance visual (m)", 10, 300, 100)
            }
            "Aim" -> {
                label("Simulador de mira (sem controle do jogo)")
                slider("fov", "FOV", 20, 300, 110)
                slider("smooth", "Smoothing", 1, 100, 65)
                slider("deadzone", "Deadzone", 0, 50, 8)
                slider("acceleration", "Aceleração", 0, 100, 30)
            }
            "Loot" -> {
                label("Classificação de itens simulados")
                toggle("lootNames", "Nome do item", true)
                toggle("lootValue", "Valor em Koen", true)
                toggle("lootSlots", "Valor por slot", true)
                slider("lootMin", "Valor mínimo (mil Koen)", 0, 500, 50)
                slider("lootRange", "Distância máxima (m)", 10, 300, 100)
            }
            "Visual" -> {
                slider("opacity", "Opacidade (%)", 30, 100, 93)
                toggle("animations", "Animações leves", true)
                toggle("fps", "Indicador de desempenho")
            }
            else -> {
                label("Preferências são salvas automaticamente.")
                body.addView(Button(this).apply {
                    text = "Restaurar configurações"
                    setOnClickListener { prefs.edit().clear().apply(); renderTab() }
                })
                body.addView(Button(this).apply {
                    text = "Fechar painel"
                    setOnClickListener { stopSelf() }
                })
            }
        }
    }

    override fun onDestroy() {
        if (::root.isInitialized) wm.removeView(root)
        super.onDestroy()
    }
}
