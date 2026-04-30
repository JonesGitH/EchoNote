package com.example.keywordrecorder.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.audio.VoskWakeWordDetector
import com.example.keywordrecorder.data.SettingsDataStore
import com.example.keywordrecorder.data.TranscriptionMode
import com.example.keywordrecorder.notification.ListeningNotification
import com.example.keywordrecorder.notification.NOTIFICATION_ID
import com.example.keywordrecorder.worker.TranscriptionScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SILENCE_AMPLITUDE_THRESHOLD = 500
private const val POLL_INTERVAL_MS = 200L

class KeywordListeningService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var detector: VoskWakeWordDetector? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ListeningNotification.createChannel(this)
        startForeground(NOTIFICATION_ID, ListeningNotification.build(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            else -> startListeningLoop()
        }
        return START_STICKY
    }

    private fun startListeningLoop() {
        val app = application as KeywordRecorderApp
        scope.launch {
            val settings = app.settingsDataStore.settingsFlow.first()
            val modelManager = app.modelManager
            val recorder = app.audioRecorder

            val det = VoskWakeWordDetector(modelManager, settings.wakeKeyword)
            detector = det
            det.start()

            ListenerStateBus.emit(ListenerState.LISTENING)

            while (true) {
                det.awaitWakeWord()
                ListenerStateBus.emit(ListenerState.WAKE_WORD_DETECTED)

                val currentSettings = app.settingsDataStore.settingsFlow.first()
                ListenerStateBus.emit(ListenerState.RECORDING)

                val outputDir = "${filesDir.absolutePath}/recordings"
                recorder.startRecording(outputDir)

                val maxMs = currentSettings.maxRecordingSeconds * 1000L
                val silenceMs = currentSettings.silenceTimeoutSeconds * 1000L
                val startTime = System.currentTimeMillis()
                var silentSince: Long? = null

                try {
                    while (true) {
                        kotlinx.coroutines.delay(POLL_INTERVAL_MS)
                        val amplitude = recorder.getMaxAmplitude()
                        val now = System.currentTimeMillis()
                        val elapsed = now - startTime

                        if (amplitude < SILENCE_AMPLITUDE_THRESHOLD) {
                            if (silentSince == null) silentSince = now
                            if (now - silentSince!! >= silenceMs) break
                        } else {
                            silentSince = null
                        }

                        if (elapsed >= maxMs) break
                    }
                } finally {
                    withContext(NonCancellable) {
                        val result = recorder.stopRecording()
                        val id = app.recordingRepository.insertRecording(result)
                        if (currentSettings.transcriptionMode == TranscriptionMode.IMMEDIATE) {
                            TranscriptionScheduler.enqueueImmediate(this@KeywordListeningService, id)
                        }
                    }
                }

                ListenerStateBus.emit(ListenerState.LISTENING)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector?.stop()
        scope.cancel()
        ListenerStateBus.emit(ListenerState.IDLE)
    }

    companion object {
        const val ACTION_START = "com.example.keywordrecorder.ACTION_START"
        const val ACTION_STOP = "com.example.keywordrecorder.ACTION_STOP"
    }
}
