package com.example.keywordrecorder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    val dateEpochMillis: Long,
    val summaryText: String,
    val recordingCount: Int,
)
