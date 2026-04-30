package com.example.keywordrecorder.ui.recordings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class RecordingsViewModel(app: Application) : AndroidViewModel(app) {
    private val echoApp = app as KeywordRecorderApp

    val summaries: StateFlow<List<DailySummaryEntity>> = echoApp.dailySummaryRepository
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recordings: StateFlow<List<RecordingEntity>> = echoApp.recordingRepository
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
