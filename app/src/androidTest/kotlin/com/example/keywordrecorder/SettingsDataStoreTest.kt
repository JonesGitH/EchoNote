package com.example.keywordrecorder

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.example.keywordrecorder.data.AppSettings
import com.example.keywordrecorder.data.SettingsDataStore
import com.example.keywordrecorder.data.TranscriptionMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SettingsDataStoreTest {
    private lateinit var context: Context
    private lateinit var store: SettingsDataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Delete any existing datastore file so each test starts fresh
        File(context.filesDir, "datastore/app_settings.preferences_pb").delete()
        store = SettingsDataStore(context)
    }

    @Test
    fun defaultsMatchSpecification() = runTest {
        val settings = store.settingsFlow.first()
        assertEquals("keyword", settings.wakeKeyword)
        assertEquals(TranscriptionMode.IMMEDIATE, settings.transcriptionMode)
        assertEquals(30, settings.maxRecordingSeconds)
        assertEquals(2, settings.silenceTimeoutSeconds)
        assertEquals(21, settings.dailyTranscriptionHour)
        assertEquals(0, settings.dailyTranscriptionMinute)
        assertTrue(settings.retryFailed)
        assertEquals(3, settings.maxRetryCount)
        assertFalse(settings.deleteAudioAfterTranscription)
        assertFalse(settings.onlyWifi)
        assertFalse(settings.onlyCharging)
    }

    @Test
    fun updateWakeKeywordRoundTrips() = runTest {
        store.updateWakeKeyword("echo")
        assertEquals("echo", store.settingsFlow.first().wakeKeyword)
    }

    @Test
    fun updateTranscriptionModeRoundTrips() = runTest {
        store.updateTranscriptionMode(TranscriptionMode.DAILY)
        assertEquals(TranscriptionMode.DAILY, store.settingsFlow.first().transcriptionMode)

        store.updateTranscriptionMode(TranscriptionMode.OFF)
        assertEquals(TranscriptionMode.OFF, store.settingsFlow.first().transcriptionMode)
    }

    @Test
    fun updateDailyTranscriptionTimeRoundTrips() = runTest {
        store.updateDailyTranscriptionTime(22, 30)
        val settings = store.settingsFlow.first()
        assertEquals(22, settings.dailyTranscriptionHour)
        assertEquals(30, settings.dailyTranscriptionMinute)
    }
}
