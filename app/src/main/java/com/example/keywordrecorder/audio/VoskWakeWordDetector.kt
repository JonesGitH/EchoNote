package com.example.keywordrecorder.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder as AndroidMediaRecorder
import com.example.keywordrecorder.domain.WakeWordDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Recognizer

class VoskWakeWordDetector(
    private val modelManager: VoskModelManager,
    private val keyword: String,
) : WakeWordDetector {

    override suspend fun start() {
        modelManager.ensureModel()
    }

    override suspend fun stop() {}

    override suspend fun awaitWakeWord() = withContext(Dispatchers.IO) {
        val model = checkNotNull(modelManager.model) { "Vosk model not loaded" }
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val audioRecord = AudioRecord(
            AndroidMediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 4,
        )

        // Grammar-constrained recognizer makes detection faster and more accurate.
        val grammar = """["${keyword.lowercase()}", "[unk]"]"""
        val recognizer = Recognizer(model, sampleRate.toFloat(), grammar)
        val buffer = ByteArray(minBuf)

        try {
            audioRecord.startRecording()
            while (isActive) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    if (recognizer.acceptWaveForm(buffer, read)) {
                        val text = JSONObject(recognizer.result).optString("text")
                        if (text.contains(keyword.lowercase())) break
                    } else {
                        val partial = JSONObject(recognizer.partialResult).optString("partial")
                        if (partial.contains(keyword.lowercase())) break
                    }
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
            recognizer.close()
        }
    }
}
