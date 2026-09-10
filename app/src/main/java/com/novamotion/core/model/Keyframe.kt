package com.novamotion.core.model

/**
 * Represents a Cubic Bézier control handle for keyframe easing.
 * (x1, y1) and (x2, y2) are normalized in [0, 1].
 */
data class BezierControlPoints(
    val x1: Float = 0.42f,
    val y1: Float = 0.0f,
    val x2: Float = 0.58f,
    val y2: Float = 1.0f
)

enum class InterpolationType { HOLD, LINEAR, BEZIER, AUTO_BEZIER }
enum class HandleType { FREE, ALIGNED, VECTOR }

 /**
 * Keyframe for animatable numeric properties (position, scale, rotation, opacity, etc.).
 */
data class Keyframe<T>(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timeMs: Long,
    val value: T,
    val curve: BezierControlPoints = BezierControlPoints(),
    val interpolation: InterpolationType = InterpolationType.BEZIER,
    val handleType: HandleType = HandleType.FREE
)
