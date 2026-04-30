package com.example.keywordrecorder.ui.detail

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.worker.TranscriptionScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RecordingDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as KeywordRecorderApp
    private val _recording = MutableStateFlow<RecordingEntity?>(null)
    val recording: StateFlow<RecordingEntity?> = _recording

    private val _event = MutableSharedFlow<String>()
    val event: SharedFlow<String> = _event.asSharedFlow()

    private var mediaPlayer: MediaPlayer? = null

    fun load(id: Long) {
        viewModelScope.launch {
            app.recordingRepository.observeRecording(id).collect { _recording.value = it }
        }
    }

    fun play() {
        val current = _recording.value ?: return
        viewModelScope.launch {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(current.filePath)
                    prepare()
                    start()
                    setOnCompletionListener { release(); mediaPlayer = null }
                }
                _event.emit("Playing recording")
            } catch (e: Exception) {
                _event.emit("Could not play recording")
            }
        }
    }

    fun transcribeNow() {
        _recording.value?.let {
            TranscriptionScheduler.enqueueManualTranscription(getApplication(), it.id)
            viewModelScope.launch { _event.emit("Transcription queued") }
        }
    }

    fun delete() {
        _recording.value?.let { item ->
            viewModelScope.launch { app.recordingRepository.softDelete(item.id) }
        }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }
}
