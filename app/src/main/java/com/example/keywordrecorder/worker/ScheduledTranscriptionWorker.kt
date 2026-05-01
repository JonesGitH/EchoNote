package com.example.keywordrecorder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.keywordrecorder.KeywordRecorderApp

class ScheduledTranscriptionWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KeywordRecorderApp
        val recordingId = inputData.getLong("recording_id", -1L)
        return try {
            if (recordingId >= 0) {
                app.transcriptionRepository.transcribe(recordingId)
            } else {
                val pending = app.recordingRepository.getPending()
                for (rec in pending) {
                    app.transcriptionRepository.transcribe(rec.id)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
