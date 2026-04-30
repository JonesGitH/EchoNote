package com.example.keywordrecorder.audio

import com.example.keywordrecorder.domain.WakeWordDetector
import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeWakeWordDetector : WakeWordDetector {
    override suspend fun start() {}
    override suspend fun stop() {}

    override suspend fun awaitWakeWord() {
        // Simulate a random trigger between 5 and 15 seconds.
        delay(Random.nextLong(5_000, 15_000))
    }
}
