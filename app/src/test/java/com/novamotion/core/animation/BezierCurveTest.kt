package com.novamotion.core.animation

import com.novamotion.core.model.BezierControlPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BezierCurveTest {

    @Test
    fun testLinearCurve() {
        val linear = BezierControlPoints(0f, 0f, 1f, 1f)
        val mid = BezierCurve.evaluate(0.5f, linear)
        assertEquals(0.5f, mid, 0.01f)
    }

    @Test
    fun testEaseInOutCurve() {
        val easeInOut = BezierControlPoints(0.42f, 0.0f, 0.58f, 1.0f)
        val start = BezierCurve.evaluate(0f, easeInOut)
        val end = BezierCurve.evaluate(1f, easeInOut)
        val mid = BezierCurve.evaluate(0.5f, easeInOut)

        assertEquals(0f, start, 0.001f)
        assertEquals(1f, end, 0.001f)
        assertEquals(0.5f, mid, 0.05f)
    }

    @Test
    fun testDerivativeVelocity() {
        val linear = BezierControlPoints(0f, 0f, 1f, 1f)
        val velocity = BezierCurve.evaluateDerivative(0.5f, linear)
        assertTrue("Derivative should be positive", velocity > 0.9f)
    }
}
