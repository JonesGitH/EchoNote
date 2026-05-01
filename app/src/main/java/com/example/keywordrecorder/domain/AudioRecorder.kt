package com.example.keywordrecorder.domain


data class RecordingResult(
    val filePath: String,
    val fileName: String,
    val durationMillis: Long,
)

interface AudioRecorder {
}
