package com.example.keywordrecorder

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.keywordrecorder.data.AppDatabase
import com.example.keywordrecorder.data.DailySummaryDao
import com.example.keywordrecorder.data.DailySummaryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailySummaryDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: DailySummaryDao

    private fun makeSummary(dateMillis: Long, text: String = "summary") = DailySummaryEntity(
        dateEpochMillis = dateMillis,
        summaryText = text,
        recordingCount = 1,
        createdAtEpochMillis = System.currentTimeMillis(),
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = db.dailySummaryDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndObserveAll() = runTest {
        dao.insert(makeSummary(1_000L))
        dao.insert(makeSummary(2_000L))

        val all = dao.observeAll().first()
        assertEquals(2, all.size)
    }

    @Test
    fun observeAllReturnsMostRecentFirst() = runTest {
        dao.insert(makeSummary(1_000L, "older"))
        dao.insert(makeSummary(3_000L, "newer"))
        dao.insert(makeSummary(2_000L, "middle"))

        val all = dao.observeAll().first()
        assertEquals(3_000L, all[0].dateEpochMillis)
        assertEquals(2_000L, all[1].dateEpochMillis)
        assertEquals(1_000L, all[2].dateEpochMillis)
    }

    @Test
    fun replacingExistingSummaryOnSameDateWorks() = runTest {
        dao.insert(makeSummary(1_000L, "first"))
        dao.insert(makeSummary(1_000L, "updated"))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals("updated", all[0].summaryText)
    }
}
