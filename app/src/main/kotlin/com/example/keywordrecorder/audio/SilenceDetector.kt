package com.example.keywordrecorder.audio

// Unused — KeywordListeningService polls getMaxAmplitude() directly.
class SilenceDetector(private val threshold: Int = 500) {
    private var silentSince: Long? = null

    fun isSilent(amplitude: Int, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (amplitude >= threshold) {
            silentSince = null
            return false
        }
        if (silentSince == null) silentSince = nowMs
        return true
    }

    fun silentDurationMs(nowMs: Long = System.currentTimeMillis()): Long =
        silentSince?.let { nowMs - it } ?: 0L

    fun reset() {
        silentSince = null
    }
}
