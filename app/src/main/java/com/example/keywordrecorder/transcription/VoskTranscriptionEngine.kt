package com.example.keywordrecorder.transcription

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.example.keywordrecorder.audio.VoskModelManager
import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Recognizer

class VoskTranscriptionEngine(private val modelManager: VoskModelManager) : TranscriptionEngine {

    override suspend fun transcribe(filePath: String): TranscriptionResult = withContext(Dispatchers.IO) {
        val model = checkNotNull(modelManager.model) { "Vosk model not loaded" }

        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            if (trackFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i
                format = trackFormat
                break
            }
        }
        checkNotNull(format) { "No audio track in $filePath" }
        extractor.selectTrack(trackIndex)

        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val recognizer = Recognizer(model, sampleRate.toFloat())
        val info = MediaCodec.BufferInfo()
        var inputDone = false

        try {
            while (true) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(info, 10_000)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                    val chunk = ByteArray(info.size)
                    outputBuffer.get(chunk)
                    codec.releaseOutputBuffer(outputIndex, false)
                    recognizer.acceptWaveForm(chunk, chunk.size)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        val text = JSONObject(recognizer.finalResult).optString("text", "").trim()
        recognizer.close()

        TranscriptionResult(text = text.ifBlank { "[No speech detected]" })
    }
}
