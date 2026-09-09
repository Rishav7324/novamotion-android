package com.novamotion.core.shape

import kotlin.math.cos
import kotlin.math.sin

object ShapeGeometry {

    /**
     * Generates triangle fan vertices for a Star with N points.
     */
    fun generateStarVertices(points: Int, outerRadius: Float, innerRadius: Float): FloatArray {
        val totalVertices = points * 2 + 2
        val vertices = FloatArray(totalVertices * 2)

        // Center vertex
        vertices[0] = 0f
        vertices[1] = 0f

        val angleStep = Math.PI / points
        var currentAngle = -Math.PI / 2 // Start at top

        for (i in 0..points * 2) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val idx = (i + 1) * 2
            vertices[idx] = (r * cos(currentAngle)).toFloat()
            vertices[idx + 1] = (r * sin(currentAngle)).toFloat()
            currentAngle += angleStep
        }

        return vertices
    }

    /**
     * Generates triangle fan vertices for a regular Polygon with N sides.
     */
    fun generatePolygonVertices(sides: Int, radius: Float): FloatArray {
        val totalVertices = sides + 2
        val vertices = FloatArray(totalVertices * 2)

        // Center
        vertices[0] = 0f
        vertices[1] = 0f

        val angleStep = (2 * Math.PI) / sides
        var currentAngle = -Math.PI / 2

        for (i in 0..sides) {
            val idx = (i + 1) * 2
            vertices[idx] = (radius * cos(currentAngle)).toFloat()
            vertices[idx + 1] = (radius * sin(currentAngle)).toFloat()
            currentAngle += angleStep
        }

        return vertices
    }
}
