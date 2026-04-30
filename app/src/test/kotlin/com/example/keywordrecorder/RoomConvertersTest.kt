package com.example.keywordrecorder

import com.example.keywordrecorder.data.RoomConverters
import com.example.keywordrecorder.data.TranscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomConvertersTest {
    private val converters = RoomConverters()

    @Test
    fun `every status round-trips through string`() {
        TranscriptionStatus.entries.forEach { status ->
            val serialized = converters.fromTranscriptionStatus(status)
            val deserialized = converters.toTranscriptionStatus(serialized)
            assertEquals(status, deserialized)
        }
    }

    @Test
    fun `unknown string falls back to PENDING`() {
        val result = converters.toTranscriptionStatus("TOTALLY_UNKNOWN_VALUE")
        assertEquals(TranscriptionStatus.PENDING, result)
    }

    @Test
    fun `serialized value matches enum name`() {
        assertEquals("COMPLETED", converters.fromTranscriptionStatus(TranscriptionStatus.COMPLETED))
        assertEquals("FAILED", converters.fromTranscriptionStatus(TranscriptionStatus.FAILED))
        assertEquals("SKIPPED", converters.fromTranscriptionStatus(TranscriptionStatus.SKIPPED))
    }
}
