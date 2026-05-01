package com.example.keywordrecorder.audio

import com.example.keywordrecorder.domain.WakeWordDetector
import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeWakeWordDetector : WakeWordDetector {
    override suspend fun start() {}
    override suspend fun awaitWakeWord() {
        delay(Random.nextLong(5_000, 15_000))
    }
    override fun stop() {}
}
