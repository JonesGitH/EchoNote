package com.example.keywordrecorder.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.worker.TranscriptionScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecordingDetailViewModel(
    app: Application,
    savedState: SavedStateHandle,
) : AndroidViewModel(app) {
    private val echoApp = app as KeywordRecorderApp
    private val recordingId: Long = checkNotNull(savedState["recordingId"])

    private val _recording = MutableStateFlow<RecordingEntity?>(null)
    val recording: StateFlow<RecordingEntity?> = _recording

    init {
        viewModelScope.launch { _recording.value = echoApp.recordingRepository.getById(recordingId) }
    }

    fun retranscribe() {
        TranscriptionScheduler.enqueueManual(getApplication(), recordingId)
    }

    fun delete() {
        viewModelScope.launch {
            echoApp.recordingRepository.softDelete(recordingId)
        }
    }
}
