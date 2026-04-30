package com.example.keywordrecorder.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val formatter = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.US)

    fun formatEpoch(epochMillis: Long): String = formatter.format(Date(epochMillis))

    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) "$minutes:${seconds.toString().padStart(2, '0')} min"
        else "$seconds sec"
    }
}
