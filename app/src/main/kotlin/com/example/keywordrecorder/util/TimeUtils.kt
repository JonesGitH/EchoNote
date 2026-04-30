package com.example.keywordrecorder.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val epochFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    fun formatEpoch(epochMillis: Long): String =
        epochFormatter.format(Date(epochMillis))

    fun formatDate(epochMillis: Long): String =
        dateFormatter.format(Date(epochMillis))

    fun formatDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    fun midnightOf(epochMillis: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = epochMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
