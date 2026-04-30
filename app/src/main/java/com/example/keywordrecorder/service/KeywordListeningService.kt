package com.example.keywordrecorder.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.audio.AndroidAudioRecorder
import com.example.keywordrecorder.audio.VoskWakeWordDetector
import com.example.keywordrecorder.data.TranscriptionMode
import com.example.keywordrecorder.notification.ListeningNotification
import com.example.keywordrecorder.worker.TranscriptionScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ListenerState {
    IDLE,
    LOADING_MODEL,
    LISTENING,
    WAKE_WORD_DETECTED,
    RECORDING,
    STOPPING,
    ERROR,
}

class KeywordListeningService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val stateBus = ListenerStateBus
    private val recorder by lazy { AndroidAudioRecorder(this) }
    private var listeningJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopListening()
            else -> startListening()
        }
        return START_STICKY
    }

    private fun startListening() {
        ListeningNotification.ensureChannel(this)
        startForeground(
            ListeningNotification.NOTIFICATION_ID,
            ListeningNotification.build(this),
        )

        listeningJob?.cancel()
        listeningJob = scope.launch {
            val app = application as KeywordRecorderApp
            val settings = app.settingsDataStore.settingsFlow.first()
            val detector = VoskWakeWordDetector(app.modelManager, settings.wakeKeyword)

            stateBus.update(ListenerState.LOADING_MODEL)
            runCatching { detector.start() }.onFailure {
                stateBus.update(ListenerState.ERROR)
                return@launch
            }

            stateBus.update(ListenerState.LISTENING)
            while (isActive) {
                detector.awaitWakeWord()
                if (!isActive) break

                stateBus.update(ListenerState.WAKE_WORD_DETECTED)
                val currentSettings = app.settingsDataStore.settingsFlow.first()
                stateBus.update(ListenerState.RECORDING)
                recorder.startRecording()

                try {
                    val maxMs = currentSettings.maxRecordingSeconds * 1_000L
                    val silenceMs = currentSettings.silenceTimeoutSeconds * 1_000L
                    val deadline = System.currentTimeMillis() + maxMs
                    var silenceSince = -1L

                    while (isActive && System.currentTimeMillis() < deadline) {
                        delay(200)
                        val amp = recorder.getMaxAmplitude()
                        if (amp < SILENCE_AMPLITUDE_THRESHOLD) {
                            if (silenceSince < 0) silenceSince = System.currentTimeMillis()
                            else if (System.currentTimeMillis() - silenceSince >= silenceMs) break
                        } else {
                            silenceSince = -1L
                        }
                    }
                } finally {
                    withContext(NonCancellable) {
                        val result = recorder.stopRecording()
                        val id = app.recordingRepository.insertRecording(result)
                        if (currentSettings.transcriptionMode == TranscriptionMode.IMMEDIATE) {
                            TranscriptionScheduler.enqueueImmediateTranscription(this@KeywordListeningService, id)
                        }
                    }
                }
                if (isActive) stateBus.update(ListenerState.LISTENING)
            }
        }
    }

    private fun stopListening() {
        scope.launch {
            stateBus.update(ListenerState.STOPPING)
            listeningJob?.cancel()
            stateBus.update(ListenerState.IDLE)
            ServiceCompat.stopForeground(this@KeywordListeningService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        listeningJob?.cancel()
        recorder.releaseRecorder()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.keywordrecorder.action.START"
        const val ACTION_STOP = "com.example.keywordrecorder.action.STOP"
        private const val SILENCE_AMPLITUDE_THRESHOLD = 500
    }
}
