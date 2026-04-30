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

class RecordingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as KeywordRecorderApp
    private val repository = app.recordingRepository
    private val summaryRepository = app.dailySummaryRepository

    val recordings: StateFlow<List<RecordingEntity>> = repository.observeRecordings().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val dailySummaries: StateFlow<List<DailySummaryEntity>> = summaryRepository.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    fun deleteRecording(id: Long) {
        viewModelScope.launch {
            repository.softDelete(id)
        }
    }
}
