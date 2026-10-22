package com.zyon.prototype

import android.app.Activity
import android.media.projection.MediaProjectionManager
import android.content.Context
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView

class MainActivity : Activity() {
    private val captureRequest = 504

    @Deprecated("Android Activity result callback used without additional dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureRequest && resultCode == RESULT_OK && data != null) {
            val intent = Intent(this, ScreenCaptureService::class.java)
                .putExtra(ScreenCaptureService.EXTRA_RESULT, resultCode)
                .putExtra(ScreenCaptureService.EXTRA_DATA, data)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 70, 40, 30)
            setBackgroundColor(0xff10141c.toInt())
        }
        root.addView(TextView(this).apply {
            text = "ZYON • Android v0.7"
            textSize = 24f
            setTextColor(0xff65e1e5.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Overlay demonstrativo e diagnóstico real de captura de tela com autorização. Sem leitura de processos de jogos."
            textSize = 16f
            setTextColor(-1)
            setPadding(0, 24, 0, 24)
        })
        root.addView(Button(this).apply {
            text = "Abrir menu flutuante"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this@MainActivity, FloatingMenuService::class.java))
                }
            }
        })
        root.addView(Button(this).apply {
            text = "Fechar menu"
            setOnClickListener { stopService(Intent(this@MainActivity, FloatingMenuService::class.java)) }
        })
        root.addView(Button(this).apply {
            text = "Iniciar captura de tela (autorizar)"
            setOnClickListener {
                val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
            }
        })
        root.addView(Button(this).apply {
            text = "Parar captura de tela"
            setOnClickListener {
                stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
            }
        })
        root.addView(TextView(this).apply {
            text = "Na aba Visual do menu, consulte o FPS da captura. A captura não grava imagens nem obtém dados ocultos."
            textSize = 13f
            setTextColor(-1)
        })
        val memoryResult = TextView(this).apply {
            text = "Memória interna: teste não executado."
            textSize = 13f
            setTextColor(0xff65e1e5.toInt())
            setPadding(0, 10, 0, 20)
        }
        root.addView(Button(this).apply {
            text = "Testar reconhecimento de memória do ZYON"
            setOnClickListener {
                memoryResult.text = runCatching { SelfMemoryDiagnostics.inspect().summary() }
                    .getOrElse { "Falha no diagnóstico local: ${it.javaClass.simpleName}" }
            }
        })
        root.addView(memoryResult)
        root.addView(TextView(this).apply {
            text = "Escopo deste teste: apenas o próprio processo ZYON. Nenhuma memória interna do Arena Breakout é lida."
            textSize = 12f
            setTextColor(-1)
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
