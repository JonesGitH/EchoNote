package com.example.keywordrecorder.domain

import com.example.keywordrecorder.data.RecordingDao
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import kotlinx.coroutines.flow.Flow

class RecordingRepository(private val dao: RecordingDao) {
    fun observeRecordings(): Flow<List<RecordingEntity>> = dao.observeRecordings()

    fun observeRecording(id: Long): Flow<RecordingEntity?> = dao.observeRecording(id)

    suspend fun insertRecording(result: RecordingResult): Long = dao.insert(
        RecordingEntity(
            filePath = result.filePath,
            fileName = result.fileName,
            createdAtEpochMillis = System.currentTimeMillis(),
            durationMillis = result.durationMillis,
            transcriptionStatus = TranscriptionStatus.PENDING,
        )
    )

    suspend fun softDelete(id: Long) = dao.softDelete(id)

    suspend fun getPendingForTranscription(): List<RecordingEntity> = dao.getPendingForTranscription()

    suspend fun getCompletedSince(epochMillis: Long): List<RecordingEntity> = dao.getCompletedSince(epochMillis)

    suspend fun update(recordingEntity: RecordingEntity) = dao.update(recordingEntity)
}
