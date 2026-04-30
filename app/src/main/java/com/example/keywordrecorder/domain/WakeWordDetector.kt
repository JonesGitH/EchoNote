package com.example.keywordrecorder.domain

interface WakeWordDetector {
    suspend fun start()
    suspend fun stop()
    suspend fun awaitWakeWord()
}
