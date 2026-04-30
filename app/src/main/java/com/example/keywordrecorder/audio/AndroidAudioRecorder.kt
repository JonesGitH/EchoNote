package com.example.keywordrecorder.audio

import android.content.Context
import android.media.MediaRecorder
import com.example.keywordrecorder.domain.AudioRecorder
import com.example.keywordrecorder.domain.RecordingResult
import com.example.keywordrecorder.domain.RecordingSession
import java.io.File

class AndroidAudioRecorder(private val context: Context) : AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt: Long = 0

    override suspend fun startRecording(): RecordingSession {
        releaseRecorder()
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "recording_${System.currentTimeMillis()}.m4a")

        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16000)
            setAudioChannels(1)
            setOutputFile(file.absolutePath)
            setOnErrorListener { _, _, _ -> releaseRecorder() }
            prepare()
            start()
        }
        outputFile = file
        startedAt = System.currentTimeMillis()
        return RecordingSession(startedAt)
    }

    override suspend fun stopRecording(): RecordingResult {
        val file = requireNotNull(outputFile)
        val duration = System.currentTimeMillis() - startedAt
        releaseRecorder()
        return RecordingResult(file.absolutePath, file.name, duration)
    }

    fun getMaxAmplitude(): Int = recorder?.maxAmplitude ?: 0

    fun releaseRecorder() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
    }
}
