package com.example.keywordrecorder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.domain.TranscriptionRepository
import com.example.keywordrecorder.util.FileUtils
import kotlinx.coroutines.flow.first

class ScheduledTranscriptionWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KeywordRecorderApp
        val settings = app.settingsDataStore.settingsFlow.first()
        val targetId = inputData.getLong(KEY_RECORDING_ID, -1)

        val candidates = app.recordingRepository.getPendingForTranscription()
            .filter { targetId == -1L || it.id == targetId }

        val repository = TranscriptionRepository(app.recordingRepository, app.transcriptionEngine())

        candidates.forEach { candidate ->
            if (candidate.transcriptionStatus == TranscriptionStatus.PROCESSING) return@forEach
            if (!settings.retryFailed && candidate.transcriptionStatus == TranscriptionStatus.FAILED) return@forEach
            if (candidate.retryCount > settings.maxRetryCount) return@forEach

            app.recordingRepository.update(candidate.copy(transcriptionStatus = TranscriptionStatus.PROCESSING))
            val result = repository.transcribeRecording(candidate)
            repository.persist(result)

            if (settings.deleteAudioAfterTranscription && result.transcriptionStatus == TranscriptionStatus.COMPLETED) {
                FileUtils.deleteIfExists(result.filePath)
            }
        }

        // Only re-schedule the daily batch when this run was the batch (not a targeted single recording).
        if (targetId == -1L) {
            TranscriptionScheduler.scheduleDaily(applicationContext, settings)
        }
        return Result.success()
    }

    companion object {
        private const val KEY_RECORDING_ID = "recording_id"
        fun inputForRecordingId(recordingId: Long): Data = Data.Builder()
            .putLong(KEY_RECORDING_ID, recordingId)
            .build()
    }
}
