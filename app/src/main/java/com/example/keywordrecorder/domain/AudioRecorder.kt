package com.example.keywordrecorder.domain

data class RecordingSession(val startedAtMillis: Long)

data class RecordingResult(
    val filePath: String,
    val fileName: String,
    val durationMillis: Long,
)

interface AudioRecorder {
    suspend fun startRecording(): RecordingSession
    suspend fun stopRecording(): RecordingResult
}
