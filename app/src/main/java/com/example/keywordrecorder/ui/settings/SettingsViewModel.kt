package com.example.keywordrecorder.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.AppSettings
import com.example.keywordrecorder.data.SettingsDataStore
import com.example.keywordrecorder.data.TranscriptionMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsDataStore = (application as KeywordRecorderApp).settingsDataStore

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings(),
    )

    fun saveDailyTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.update {
                it[SettingsDataStore.Keys.dailyTranscriptionHour] = hour
                it[SettingsDataStore.Keys.dailyTranscriptionMinute] = minute
            }
        }
    }

    fun saveWakeKeyword(keyword: String) {
        viewModelScope.launch {
            settingsDataStore.update {
                it[SettingsDataStore.Keys.wakeKeyword] = keyword
            }
        }
    }

    fun saveTranscriptionMode(mode: TranscriptionMode) {
        viewModelScope.launch {
            settingsDataStore.update {
                it[SettingsDataStore.Keys.transcriptionMode] = mode.name
            }
        }
    }

    fun saveSilenceTimeout(seconds: Int) {
        viewModelScope.launch {
            settingsDataStore.update {
                it[SettingsDataStore.Keys.silenceTimeoutSeconds] = seconds
            }
        }
    }

    fun saveRetryFailed(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.update {
                it[SettingsDataStore.Keys.retryFailed] = enabled
            }
        }
    }

    fun saveOnlyWifi(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.update {
                it[SettingsDataStore.Keys.onlyWifi] = enabled
            }
        }
    }

    fun saveOnlyCharging(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.update {
                it[SettingsDataStore.Keys.onlyCharging] = enabled
            }
        }
    }

    fun saveDeleteAudioAfterTranscription(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.update {
                it[SettingsDataStore.Keys.deleteAudioAfterTranscription] = enabled
            }
        }
    }
}
