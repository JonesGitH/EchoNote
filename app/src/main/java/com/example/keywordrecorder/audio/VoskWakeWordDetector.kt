package com.example.keywordrecorder.audio

import android.media.AudioFormat
import android.media.AudioRecord
import com.example.keywordrecorder.domain.WakeWordDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.vosk.Recognizer

class VoskWakeWordDetector(
    private val modelManager: VoskModelManager,
) : WakeWordDetector {

    override suspend fun start() {
    }

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
        )
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        try {
            while (isActive) {
                    } else {
                }
            }
        } finally {
        }
    }
}
