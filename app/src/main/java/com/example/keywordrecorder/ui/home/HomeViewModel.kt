package com.example.keywordrecorder.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.audio.ModelState
import com.example.keywordrecorder.service.ListenerState
import com.example.keywordrecorder.service.ListenerStateBus
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as KeywordRecorderApp

    val listenerState: StateFlow<ListenerState> = ListenerStateBus.state
    val modelState: StateFlow<ModelState> = application.modelManager.state
}
