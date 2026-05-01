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
import java.io.ByteArrayOutputStream

class VoskTranscriptionEngine(private val modelManager: VoskModelManager) : TranscriptionEngine {

    override suspend fun transcribe(filePath: String): TranscriptionResult =
        withContext(Dispatchers.IO) {
            val model = modelManager.ensureModel()
            val start = System.currentTimeMillis()
            val pcm = decodeToPcm(filePath)
            val recognizer = Recognizer(model, 16000f)
            try {
                var offset = 0
                var finalText = ""
                while (offset < pcm.size) {
                    val end = (offset + 4096).coerceAtMost(pcm.size)
                    val chunk = pcm.copyOfRange(offset, end)
                    if (recognizer.acceptWaveForm(chunk, chunk.size)) {
                        val text = JSONObject(recognizer.result).optString("text", "")
                        if (text.isNotBlank()) finalText += " $text"
                    }
                    offset = end
                }
                val last = JSONObject(recognizer.finalResult).optString("text", "")
                if (last.isNotBlank()) finalText += " $last"
                TranscriptionResult(
                    text = finalText.trim(),
                    durationMillis = System.currentTimeMillis() - start
                )
            } finally {
                recognizer.close()
            }
        }

    private fun decodeToPcm(filePath: String): ByteArray {
        val pcm = ByteArrayOutputStream()
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)
        var trackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) { trackIndex = i; break }
        }
        if (trackIndex < 0) {
            extractor.release()
            return pcm.toByteArray()
        }
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        try {
            codec.configure(format, null, null, 0)
            codec.start()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val idx = codec.dequeueInputBuffer(10_000)
                    if (idx >= 0) {
                        val buf = codec.getInputBuffer(idx)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
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
                    pcm.write(chunk)
                    codec.releaseOutputBuffer(outIdx, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }
        return pcm.toByteArray()
    }
}
