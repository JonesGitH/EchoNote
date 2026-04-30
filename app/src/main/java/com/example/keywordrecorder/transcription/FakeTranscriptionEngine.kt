package com.example.keywordrecorder.transcription

import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult
import java.io.File

class FakeTranscriptionEngine : TranscriptionEngine {
    override suspend fun transcribe(filePath: String): TranscriptionResult {
        val fileName = File(filePath).name
        return TranscriptionResult(text = "This is a placeholder transcript for $fileName.")
    }
}
