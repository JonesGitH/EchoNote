package com.example.keywordrecorder.domain

import com.example.keywordrecorder.data.DailySummaryDao
import com.example.keywordrecorder.data.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

class DailySummaryRepository(private val dao: DailySummaryDao) {
    fun observeAll(): Flow<List<DailySummaryEntity>> = dao.observeAll()

    suspend fun insert(summary: DailySummaryEntity): Long = dao.insert(summary)
}
