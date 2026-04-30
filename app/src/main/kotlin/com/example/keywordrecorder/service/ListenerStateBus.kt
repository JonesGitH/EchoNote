package com.example.keywordrecorder.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ListenerState { IDLE, LISTENING, WAKE_WORD_DETECTED, RECORDING }

object ListenerStateBus {
    private val _state = MutableStateFlow(ListenerState.IDLE)
    val state: StateFlow<ListenerState> = _state

    fun emit(state: ListenerState) {
        _state.value = state
    }
}
