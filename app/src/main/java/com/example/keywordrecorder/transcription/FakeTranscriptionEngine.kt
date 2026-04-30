package com.example.keywordrecorder.transcription

import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult
import kotlinx.coroutines.delay

class FakeTranscriptionEngine : TranscriptionEngine {
    override suspend fun transcribe(filePath: String): TranscriptionResult {
        delay(500)
        return TranscriptionResult(text = "This is a fake transcription for testing.", durationMillis = 500)
    }
}
