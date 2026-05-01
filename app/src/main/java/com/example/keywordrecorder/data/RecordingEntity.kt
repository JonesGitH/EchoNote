package com.example.keywordrecorder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    val filePath: String,
    val fileName: String,
    val createdAtEpochMillis: Long,
    val durationMillis: Long,
    val transcriptText: String? = null,
    val transcribedAtEpochMillis: Long? = null,
    val retryCount: Int = 0,
    val lastErrorMessage: String? = null,
)
