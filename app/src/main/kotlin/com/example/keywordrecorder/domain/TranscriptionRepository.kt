package com.example.keywordrecorder.domain

import com.example.keywordrecorder.data.AppSettings

class TranscriptionRepository(
    private val engine: TranscriptionEngine,
    private val recordingRepository: RecordingRepository,
) {
    suspend fun transcribeRecording(id: Long, settings: AppSettings) {
        val recording = recordingRepository.getById(id) ?: return

        if (recording.retryCount >= settings.maxRetryCount) {
            recordingRepository.markSkipped(id)
            return
        }

        recordingRepository.markProcessing(id)

        when (val result = engine.transcribe(recording.filePath)) {
            is TranscriptionResult.Success -> recordingRepository.markCompleted(id, result.text)
            is TranscriptionResult.Failure -> {
                val updated = recordingRepository.getById(id)
                if (updated != null && updated.retryCount + 1 >= settings.maxRetryCount) {
                    recordingRepository.markSkipped(id)
                } else {
                    recordingRepository.markFailed(id, result.error)
                }
            }
        }
    }
}
