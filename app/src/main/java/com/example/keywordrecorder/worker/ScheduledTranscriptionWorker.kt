package com.example.keywordrecorder.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.keywordrecorder.KeywordRecorderApp
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

class ScheduledTranscriptionWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KeywordRecorderApp
        val recordingId = inputData.getLong("recording_id", -1L)
        return try {
            if (recordingId >= 0) {
                app.transcriptionRepository.transcribe(recordingId)
            } else {
                // Batch mode: process pending recordings and auto-retry eligible failures
                val settings = app.settingsDataStore.settings.first()
                val candidates = if (settings.retryFailed) {
                    app.recordingRepository.getRetryable(settings.maxRetryCount)
                } else {
                    app.recordingRepository.getPending()
                }
                for (rec in candidates) {
                    try {
                        app.transcriptionRepository.transcribe(rec.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e("TranscriptionWorker", "Failed to transcribe ${rec.id}", e)
                    }
                }
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TranscriptionWorker", "Worker failed", e)
            Result.retry()
        }
    }
}
