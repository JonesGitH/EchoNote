package com.example.keywordrecorder.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ListenerStateBus {
    val state: StateFlow<ListenerState> = _state

}
