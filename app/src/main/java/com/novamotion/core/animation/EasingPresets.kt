package com.novamotion.core.animation

import com.novamotion.core.model.BezierControlPoints

enum class EasingType(val title: String, val curve: BezierControlPoints, val isHold: Boolean = false) {
    HOLD("Hold", BezierControlPoints(0f, 0f, 1f, 1f), isHold = true),
    LINEAR("Linear", BezierControlPoints(0.0f, 0.0f, 1.0f, 1.0f)),
    EASE_IN("Ease In", BezierControlPoints(0.42f, 0.0f, 1.0f, 1.0f)),
    EASE_OUT("Ease Out", BezierControlPoints(0.0f, 0.0f, 0.58f, 1.0f)),
    EASE_IN_OUT("Easy Ease", BezierControlPoints(0.42f, 0.0f, 0.58f, 1.0f)),
    EMPHASIZED("Emphasized", BezierControlPoints(0.2f, 0.0f, 0.0f, 1.0f)),
    EMPHASIZED_DECEL("Emph Decel", BezierControlPoints(0.05f, 0.7f, 0.1f, 1.0f)),
    EMPHASIZED_ACCEL("Emph Accel", BezierControlPoints(0.3f, 0.0f, 0.8f, 0.15f)),
    OVERSHOOT("Overshoot", BezierControlPoints(0.34f, 1.56f, 0.64f, 1.0f)),
    ANTICIPATE("Anticipate", BezierControlPoints(0.36f, 0.0f, 0.66f, -0.56f)),
    BOUNCE("Bounce", BezierControlPoints(0.175f, 0.885f, 0.32f, 1.275f)),
    ELASTIC("Smooth Pop", BezierControlPoints(0.25f, 0.1f, 0.25f, 1.0f)),
    SPRING("Spring", BezierControlPoints(0.34f, 1.1f, 0.64f, 1.0f))
}
