package com.example.keywordrecorder.domain

import com.example.keywordrecorder.data.RecordingDao
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import kotlinx.coroutines.flow.Flow

class RecordingRepository(private val dao: RecordingDao) {

            filePath = result.filePath,
            fileName = result.fileName,
            durationMillis = result.durationMillis,
        )
    )

    suspend fun softDelete(id: Long) = dao.softDelete(id)

}
