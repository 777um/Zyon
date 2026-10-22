package com.zyon.prototype

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager

/**
 * User-consented screen capture diagnostics. Counts incoming frames only.
 * Frames are never stored, uploaded, or used to access hidden game state.
 */
class ScreenCaptureService : Service() {
    companion object {
        const val ACTION_STOP = "com.zyon.prototype.STOP_CAPTURE"
        const val EXTRA_RESULT = "capture_result"
        const val EXTRA_DATA = "capture_data"
        private const val CHANNEL = "zyon_capture"
        private const val NOTIFICATION_ID = 204
    }

    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("zyon", MODE_PRIVATE) }
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var frameCount = 0
    private var windowStart = 0L
    private var shuttingDown = false

    private val callback = object : MediaProjection.Callback() {
        override fun onStop() { stopSelf() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (projection != null) return START_NOT_STICKY
        val resultCode = intent?.getIntExtra(EXTRA_RESULT, 0) ?: 0
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_DATA)
        }
        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "ZYON captura", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("ZYON — captura de tela")
            .setContentText("Diagnóstico ativo. Pare a captura pelo aplicativo.")
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            projection?.registerCallback(callback, handler)
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels.coerceAtLeast(1)
            val height = metrics.heightPixels.coerceAtLeast(1)
            val density = metrics.densityDpi
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            windowStart = android.os.SystemClock.elapsedRealtime()
            frameCount = 0
            prefs.edit().putBoolean("captureActive", true).putInt("captureFps", 0).apply()
            reader?.setOnImageAvailableListener({ imageReader ->
                val image = try { imageReader.acquireLatestImage() } catch (_: Exception) { null }
                if (image != null) {
                    image.close()
                    frameCount++
                    val now = android.os.SystemClock.elapsedRealtime()
                    val elapsed = now - windowStart
                    if (elapsed >= 1000L) {
                        val fps = (frameCount * 1000L / elapsed).toInt()
                        prefs.edit().putInt("captureFps", fps).putLong("captureUpdated", now).apply()
                        frameCount = 0
                        windowStart = now
                    }
                }
            }, handler)
            display = projection?.createVirtualDisplay(
                "ZYON Capture Diagnostics", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader?.surface, null, handler
            )
        } catch (_: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (shuttingDown) return
        shuttingDown = true
        prefs.edit().putBoolean("captureActive", false).putInt("captureFps", 0).apply()
        reader?.setOnImageAvailableListener(null, null)
        display?.release()
        display = null
        reader?.close()
        reader = null
        projection?.unregisterCallback(callback)
        projection?.stop()
        projection = null
        super.onDestroy()
    }
}
