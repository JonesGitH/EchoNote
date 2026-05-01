package com.example.keywordrecorder.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.audio.VoskWakeWordDetector
import com.example.keywordrecorder.data.TranscriptionMode
import com.example.keywordrecorder.notification.ListeningNotification
import com.example.keywordrecorder.worker.TranscriptionScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class KeywordListeningService : Service() {

    companion object {
        const val ACTION_START = "com.example.keywordrecorder.START"
        const val ACTION_STOP = "com.example.keywordrecorder.STOP"
        private const val SILENCE_AMPLITUDE_THRESHOLD = 500
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listeningJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(
            ListeningNotification.NOTIFICATION_ID,
            ListeningNotification.build(this)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> { stopSelf(); START_NOT_STICKY }
            else -> { startListening(); START_STICKY }
        }
    }

    private fun startListening() {
        if (listeningJob?.isActive == true) return
        listeningJob = scope.launch {
            ListenerStateBus.emit(ListenerState.STARTING)
            val app = application as KeywordRecorderApp
            try {
                while (isActive) {
                    val settings = app.settingsDataStore.settings.first()
                    val detector = VoskWakeWordDetector(app.modelManager, settings.wakeKeyword)
                    try {
                        detector.start()
                        ListenerStateBus.emit(ListenerState.LISTENING)

                        detector.awaitWakeWord()
                        if (!isActive) break

                        ListenerStateBus.emit(ListenerState.WAKE_WORD_DETECTED)
                        val freshSettings = app.settingsDataStore.settings.first()
                        ListenerStateBus.emit(ListenerState.RECORDING)

                        val recorder = app.audioRecorder
                        recorder.startRecording()

                        val maxMs = freshSettings.maxRecordingSeconds * 1000L
                        val silenceMs = freshSettings.silenceTimeoutSeconds * 1000L
                        val recordStart = System.currentTimeMillis()
                        var silenceStart: Long? = null

                        try {
                            while (isActive) {
                                delay(200)
                                val amplitude = recorder.getMaxAmplitude()
                                val elapsed = System.currentTimeMillis() - recordStart
                                if (elapsed >= maxMs) break
                                if (amplitude < SILENCE_AMPLITUDE_THRESHOLD) {
                                    val ss = silenceStart ?: run { silenceStart = System.currentTimeMillis(); System.currentTimeMillis() }
                                    if ((System.currentTimeMillis() - ss) >= silenceMs) break
                                } else {
                                    silenceStart = null
                                }
                            }
                        } finally {
                            withContext(NonCancellable) {
                                val result = recorder.stopRecording()
                                val id = app.recordingRepository.insertRecording(result)
                                if (freshSettings.transcriptionMode == TranscriptionMode.IMMEDIATE) {
                                    TranscriptionScheduler.enqueueImmediate(applicationContext, id)
                                }
                            }
                        }
                    } finally {
                        detector.stop()
                    }
                    ListenerStateBus.emit(ListenerState.LISTENING)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("KeywordListeningService", "Listener failed: ${e.javaClass.simpleName}: ${e.message}", e)
                ListenerStateBus.emit(ListenerState.ERROR)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        ListenerStateBus.emit(ListenerState.STOPPED)
        super.onDestroy()
    }
}
