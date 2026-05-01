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
import com.example.keywordrecorder.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as KeywordRecorderApp

    val listenerState: StateFlow<ListenerState> = ListenerStateBus.state
    val modelState: StateFlow<ModelState> = application.modelManager.state

    private val _requestAudioPermission = MutableStateFlow(false)
    val requestAudioPermission: StateFlow<Boolean> = _requestAudioPermission.asStateFlow()

    fun onPermissionRequestHandled() { _requestAudioPermission.value = false }

    fun toggleListening() {
        val currentState = listenerState.value
        if (currentState == ListenerState.STOPPED || currentState == ListenerState.ERROR) {
            if (!PermissionUtils.hasRecordAudio(application)) {
                _requestAudioPermission.value = true
                return
            }
            startService()
        } else {
            stopService()
        }
    }

    private fun startService() {
        val intent = Intent(application, KeywordListeningService::class.java).apply {
            action = KeywordListeningService.ACTION_START
        }
        application.startForegroundService(intent)
    }

    private fun stopService() {
        val intent = Intent(application, KeywordListeningService::class.java).apply {
            action = KeywordListeningService.ACTION_STOP
        }
        application.startForegroundService(intent)
    }
}
