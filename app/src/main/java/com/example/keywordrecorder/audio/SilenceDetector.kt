package com.example.keywordrecorder.audio

class SilenceDetector(private val thresholdAmplitude: Int = 500, private val silenceDurationMs: Long = 2000) {
    private var silenceStart: Long? = null

    fun isSilent(amplitude: Int, nowMs: Long = System.currentTimeMillis()): Boolean {
        return if (amplitude < thresholdAmplitude) {
            val start = silenceStart ?: run { silenceStart = nowMs; nowMs }
            (nowMs - start) >= silenceDurationMs
        } else {
            silenceStart = null
            false
        }
    }

    fun reset() { silenceStart = null }
}
