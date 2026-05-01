package com.example.keywordrecorder.data

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun fromTranscriptionStatus(status: TranscriptionStatus): String = status.name

    @TypeConverter
    fun toTranscriptionStatus(value: String): TranscriptionStatus =
        TranscriptionStatus.entries.find { it.name == value } ?: TranscriptionStatus.PENDING
}
