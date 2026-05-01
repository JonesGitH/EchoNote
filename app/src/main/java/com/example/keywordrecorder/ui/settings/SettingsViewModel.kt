package com.example.keywordrecorder.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.AppSettings
import com.example.keywordrecorder.data.TranscriptionMode
import com.example.keywordrecorder.worker.TranscriptionScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as KeywordRecorderApp
    private val store = application.settingsDataStore

    val settings: StateFlow<AppSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateKeyword(keyword: String) = viewModelScope.launch {
        store.updateWakeKeyword(keyword.trim().ifBlank { "keyword" })
    }

    fun updateMode(mode: TranscriptionMode) = viewModelScope.launch {
        store.updateTranscriptionMode(mode)
    }

    fun updateMaxRecordingSeconds(seconds: Int) = viewModelScope.launch {
        store.updateMaxRecordingSeconds(seconds.coerceIn(30, 300))
    }

    fun updateDailyTime(hour: Int, minute: Int) = viewModelScope.launch {
        store.updateDailyTranscriptionTime(hour, minute)
        TranscriptionScheduler.scheduleBatchTranscription(application, hour, minute)
    }

    fun deleteAllRecordings() = viewModelScope.launch {
        application.recordingRepository.deleteAll()
    }
}
