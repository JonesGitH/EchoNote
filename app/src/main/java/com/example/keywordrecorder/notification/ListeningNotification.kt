package com.example.keywordrecorder.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.keywordrecorder.service.KeywordListeningService

object ListeningNotification {
    const val NOTIFICATION_ID = 1001

            val channel = NotificationChannel(
                CHANNEL_ID,
            )
    }

    fun build(context: Context): Notification {
            action = KeywordListeningService.ACTION_STOP
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }
}
