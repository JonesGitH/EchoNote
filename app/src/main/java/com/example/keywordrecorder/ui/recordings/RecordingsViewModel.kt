package com.example.keywordrecorder.ui.recordings

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.util.FileUtils
import com.example.keywordrecorder.util.TimeUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class RecordingsViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as KeywordRecorderApp

    val recordings: StateFlow<List<RecordingEntity>> =
        application.recordingRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summaries: StateFlow<List<DailySummaryEntity>> =
        application.dailySummaryRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exportMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val exportMessage: SharedFlow<String> = _exportMessage

    fun softDeleteRecording(id: Long) = viewModelScope.launch {
        application.recordingRepository.softDelete(id)
    }

    fun restoreRecording(id: Long) = viewModelScope.launch {
        application.recordingRepository.restoreRecording(id)
    }

    fun deleteRecordingFile(filePath: String) = viewModelScope.launch {
        FileUtils.deleteIfExists(filePath)
    }

    fun deleteAllRecordings() = viewModelScope.launch {
        application.recordingRepository.deleteAll()
    }

    fun exportAllTranscribed() = viewModelScope.launch {
        val transcribed = recordings.value
            .filter { it.transcriptionStatus == TranscriptionStatus.COMPLETED && !it.transcriptText.isNullOrBlank() }
            .sortedBy { it.createdAtEpochMillis }

        if (transcribed.isEmpty()) {
            _exportMessage.tryEmit("No transcribed recordings to export")
            return@launch
        }

        try {
            val fileName = withContext(Dispatchers.IO) { writeExportFile(transcribed) }
            _exportMessage.tryEmit("Saved to Downloads/$fileName")
        } catch (e: Exception) {
            _exportMessage.tryEmit("Export failed: ${e.message}")
        }
    }

    private fun writeExportFile(transcribed: List<RecordingEntity>): String {
        val dateLabel = SimpleDateFormat("yyyy_MM_dd", Locale.US).format(Date())
        val fileName = "${dateLabel}_EchoNote.md"
        val content = buildExportMarkdown(transcribed).toByteArray(Charsets.UTF_8)
        val ctx = application

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = ctx.contentResolver
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                arrayOf(fileName), null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI.buildUpon()
                            .appendPath(cursor.getLong(0).toString()).build(),
                        null, null
                    )
                }
            }
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/markdown")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("Could not create file in Downloads")
            resolver.openOutputStream(uri)?.use { it.write(content) }
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            FileOutputStream(File(dir, fileName)).use { it.write(content) }
        }
        return fileName
    }

    private fun buildExportMarkdown(transcribed: List<RecordingEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("# EchoNote — All Transcribed Recordings")
        sb.appendLine()

        val byDay = transcribed.groupBy { rec ->
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(rec.createdAtEpochMillis))
        }

        byDay.forEach { (_, dayRecs) ->
            sb.appendLine("## ${TimeUtils.formatDate(dayRecs.first().createdAtEpochMillis)}")
            sb.appendLine()
            dayRecs.forEach { rec ->
                val time = TimeUtils.formatEpoch(rec.createdAtEpochMillis)
                val duration = TimeUtils.formatDuration(rec.durationMillis)
                sb.appendLine("### $time · $duration")
                sb.appendLine()
                sb.appendLine(rec.transcriptText!!)
                sb.appendLine()
                sb.appendLine("---")
                sb.appendLine()
            }
        }
        return sb.toString().trimEnd()
    }
}
