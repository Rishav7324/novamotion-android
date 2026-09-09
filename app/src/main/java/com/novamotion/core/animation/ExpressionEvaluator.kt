package com.novamotion.core.animation

import kotlin.math.sin
import kotlin.random.Random

/**
 * High-performance Expression Evaluator for procedural animation.
 * Evaluates math-driven motion such as camera shake (wiggle), periodic oscillation (sin),
 * and audio-reactive frequency binding.
 */
object ExpressionEvaluator {

    /**
     * Wiggle function (After Effects style):
     * Generates smooth pseudo-random continuous displacement around base value.
     */
    fun wiggle(
        timeMs: Long,
        frequency: Float = 3f, // Oscillations per second
        amplitude: Float = 20f, // Maximum displacement
        seed: Int = 42
    ): Float {
        val t = (timeMs / 1000f) * frequency
        val baseIndex = t.toInt()
        val fract = t - baseIndex

        val rand1 = pseudoRandom(baseIndex, seed)
        val rand2 = pseudoRandom(baseIndex + 1, seed)

        // Smooth cosine interpolation between random points
        val smoothProgress = (1f - kotlin.math.cos(fract * Math.PI.toFloat())) * 0.5f
        val interpolated = rand1 + (rand2 - rand1) * smoothProgress

        return (interpolated - 0.5f) * 2f * amplitude
    }

    /**
     * Harmonic sine oscillation:
     * value = center + sin(time * speed) * amplitude
     */
    fun oscillate(
        timeMs: Long,
        speed: Float = 2f,
        amplitude: Float = 50f
    ): Float {
        val t = (timeMs / 1000f) * speed * 2f * Math.PI.toFloat()
        return (sin(t) * amplitude).toFloat()
    }

    /**
     * Audio-reactive pulse:
     * Scales a property based on audio peak at timeMs.
     */
    fun audioPulse(
        timeMs: Long,
        audioPeaks: FloatArray,
        totalDurationMs: Long,
        multiplier: Float = 1.5f
    ): Float {
        if (audioPeaks.isEmpty()) return 1.0f
        val progress = (timeMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        val idx = (progress * (audioPeaks.size - 1)).toInt()
        val peak = audioPeaks[idx]
        return 1.0f + (peak * multiplier)
    }

    private fun pseudoRandom(index: Int, seed: Int): Float {
        val random = Random(index * 31L + seed * 17L)
        return random.nextFloat()
    }
}
