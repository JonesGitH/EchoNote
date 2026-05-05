package com.example.keywordrecorder.worker

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.util.FileUtils
import com.example.keywordrecorder.util.TimeUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class DailySummaryWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KeywordRecorderApp

        // Transcribe any recordings the 21:00 batch worker may have missed
        val settings = app.settingsDataStore.settings.first()
        val pending = if (settings.retryFailed) {
            app.recordingRepository.getRetryable(settings.maxRetryCount)
        } else {
            app.recordingRepository.getPending()
        }
        for (rec in pending) {
            try {
                app.transcriptionRepository.transcribe(rec.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DailySummaryWorker", "Transcription failed for ${rec.id}", e)
            }
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightMillis = cal.timeInMillis

        val allRecordings = app.recordingRepository.getAllForDay(midnightMillis)
        if (allRecordings.isEmpty()) return Result.success()

        val summaryText = allRecordings.joinToString("\n\n") { rec ->
            "[${TimeUtils.formatEpoch(rec.createdAtEpochMillis)}] ${rec.transcriptText ?: ""}"
        }
        val markdownContent = buildMarkdown(allRecordings, midnightMillis)

        val entity = DailySummaryEntity(
            dateEpochMillis = midnightMillis,
            summaryText = summaryText,
            recordingCount = allRecordings.size,
            createdAtEpochMillis = System.currentTimeMillis()
        )
        app.dailySummaryRepository.insert(entity)

        try { exportToDownloads(markdownContent, midnightMillis) } catch (e: Exception) {
            android.util.Log.e("DailySummaryWorker", "Export to Downloads failed", e)
        }

        val completed = allRecordings.filter {
            it.transcriptionStatus == TranscriptionStatus.COMPLETED
        }
        for (rec in completed) {
            app.recordingRepository.softDelete(rec.id)
            FileUtils.deleteIfExists(rec.filePath)
        }

        return Result.success()
    }

    private fun buildMarkdown(recordings: List<RecordingEntity>, dateMillis: Long): String {
        val dateLabel = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date(dateMillis))
        val sb = StringBuilder()
        sb.appendLine("# EchoNote — $dateLabel")
        sb.appendLine()
        recordings.forEach { rec ->
            val time = TimeUtils.formatEpoch(rec.createdAtEpochMillis)
            val duration = TimeUtils.formatDuration(rec.durationMillis)
            sb.appendLine("## $time · $duration")
            sb.appendLine()
            if (!rec.transcriptText.isNullOrBlank()) {
                sb.appendLine(rec.transcriptText)
            } else {
                sb.appendLine("*(no transcript)*")
            }
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    private fun exportToDownloads(markdown: String, dateMillis: Long) {
        val dateLabel = SimpleDateFormat("yyyy_MM_dd", Locale.US).format(Date(dateMillis))
        val fileName = "${dateLabel}_EchoNote.md"
        val content = markdown.toByteArray(Charsets.UTF_8)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = applicationContext.contentResolver
            val existing = resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                arrayOf(fileName),
                null
            )
            existing?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI.buildUpon().appendPath(id.toString()).build(),
                        null, null
                    )
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/markdown")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { it.write(content) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            FileOutputStream(File(dir, fileName)).use { it.write(content) }
        }
    }
}
