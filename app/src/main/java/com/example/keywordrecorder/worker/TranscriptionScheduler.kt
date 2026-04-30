package com.example.keywordrecorder.worker

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TranscriptionScheduler {

    fun scheduleBatchTranscription(context: Context, hour: Int, minute: Int) {
        val delay = millisUntil(hour, minute)
        val request = PeriodicWorkRequestBuilder<ScheduledTranscriptionWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "scheduled_batch_transcription",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleDailySummary(context: Context) {
        val delay = millisUntil(22, 0)
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

    private fun millisUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
