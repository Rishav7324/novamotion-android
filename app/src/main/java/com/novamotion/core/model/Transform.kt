package com.novamotion.core.model

import com.novamotion.core.animation.BezierCurve

/**
 * Animated property with a list of keyframes.
 */
data class AnimatableProperty<T>(
    val defaultValue: T,
    val keyframes: List<Keyframe<T>> = emptyList()
) {
    fun hasKeyframes(): Boolean = keyframes.isNotEmpty()
}

/**
 * Evaluator extensions for interpolating float and pair values across keyframes.
 */
fun AnimatableProperty<Float>.evaluate(timeMs: Long): Float {
    if (keyframes.isEmpty()) return defaultValue
    if (keyframes.size == 1) return keyframes.first().value

    val sorted = keyframes.sortedBy { it.timeMs }
    if (timeMs <= sorted.first().timeMs) return sorted.first().value
    if (timeMs >= sorted.last().timeMs) return sorted.last().value

    // Find the bounding keyframes
    for (i in 0 until sorted.size - 1) {
        val k1 = sorted[i]
        val k2 = sorted[i + 1]
        if (timeMs in k1.timeMs..k2.timeMs) {
            if (k1.interpolation == InterpolationType.HOLD) return k1.value
            val span = (k2.timeMs - k1.timeMs).toFloat()
            val progress = if (span > 0) (timeMs - k1.timeMs) / span else 0f
            val easedProgress = when (k1.interpolation) {
                InterpolationType.LINEAR -> progress
                else -> BezierCurve.evaluate(progress, k1.curve)
            }
            return k1.value + (k2.value - k1.value) * easedProgress
        }
    }
    return defaultValue
}

/**
 * 2D/3D Transform properties for a layer.
 */
data class LayerTransform(
    val posX: AnimatableProperty<Float> = AnimatableProperty(0f),
    val posY: AnimatableProperty<Float> = AnimatableProperty(0f),
    val posZ: AnimatableProperty<Float> = AnimatableProperty(0f),
    val scaleX: AnimatableProperty<Float> = AnimatableProperty(1f),
    val scaleY: AnimatableProperty<Float> = AnimatableProperty(1f),
    val rotation: AnimatableProperty<Float> = AnimatableProperty(0f),
    val opacity: AnimatableProperty<Float> = AnimatableProperty(1f),
    val pivotX: Float = 0.5f,
    val pivotY: Float = 0.5f
)
