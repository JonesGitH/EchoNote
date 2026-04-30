package com.example.keywordrecorder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.util.FileUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class DailySummaryWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KeywordRecorderApp
        val settings = app.settingsDataStore.settingsFlow.first()

        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val completedToday = app.recordingRepository.getCompletedSince(startOfDay)

        if (completedToday.isNotEmpty()) {
            val timeFormatter = SimpleDateFormat("h:mm a", Locale.US)
            val summaryText = completedToday.joinToString("\n\n") { recording ->
                val time = timeFormatter.format(Date(recording.createdAtEpochMillis))
                "[$time] ${recording.transcriptText?.trim().orEmpty()}"
            }

            app.dailySummaryRepository.insert(
                DailySummaryEntity(
                    dateEpochMillis = startOfDay,
                    summaryText = summaryText,
                    recordingCount = completedToday.size,
                    createdAtEpochMillis = System.currentTimeMillis(),
                )
            )

            completedToday.forEach { recording ->
                app.recordingRepository.softDelete(recording.id)
                FileUtils.deleteIfExists(recording.filePath)
            }
        }

        TranscriptionScheduler.scheduleDailySummary(app, settings)
        return Result.success()
    }
}
