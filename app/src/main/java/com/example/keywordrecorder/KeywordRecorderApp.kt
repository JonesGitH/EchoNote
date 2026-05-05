package com.example.keywordrecorder

import android.app.Application
import com.example.keywordrecorder.audio.AndroidAudioRecorder
import com.example.keywordrecorder.audio.VoskModelManager
import com.example.keywordrecorder.audio.WhisperModelManager
import com.example.keywordrecorder.data.AppDatabase
import com.example.keywordrecorder.data.SettingsDataStore
import com.example.keywordrecorder.domain.DailySummaryRepository
import com.example.keywordrecorder.domain.RecordingRepository
import com.example.keywordrecorder.domain.TranscriptionRepository
import com.example.keywordrecorder.transcription.LocalTranscriptionEngine
import com.example.keywordrecorder.worker.TranscriptionScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KeywordRecorderApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var db: AppDatabase
    lateinit var settingsDataStore: SettingsDataStore
    lateinit var modelManager: VoskModelManager
    lateinit var whisperModelManager: WhisperModelManager
    lateinit var audioRecorder: AndroidAudioRecorder
    lateinit var recordingRepository: RecordingRepository
    lateinit var dailySummaryRepository: DailySummaryRepository
    lateinit var transcriptionRepository: TranscriptionRepository

    override fun onCreate() {
        super.onCreate()

        db = AppDatabase.create(this)
        settingsDataStore = SettingsDataStore(this)
        modelManager = VoskModelManager(this)
        whisperModelManager = WhisperModelManager(this)
        audioRecorder = AndroidAudioRecorder(this)
        recordingRepository = RecordingRepository(db.recordingDao())
        dailySummaryRepository = DailySummaryRepository(db.dailySummaryDao())
        transcriptionRepository = TranscriptionRepository(recordingRepository, LocalTranscriptionEngine(this, whisperModelManager))

        appScope.launch {
            val settings = settingsDataStore.settings.first()
            TranscriptionScheduler.scheduleBatchTranscription(
                this@KeywordRecorderApp,
                settings.dailyTranscriptionHour,
                settings.dailyTranscriptionMinute
            )
            TranscriptionScheduler.scheduleDailySummary(this@KeywordRecorderApp)
        }
    }
}
