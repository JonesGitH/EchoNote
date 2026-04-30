package com.example.keywordrecorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Update
    suspend fun update(recording: RecordingEntity)

    @Query("SELECT * FROM recordings WHERE deleted = 0 ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: Long): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE transcriptionStatus = 'PENDING' AND deleted = 0")
    suspend fun getPending(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE transcriptionStatus = 'COMPLETED' AND deleted = 0 AND createdAtEpochMillis >= :sinceEpochMillis")
    suspend fun getCompletedSince(sinceEpochMillis: Long): List<RecordingEntity>

    @Query("UPDATE recordings SET deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM recordings WHERE transcriptionStatus IN ('PENDING','FAILED') AND deleted = 0 AND retryCount < :maxRetry")
    suspend fun getRetryable(maxRetry: Int): List<RecordingEntity>
}
