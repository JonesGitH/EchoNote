package com.example.keywordrecorder.ui.recordings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.util.FileUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordingsViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as KeywordRecorderApp

    val recordings: StateFlow<List<RecordingEntity>> =
        application.recordingRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summaries: StateFlow<List<DailySummaryEntity>> =
        application.dailySummaryRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Soft-delete only (DB). Call [deleteRecordingFile] after undo window expires. */
    fun softDeleteRecording(id: Long) = viewModelScope.launch {
        application.recordingRepository.softDelete(id)
    }

    /** Restore a soft-deleted recording (undo support). */
    fun restoreRecording(id: Long) = viewModelScope.launch {
        application.recordingRepository.restoreRecording(id)
    }

    /** Delete the audio file from disk after the undo window has passed. */
    fun deleteRecordingFile(filePath: String) = viewModelScope.launch {
        FileUtils.deleteIfExists(filePath)
    }

    fun deleteAllRecordings() = viewModelScope.launch {
        application.recordingRepository.deleteAll()
    }
}
