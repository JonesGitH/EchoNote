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
        // Minimum silence threshold even in a completely silent room.
        private const val MIN_SILENCE_THRESHOLD = 1500
        // Cap so that a noisy calibration period (user speaks immediately) can't
        // push the threshold so high that silence is never detected.
        private const val MAX_SILENCE_THRESHOLD = 8000
        // Effective threshold = measured noise floor × this multiplier.
        private const val NOISE_FLOOR_MULTIPLIER = 3.0f
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
            var consecutiveErrors = 0

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
                        // Calibration: measure ambient noise over the first 600 ms so the
                        // silence threshold adapts to the user's environment rather than
                        // using a fixed value that background noise can easily exceed.
                        var noiseSum = 0L
                        var noiseCount = 0
                        while (isActive && System.currentTimeMillis() - recordStart < 600L) {
                            delay(200)
                            noiseSum += recorder.getMaxAmplitude()
                            noiseCount++
                        }
                        val noiseFloor = if (noiseCount > 0) noiseSum / noiseCount else 0L
                        val effectiveThreshold = (noiseFloor * NOISE_FLOOR_MULTIPLIER)
                            .toInt()
                            .coerceIn(MIN_SILENCE_THRESHOLD, MAX_SILENCE_THRESHOLD)

                        while (isActive) {
                            delay(200)
                            val amplitude = recorder.getMaxAmplitude()
                            val elapsed = System.currentTimeMillis() - recordStart
                            if (elapsed >= maxMs) break
                            if (amplitude < effectiveThreshold) {
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
                    consecutiveErrors = 0  // reset after a full cycle completes without error
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("KeywordListeningService", "Listen cycle failed: ${e.javaClass.simpleName}: ${e.message}", e)
                    consecutiveErrors++
                    if (consecutiveErrors >= 3) {
                        Log.e("KeywordListeningService", "Too many consecutive errors — stopping listener")
                        ListenerStateBus.emit(ListenerState.ERROR)
                        break
                    }
                    val backoffMs = if (consecutiveErrors == 1) 2_000L else 5_000L
                    ListenerStateBus.emit(ListenerState.ERROR)
                    delay(backoffMs)
                    if (isActive) ListenerStateBus.emit(ListenerState.STARTING)
                } finally {
                    detector.stop()
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        ListenerStateBus.emit(ListenerState.STOPPED)
        super.onDestroy()
    }
}
