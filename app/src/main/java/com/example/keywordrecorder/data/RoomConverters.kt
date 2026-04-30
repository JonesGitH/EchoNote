package com.example.keywordrecorder.data

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun transcriptionStatusToString(status: TranscriptionStatus): String = status.name

    @TypeConverter
    fun stringToTranscriptionStatus(raw: String): TranscriptionStatus =
        TranscriptionStatus.valueOf(raw)
}
