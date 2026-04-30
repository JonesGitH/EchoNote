package com.example.keywordrecorder.audio

import com.example.keywordrecorder.domain.WakeWordDetector

class PorcupineWakeWordDetector : WakeWordDetector {
    override suspend fun start() {
        // TODO: initialize Porcupine engine.
    }

    override suspend fun stop() {
        // TODO: release Porcupine engine.
    }

    override suspend fun awaitWakeWord() {
        // TODO: bind Porcupine process call and suspend until keyword detected.
    }
}
