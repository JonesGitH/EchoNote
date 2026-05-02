package com.example.keywordrecorder.domain

import com.example.keywordrecorder.data.RecordingDao
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.util.FileUtils
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

    fun observeById(id: Long): Flow<RecordingEntity?> = dao.observeById(id)

    suspend fun updateStatus(id: Long, status: TranscriptionStatus, text: String? = null, error: String? = null) {
        dao.updateStatusAtomic(id, status.name, text, error, System.currentTimeMillis())
    }

    suspend fun getPending(): List<RecordingEntity> = dao.getPending()

    suspend fun deleteAll() {
        dao.getAllFilePaths().forEach { FileUtils.deleteIfExists(it) }
        dao.deleteAll()
    }

    suspend fun getCompletedSince(sinceEpochMillis: Long): List<RecordingEntity> =
        dao.getCompletedSince(sinceEpochMillis)

    suspend fun softDelete(id: Long) = dao.softDelete(id)

    suspend fun restoreRecording(id: Long) = dao.restoreRecording(id)

    suspend fun getRetryable(maxRetry: Int): List<RecordingEntity> = dao.getRetryable(maxRetry)
}
