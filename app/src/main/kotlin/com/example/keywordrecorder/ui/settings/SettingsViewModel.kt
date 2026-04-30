package com.example.keywordrecorder.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.AppSettings
import com.example.keywordrecorder.data.TranscriptionMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val echoApp = app as KeywordRecorderApp
    private val store = echoApp.settingsDataStore

    val settings: StateFlow<AppSettings> = store.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun updateKeyword(keyword: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch { store.updateWakeKeyword(keyword.trim()) }
    }

    fun updateMode(mode: TranscriptionMode) {
        viewModelScope.launch { store.updateTranscriptionMode(mode) }
    }

    fun updateDailyTime(hour: Int, minute: Int) {
        viewModelScope.launch { store.updateDailyTranscriptionTime(hour, minute) }
    }
}
