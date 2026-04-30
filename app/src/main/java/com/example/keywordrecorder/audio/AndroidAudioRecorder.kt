package com.example.keywordrecorder.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.example.keywordrecorder.domain.AudioRecorder
import com.example.keywordrecorder.domain.RecordingResult
import com.example.keywordrecorder.domain.RecordingSession
import java.io.File

class AndroidAudioRecorder(private val context: Context) : AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMillis: Long = 0

    override fun startRecording(): RecordingSession {
        val dir = File(context.filesDir, "recordings").also { it.mkdirs() }
        val ts = System.currentTimeMillis()
        val file = File(dir, "recording_$ts.m4a")
        currentFile = file
        startTimeMillis = ts

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mr.setAudioSamplingRate(16000)
        mr.setAudioChannels(1)
        mr.setOutputFile(file.absolutePath)
        mr.prepare()
        mr.start()
        recorder = mr

        return RecordingSession(filePath = file.absolutePath, startTimeMillis = ts)
    }

    override fun stopRecording(): RecordingResult {
        val mr = recorder ?: throw IllegalStateException("Not recording")
        val file = requireNotNull(currentFile)
        val duration = System.currentTimeMillis() - startTimeMillis
        mr.stop()
        mr.release()
        recorder = null
        return RecordingResult(
            filePath = file.absolutePath,
            fileName = file.name,
            durationMillis = duration,
            createdAtEpochMillis = startTimeMillis
        )
    }

    override fun getMaxAmplitude(): Int = recorder?.maxAmplitude ?: 0
}
