package com.novamotion.core.animation

import kotlin.math.exp
import kotlin.math.sin

data class ShakeOutput(
    val offsetX: Float,
    val offsetY: Float,
    val rotationAngle: Float,
    val scaleFactor: Float
)

object ShakeGenerator {

    /**
     * Generates complex multi-frequency camera shake with exponential decay.
     */
    fun computeShake(
        timeMs: Long,
        impactTimeMs: Long,
        durationMs: Long = 600L,
        intensity: Float = 35f,
        frequency: Float = 14f // Fast vibration
    ): ShakeOutput {
        val elapsed = timeMs - impactTimeMs
        if (elapsed < 0 || elapsed > durationMs) {
            return ShakeOutput(0f, 0f, 0f, 1f)
        }

        val progress = elapsed.toFloat() / durationMs.toFloat()
        // Exponential decay envelope (starts strong, fades out smoothly)
        val envelope = exp(-progress * 4.5f)

        val t = elapsed / 1000f
        val phaseX = t * frequency * 2f * Math.PI.toFloat()
        val phaseY = t * (frequency * 1.37f) * 2f * Math.PI.toFloat()
        val phaseRot = t * (frequency * 0.73f) * 2f * Math.PI.toFloat()

        val ox = (sin(phaseX) * intensity * envelope).toFloat()
        val oy = (sin(phaseY) * (intensity * 0.8f) * envelope).toFloat()
        val rot = (sin(phaseRot) * (intensity * 0.12f) * envelope).toFloat()
        val zoom = 1.0f + (sin(phaseX * 0.5f) * 0.05f * envelope).toFloat()

        return ShakeOutput(
            offsetX = ox,
            offsetY = oy,
            rotationAngle = rot,
            scaleFactor = zoom
        )
    }
}
