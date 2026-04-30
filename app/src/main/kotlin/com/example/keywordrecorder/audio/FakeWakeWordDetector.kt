package com.example.keywordrecorder.audio

import com.example.keywordrecorder.domain.WakeWordDetector
import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeWakeWordDetector : WakeWordDetector {
    override suspend fun start() = Unit

    override suspend fun awaitWakeWord() {
        val delayMs = Random.nextLong(5_000L, 15_000L)
        delay(delayMs)
    }

    override fun stop() = Unit
}
