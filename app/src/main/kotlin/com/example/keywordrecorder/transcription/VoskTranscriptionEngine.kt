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
import java.nio.ByteBuffer

private const val SAMPLE_RATE = 16000

class VoskTranscriptionEngine(private val modelManager: VoskModelManager) : TranscriptionEngine {
    override suspend fun transcribe(filePath: String): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            val model = modelManager.ensureModel()
            val pcm = decodeToPcm(filePath)

            Recognizer(model, SAMPLE_RATE.toFloat()).use { recognizer ->
                recognizer.acceptWaveForm(pcm, pcm.size)
                val text = JSONObject(recognizer.finalResult).optString("text", "").trim()
                val result = text.ifBlank { "[No speech detected]" }
                TranscriptionResult.Success(result)
            }
        } catch (e: Exception) {
            TranscriptionResult.Failure(e.message ?: "Unknown error")
        }
    }

    private fun decodeToPcm(filePath: String): ByteArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)

        var trackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                break
            }
        }
        require(trackIndex >= 0) { "No audio track found in $filePath" }

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val output = mutableListOf<Byte>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer: ByteBuffer = codec.getInputBuffer(inputIndex)!!
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

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                val outputBuffer: ByteBuffer = codec.getOutputBuffer(outputIndex)!!
                val chunk = ByteArray(bufferInfo.size)
                outputBuffer.get(chunk)
                output.addAll(chunk.toList())
                codec.releaseOutputBuffer(outputIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    outputDone = true
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        return output.toByteArray()
    }
}
