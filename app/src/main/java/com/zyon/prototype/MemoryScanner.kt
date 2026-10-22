package com.zyon.prototype

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Scans a bounded, caller-owned ByteBuffer for a documented ZYON test record.
 * Never accesses arbitrary addresses or another process.
 */
object MemoryScanner {
    const val MAGIC = 0x5A594F4E // "ZYON"
    const val VERSION = 1
    const val RECORD_SIZE = 20

    data class Entity(val x: Float, val y: Float, val health: Int, val offset: Int)

    fun scan(source: ByteBuffer, maxResults: Int = 32): List<Entity> {
        if (maxResults <= 0 || source.limit() < RECORD_SIZE) return emptyList()
        val data = source.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val entities = ArrayList<Entity>()
        var offset = 0
        while (offset <= data.limit() - RECORD_SIZE && entities.size < maxResults) {
            if (data.getInt(offset) == MAGIC && data.getInt(offset + 4) == VERSION) {
                val x = data.getFloat(offset + 8)
                val y = data.getFloat(offset + 12)
                val health = data.getInt(offset + 16)
                if (x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f &&
                    health in 0..100) {
                    entities.add(Entity(x, y, health, offset))
                    offset += RECORD_SIZE
                    continue
                }
            }
            offset += 4
        }
        return entities
    }

    fun writeRecord(data: ByteBuffer, offset: Int, x: Float, y: Float, health: Int) {
        require(offset >= 0 && offset.toLong() + RECORD_SIZE <= data.capacity().toLong())
        val view = data.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        view.putInt(offset, MAGIC)
        view.putInt(offset + 4, VERSION)
        view.putFloat(offset + 8, x)
        view.putFloat(offset + 12, y)
        view.putInt(offset + 16, health)
    }
}
