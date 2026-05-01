package com.example.keywordrecorder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochMillis: Long,
    val summaryText: String,
    val recordingCount: Int,
    val createdAtEpochMillis: Long
)
