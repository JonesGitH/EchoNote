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
        // We use a limited grammar to improve accuracy, but keep [unk] 
        // to handle non-keyword speech without forcing it to the keyword.
        val grammar = """["${keyword.lowercase()}", "[unk]"]"""
        recognizer = Recognizer(model, sampleRate.toFloat(), grammar).apply {
            setWords(true) // Required to get confidence scores in the JSON
        }
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
                    // Full result (silence detected). Check for high-confidence match.
                    val json = JSONObject(rec.result)
                    if (hasHighConfidenceMatch(json)) return@withContext
                } else {
                    // Partial result (ongoing speech).
                    // Trigger ONLY if the keyword is the ONLY thing heard in the partial.
                    // This prevents "cat" matching "keyword" if the keyword was "key".
                    val partialJson = JSONObject(rec.partialResult)
                    val partialText = partialJson.optString("partial", "").trim()
                    if (partialText == keyword.lowercase()) {
                        return@withContext
                    }
                }
            }
        } finally {
            ar.stop()
            ar.release()
            audioRecord = null
        }
    }

    private fun hasHighConfidenceMatch(json: JSONObject): Boolean {
        val text = json.optString("text", "")
        if (!text.contains(keyword.lowercase())) return false
        
        val words = json.optJSONArray("result") ?: return true // If no word details, fallback to text match
        for (i in 0 until words.length()) {
            val wordObj = words.getJSONObject(i)
            val word = wordObj.optString("word", "")
            val conf = wordObj.optDouble("conf", 0.0)
            
            if (word == keyword.lowercase() && conf >= 0.8) {
                return true
            }
        }
        return false
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
