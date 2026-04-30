package com.example.keywordrecorder.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.keywordrecorder.domain.WakeWordDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Recognizer
import org.json.JSONObject

private const val SAMPLE_RATE = 16000
private const val BUFFER_SIZE = 4096

class VoskWakeWordDetector(
    private val modelManager: VoskModelManager,
    private val keyword: String,
) : WakeWordDetector {
    private var audioRecord: AudioRecord? = null
    private var recognizer: Recognizer? = null

    override suspend fun start() {
        val model = modelManager.ensureModel()
        recognizer = Recognizer(model, SAMPLE_RATE.toFloat(), """["$keyword","[unk]"]""")
    }

    override suspend fun awaitWakeWord() = withContext(Dispatchers.IO) {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, BUFFER_SIZE),
        )
        audioRecord = rec
        rec.startRecording()

        val buffer = ByteArray(BUFFER_SIZE)
        try {
            while (true) {
                val bytesRead = rec.read(buffer, 0, buffer.size)
                if (bytesRead <= 0) continue

                val reco = recognizer ?: break
                if (reco.acceptWaveForm(buffer, bytesRead)) {
                    val text = JSONObject(reco.finalResult).optString("text", "")
                    if (text.contains(keyword, ignoreCase = true)) break
                } else {
                    val partial = JSONObject(reco.partialResult).optString("partial", "")
                    if (partial.contains(keyword, ignoreCase = true)) break
                }
            }
        } finally {
            rec.stop()
            rec.release()
            audioRecord = null
        }
    }

    override fun stop() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recognizer?.close()
        recognizer = null
    }
}
