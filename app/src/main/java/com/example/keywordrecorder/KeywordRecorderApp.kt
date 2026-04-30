package com.example.keywordrecorder

import android.app.Application
import android.content.Intent
import androidx.room.Room
import com.example.keywordrecorder.audio.VoskModelManager
import com.example.keywordrecorder.data.AppDatabase
import com.example.keywordrecorder.data.SettingsDataStore
import com.example.keywordrecorder.domain.DailySummaryRepository
import com.example.keywordrecorder.domain.RecordingRepository
import com.example.keywordrecorder.service.KeywordListeningService
import com.example.keywordrecorder.transcription.VoskTranscriptionEngine
import com.example.keywordrecorder.worker.TranscriptionScheduler

class KeywordRecorderApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var recordingRepository: RecordingRepository
        private set
    lateinit var dailySummaryRepository: DailySummaryRepository
        private set
    lateinit var modelManager: VoskModelManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "keyword_recorder.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        settingsDataStore = SettingsDataStore(this)
        recordingRepository = RecordingRepository(database.recordingDao())
        dailySummaryRepository = DailySummaryRepository(database.dailySummaryDao())
        modelManager = VoskModelManager(this)

        TranscriptionScheduler.scheduleDaily(this, settingsDataStore)
        TranscriptionScheduler.scheduleDailySummary(this, settingsDataStore)

        startForegroundService(
            Intent(this, KeywordListeningService::class.java).apply {
                action = KeywordListeningService.ACTION_START
            }
        )
    }

    fun transcriptionEngine() = VoskTranscriptionEngine(modelManager)
}
