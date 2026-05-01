package com.example.keywordrecorder

import android.app.Application
import com.example.keywordrecorder.audio.VoskModelManager
import com.example.keywordrecorder.data.AppDatabase
import com.example.keywordrecorder.data.SettingsDataStore
import com.example.keywordrecorder.domain.DailySummaryRepository
import com.example.keywordrecorder.domain.RecordingRepository
import com.example.keywordrecorder.service.KeywordListeningService
import com.example.keywordrecorder.transcription.VoskTranscriptionEngine
import com.example.keywordrecorder.worker.TranscriptionScheduler

class KeywordRecorderApp : Application() {
    lateinit var settingsDataStore: SettingsDataStore
    lateinit var recordingRepository: RecordingRepository
    lateinit var dailySummaryRepository: DailySummaryRepository

    override fun onCreate() {
        super.onCreate()
        settingsDataStore = SettingsDataStore(this)
        modelManager = VoskModelManager(this)


                action = KeywordListeningService.ACTION_START
            }
    }
}
