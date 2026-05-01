package com.example.keywordrecorder.ui.recordings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
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

    fun deleteAllRecordings() {
        viewModelScope.launch {
            application.recordingRepository.deleteAll()
        }
    }
}
