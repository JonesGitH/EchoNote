package com.example.keywordrecorder.worker

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptionSchedulerTest {
    @Test
    fun computeNextDelay_sameDayFuture() {
        val now = LocalDateTime.of(2026, 4, 24, 10, 0)
        val delay = TranscriptionScheduler.computeNextDelayMillis(11, 0, now)
        assertEquals(60 * 60 * 1000L, delay)
    }

    @Test
    fun computeNextDelay_nextDayWhenPast() {
        val now = LocalDateTime.of(2026, 4, 24, 23, 0)
        val delay = TranscriptionScheduler.computeNextDelayMillis(22, 0, now)
        assertEquals(23 * 60 * 60 * 1000L, delay)
    }
}
