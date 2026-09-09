package com.novamotion.core.animation

import com.novamotion.core.model.Keyframe
import kotlin.math.abs

data class SpeedSample(
    val timeMs: Long,
    val speedPixelsPerSec: Float
)

object SpeedGraphCalculator {

    /**
     * Calculates the continuous speed graph samples (pixels/sec) between keyframe k1 and k2.
     */
    fun computeSpeedGraph(
        k1: Keyframe<Float>,
        k2: Keyframe<Float>,
        sampleCount: Int = 50
    ): List<SpeedSample> {
        val samples = mutableListOf<SpeedSample>()
        val timeSpanMs = k2.timeMs - k1.timeMs
        if (timeSpanMs <= 0) return samples

        val deltaValue = k2.value - k1.value
        val timeSpanSec = timeSpanMs / 1000f
        val avgSpeed = deltaValue / timeSpanSec

        for (i in 0..sampleCount) {
            val progress = i.toFloat() / sampleCount.toFloat()
            val timeMs = k1.timeMs + (progress * timeSpanMs).toLong()

            // Instantaneous velocity factor from Bézier derivative
            val derivative = BezierCurve.evaluateDerivative(progress, k1.curve)
            val instantaneousSpeed = abs(avgSpeed * derivative)

            samples.add(SpeedSample(timeMs, instantaneousSpeed))
        }

        return samples
    }
}
