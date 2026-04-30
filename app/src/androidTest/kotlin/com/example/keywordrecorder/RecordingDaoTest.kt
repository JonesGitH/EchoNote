package com.example.keywordrecorder

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.keywordrecorder.data.AppDatabase
import com.example.keywordrecorder.data.RecordingDao
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: RecordingDao

    private fun makeRecording(
        filePath: String = "/fake/rec.m4a",
        status: TranscriptionStatus = TranscriptionStatus.PENDING,
        retryCount: Int = 0,
        createdAt: Long = System.currentTimeMillis(),
    ) = RecordingEntity(
        filePath = filePath,
        fileName = filePath.substringAfterLast('/'),
        createdAtEpochMillis = createdAt,
        durationMillis = 5_000L,
        transcriptionStatus = status,
        retryCount = retryCount,
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = db.recordingDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndObserve() = runTest {
        val id = dao.insert(makeRecording())
        val all = dao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(id, all[0].id)
    }

    @Test
    fun getPendingExcludesNonPending() = runTest {
        dao.insert(makeRecording(status = TranscriptionStatus.PENDING))
        dao.insert(makeRecording(status = TranscriptionStatus.COMPLETED))
        dao.insert(makeRecording(status = TranscriptionStatus.FAILED))

        val pending = dao.getPendingRecordings()
        assertEquals(1, pending.size)
        assertEquals(TranscriptionStatus.PENDING, pending[0].transcriptionStatus)
    }

    @Test
    fun getCompletedSinceFiltersCorrectly() = runTest {
        val cutoff = 1_000L
        dao.insert(makeRecording(status = TranscriptionStatus.COMPLETED, createdAt = 500L))
        dao.insert(makeRecording(status = TranscriptionStatus.COMPLETED, createdAt = 1_500L))
        dao.insert(makeRecording(status = TranscriptionStatus.PENDING, createdAt = 2_000L))

        val results = dao.getCompletedSince(cutoff)
        assertEquals(1, results.size)
        assertEquals(1_500L, results[0].createdAtEpochMillis)
    }

    @Test
    fun getCompletedSinceExcludesSoftDeleted() = runTest {
        val cutoff = 0L
        val id = dao.insert(makeRecording(status = TranscriptionStatus.COMPLETED))
        dao.softDelete(id)

        val results = dao.getCompletedSince(cutoff)
        assertTrue(results.isEmpty())
    }

    @Test
    fun softDeleteSetsDeletedFlag() = runTest {
        val id = dao.insert(makeRecording())
        dao.softDelete(id)

        // Row still exists but observeAll (which filters deleted=0) won't show it
        val visible = dao.observeAll().first()
        assertTrue(visible.isEmpty())
    }

    @Test
    fun getByIdReturnsNullAfterSoftDelete() = runTest {
        val id = dao.insert(makeRecording())
        dao.softDelete(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun updateCompletedSetsStatusAndText() = runTest {
        val id = dao.insert(makeRecording())
        dao.updateCompleted(id, TranscriptionStatus.COMPLETED, "hello", System.currentTimeMillis())

        val rec = dao.getById(id)!!
        assertEquals(TranscriptionStatus.COMPLETED, rec.transcriptionStatus)
        assertEquals("hello", rec.transcriptText)
    }

    @Test
    fun updateFailedIncrementsRetryCount() = runTest {
        val id = dao.insert(makeRecording(retryCount = 1))
        dao.updateFailed(id, TranscriptionStatus.FAILED, "oops")

        val rec = dao.getById(id)!!
        assertEquals(TranscriptionStatus.FAILED, rec.transcriptionStatus)
        assertEquals(2, rec.retryCount)
        assertEquals("oops", rec.lastErrorMessage)
    }

    @Test
    fun getRetryableFailedRecordingsRespectsMaxRetries() = runTest {
        dao.insert(makeRecording(status = TranscriptionStatus.FAILED, retryCount = 1))
        dao.insert(makeRecording(status = TranscriptionStatus.FAILED, retryCount = 3))

        val retryable = dao.getRetryableFailedRecordings(maxRetries = 3)
        assertEquals(1, retryable.size)
        assertEquals(1, retryable[0].retryCount)
    }
}
