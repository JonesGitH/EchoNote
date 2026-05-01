package com.example.keywordrecorder.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


enum class TranscriptionMode { OFF, IMMEDIATE, DAILY }

data class AppSettings(
    val wakeKeyword: String = "keyword",
    val maxRecordingSeconds: Int = 30,
    val silenceTimeoutSeconds: Int = 2,
    val dailyTranscriptionHour: Int = 21,
    val dailyTranscriptionMinute: Int = 0,
    val retryFailed: Boolean = true,
    val maxRetryCount: Int = 3,
    val deleteAudioAfterTranscription: Boolean = false,
)

class SettingsDataStore(private val context: Context) {

    }

                }
            }
        }
    }

}
