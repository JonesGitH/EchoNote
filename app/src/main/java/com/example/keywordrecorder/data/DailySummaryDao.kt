package com.example.keywordrecorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {
    @Insert
    suspend fun insert(summary: DailySummaryEntity): Long

    @Query("SELECT * FROM daily_summaries ORDER BY dateEpochMillis DESC")
    fun observeAll(): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE id = :id")
    suspend fun getById(id: Long): DailySummaryEntity?
}
