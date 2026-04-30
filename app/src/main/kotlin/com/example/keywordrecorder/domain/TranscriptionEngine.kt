package com.example.keywordrecorder.domain

sealed class TranscriptionResult {
    data class Success(val text: String) : TranscriptionResult()
    data class Failure(val error: String) : TranscriptionResult()
}

interface TranscriptionEngine {
    suspend fun transcribe(filePath: String): TranscriptionResult
}
