package com.example.keywordrecorder.util

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val dateShortFormat = SimpleDateFormat("MMM d", Locale.US)
    private val fullFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)

    fun formatEpoch(epochMillis: Long): String = timeFormat.format(Date(epochMillis))
    fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
    fun formatDateShort(epochMillis: Long): String = dateShortFormat.format(Date(epochMillis))
    fun formatFull(epochMillis: Long): String = fullFormat.format(Date(epochMillis))

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
