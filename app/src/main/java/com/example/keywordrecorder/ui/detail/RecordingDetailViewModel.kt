package com.example.keywordrecorder.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.worker.TranscriptionScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecordingDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as KeywordRecorderApp

    private val _recordingId = MutableStateFlow<Long?>(null)
    
    val recording: StateFlow<RecordingEntity?> = _recordingId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else application.recordingRepository.observeById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun load(id: Long) {
        _recordingId.value = id
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
