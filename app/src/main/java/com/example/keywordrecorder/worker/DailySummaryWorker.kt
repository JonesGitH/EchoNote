package com.example.keywordrecorder.worker

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.util.FileUtils
import com.example.keywordrecorder.util.TimeUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class DailySummaryWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KeywordRecorderApp

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightMillis = cal.timeInMillis

        val completed = app.recordingRepository.getCompletedSince(midnightMillis)
        if (completed.isEmpty()) return Result.success()

        val lines = completed.map { rec ->
            val time = TimeUtils.formatEpoch(rec.createdAtEpochMillis)
            "[$time] ${rec.transcriptText ?: ""}"
        }
        val summaryText = lines.joinToString("\n\n")

        val entity = DailySummaryEntity(
            dateEpochMillis = midnightMillis,
            summaryText = summaryText,
            recordingCount = completed.size,
            createdAtEpochMillis = System.currentTimeMillis()
        )
        app.dailySummaryRepository.insert(entity)

        try { exportToDownloads(summaryText, midnightMillis) } catch (e: Exception) {
            android.util.Log.e("DailySummaryWorker", "Export to Downloads failed", e)
        }

        for (rec in completed) {
            app.recordingRepository.softDelete(rec.id)
            FileUtils.deleteIfExists(rec.filePath)
        }

        return Result.success()
    }

    private fun exportToDownloads(text: String, dateMillis: Long) {
        val dateLabel = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(dateMillis))
        val fileName = "EchoNote_$dateLabel.txt"
        val content = text.toByteArray(Charsets.UTF_8)

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
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
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
