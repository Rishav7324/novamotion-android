package com.novamotion.core.shape

import kotlin.math.*

/**
 * Multi-Point Cubic Bezier Vector Pen Mask.
 * Allows arbitrary shape outlines with smooth incoming/outgoing handles,
 * edge feathering, inversion, and expansion.
 */
data class BezierControlPoint(
    val anchorX: Float,
    val anchorY: Float,
    val handleInX: Float = anchorX,
    val handleInY: Float = anchorY,
    val handleOutX: Float = anchorX,
    val handleOutY: Float = anchorY
)

data class BezierMaskPath(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Mask 1",
    val points: List<BezierControlPoint> = emptyList(),
    val isClosed: Boolean = true,
    val featherRadius: Float = 0f, // in pixels
    val isInverted: Boolean = false,
    val opacity: Float = 1.0f,
    val expansion: Float = 0f
) {
    /**
     * Samples the continuous cubic curve segment between two control points.
     */
    fun samplePointsAlongPath(samplesPerSegment: Int = 16): List<Pair<Float, Float>> {
        if (points.size < 2) return points.map { Pair(it.anchorX, it.anchorY) }

        val sampled = mutableListOf<Pair<Float, Float>>()
        val count = if (isClosed) points.size else points.size - 1

        for (i in 0 until count) {
            val p0 = points[i]
            val p1 = points[(i + 1) % points.size]

            for (step in 0..samplesPerSegment) {
                val t = step.toFloat() / samplesPerSegment
                val oneMinusT = 1f - t

                // Cubic Bézier formula: B(t) = (1-t)^3*P0 + 3(1-t)^2*t*P1 + 3(1-t)*t^2*P2 + t^3*P3
                val x = oneMinusT.pow(3) * p0.anchorX +
                        3f * oneMinusT.pow(2) * t * p0.handleOutX +
                        3f * oneMinusT * t.pow(2) * p1.handleInX +
                        t.pow(3) * p1.anchorX

                val y = oneMinusT.pow(3) * p0.anchorY +
                        3f * oneMinusT.pow(2) * t * p0.handleOutY +
                        3f * oneMinusT * t.pow(2) * p1.handleInY +
                        t.pow(3) * p1.anchorY

                sampled.add(Pair(x, y))
            }
        }
        return sampled
    }

    /**
     * Determines whether a given coordinate (px, py) is inside the closed polygon
     * using the Jordan Curve Theorem (Ray Casting algorithm).
     */
    fun isPointInside(px: Float, py: Float, sampledBoundary: List<Pair<Float, Float>>): Boolean {
        if (sampledBoundary.size < 3) return false
        var inside = false
        var j = sampledBoundary.size - 1

        for (i in sampledBoundary.indices) {
            val xi = sampledBoundary[i].first
            val yi = sampledBoundary[i].second
            val xj = sampledBoundary[j].first
            val yj = sampledBoundary[j].second

            val intersect = ((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
            j = i
        }

        return if (isInverted) !inside else inside
    }

    /**
     * Computes the minimum distance from point (px, py) to the polygon boundary.
     */
    fun minDistanceToBoundary(px: Float, py: Float, sampledBoundary: List<Pair<Float, Float>>): Float {
        if (sampledBoundary.isEmpty()) return Float.MAX_VALUE
        var minDistSq = Float.MAX_VALUE

        for (i in sampledBoundary.indices) {
            val p1 = sampledBoundary[i]
            val p2 = sampledBoundary[(i + 1) % sampledBoundary.size]

            val distSq = distToSegmentSquared(px, py, p1.first, p1.second, p2.first, p2.second)
            if (distSq < minDistSq) {
                minDistSq = distSq
            }
        }
        return sqrt(minDistSq)
    }

    private fun distToSegmentSquared(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        val l2 = (x2 - x1).pow(2) + (y2 - y1).pow(2)
        if (l2 == 0f) return (px - x1).pow(2) + (py - y1).pow(2)
        val t = max(0f, min(1f, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2))
        val projX = x1 + t * (x2 - x1)
        val projY = y1 + t * (y2 - y1)
        return (px - projX).pow(2) + (py - projY).pow(2)
    }
}
