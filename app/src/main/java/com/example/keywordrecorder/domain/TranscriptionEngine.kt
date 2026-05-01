package com.example.keywordrecorder.domain

data class TranscriptionResult(
    val text: String,
)

interface TranscriptionEngine {
    suspend fun transcribe(filePath: String): TranscriptionResult
}
