package com.zyon.prototype

import android.os.SystemClock

/**
 * Ephemeral state shared by the capture service and on-screen overlays.
 * Recognized text is only stored in RAM; no database, network or log output.
 */
object ScreenRecognitionState {
    data class Snapshot(
        val lines: List<String> = emptyList(),
        val recognizedBlocks: Int = 0,
        val latencyMs: Long = 0L,
        val updatedAtMs: Long = 0L,
        val error: String? = null
    ) {
        fun isFresh(): Boolean = updatedAtMs > 0L &&
            SystemClock.elapsedRealtime() - updatedAtMs < 4000L
    }

    @Volatile private var latest = Snapshot()

    fun current(): Snapshot = latest

    fun publish(lines: List<String>, blocks: Int, latency: Long) {
        latest = Snapshot(
            lines = lines.take(4),
            recognizedBlocks = blocks.coerceAtLeast(0),
            latencyMs = latency.coerceAtLeast(0),
            updatedAtMs = SystemClock.elapsedRealtime()
        )
    }

    fun reportError(kind: String) {
        latest = Snapshot(error = kind)
    }

    fun clear() {
        latest = Snapshot()
    }
}
