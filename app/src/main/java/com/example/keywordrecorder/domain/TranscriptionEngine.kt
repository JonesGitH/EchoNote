package com.example.keywordrecorder.domain

data class TranscriptionResult(
    val text: String,
    val durationMillis: Long? = null,
)

interface TranscriptionEngine {
    suspend fun transcribe(filePath: String): TranscriptionResult
}
