package com.zyon.prototype

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.*
import kotlin.math.roundToInt

class FloatingMenuService : Service() {
    private lateinit var window: WindowManager
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private var demoView: DemoOverlayView? = null
    private lateinit var tabs: LinearLayout
    private lateinit var header: TextView
    private lateinit var minimizeButton: Button
    private var minimized = false
    private var fpsStatus: TextView? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    private val fpsUpdater = object : Runnable {
        override fun run() {
            fpsStatus?.let { status ->
                val active = preferences.getBoolean("captureActive", false)
                val lastUpdate = preferences.getLong("captureUpdated", 0L)
                val fresh = android.os.SystemClock.elapsedRealtime() - lastUpdate < 3000L
                status.text = if (!active) "Captura: desligada"
                    else if (!fresh) "Captura: aguardando quadros..."
                    else "Captura: ativa • ${preferences.getInt("captureFps", 0)} FPS"
            }
            uiHandler.postDelayed(this, 1000L)
        }
    }
    private lateinit var params: WindowManager.LayoutParams
    private val preferences by lazy { getSharedPreferences("zyon", MODE_PRIVATE) }
    private val accent = Color.rgb(94, 226, 230)

    override fun onBind(intent: Intent?): IBinder? = null
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    override fun onCreate() {
        super.onCreate()
        window = getSystemService(WINDOW_SERVICE) as WindowManager
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(0xee111824.toInt())
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), accent)
            }
        }
        header = TextView(this).apply {
            text = "✦ ZYON  ·  PREVIEW  (arraste)"
            textSize = 15f
            setTextColor(accent)
            setPadding(6, 8, 6, 16)
        }
        val headerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        headerRow.addView(header, LinearLayout.LayoutParams(0, dp(48), 1f))
        minimizeButton = Button(this).apply {
            text = "−"
            contentDescription = "Minimizar menu flutuante"
            setOnClickListener { setMinimized(!minimized) }
        }
        headerRow.addView(minimizeButton, LinearLayout.LayoutParams(dp(52), dp(48)))
        headerRow.addView(Button(this).apply {
            text = "×"
            contentDescription = "Fechar menu flutuante"
            setOnClickListener { stopSelf() }
        }, LinearLayout.LayoutParams(dp(52), dp(48)))
        root.addView(headerRow)
        tabs = LinearLayout(this)
        root.addView(tabs)
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(body)
        listOf("ESP", "Aim", "Loot", "Visual", "Config").forEach { tab ->
            tabs.addView(Button(this).apply {
                text = tab
                textSize = 10f
                setPadding(0, 0, 0, 0)
                setOnClickListener { showTab(tab) }
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        params = WindowManager.LayoutParams(
            dp(345), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(100)
        }
        var sx = 0; var sy = 0; var dx = 0f; var dy = 0f
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = params.x; sy = params.y
                    dx = event.rawX; dy = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = sx + (event.rawX - dx).roundToInt()
                    params.y = sy + (event.rawY - dy).roundToInt()
                    window.updateViewLayout(root, params)
                    true
                }
                else -> true
            }
        }
        window.addView(root, params)
        if (preferences.getBoolean("demoOverlay", false)) setDemoOverlay(true)
        showTab("ESP")
        uiHandler.post(fpsUpdater)
    }

    private fun setMinimized(value: Boolean) {
        minimized = value
        tabs.visibility = if (value) View.GONE else View.VISIBLE
        body.visibility = if (value) View.GONE else View.VISIBLE
        header.text = if (value) "✦ ZYON" else "✦ ZYON  ·  PREVIEW  (arraste)"
        minimizeButton.text = if (value) "+" else "−"
        minimizeButton.contentDescription = if (value) "Expandir menu flutuante" else "Minimizar menu flutuante"
        params.width = dp(if (value) 185 else 345)
        window.updateViewLayout(root, params)
    }

    private fun setDemoOverlay(enabled: Boolean) {
        preferences.edit().putBoolean("demoOverlay", enabled).apply()
        if (enabled && demoView == null) {
            val view = DemoOverlayView(this)
            val p = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            window.addView(view, p)
            demoView = view
        } else if (!enabled) {
            demoView?.let { window.removeView(it) }
            demoView = null
        }
    }

    private fun title(label: String) {
        body.addView(TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(accent)
        })
    }

    private fun toggle(key: String, label: String, initial: Boolean = false) {
        body.addView(Switch(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.WHITE)
            isChecked = preferences.getBoolean(key, initial)
            setOnCheckedChangeListener { _, checked ->
                preferences.edit().putBoolean(key, checked).apply()
            }
        })
    }

    private fun slider(key: String, label: String, min: Int, max: Int, initial: Int) {
        val heading = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
        }
        body.addView(heading)
        body.addView(SeekBar(this).apply {
            this.max = max - min
            progress = preferences.getInt(key, initial).coerceIn(min, max) - min
            heading.text = "$label: ${progress + min}"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress + min
                    heading.text = "$label: $value"
                    if (fromUser) preferences.edit().putInt(key, value).apply()
                }
                override fun onStartTrackingTouch(seek: SeekBar?) {}
                override fun onStopTrackingTouch(seek: SeekBar?) {}
            })
        })
    }

    private fun showTab(tab: String) {
        body.removeAllViews()
        fpsStatus = null
        when (tab) {
            "ESP" -> {
                title("Visualização demonstrativa")
                toggle("box", "Caixa 2D", true)
                toggle("health", "Barra de vida", true)
                toggle("skeleton", "Skeleton")
                toggle("distance", "Distância", true)
                slider("range", "Alcance (m)", 10, 300, 100)
            }
            "Aim" -> {
                title("Simulação de mira — não controla jogos")
                body.addView(Switch(this).apply {
                    text = "Ativar simulação de Aim"
                    textSize = 13f
                    setTextColor(Color.WHITE)
                    isChecked = preferences.getBoolean("aimEnabled", false)
                    setOnCheckedChangeListener { _, enabled ->
                        preferences.edit().putBoolean("aimEnabled", enabled).apply()
                        demoView?.invalidate()
                        showTab("Aim")
                    }
                })
                if (preferences.getBoolean("aimEnabled", false)) {
                    slider("fov", "FOV", 20, 300, 110)
                    slider("smooth", "Suavização", 1, 100, 65)
                    slider("deadzone", "Deadzone", 0, 50, 8)
                }
            }
            "Loot" -> {
                title("Classificação de itens simulados")
                toggle("lootName", "Mostrar nome", true)
                toggle("lootValue", "Mostrar Koen", true)
                slider("minLoot", "Valor mínimo (mil)", 0, 500, 50)
            }
            "Visual" -> {
                title("Teste visual sobre outros aplicativos")
                fpsStatus = TextView(this).apply {
                    textSize = 13f
                    setTextColor(accent)
                    text = "Captura: verificando..."
                }
                body.addView(fpsStatus)
                fpsStatus?.text = if (preferences.getBoolean("captureActive", false))
                    "Captura: ativa • ${preferences.getInt("captureFps", 0)} FPS"
                else "Captura: desligada"
                title("Dados capturados: somente contagem de quadros")
                body.addView(Switch(this).apply {
                    text = "Exibir overlay DEMO"
                    textSize = 13f
                    setTextColor(Color.WHITE)
                    isChecked = preferences.getBoolean("demoOverlay", false)
                    setOnCheckedChangeListener { _, enabled -> setDemoOverlay(enabled) }
                })
                slider("opacity", "Opacidade (%)", 30, 100, 93)
                toggle("fps", "Indicador de FPS")
            }
            else -> {
                body.addView(Button(this).apply {
                    text = "Restaurar configurações"
                    setOnClickListener { preferences.edit().clear().apply(); showTab("Config") }
                })
                body.addView(Button(this).apply {
                    text = "Fechar menu"
                    setOnClickListener { stopSelf() }
                })
            }
        }
    }

    override fun onDestroy() {
        uiHandler.removeCallbacks(fpsUpdater)
        fpsStatus = null
        demoView?.let { window.removeView(it) }
        demoView = null
        if (::root.isInitialized) window.removeView(root)
        super.onDestroy()
    }
}
