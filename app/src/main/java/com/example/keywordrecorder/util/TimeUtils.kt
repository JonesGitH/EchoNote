package com.example.keywordrecorder.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val fullFormat = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)

    private fun toLocal(epochMillis: Long) =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())

    fun formatEpoch(epochMillis: Long): String = timeFormat.format(toLocal(epochMillis))
    fun formatDate(epochMillis: Long): String = dateFormat.format(toLocal(epochMillis))
    fun formatFull(epochMillis: Long): String = fullFormat.format(toLocal(epochMillis))

    fun formatDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    fun formatSeconds(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
