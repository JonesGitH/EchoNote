package com.example.keywordrecorder.ui.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.audio.ModelState
import com.example.keywordrecorder.service.KeywordListeningService
import com.example.keywordrecorder.service.ListenerState
import com.example.keywordrecorder.service.ListenerStateBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<KeywordRecorderApp>()

    private val _listenerState = MutableStateFlow(ListenerState.IDLE)
    val listenerState: StateFlow<ListenerState> = _listenerState

    val modelState: StateFlow<ModelState> = app.modelManager.state.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ModelState.Idle,
    )

    init {
        viewModelScope.launch {
            ListenerStateBus.state.collect { _listenerState.value = it }
        }
    }

    fun startListening() {
        app.startForegroundService(
            Intent(app, KeywordListeningService::class.java).apply {
                action = KeywordListeningService.ACTION_START
            }
        )
    }

    fun stopListening() {
        app.startService(
            Intent(app, KeywordListeningService::class.java).apply {
                action = KeywordListeningService.ACTION_STOP
            }
        )
    }
}
