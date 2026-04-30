package com.example.keywordrecorder.domain

data class RecordingSession(val filePath: String, val startTimeMillis: Long)

data class RecordingResult(
    val filePath: String,
    val fileName: String,
    val durationMillis: Long,
    val createdAtEpochMillis: Long
)

interface AudioRecorder {
    fun startRecording(): RecordingSession
    fun stopRecording(): RecordingResult
    fun getMaxAmplitude(): Int
}
