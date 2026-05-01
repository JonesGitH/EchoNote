package com.example.keywordrecorder.worker

import android.content.Context
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object TranscriptionScheduler {

    fun scheduleBatchTranscription(context: Context, hour: Int, minute: Int) {
        val delay = computeNextDelayMillis(hour, minute)
        val request = PeriodicWorkRequestBuilder<ScheduledTranscriptionWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "scheduled_batch_transcription",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleDailySummary(context: Context) {
        val delay = computeNextDelayMillis(22, 0)
        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_summary",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueImmediate(context: Context, recordingId: Long) {
        val data = workDataOf("recording_id" to recordingId)
        val request = OneTimeWorkRequestBuilder<ScheduledTranscriptionWorker>()
            .setInputData(data)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "immediate_transcription_recording_$recordingId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueManual(context: Context, recordingId: Long) {
        val data = workDataOf("recording_id" to recordingId)
        val request = OneTimeWorkRequestBuilder<ScheduledTranscriptionWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "manual_transcription_recording_$recordingId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun computeNextDelayMillis(
        hour: Int,
        minute: Int,
        now: LocalDateTime = LocalDateTime.now()
    ): Long {
        val target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        val finalTarget = if (target.isBefore(now)) {
            target.plusDays(1)
        } else {
            target
        }
        return Duration.between(now, finalTarget).toMillis()
    }
}
