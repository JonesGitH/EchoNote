package com.example.keywordrecorder.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TranscriptionScheduler {
    const val KEY_RECORDING_ID = "recording_id"

    fun enqueueImmediate(context: Context, recordingId: Long) {
        val request = OneTimeWorkRequestBuilder<ScheduledTranscriptionWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(KEY_RECORDING_ID to recordingId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "immediate_transcription_recording_$recordingId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueManual(context: Context, recordingId: Long) {
        val request = OneTimeWorkRequestBuilder<ScheduledTranscriptionWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(KEY_RECORDING_ID to recordingId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "manual_transcription_recording_$recordingId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val delay = delayUntil(hour, minute)
        val request = OneTimeWorkRequestBuilder<ScheduledTranscriptionWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "scheduled_batch_transcription",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun scheduleDailySummary(context: Context) {
        val delay = delayUntil(22, 0)
        val request = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "daily_summary",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun delayUntil(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis - now
    }
}
