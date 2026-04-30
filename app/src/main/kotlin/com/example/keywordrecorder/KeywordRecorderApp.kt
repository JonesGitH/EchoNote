package com.example.keywordrecorder

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import com.example.keywordrecorder.audio.AndroidAudioRecorder
import com.example.keywordrecorder.audio.VoskModelManager
import com.example.keywordrecorder.data.AppDatabase
import com.example.keywordrecorder.data.SettingsDataStore
import com.example.keywordrecorder.domain.DailySummaryRepository
import com.example.keywordrecorder.domain.RecordingRepository
import com.example.keywordrecorder.domain.TranscriptionRepository
import com.example.keywordrecorder.service.KeywordListeningService
import com.example.keywordrecorder.transcription.VoskTranscriptionEngine
import com.example.keywordrecorder.worker.TranscriptionScheduler
import androidx.core.content.ContextCompat

class KeywordRecorderApp : Application(), Configuration.Provider {
    lateinit var database: AppDatabase
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var modelManager: VoskModelManager
        private set

    lateinit var audioRecorder: AndroidAudioRecorder
        private set

    lateinit var recordingRepository: RecordingRepository
        private set

    lateinit var dailySummaryRepository: DailySummaryRepository
        private set

    lateinit var transcriptionRepository: TranscriptionRepository
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(this, AppDatabase::class.java, "keyword_recorder.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        settingsDataStore = SettingsDataStore(this)
        modelManager = VoskModelManager(this)
        audioRecorder = AndroidAudioRecorder(this)

        recordingRepository = RecordingRepository(database.recordingDao())
        dailySummaryRepository = DailySummaryRepository(database.dailySummaryDao())
        transcriptionRepository = TranscriptionRepository(
            engine = VoskTranscriptionEngine(modelManager),
            recordingRepository = recordingRepository,
        )

        val serviceIntent = android.content.Intent(this, KeywordListeningService::class.java).apply {
            action = KeywordListeningService.ACTION_START
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        TranscriptionScheduler.scheduleDailySummary(this)
    }
}
