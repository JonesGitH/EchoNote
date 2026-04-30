package com.example.keywordrecorder.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class TranscriptionMode { OFF, IMMEDIATE, DAILY }

data class AppSettings(
    val wakeKeyword: String = "keyword",
    val keywordSensitivity: String = "MEDIUM",
    val maxRecordingSeconds: Int = 30,
    val silenceTimeoutSeconds: Int = 2,
    val beepOnTrigger: Boolean = false,
    val vibrateOnTrigger: Boolean = false,
    val transcriptionMode: TranscriptionMode = TranscriptionMode.IMMEDIATE,
    val dailyTranscriptionHour: Int = 21,
    val dailyTranscriptionMinute: Int = 0,
    val onlyWifi: Boolean = false,
    val onlyCharging: Boolean = false,
    val retryFailed: Boolean = true,
    val maxRetryCount: Int = 3,
    val deleteAudioAfterTranscription: Boolean = false,
    val autoDeleteDays: Int = 30,
)

class SettingsDataStore(private val context: Context) {
    object Keys {
        val wakeKeyword = stringPreferencesKey("wake_keyword")
        val keywordSensitivity = stringPreferencesKey("keyword_sensitivity")
        val maxRecordingSeconds = intPreferencesKey("max_recording_seconds")
        val silenceTimeoutSeconds = intPreferencesKey("silence_timeout_seconds")
        val beepOnTrigger = booleanPreferencesKey("beep_on_trigger")
        val vibrateOnTrigger = booleanPreferencesKey("vibrate_on_trigger")
        val transcriptionMode = stringPreferencesKey("transcription_mode")
        val dailyTranscriptionHour = intPreferencesKey("daily_transcription_hour")
        val dailyTranscriptionMinute = intPreferencesKey("daily_transcription_minute")
        val onlyWifi = booleanPreferencesKey("only_wifi")
        val onlyCharging = booleanPreferencesKey("only_charging")
        val retryFailed = booleanPreferencesKey("retry_failed")
        val maxRetryCount = intPreferencesKey("max_retry_count")
        val deleteAudioAfterTranscription = booleanPreferencesKey("delete_audio_after_transcription")
        val autoDeleteDays = intPreferencesKey("auto_delete_days")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs.toSettings()
    }

    suspend fun update(block: (MutableMap<Preferences.Key<*>, Any>) -> Unit) {
        context.dataStore.edit { prefs ->
            val mutable: MutableMap<Preferences.Key<*>, Any> = mutableMapOf()
            block(mutable)
            mutable.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                when (value) {
                    is String -> prefs[key as Preferences.Key<String>] = value
                    is Int -> prefs[key as Preferences.Key<Int>] = value
                    is Boolean -> prefs[key as Preferences.Key<Boolean>] = value
                }
            }
        }
    }

    private fun Preferences.toSettings() = AppSettings(
        wakeKeyword = this[Keys.wakeKeyword] ?: "keyword",
        keywordSensitivity = this[Keys.keywordSensitivity] ?: "MEDIUM",
        maxRecordingSeconds = this[Keys.maxRecordingSeconds] ?: 30,
        silenceTimeoutSeconds = this[Keys.silenceTimeoutSeconds] ?: 2,
        beepOnTrigger = this[Keys.beepOnTrigger] ?: false,
        vibrateOnTrigger = this[Keys.vibrateOnTrigger] ?: false,
        transcriptionMode = runCatching {
            TranscriptionMode.valueOf(this[Keys.transcriptionMode] ?: TranscriptionMode.DAILY.name)
        }.getOrDefault(TranscriptionMode.DAILY),
        dailyTranscriptionHour = this[Keys.dailyTranscriptionHour] ?: 21,
        dailyTranscriptionMinute = this[Keys.dailyTranscriptionMinute] ?: 0,
        onlyWifi = this[Keys.onlyWifi] ?: false,
        onlyCharging = this[Keys.onlyCharging] ?: false,
        retryFailed = this[Keys.retryFailed] ?: true,
        maxRetryCount = this[Keys.maxRetryCount] ?: 3,
        deleteAudioAfterTranscription = this[Keys.deleteAudioAfterTranscription] ?: false,
        autoDeleteDays = this[Keys.autoDeleteDays] ?: 30,
    )
}
