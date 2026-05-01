package com.example.keywordrecorder.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.keywordrecorder.KeywordRecorderApp
import com.example.keywordrecorder.audio.VoskWakeWordDetector
import com.example.keywordrecorder.data.TranscriptionMode
import com.example.keywordrecorder.notification.ListeningNotification
import com.example.keywordrecorder.worker.TranscriptionScheduler
import kotlinx.coroutines.flow.first

class KeywordListeningService : Service() {
    private var listeningJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            else -> startListening()
        }
        return START_STICKY
    }

    private fun startListening() {
        listeningJob = scope.launch {
            val app = application as KeywordRecorderApp
            while (isActive) {
                detector.awaitWakeWord()
                if (!isActive) break

                recorder.startRecording()

                try {
                        delay(200)
                        } else {
                        }
                    }
                } finally {
                    withContext(NonCancellable) {
                        val result = recorder.stopRecording()
                        val id = app.recordingRepository.insertRecording(result)
                    }
                }
            }
        }
    }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
