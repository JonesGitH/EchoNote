package com.example.keywordrecorder.domain

interface WakeWordDetector {
    suspend fun start()
    suspend fun awaitWakeWord()
}
