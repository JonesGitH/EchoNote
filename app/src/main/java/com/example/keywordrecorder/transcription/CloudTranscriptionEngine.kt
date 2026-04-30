package com.example.keywordrecorder.transcription

import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult

class CloudTranscriptionEngine : TranscriptionEngine {
    override suspend fun transcribe(filePath: String): TranscriptionResult {
        throw UnsupportedOperationException("Cloud transcription is not yet implemented")
    }
}
