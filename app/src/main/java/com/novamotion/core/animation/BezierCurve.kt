package com.novamotion.core.animation

import com.novamotion.core.model.BezierControlPoints
import kotlin.math.abs

/**
 * Analytical Cubic Bézier Solver using the Newton-Raphson method.
 * Accurately calculates eased property values and derivatives (velocity).
 */
object BezierCurve {

    private const val NEWTON_ITERATIONS = 8
    private const val NEWTON_MIN_SLOPE = 0.001f
    private const val SUBDIVISION_PRECISION = 0.0000001f
    private const val SUBDIVISION_MAX_ITERATIONS = 10

    /**
     * Solves the Cubic Bézier curve y given x in range [0, 1].
     */
    fun evaluate(x: Float, curve: BezierControlPoints): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f

        val t = solveCurveX(x, curve.x1, curve.x2)
        return sampleCurveY(t, curve.y1, curve.y2)
    }

    /**
     * Calculates the instantaneous derivative (velocity) at normalized time x.
     */
    fun evaluateDerivative(x: Float, curve: BezierControlPoints): Float {
        val clampedX = x.coerceIn(0f, 1f)
        val t = solveCurveX(clampedX, curve.x1, curve.x2)
        val dx = sampleCurveDerivativeX(t, curve.x1, curve.x2)
        val dy = sampleCurveDerivativeY(t, curve.y1, curve.y2)
        return if (abs(dx) > 1e-6f) dy / dx else 0f
    }

    private fun sampleCurveX(t: Float, x1: Float, x2: Float): Float {
        // (1-t)^3*0 + 3*(1-t)^2*t*x1 + 3*(1-t)*t^2*x2 + t^3*1
        val c = 3f * x1
        val b = 3f * (x2 - x1) - c
        val a = 1f - c - b
        return ((a * t + b) * t + c) * t
    }

    private fun sampleCurveY(t: Float, y1: Float, y2: Float): Float {
        val c = 3f * y1
        val b = 3f * (y2 - y1) - c
        val a = 1f - c - b
        return ((a * t + b) * t + c) * t
    }

    private fun sampleCurveDerivativeX(t: Float, x1: Float, x2: Float): Float {
        val c = 3f * x1
        val b = 3f * (x2 - x1) - c
        val a = 1f - c - b
        return (3f * a * t + 2f * b) * t + c
    }

    private fun sampleCurveDerivativeY(t: Float, y1: Float, y2: Float): Float {
        val c = 3f * y1
        val b = 3f * (y2 - y1) - c
        val a = 1f - c - b
        return (3f * a * t + 2f * b) * t + c
    }

    private fun solveCurveX(x: Float, x1: Float, x2: Float): Float {
        var t2 = x

        // First try Newton-Raphson
        for (i in 0 until NEWTON_ITERATIONS) {
            val x2Estimate = sampleCurveX(t2, x1, x2) - x
            if (abs(x2Estimate) < SUBDIVISION_PRECISION) return t2
            val d2 = sampleCurveDerivativeX(t2, x1, x2)
            if (abs(d2) < NEWTON_MIN_SLOPE) break
            t2 -= x2Estimate / d2
        }

        // Fallback to bisection
        var t0 = 0f
        var t1 = 1f
        t2 = x

        var i = 0
        while (t0 < t1 && i < SUBDIVISION_MAX_ITERATIONS) {
            val x2Estimate = sampleCurveX(t2, x1, x2)
            if (abs(x2Estimate - x) < SUBDIVISION_PRECISION) return t2
            if (x > x2Estimate) {
                t0 = t2
            } else {
                t1 = t2
            }
            t2 = (t1 - t0) * 0.5f + t0
            i++
        }

        return t2
    }
}
