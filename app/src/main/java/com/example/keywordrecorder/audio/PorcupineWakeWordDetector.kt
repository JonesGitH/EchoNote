package com.example.keywordrecorder.audio

import com.example.keywordrecorder.domain.WakeWordDetector

class PorcupineWakeWordDetector : WakeWordDetector {
    override suspend fun start() = Unit
    override suspend fun awaitWakeWord() = Unit
    override fun stop() = Unit
}
