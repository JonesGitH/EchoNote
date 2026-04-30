package com.example.keywordrecorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE deleted = 0 ORDER BY createdAtEpochMillis DESC")
    fun observeRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun observeRecording(id: Long): Flow<RecordingEntity?>

    @Query("SELECT * FROM recordings WHERE transcriptionStatus IN ('PENDING', 'FAILED') AND deleted = 0")
    suspend fun getPendingForTranscription(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE transcriptionStatus = 'COMPLETED' AND deleted = 0 AND createdAtEpochMillis >= :sinceEpochMillis ORDER BY createdAtEpochMillis ASC")
    suspend fun getCompletedSince(sinceEpochMillis: Long): List<RecordingEntity>

    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Update
    suspend fun update(recording: RecordingEntity)

    @Query("UPDATE recordings SET deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}
