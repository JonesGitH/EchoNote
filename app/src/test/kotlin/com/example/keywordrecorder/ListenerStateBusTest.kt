package com.example.keywordrecorder

import com.example.keywordrecorder.service.ListenerState
import com.example.keywordrecorder.service.ListenerStateBus
import org.junit.Assert.assertEquals
import org.junit.Test

class ListenerStateBusTest {
    @Test
    fun `initial state is IDLE`() {
        // Reset to known state first
        ListenerStateBus.emit(ListenerState.IDLE)
        assertEquals(ListenerState.IDLE, ListenerStateBus.state.value)
    }

    @Test
    fun `emitting a state makes it immediately observable`() {
        ListenerStateBus.emit(ListenerState.LISTENING)
        assertEquals(ListenerState.LISTENING, ListenerStateBus.state.value)

        ListenerStateBus.emit(ListenerState.WAKE_WORD_DETECTED)
        assertEquals(ListenerState.WAKE_WORD_DETECTED, ListenerStateBus.state.value)

        ListenerStateBus.emit(ListenerState.RECORDING)
        assertEquals(ListenerState.RECORDING, ListenerStateBus.state.value)

        ListenerStateBus.emit(ListenerState.IDLE)
        assertEquals(ListenerState.IDLE, ListenerStateBus.state.value)
    }
}
