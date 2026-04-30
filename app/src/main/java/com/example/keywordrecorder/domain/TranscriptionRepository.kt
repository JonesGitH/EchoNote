package com.example.keywordrecorder.domain

import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus

class TranscriptionRepository(
    private val recordingRepository: RecordingRepository,
    private val transcriptionEngine: TranscriptionEngine,
) {
    suspend fun transcribeRecording(recording: RecordingEntity): RecordingEntity {
        return runCatching {
            val result = transcriptionEngine.transcribe(recording.filePath)
            recording.copy(
                transcriptText = result.text,
                transcriptionStatus = TranscriptionStatus.COMPLETED,
                transcribedAtEpochMillis = System.currentTimeMillis(),
                lastErrorMessage = null,
            )
        }.getOrElse { error ->
            recording.copy(
                retryCount = recording.retryCount + 1,
                lastErrorMessage = error.message,
                transcriptionStatus = TranscriptionStatus.FAILED,
            )
        }
    }

    suspend fun persist(entity: RecordingEntity) = recordingRepository.update(entity)
}
