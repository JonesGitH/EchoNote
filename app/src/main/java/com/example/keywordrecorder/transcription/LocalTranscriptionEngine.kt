package com.example.keywordrecorder.transcription

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.redravencomputing.whispercore.Whisper
import com.redravencomputing.whispercore.WhisperDelegate
import com.redravencomputing.whispercore.WhisperOperationError
import com.example.keywordrecorder.audio.WhisperModelManager
import com.example.keywordrecorder.domain.TranscriptionEngine
import com.example.keywordrecorder.domain.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocalTranscriptionEngine(
    private val context: Context,
    private val modelManager: WhisperModelManager
) : TranscriptionEngine {

    private val mutex = Mutex()
    private var whisper: Whisper? = null

    override suspend fun transcribe(filePath: String): TranscriptionResult =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            val pcmBytes = decodeToPcm(filePath)
            val wavFile = writeTempWav(pcmBytes)
            try {
                // Serial access: prevents two jobs sharing the same Whisper instance concurrently.
                val text = mutex.withLock {
                    val instance = whisper ?: run {
                        val modelPath = modelManager.ensureModel()
                        Whisper(context).also {
                            it.initializeModel(modelPath)
                            whisper = it
                        }
                    }
                    suspendCancellableCoroutine { cont ->
                        instance.delegate = object : WhisperDelegate {
                            override fun didTranscribe(text: String) {
                                if (cont.isActive) cont.resume(text)
                            }
                            override fun failedToTranscribe(error: WhisperOperationError) {
                                if (cont.isActive) cont.resumeWithException(Exception(error.toString()))
                            }
                            override fun recordingFailed(error: WhisperOperationError) {}
                            override fun permissionRequestNeeded() {}
                            override fun didStartRecording() {}
                            override fun didStopRecording() {}
                        }
                        instance.transcribeAudioFile(wavFile)
                    }
                }
                TranscriptionResult(text.trim(), System.currentTimeMillis() - start)
            } finally {
                wavFile.delete()
            }
        }

    // Writes raw int16 PCM bytes to a temp WAV file at 16 kHz mono.
    private fun writeTempWav(pcmBytes: ByteArray): File {
        val sampleRate = 16000
        val channels: Short = 1
        val bitsPerSample: Short = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val dataSize = pcmBytes.size

        val file = File.createTempFile("whisper_", ".wav", context.cacheDir)
        file.outputStream().use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray())
                putInt(36 + dataSize)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1)             // PCM
                putShort(channels)
                putInt(sampleRate)
                putInt(byteRate)
                putShort(blockAlign)
                putShort(bitsPerSample)
                put("data".toByteArray())
                putInt(dataSize)
            }
            out.write(header.array())
            out.write(pcmBytes)
        }
        return file
    }

    // Decodes an M4A/AAC file to raw int16 little-endian PCM via MediaCodec.
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
                        val inputBuf = codec.getInputBuffer(idx)!!
                        val sampleSize = extractor.readSampleData(inputBuf, 0)
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
