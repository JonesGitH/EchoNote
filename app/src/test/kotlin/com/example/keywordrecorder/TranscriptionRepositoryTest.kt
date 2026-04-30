package com.example.keywordrecorder

import com.example.keywordrecorder.data.AppSettings
import com.example.keywordrecorder.data.RecordingEntity
import com.example.keywordrecorder.data.TranscriptionStatus
import com.example.keywordrecorder.domain.RecordingRepository
import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionRepository
import com.example.keywordrecorder.domain.TranscriptionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TranscriptionRepositoryTest {
    private lateinit var engine: TranscriptionEngine
    private lateinit var recordingRepo: RecordingRepository
    private lateinit var repo: TranscriptionRepository

    private val defaultSettings = AppSettings()

    private fun makeRecording(id: Long, retryCount: Int = 0) = RecordingEntity(
        id = id,
        filePath = "/fake/path.m4a",
        fileName = "path.m4a",
        createdAtEpochMillis = 1_000L,
        durationMillis = 5_000L,
        retryCount = retryCount,
    )

    @Before
    fun setUp() {
        engine = mockk()
        recordingRepo = mockk(relaxed = true)
        repo = TranscriptionRepository(engine, recordingRepo)
    }

    @Test
    fun `success marks recording as completed with text`() = runTest {
        val recording = makeRecording(id = 1)
        coEvery { recordingRepo.getById(1L) } returns recording
        coEvery { engine.transcribe(any()) } returns TranscriptionResult.Success("hello world")

        repo.transcribeRecording(1L, defaultSettings)

        coVerify { recordingRepo.markProcessing(1L) }
        coVerify { recordingRepo.markCompleted(1L, "hello world") }
    }

    @Test
    fun `failure marks recording as failed and stores error`() = runTest {
        val recording = makeRecording(id = 2, retryCount = 0)
        val updated = recording.copy(retryCount = 1)
        coEvery { recordingRepo.getById(2L) } returnsMany listOf(recording, updated)
        coEvery { engine.transcribe(any()) } returns TranscriptionResult.Failure("network error")

        repo.transcribeRecording(2L, defaultSettings)

        coVerify { recordingRepo.markProcessing(2L) }
        coVerify { recordingRepo.markFailed(2L, "network error") }
    }

    @Test
    fun `recording at max retries is skipped without calling engine`() = runTest {
        val recording = makeRecording(id = 3, retryCount = 3)
        coEvery { recordingRepo.getById(3L) } returns recording

        repo.transcribeRecording(3L, defaultSettings.copy(maxRetryCount = 3))

        coVerify { recordingRepo.markSkipped(3L) }
        coVerify(exactly = 0) { engine.transcribe(any()) }
    }

    @Test
    fun `returns immediately when recording not found`() = runTest {
        coEvery { recordingRepo.getById(99L) } returns null

        repo.transcribeRecording(99L, defaultSettings)

        coVerify(exactly = 0) { recordingRepo.markProcessing(any()) }
        coVerify(exactly = 0) { engine.transcribe(any()) }
    }

    @Test
    fun `failure that hits max retries after increment marks skipped`() = runTest {
        val recording = makeRecording(id = 5, retryCount = 2)
        val afterFail = recording.copy(retryCount = 3)
        coEvery { recordingRepo.getById(5L) } returnsMany listOf(recording, afterFail)
        coEvery { engine.transcribe(any()) } returns TranscriptionResult.Failure("error")

        repo.transcribeRecording(5L, defaultSettings.copy(maxRetryCount = 3))

        coVerify { recordingRepo.markSkipped(5L) }
    }
}
