package com.example.keywordrecorder.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.keywordrecorder.data.AppSettings
import com.example.keywordrecorder.data.SettingsDataStore
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DAILY_SUMMARY_HOUR = 22
private const val DAILY_SUMMARY_MINUTE = 0

object TranscriptionScheduler {
    private const val SCHEDULED_BATCH_WORK = "scheduled_batch_transcription"

    fun scheduleDaily(context: Context, settingsDataStore: SettingsDataStore) {
        CoroutineScope(Dispatchers.IO).launch {
            val settings = settingsDataStore.settingsFlow.first()
            scheduleDaily(context, settings)
        }
    }

    fun scheduleDaily(context: Context, settings: AppSettings) {
        val delayMillis = computeNextDelayMillis(settings.dailyTranscriptionHour, settings.dailyTranscriptionMinute)
        val constraints = constraintsFrom(settings)
        val request = OneTimeWorkRequestBuilder<ScheduledTranscriptionWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SCHEDULED_BATCH_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun enqueueManualTranscription(context: Context, recordingId: Long) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "manual_transcription_recording_$recordingId",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ScheduledTranscriptionWorker>()
                .setInputData(ScheduledTranscriptionWorker.inputForRecordingId(recordingId))
                .build(),
        )
    }

    fun enqueueImmediateTranscription(context: Context, recordingId: Long) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "immediate_transcription_recording_$recordingId",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ScheduledTranscriptionWorker>()
                .setInputData(ScheduledTranscriptionWorker.inputForRecordingId(recordingId))
                .build(),
        )
    }

    fun scheduleDailySummary(context: Context, settingsDataStore: SettingsDataStore) {
        CoroutineScope(Dispatchers.IO).launch {
            scheduleDailySummary(context, settingsDataStore.settingsFlow.first())
        }
    }

    fun scheduleDailySummary(context: Context, settings: AppSettings) {
        val delayMillis = computeNextDelayMillis(DAILY_SUMMARY_HOUR, DAILY_SUMMARY_MINUTE)
        val constraints = constraintsFrom(settings)
        val request = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "daily_summary",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun computeNextDelayMillis(hour: Int, minute: Int, now: LocalDateTime = LocalDateTime.now()): Long {
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }

    private fun constraintsFrom(settings: AppSettings): Constraints {
        val networkType = when {
            settings.onlyWifi -> NetworkType.UNMETERED
            else -> NetworkType.NOT_REQUIRED
        }
        return Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresCharging(settings.onlyCharging)
            .build()
    }
}
