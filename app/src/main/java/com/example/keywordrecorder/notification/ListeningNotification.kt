package com.example.keywordrecorder.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.keywordrecorder.R
import com.example.keywordrecorder.service.KeywordListeningService

object ListeningNotification {
    const val CHANNEL_ID = "keyword_listening"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Keyword listening",
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }
    }

    fun build(context: Context): Notification {
        val stopIntent = Intent(context, KeywordListeningService::class.java).apply {
            action = KeywordListeningService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            7,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Listening for wake keyword")
            .setOngoing(true)
            .addAction(0, "Stop Listening", stopPendingIntent)
            .build()
    }
}
