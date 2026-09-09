package com.novamotion.core.audio

data class BeatMarker(
    val timeMs: Long,
    val intensity: Float // 0.0 to 1.0 (Kick strength)
)

object BeatDetector {

    /**
     * Detects musical beats and drops from normalized audio amplitude peaks.
     * Uses adaptive energy thresholding with refractory window.
     */
    fun detectBeats(
        peakAmplitudes: FloatArray,
        totalDurationMs: Long,
        sensitivity: Float = 1.35f,
        minIntervalMs: Long = 180L
    ): List<BeatMarker> {
        val markers = mutableListOf<BeatMarker>()
        if (peakAmplitudes.isEmpty()) return markers

        val msPerSample = totalDurationMs.toFloat() / peakAmplitudes.size.toFloat()
        val windowSize = 20 // Local moving average window
        var lastBeatTime = -minIntervalMs

        for (i in peakAmplitudes.indices) {
            val currentTimeMs = (i * msPerSample).toLong()
            val currentEnergy = peakAmplitudes[i] * peakAmplitudes[i]

            // Calculate local average energy around sample i
            val startIdx = (i - windowSize / 2).coerceAtLeast(0)
            val endIdx = (i + windowSize / 2).coerceAtMost(peakAmplitudes.lastIndex)
            var sum = 0f
            for (j in startIdx..endIdx) {
                sum += peakAmplitudes[j] * peakAmplitudes[j]
            }
            val localAvg = sum / (endIdx - startIdx + 1)

            // Check if current energy exceeds adaptive threshold
            val threshold = localAvg * sensitivity
            if (currentEnergy > threshold && currentEnergy > 0.05f) {
                if (currentTimeMs - lastBeatTime >= minIntervalMs) {
                    markers.add(BeatMarker(timeMs = currentTimeMs, intensity = peakAmplitudes[i]))
                    lastBeatTime = currentTimeMs
                }
            }
        }

        return markers
    }
}
