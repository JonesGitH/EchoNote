package com.example.keywordrecorder.transcription

import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult

class LocalTranscriptionEngine : TranscriptionEngine {
    override suspend fun transcribe(filePath: String): TranscriptionResult {
        throw UnsupportedOperationException("Local (Whisper.cpp) transcription is not yet implemented")
    }
}
