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
        )
        return dao.insert(entity)
    }

    suspend fun getById(id: Long): RecordingEntity? = dao.getById(id)

    suspend fun getPendingRecordings(): List<RecordingEntity> = dao.getPendingRecordings()

    suspend fun getRetryableFailedRecordings(maxRetries: Int): List<RecordingEntity> =
        dao.getRetryableFailedRecordings(maxRetries)

    suspend fun getCompletedSince(sinceEpochMillis: Long): List<RecordingEntity> =
        dao.getCompletedSince(sinceEpochMillis)

    suspend fun markCompleted(id: Long, text: String) {
        dao.updateCompleted(
            id = id,
            status = TranscriptionStatus.COMPLETED,
            text = text,
            transcribedAt = System.currentTimeMillis(),
        )
    }

    suspend fun markFailed(id: Long, error: String) {
        dao.updateFailed(id = id, status = TranscriptionStatus.FAILED, error = error)
    }

    suspend fun markSkipped(id: Long) {
        dao.updateStatus(id = id, status = TranscriptionStatus.SKIPPED)
    }

    suspend fun markProcessing(id: Long) {
        dao.updateStatus(id = id, status = TranscriptionStatus.PROCESSING)
    }

    suspend fun softDelete(id: Long) {
        dao.softDelete(id)
    }
}
