package com.zyon.prototype

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 70, 40, 30)
            setBackgroundColor(0xff10141c.toInt())
        }
        root.addView(TextView(this).apply {
            text = "ZYON • Android Prototype"
            textSize = 24f
            setTextColor(0xff65e1e5.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Painel flutuante de pré-visualização com alvos e itens simulados. Não acessa dados de jogos."
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
            text = "Abrir pré-visualização de alvos"
            setOnClickListener { setContentView(PreviewRenderer(this@MainActivity)) }
        })
        root.addView(Button(this).apply {
            text = "Fechar menu"
            setOnClickListener { stopService(Intent(this@MainActivity, FloatingMenuService::class.java)) }
        })
        setContentView(root)
    }
}
