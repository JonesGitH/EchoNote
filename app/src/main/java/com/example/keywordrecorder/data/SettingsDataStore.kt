package com.example.keywordrecorder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

enum class TranscriptionMode { OFF, IMMEDIATE, DAILY }

data class AppSettings(
    val wakeKeyword: String = "keyword",
    val transcriptionMode: TranscriptionMode = TranscriptionMode.IMMEDIATE,
    val maxRecordingSeconds: Int = 30,
    val silenceTimeoutSeconds: Int = 2,
    val dailyTranscriptionHour: Int = 21,
    val dailyTranscriptionMinute: Int = 0,
    val retryFailed: Boolean = true,
    val maxRetryCount: Int = 3,
    val deleteAudioAfterTranscription: Boolean = false,
    val onlyWifi: Boolean = false,
    val onlyCharging: Boolean = false
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val WAKE_KEYWORD = stringPreferencesKey("wake_keyword")
        val TRANSCRIPTION_MODE = stringPreferencesKey("transcription_mode")
        val MAX_RECORDING_SECONDS = intPreferencesKey("max_recording_seconds")
        val SILENCE_TIMEOUT_SECONDS = intPreferencesKey("silence_timeout_seconds")
        val DAILY_TRANSCRIPTION_HOUR = intPreferencesKey("daily_transcription_hour")
        val DAILY_TRANSCRIPTION_MINUTE = intPreferencesKey("daily_transcription_minute")
        val RETRY_FAILED = booleanPreferencesKey("retry_failed")
        val MAX_RETRY_COUNT = intPreferencesKey("max_retry_count")
        val DELETE_AUDIO = booleanPreferencesKey("delete_audio_after_transcription")
        val ONLY_WIFI = booleanPreferencesKey("only_wifi")
        val ONLY_CHARGING = booleanPreferencesKey("only_charging")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            wakeKeyword = prefs[Keys.WAKE_KEYWORD] ?: "keyword",
            transcriptionMode = prefs[Keys.TRANSCRIPTION_MODE]?.let { name ->
                TranscriptionMode.entries.find { it.name == name }
            } ?: TranscriptionMode.IMMEDIATE,
            maxRecordingSeconds = prefs[Keys.MAX_RECORDING_SECONDS] ?: 30,
            silenceTimeoutSeconds = prefs[Keys.SILENCE_TIMEOUT_SECONDS] ?: 2,
            dailyTranscriptionHour = prefs[Keys.DAILY_TRANSCRIPTION_HOUR] ?: 21,
            dailyTranscriptionMinute = prefs[Keys.DAILY_TRANSCRIPTION_MINUTE] ?: 0,
            retryFailed = prefs[Keys.RETRY_FAILED] ?: true,
            maxRetryCount = prefs[Keys.MAX_RETRY_COUNT] ?: 3,
            deleteAudioAfterTranscription = prefs[Keys.DELETE_AUDIO] ?: false,
            onlyWifi = prefs[Keys.ONLY_WIFI] ?: false,
            onlyCharging = prefs[Keys.ONLY_CHARGING] ?: false
        )
    }

    suspend fun updateWakeKeyword(keyword: String) {
        context.dataStore.edit { it[Keys.WAKE_KEYWORD] = keyword }
    }

    suspend fun updateTranscriptionMode(mode: TranscriptionMode) {
        context.dataStore.edit { it[Keys.TRANSCRIPTION_MODE] = mode.name }
    }

    suspend fun updateMaxRecordingSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.MAX_RECORDING_SECONDS] = seconds }
    }

    suspend fun updateDailyTranscriptionTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.DAILY_TRANSCRIPTION_HOUR] = hour
            it[Keys.DAILY_TRANSCRIPTION_MINUTE] = minute
        }
    }
}
