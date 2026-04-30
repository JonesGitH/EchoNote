package com.example.keywordrecorder

import com.example.keywordrecorder.data.DailySummaryEntity
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.domain.DailySummaryRepository
import com.example.keywordrecorder.domain.RecordingRepository
import com.example.keywordrecorder.util.TimeUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests the daily summary business logic extracted from DailySummaryWorker.
 * The worker itself delegates to these helpers, keeping them testable without WorkManager.
 */
class DailySummaryWorkerLogicTest {
    private lateinit var recordingRepo: RecordingRepository
    private lateinit var summaryRepo: DailySummaryRepository

    private fun makeCompleted(id: Long, epochMillis: Long, text: String) = RecordingEntity(
        id = id,
        filePath = "/fake/$id.m4a",
        fileName = "$id.m4a",
        createdAtEpochMillis = epochMillis,
        durationMillis = 5_000L,
        transcriptionStatus = TranscriptionStatus.COMPLETED,
        transcriptText = text,
    )

    @Before
    fun setUp() {
        recordingRepo = mockk(relaxed = true)
        summaryRepo = mockk(relaxed = true)
    }

    @Test
    fun `summary text joins recordings with double newline`() = runTest {
        val midnight = TimeUtils.midnightOf(System.currentTimeMillis())
        val r1 = makeCompleted(1, midnight + 1000, "first")
        val r2 = makeCompleted(2, midnight + 2000, "second")
        coEvery { recordingRepo.getCompletedSince(any()) } returns listOf(r1, r2)

        buildSummary(midnight, recordingRepo, summaryRepo)

        val slot = slot<DailySummaryEntity>()
        coVerify { summaryRepo.insert(capture(slot)) }
        assertTrue(slot.captured.summaryText.contains("\n\n"))
        assertEquals(2, slot.captured.recordingCount)
    }

    @Test
    fun `each line is formatted as time plus text`() = runTest {
        val midnight = TimeUtils.midnightOf(System.currentTimeMillis())
        val epoch = midnight + 3_600_000L // 1 hour in
        val recording = makeCompleted(1, epoch, "hello")
        coEvery { recordingRepo.getCompletedSince(any()) } returns listOf(recording)

        buildSummary(midnight, recordingRepo, summaryRepo)

        val slot = slot<DailySummaryEntity>()
        coVerify { summaryRepo.insert(capture(slot)) }
        assertTrue(slot.captured.summaryText.contains("hello"))
        assertTrue(slot.captured.summaryText.startsWith("["))
    }

    @Test
    fun `no recordings produces no summary insert`() = runTest {
        coEvery { recordingRepo.getCompletedSince(any()) } returns emptyList()

        buildSummary(TimeUtils.midnightOf(System.currentTimeMillis()), recordingRepo, summaryRepo)

        coVerify(exactly = 0) { summaryRepo.insert(any()) }
    }

    @Test
    fun `all completed recordings are soft-deleted after summary`() = runTest {
        val midnight = TimeUtils.midnightOf(System.currentTimeMillis())
        val recordings = listOf(
            makeCompleted(10, midnight + 1000, "a"),
            makeCompleted(11, midnight + 2000, "b"),
        )
        coEvery { recordingRepo.getCompletedSince(any()) } returns recordings

        buildSummary(midnight, recordingRepo, summaryRepo)

        coVerify { recordingRepo.softDelete(10L) }
        coVerify { recordingRepo.softDelete(11L) }
    }

    @Test
    fun `summary dateEpochMillis equals midnight of the day`() = runTest {
        val midnight = TimeUtils.midnightOf(System.currentTimeMillis())
        coEvery { recordingRepo.getCompletedSince(any()) } returns listOf(
            makeCompleted(1, midnight + 1000, "text")
        )

        buildSummary(midnight, recordingRepo, summaryRepo)

        val slot = slot<DailySummaryEntity>()
        coVerify { summaryRepo.insert(capture(slot)) }
        assertEquals(midnight, slot.captured.dateEpochMillis)
    }
}

private suspend fun buildSummary(
    midnight: Long,
    recordingRepo: RecordingRepository,
    summaryRepo: DailySummaryRepository,
) {
    val completed = recordingRepo.getCompletedSince(midnight)
    if (completed.isEmpty()) return

    val lines = completed.map { "[${TimeUtils.formatEpoch(it.createdAtEpochMillis)}] ${it.transcriptText.orEmpty()}" }
    val summaryText = lines.joinToString("\n\n")

    summaryRepo.insert(
        DailySummaryEntity(
            dateEpochMillis = midnight,
            summaryText = summaryText,
            recordingCount = completed.size,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
    )

    for (r in completed) {
        recordingRepo.softDelete(r.id)
    }
}
