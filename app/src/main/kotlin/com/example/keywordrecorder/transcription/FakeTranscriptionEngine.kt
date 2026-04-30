package com.example.keywordrecorder.transcription

import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult
import kotlinx.coroutines.delay

class FakeTranscriptionEngine(
    private val delayMs: Long = 500L,
    private val result: TranscriptionResult = TranscriptionResult.Success("Fake transcription text."),
) : TranscriptionEngine {
    override suspend fun transcribe(filePath: String): TranscriptionResult {
        delay(delayMs)
        return result
    }
}
