package com.example.keywordrecorder.domain

data class TranscriptionResult(
    val text: String,
    val durationMillis: Long
)

interface TranscriptionEngine {
    suspend fun transcribe(filePath: String): TranscriptionResult
}
