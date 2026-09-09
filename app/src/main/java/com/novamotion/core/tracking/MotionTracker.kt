package com.novamotion.core.tracking

import com.novamotion.core.model.AnimatableProperty
import com.novamotion.core.model.Keyframe
import com.novamotion.core.model.Layer
import kotlin.math.abs
import kotlin.math.sqrt

data class TrackPoint(
    val timeMs: Long,
    val x: Float,
    val y: Float,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val confidence: Float = 1.0f
)

object MotionTracker {

    /**
     * Estimates displacement vector between reference frame template and search window.
     * Uses Normalized Cross-Correlation (NCC) / Sum of Absolute Differences (SAD).
     */
    fun trackPatch(
        templateLuma: ByteArray,
        templateWidth: Int,
        templateHeight: Int,
        searchWindowLuma: ByteArray,
        searchWidth: Int,
        searchHeight: Int,
        startX: Int,
        startY: Int
    ): Pair<Float, Float> {
        var bestSAD = Long.MAX_VALUE
        var bestDx = 0
        var bestDy = 0

        val maxSearchRange = 16 // Search +/- 16 pixels per frame

        for (dy in -maxSearchRange..maxSearchRange) {
            for (dx in -maxSearchRange..maxSearchRange) {
                var sad = 0L

                for (ty in 0 until templateHeight) {
                    for (tx in 0 until templateWidth) {
                        val templatePixel = templateLuma[ty * templateWidth + tx].toInt() and 0xFF
                        val sx = (startX + tx + dx).coerceIn(0, searchWidth - 1)
                        val sy = (startY + ty + dy).coerceIn(0, searchHeight - 1)
                        val searchPixel = searchWindowLuma[sy * searchWidth + sx].toInt() and 0xFF

                        sad += abs(templatePixel - searchPixel)
                    }
                }

                if (sad < bestSAD) {
                    bestSAD = sad
                    bestDx = dx
                    bestDy = dy
                }
            }
        }

        return Pair(bestDx.toFloat(), bestDy.toFloat())
    }

    /**
     * Binds a sequence of tracked points into actual keyframes on a target Layer's transform.
     */
    fun applyTrackingToLayer(layer: Layer, trackPoints: List<TrackPoint>): Layer {
        val posXKeyframes = trackPoints.map { pt ->
            Keyframe(timeMs = pt.timeMs, value = pt.x)
        }
        val posYKeyframes = trackPoints.map { pt ->
            Keyframe(timeMs = pt.timeMs, value = pt.y)
        }
        val scaleKeyframes = trackPoints.map { pt ->
            Keyframe(timeMs = pt.timeMs, value = pt.scale)
        }
        val rotKeyframes = trackPoints.map { pt ->
            Keyframe(timeMs = pt.timeMs, value = pt.rotation)
        }

        val updatedTransform = layer.transform.copy(
            posX = AnimatableProperty(defaultValue = layer.transform.posX.defaultValue, keyframes = posXKeyframes),
            posY = AnimatableProperty(defaultValue = layer.transform.posY.defaultValue, keyframes = posYKeyframes),
            scaleX = AnimatableProperty(defaultValue = 1f, keyframes = scaleKeyframes),
            scaleY = AnimatableProperty(defaultValue = 1f, keyframes = scaleKeyframes),
            rotation = AnimatableProperty(defaultValue = 0f, keyframes = rotKeyframes)
        )

        return layer.copy(transform = updatedTransform)
    }
}
