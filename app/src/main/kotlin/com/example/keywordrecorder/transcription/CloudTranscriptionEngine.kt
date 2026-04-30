package com.example.keywordrecorder.transcription

import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult

// TODO: No cloud endpoint wired — not integrated
class CloudTranscriptionEngine : TranscriptionEngine {
    override suspend fun transcribe(filePath: String): TranscriptionResult {
        throw UnsupportedOperationException("CloudTranscriptionEngine is not implemented")
    }
}
