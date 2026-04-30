package com.example.keywordrecorder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.keywordrecorder.KeywordRecorderApp

class ScheduledTranscriptionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KeywordRecorderApp
        val settings = app.settingsDataStore.settingsFlow.let {
            kotlinx.coroutines.flow.first(it)
        }

        val recordingId = inputData.getLong(TranscriptionScheduler.KEY_RECORDING_ID, -1L)

        return try {
            if (recordingId >= 0) {
                app.transcriptionRepository.transcribeRecording(recordingId, settings)
            } else {
                val pending = app.recordingRepository.getPendingRecordings()
                for (recording in pending) {
                    app.transcriptionRepository.transcribeRecording(recording.id, settings)
                }
                if (settings.retryFailed) {
                    val retryable = app.recordingRepository.getRetryableFailedRecordings(settings.maxRetryCount)
                    for (recording in retryable) {
                        app.transcriptionRepository.transcribeRecording(recording.id, settings)
                    }
                }
                TranscriptionScheduler.scheduleDaily(
                    applicationContext,
                    settings.dailyTranscriptionHour,
                    settings.dailyTranscriptionMinute,
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
