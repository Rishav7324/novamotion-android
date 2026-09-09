package com.novamotion.core.animation

import com.novamotion.core.model.AnimatableProperty
import kotlin.math.sqrt

data class VelocityVector(val vx: Float, val vy: Float) {
    val magnitude: Float get() = sqrt(vx * vx + vy * vy)
}

object VelocityCalculator {

    /**
     * Calculates the instantaneous velocity vector (pixels/second) of position at timeMs.
     */
    fun calculatePositionVelocity(
        posX: AnimatableProperty<Float>,
        posY: AnimatableProperty<Float>,
        timeMs: Long
    ): VelocityVector {
        val dt = 16L // ~1 frame at 60fps
        val x1 = posX.evaluate(timeMs - dt)
        val x2 = posX.evaluate(timeMs + dt)
        val y1 = posY.evaluate(timeMs - dt)
        val y2 = posY.evaluate(timeMs + dt)

        val timeSpanSec = (2 * dt) / 1000f
        val vx = if (timeSpanSec > 0) (x2 - x1) / timeSpanSec else 0f
        val vy = if (timeSpanSec > 0) (y2 - y1) / timeSpanSec else 0f

        return VelocityVector(vx, vy)
    }
}
