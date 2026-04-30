package com.example.keywordrecorder.transcription

import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult

// TODO: Whisper.cpp not integrated
class LocalTranscriptionEngine : TranscriptionEngine {
    override suspend fun transcribe(filePath: String): TranscriptionResult {
        throw UnsupportedOperationException("LocalTranscriptionEngine is not implemented")
    }
}
