package com.zyon.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class MemoryScannerTest {
    @Test fun findsTwoValidatedRecordsInDirectMemory() {
        val buffer = ByteBuffer.allocateDirect(4096)
        MemoryScanner.writeRecord(buffer, 128, 0.25f, 0.45f, 95)
        MemoryScanner.writeRecord(buffer, 1512, 0.71f, 0.58f, 63)
        val result = MemoryScanner.scan(buffer)
        assertEquals(2, result.size)
        assertEquals(128, result[0].offset)
        assertEquals(95, result[0].health)
        assertEquals(1512, result[1].offset)
        assertEquals(63, result[1].health)
    }

    @Test fun rejectsRecordsWithInvalidCoordinatesOrHealth() {
        val buffer = ByteBuffer.allocateDirect(128)
        MemoryScanner.writeRecord(buffer, 0, Float.NaN, 0.4f, 90)
        MemoryScanner.writeRecord(buffer, 32, 0.2f, 0.5f, 999)
        MemoryScanner.writeRecord(buffer, 64, 0.3f, 1.4f, 50)
        assertTrue(MemoryScanner.scan(buffer).isEmpty())
    }

    @Test fun respectsCallerLimitAndResultCount() {
        val buffer = ByteBuffer.allocateDirect(128)
        MemoryScanner.writeRecord(buffer, 0, 0.2f, 0.3f, 90)
        MemoryScanner.writeRecord(buffer, 64, 0.4f, 0.6f, 70)
        assertEquals(1, MemoryScanner.scan(buffer, 1).size)
        buffer.limit(40)
        assertEquals(1, MemoryScanner.scan(buffer).size)
        buffer.limit(10)
        assertTrue(MemoryScanner.scan(buffer).isEmpty())
    }

    @Test fun doesNotAlterCallerPosition() {
        val buffer = ByteBuffer.allocateDirect(64)
        MemoryScanner.writeRecord(buffer, 8, 0.5f, 0.5f, 100)
        buffer.position(40)
        assertEquals(1, MemoryScanner.scan(buffer).size)
        assertEquals(40, buffer.position())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfBoundsWrite() {
        val buffer = ByteBuffer.allocateDirect(32)
        MemoryScanner.writeRecord(buffer, 24, 0.5f, 0.5f, 100)
    }
}
