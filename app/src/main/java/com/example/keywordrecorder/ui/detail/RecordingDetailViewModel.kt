package com.example.keywordrecorder.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.worker.TranscriptionScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecordingDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as KeywordRecorderApp

    private val _recording = MutableStateFlow<RecordingEntity?>(null)
    val recording: StateFlow<RecordingEntity?> = _recording

    fun load(id: Long) {
        viewModelScope.launch {
            _recording.value = application.recordingRepository.getById(id)
        }
    }

    fun retranscribe(id: Long) {
        TranscriptionScheduler.enqueueManual(application, id)
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val rec = application.recordingRepository.getById(id) ?: return@launch
            application.recordingRepository.softDelete(id)
            com.example.keywordrecorder.util.FileUtils.deleteIfExists(rec.filePath)
            onDone()
        }
    }
}
