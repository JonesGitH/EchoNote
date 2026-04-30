package com.example.keywordrecorder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.util.FileUtils
import com.example.keywordrecorder.util.TimeUtils

class DailySummaryWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KeywordRecorderApp
        val now = System.currentTimeMillis()
        val midnight = TimeUtils.midnightOf(now)

        return try {
            val completed = app.recordingRepository.getCompletedSince(midnight)

            if (completed.isNotEmpty()) {
                val lines = completed.map { recording ->
                    "[${TimeUtils.formatEpoch(recording.createdAtEpochMillis)}] ${recording.transcriptText.orEmpty()}"
                }
                val summaryText = lines.joinToString("\n\n")

                app.dailySummaryRepository.insert(
                    DailySummaryEntity(
                        dateEpochMillis = midnight,
                        summaryText = summaryText,
                        recordingCount = completed.size,
                        createdAtEpochMillis = now,
                    )
                )

                for (recording in completed) {
                    app.recordingRepository.softDelete(recording.id)
                    FileUtils.deleteIfExists(recording.filePath)
                }
            }

            TranscriptionScheduler.scheduleDailySummary(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
