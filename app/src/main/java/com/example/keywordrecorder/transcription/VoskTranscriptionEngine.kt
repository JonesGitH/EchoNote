package com.example.keywordrecorder.transcription

import com.example.keywordrecorder.audio.VoskModelManager
import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Recognizer
import java.io.File

class VoskTranscriptionEngine(private val modelManager: VoskModelManager) : TranscriptionEngine {

    override suspend fun transcribe(filePath: String): TranscriptionResult =
        withContext(Dispatchers.IO) {
            val model = modelManager.ensureModel()
            val start = System.currentTimeMillis()
            val pcm = decodeToPcm(filePath)
            val recognizer = Recognizer(model, 16000f)
            val chunks = pcm.chunked(4096)
            var finalText = ""
            for (chunk in chunks) {
                val bytes = chunk.toByteArray()
                if (recognizer.acceptWaveForm(bytes, bytes.size)) {
                    val text = JSONObject(recognizer.result).optString("text", "")
                    if (text.isNotBlank()) finalText += " $text"
                }
            }
            val last = JSONObject(recognizer.finalResult).optString("text", "")
            if (last.isNotBlank()) finalText += " $last"
            recognizer.close()
            TranscriptionResult(
                text = finalText.trim(),
                durationMillis = System.currentTimeMillis() - start
            )
        }

    private fun decodeToPcm(filePath: String): List<Byte> {
        val pcm = mutableListOf<Byte>()
        val extractor = android.media.MediaExtractor()
        extractor.setDataSource(filePath)
        var trackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) { trackIndex = i; break }
        }
        if (trackIndex < 0) return pcm
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(android.media.MediaFormat.KEY_MIME)!!
        val codec = android.media.MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()
        val bufferInfo = android.media.MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val idx = codec.dequeueInputBuffer(10_000)
                if (idx >= 0) {
                    val buf = codec.getInputBuffer(idx)!!
                    val sampleSize = extractor.readSampleData(buf, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(idx, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(idx, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outIdx >= 0) {
                val outBuf = codec.getOutputBuffer(outIdx)!!
                val chunk = ByteArray(bufferInfo.size)
                outBuf.get(chunk)
                pcm.addAll(chunk.toList())
                codec.releaseOutputBuffer(outIdx, false)
                if (bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            } else if (outIdx == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // format changed, continue
            }
        }
        codec.stop()
        codec.release()
        extractor.release()
        return pcm
    }
}
