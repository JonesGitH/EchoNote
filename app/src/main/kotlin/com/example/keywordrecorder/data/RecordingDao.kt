package com.example.keywordrecorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Query("SELECT * FROM recordings WHERE deleted = 0 ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id AND deleted = 0")
    suspend fun getById(id: Long): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE transcriptionStatus = 'PENDING' AND deleted = 0")
    suspend fun getPendingRecordings(): List<RecordingEntity>

    @Query(
        "SELECT * FROM recordings WHERE transcriptionStatus = 'FAILED' AND retryCount < :maxRetries AND deleted = 0"
    )
    suspend fun getRetryableFailedRecordings(maxRetries: Int): List<RecordingEntity>

    @Query(
        "SELECT * FROM recordings WHERE transcriptionStatus = 'COMPLETED' AND deleted = 0 AND createdAtEpochMillis >= :sinceEpochMillis"
    )
    suspend fun getCompletedSince(sinceEpochMillis: Long): List<RecordingEntity>

    @Query(
        "UPDATE recordings SET transcriptionStatus = :status, transcriptText = :text, transcribedAtEpochMillis = :transcribedAt WHERE id = :id"
    )
    suspend fun updateCompleted(id: Long, status: TranscriptionStatus, text: String, transcribedAt: Long)

    @Query(
        "UPDATE recordings SET transcriptionStatus = :status, lastErrorMessage = :error, retryCount = retryCount + 1 WHERE id = :id"
    )
    suspend fun updateFailed(id: Long, status: TranscriptionStatus, error: String)

    @Query("UPDATE recordings SET transcriptionStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TranscriptionStatus)

    @Query("UPDATE recordings SET deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}
