package com.zyon.prototype

import android.os.Debug
import java.io.File
import java.nio.ByteBuffer

/**
 * On-device, self-process diagnostics. Reads /proc/self/maps and scans only a
 * deliberately allocated direct buffer owned by the ZYON process.
 * Not an Arena Breakout reader and not a cross-process memory scanner.
 */
object SelfMemoryDiagnostics {
    data class Report(
        val mappedRegions: Int?,
        val readableRegions: Int?,
        val writableRegions: Int?,
        val nativeHeapKb: Long,
        val recordsFound: Int,
        val recordsValid: Boolean,
        val details: String
    ) {
        fun summary(): String = buildString {
            appendLine("Escopo: memória do próprio ZYON")
            appendLine("Regiões mapeadas: ${mappedRegions ?: "indisponível"}")
            appendLine("Regiões legíveis: ${readableRegions ?: "indisponível"}")
            appendLine("Regiões graváveis: ${writableRegions ?: "indisponível"}")
            appendLine("Heap nativo: ${nativeHeapKb} KiB")
            appendLine("Assinaturas reconhecidas: ${recordsFound}/2")
            appendLine("Validação: ${if (recordsValid) "OK" else "FALHOU"}")
            append(details)
        }
    }

    fun inspect(): Report {
        val maps = runCatching {
            File("/proc/self/maps").useLines { lines ->
                var count = 0
                var readable = 0
                var writable = 0
                lines.forEach { line ->
                    // Only inspect mapping permissions; do not disclose filenames or addresses.
                    val separator = line.indexOf(' ')
                    if (separator < 0 || line.length < separator + 4) return@forEach
                    val permissions = line.substring(separator + 1).trimStart().take(4)
                    count++
                    if (permissions.startsWith("r")) readable++
                    if (permissions.length > 1 && permissions[1] == 'w') writable++
                }
                Triple(count, readable, writable)
            }
        }.getOrNull()

        // This is a real direct-memory allocation, not simulated memory from another app.
        val memory = ByteBuffer.allocateDirect(4096)
        MemoryScanner.writeRecord(memory, 128, 0.25f, 0.45f, 95)
        MemoryScanner.writeRecord(memory, 1512, 0.71f, 0.58f, 63)
        val found = MemoryScanner.scan(memory)
        val valid = found.size == 2 &&
            found[0].offset == 128 && found[0].health == 95 &&
            found[1].offset == 1512 && found[1].health == 63
        return Report(
            mappedRegions = maps?.first,
            readableRegions = maps?.second,
            writableRegions = maps?.third,
            nativeHeapKb = Debug.getNativeHeapAllocatedSize() / 1024L,
            recordsFound = found.size,
            recordsValid = valid,
            details = if (maps == null) "Mapas do próprio processo não disponíveis neste dispositivo."
                else "Leitura local concluída. Nenhum processo de jogo foi acessado."
        )
    }
}
