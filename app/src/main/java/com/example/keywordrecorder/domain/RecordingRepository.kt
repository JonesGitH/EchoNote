package com.example.keywordrecorder.domain

import com.example.keywordrecorder.data.RecordingDao
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import kotlinx.coroutines.flow.Flow

class RecordingRepository(private val dao: RecordingDao) {
    fun observeAll(): Flow<List<RecordingEntity>> = dao.observeAll()

    suspend fun insertRecording(result: RecordingResult): Long {
        val entity = RecordingEntity(
            filePath = result.filePath,
            fileName = result.fileName,
            createdAtEpochMillis = result.createdAtEpochMillis,
            durationMillis = result.durationMillis,
            transcriptionStatus = TranscriptionStatus.PENDING
        )
        return dao.insert(entity)
    }

    suspend fun getById(id: Long): RecordingEntity? = dao.getById(id)

    suspend fun updateStatus(id: Long, status: TranscriptionStatus, text: String? = null, error: String? = null) {
        val entity = dao.getById(id) ?: return
        dao.update(
            entity.copy(
                transcriptionStatus = status,
                transcriptText = text ?: entity.transcriptText,
                transcribedAtEpochMillis = if (text != null) System.currentTimeMillis() else entity.transcribedAtEpochMillis,
                lastErrorMessage = error,
                retryCount = if (status == TranscriptionStatus.FAILED) entity.retryCount + 1 else entity.retryCount
            )
        )
    }

    suspend fun getPending(): List<RecordingEntity> = dao.getPending()

    suspend fun getCompletedSince(sinceEpochMillis: Long): List<RecordingEntity> =
        dao.getCompletedSince(sinceEpochMillis)

    suspend fun softDelete(id: Long) = dao.softDelete(id)

    suspend fun getRetryable(maxRetry: Int): List<RecordingEntity> = dao.getRetryable(maxRetry)
}
