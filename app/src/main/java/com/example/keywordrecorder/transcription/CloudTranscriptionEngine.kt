package com.example.keywordrecorder.transcription

import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult

class CloudTranscriptionEngine : TranscriptionEngine {
    override suspend fun transcribe(filePath: String): TranscriptionResult {
        // TODO: Wire to a backend proxy or cloud API key via secure config.
        throw UnsupportedOperationException("Cloud transcription not configured")
    }
}
