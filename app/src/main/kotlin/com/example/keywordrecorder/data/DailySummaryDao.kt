package com.example.keywordrecorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: DailySummaryEntity): Long

    @Query("SELECT * FROM daily_summaries ORDER BY dateEpochMillis DESC")
    fun observeAll(): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries ORDER BY dateEpochMillis DESC")
    suspend fun getAll(): List<DailySummaryEntity>
}
