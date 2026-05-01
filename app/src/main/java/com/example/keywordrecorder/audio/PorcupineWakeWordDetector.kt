package com.example.keywordrecorder.audio

import com.example.keywordrecorder.domain.WakeWordDetector

class PorcupineWakeWordDetector : WakeWordDetector {
    override suspend fun start(): Unit = TODO("Porcupine not integrated")
    override suspend fun awaitWakeWord(): Unit = TODO("Porcupine not integrated")
    override fun stop(): Unit = TODO("Porcupine not integrated")
}
