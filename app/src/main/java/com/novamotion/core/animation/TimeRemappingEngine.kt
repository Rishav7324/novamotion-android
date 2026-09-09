package com.novamotion.core.animation

import com.novamotion.core.model.BezierControlPoints
import kotlin.math.floor

/**
 * High-Precision Time Remapping & Optical Speed Ramping Engine.
 * Supports non-linear velocity ramps (0.1x to 10x), freeze frames,
 * and calculates frame blending factors for smooth slow-motion.
 */
data class TimeRemapKeyframe(
    val timelineTimeMs: Long,
    val sourceTimeMs: Long,
    val easingCurve: BezierControlPoints = BezierControlPoints(0.33f, 0f, 0.67f, 1f)
)

data class FrameBlendSample(
    val frameIndexA: Long,
    val frameIndexB: Long,
    val blendRatio: Float,
    val instantaneousSpeed: Float
)

class TimeRemappingEngine(
    private val keyframes: List<TimeRemapKeyframe> = emptyList(),
    private val sourceFps: Float = 60f
) {
    private val sortedKeyframes = keyframes.sortedBy { it.timelineTimeMs }

    /**
     * Maps timeline playback position (ms) to exact source media position (ms).
     */
    fun getSourceTimeMs(timelineTimeMs: Long): Long {
        if (sortedKeyframes.isEmpty()) return timelineTimeMs
        if (timelineTimeMs <= sortedKeyframes.first().timelineTimeMs) {
            return sortedKeyframes.first().sourceTimeMs
        }
        if (timelineTimeMs >= sortedKeyframes.last().timelineTimeMs) {
            return sortedKeyframes.last().sourceTimeMs
        }

        // Find surrounding keyframe pair
        for (i in 0 until sortedKeyframes.size - 1) {
            val k0 = sortedKeyframes[i]
            val k1 = sortedKeyframes[i + 1]

            if (timelineTimeMs in k0.timelineTimeMs..k1.timelineTimeMs) {
                val duration = (k1.timelineTimeMs - k0.timelineTimeMs).toFloat()
                if (duration <= 0f) return k0.sourceTimeMs

                val progress = (timelineTimeMs - k0.timelineTimeMs) / duration
                val curvedProgress = BezierCurve.evaluate(progress, k0.easingCurve)
                val sourceDiff = (k1.sourceTimeMs - k0.sourceTimeMs).toFloat()

                return (k0.sourceTimeMs + sourceDiff * curvedProgress).toLong()
            }
        }
        return timelineTimeMs
    }

    /**
     * Calculates frame indices and optical blend weight for slow-motion playback.
     */
    fun calculateFrameBlend(timelineTimeMs: Long): FrameBlendSample {
        val mappedSourceMs = getSourceTimeMs(timelineTimeMs)
        val frameDurationMs = 1000f / sourceFps
        val exactFramePos = mappedSourceMs.toFloat() / frameDurationMs

        val frameA = floor(exactFramePos).toLong()
        val frameB = frameA + 1
        val blendRatio = exactFramePos - frameA

        // Calculate instantaneous speed derivative: deltaSource / deltaTimeline
        val deltaMs = 16L
        val tNext = getSourceTimeMs(timelineTimeMs + deltaMs)
        val tPrev = getSourceTimeMs((timelineTimeMs - deltaMs).coerceAtLeast(0L))
        val speed = (tNext - tPrev).toFloat() / (2 * deltaMs)

        return FrameBlendSample(
            frameIndexA = frameA,
            frameIndexB = frameB,
            blendRatio = blendRatio.coerceIn(0f, 1f),
            instantaneousSpeed = speed
        )
    }
}
