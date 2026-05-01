package com.example.keywordrecorder.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.keywordrecorder.domain.WakeWordDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.json.JSONObject

class VoskWakeWordDetector(
    private val modelManager: VoskModelManager,
    private val keyword: String
) : WakeWordDetector {

    private val sampleRate = 16000
    private val bufferSize = 4096
    private var audioRecord: AudioRecord? = null
    private var recognizer: Recognizer? = null

    override suspend fun start() {
        val model: Model = modelManager.ensureModel()
        val grammar = """["${keyword.lowercase()}", "[unk]"]"""
        recognizer = Recognizer(model, sampleRate.toFloat(), grammar)
    }

    override suspend fun awaitWakeWord(): Unit = withContext(Dispatchers.IO) {
        val rec = requireNotNull(recognizer)
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val ar = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, bufferSize * 2)
        )
        audioRecord = ar
        ar.startRecording()
        val buf = ByteArray(bufferSize)
        try {
            while (isActive) {
                val read = ar.read(buf, 0, bufferSize)
                if (read <= 0) continue
                if (rec.acceptWaveForm(buf, read)) {
                    val result = JSONObject(rec.result).optString("text", "")
                    if (result.contains(keyword.lowercase())) return@withContext
                } else {
                    val partial = JSONObject(rec.partialResult).optString("partial", "")
                    if (partial.contains(keyword.lowercase())) return@withContext
                }
            }
        } finally {
            ar.stop()
            ar.release()
            audioRecord = null
        }
    }

    override fun stop() {
        audioRecord?.let { ar ->
            if (ar.state == AudioRecord.STATE_INITIALIZED) ar.stop()
            ar.release()
        }
        audioRecord = null
        recognizer?.close()
        recognizer = null
    }
}
