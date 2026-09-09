package com.novamotion.core.animation

import com.novamotion.core.model.BezierControlPoints

enum class EasingType(val title: String, val curve: BezierControlPoints) {
    LINEAR("Linear", BezierControlPoints(0.0f, 0.0f, 1.0f, 1.0f)),
    EASE_IN("Ease In", BezierControlPoints(0.42f, 0.0f, 1.0f, 1.0f)),
    EASE_OUT("Ease Out", BezierControlPoints(0.0f, 0.0f, 0.58f, 1.0f)),
    EASE_IN_OUT("Easy Ease", BezierControlPoints(0.42f, 0.0f, 0.58f, 1.0f)),
    OVERSHOOT("Overshoot", BezierControlPoints(0.34f, 1.56f, 0.64f, 1.0f)),
    ANTICIPATE("Anticipate", BezierControlPoints(0.36f, 0.0f, 0.66f, -0.56f)),
    BOUNCE("Bounce", BezierControlPoints(0.175f, 0.885f, 0.32f, 1.275f)),
    ELASTIC("Smooth Pop", BezierControlPoints(0.25f, 0.1f, 0.25f, 1.0f))
}
