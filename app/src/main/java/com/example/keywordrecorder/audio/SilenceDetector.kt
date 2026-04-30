package com.example.keywordrecorder.audio

class SilenceDetector(
    private val minimumSpeechThreshold: Double = 1500.0,
) {
    fun isSilent(frame: ShortArray): Boolean {
        if (frame.isEmpty()) return true
        val rms = kotlin.math.sqrt(frame.map { it.toDouble() * it }.average())
        return rms < minimumSpeechThreshold
    }
}
