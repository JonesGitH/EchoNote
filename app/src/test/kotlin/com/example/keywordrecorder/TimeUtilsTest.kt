package com.example.keywordrecorder

import com.example.keywordrecorder.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {
    @Test
    fun `formatDuration zero milliseconds`() {
        assertEquals("0:00", TimeUtils.formatDuration(0L))
    }

    @Test
    fun `formatDuration exactly 60 seconds`() {
        assertEquals("1:00", TimeUtils.formatDuration(60_000L))
    }

    @Test
    fun `formatDuration 90 seconds`() {
        assertEquals("1:30", TimeUtils.formatDuration(90_000L))
    }

    @Test
    fun `formatDuration over one hour`() {
        val result = TimeUtils.formatDuration(3_661_000L) // 1h 1m 1s
        assertEquals("1:01:01", result)
    }

    @Test
    fun `formatDuration sub-second rounds down`() {
        assertEquals("0:05", TimeUtils.formatDuration(5_999L))
    }

    @Test
    fun `midnightOf returns start of same day`() {
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.JUNE, 15, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnight = TimeUtils.midnightOf(cal.timeInMillis)

        val result = Calendar.getInstance().apply { timeInMillis = midnight }
        assertEquals(2024, result.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, result.get(Calendar.MONTH))
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, result.get(Calendar.MINUTE))
        assertEquals(0, result.get(Calendar.SECOND))
        assertEquals(0, result.get(Calendar.MILLISECOND))
    }

    @Test
    fun `midnightOf is before input time`() {
        val now = System.currentTimeMillis()
        val midnight = TimeUtils.midnightOf(now)
        assertTrue(midnight <= now)
    }
}
