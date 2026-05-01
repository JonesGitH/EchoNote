package com.example.keywordrecorder.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.keywordrecorder.service.KeywordListeningService
import com.example.keywordrecorder.ui.MainActivity

object ListeningNotification {
    const val NOTIFICATION_ID = 1001
    private const val CHANNEL_ID = "keyword_listening"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(com.example.keywordrecorder.R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun build(context: Context): Notification {
        createChannel(context)
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            context, 1,
            Intent(context, KeywordListeningService::class.java).apply {
                action = KeywordListeningService.ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(com.example.keywordrecorder.R.string.notification_listening))
            .setSmallIcon(com.example.keywordrecorder.R.drawable.ic_notif_mic)
            .setContentIntent(openIntent)
            .addAction(com.example.keywordrecorder.R.drawable.ic_notif_mic,
                context.getString(com.example.keywordrecorder.R.string.notification_stop),
                stopIntent)
            .setOngoing(true)
            .build()
    }
}
