package com.example.keywordrecorder.ui.home

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.audio.ModelState
import com.example.keywordrecorder.service.KeywordListeningService
import com.example.keywordrecorder.service.ListenerState
import com.example.keywordrecorder.service.ListenerStateBus
import com.example.keywordrecorder.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as KeywordRecorderApp

    val listenerState: StateFlow<ListenerState> = ListenerStateBus.state
    val modelState: StateFlow<ModelState> = application.modelManager.state

    private val _needsPermission = MutableStateFlow(false)
    val needsPermission: StateFlow<Boolean> = _needsPermission

    fun toggleListening() {
        val context = getApplication<Application>()
        val current = listenerState.value

        if (current == ListenerState.STOPPED || current == ListenerState.ERROR) {
            if (!PermissionUtils.hasRecordAudio(context)) {
                _needsPermission.value = true
                return
            }
            val intent = Intent(context, KeywordListeningService::class.java).apply {
                action = KeywordListeningService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            val intent = Intent(context, KeywordListeningService::class.java).apply {
                action = KeywordListeningService.ACTION_STOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun stopListening() {
        val context = getApplication<Application>()
        val intent = Intent(context, KeywordListeningService::class.java).apply {
            action = KeywordListeningService.ACTION_STOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun onPermissionHandled() {
        _needsPermission.value = false
    }
}
