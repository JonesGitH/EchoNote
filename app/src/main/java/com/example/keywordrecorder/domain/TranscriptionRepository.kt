package com.example.keywordrecorder.domain

import com.example.keywordrecorder.data.TranscriptionStatus

class TranscriptionRepository(
    private val recordingRepository: RecordingRepository,
    private val engine: TranscriptionEngine
) {
    suspend fun transcribe(recordingId: Long) {
        val entity = recordingRepository.getById(recordingId) ?: return
        recordingRepository.updateStatus(recordingId, TranscriptionStatus.PROCESSING)
        try {
            val result = engine.transcribe(entity.filePath)
            val text = result.text.ifBlank { "[No speech detected]" }
            recordingRepository.updateStatus(recordingId, TranscriptionStatus.COMPLETED, text = text)
        } catch (e: Exception) {
            recordingRepository.updateStatus(
                recordingId,
                TranscriptionStatus.FAILED,
                error = e.message ?: "Unknown error"
            )
        }
    }
}
