package com.example.keywordrecorder.domain

data class RecordingResult(
    val filePath: String,
    val fileName: String,
    val durationMillis: Long,
    val createdAtEpochMillis: Long,
)

interface AudioRecorder {
    fun startRecording(outputDir: String): String
    fun stopRecording(): RecordingResult
    fun getMaxAmplitude(): Int
}
