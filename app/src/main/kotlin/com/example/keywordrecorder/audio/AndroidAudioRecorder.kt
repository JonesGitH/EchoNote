package com.example.keywordrecorder.audio

import android.content.Context
import android.media.MediaRecorder
import com.example.keywordrecorder.domain.AudioRecorder
import com.example.keywordrecorder.domain.RecordingResult
import java.io.File

class AndroidAudioRecorder(private val context: Context) : AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var startTime: Long = 0L
    private var outputFile: File? = null

    override fun startRecording(outputDir: String): String {
        val dir = File(outputDir).also { it.mkdirs() }
        val file = File(dir, "recording_${System.currentTimeMillis()}.m4a")
        outputFile = file
        startTime = System.currentTimeMillis()

        val rec = MediaRecorder(context)
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioSamplingRate(16000)
        rec.setAudioChannels(1)
        rec.setOutputFile(file.absolutePath)
        rec.prepare()
        rec.start()
        recorder = rec

        return file.absolutePath
    }

    override fun stopRecording(): RecordingResult {
        val rec = recorder ?: error("stopRecording called without startRecording")
        val file = outputFile ?: error("outputFile is null")
        val duration = System.currentTimeMillis() - startTime

        try {
            rec.stop()
        } finally {
            rec.release()
            recorder = null
        }

        return RecordingResult(
            filePath = file.absolutePath,
            fileName = file.name,
            durationMillis = duration,
            createdAtEpochMillis = startTime,
        )
    }

    override fun getMaxAmplitude(): Int = recorder?.maxAmplitude ?: 0
}
