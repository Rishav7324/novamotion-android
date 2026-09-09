package com.novamotion.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineEvaluationTest {

    @Test
    fun testLayerActiveWindow() {
        val layer = Layer(
            name = "Test Title",
            type = LayerType.TEXT,
            startTimeMs = 1000L,
            durationMs = 3000L
        )

        assertFalse(layer.isActiveAt(500L))
        assertTrue(layer.isActiveAt(1000L))
        assertTrue(layer.isActiveAt(2500L))
        assertTrue(layer.isActiveAt(4000L))
        assertFalse(layer.isActiveAt(4500L))
    }

    @Test
    fun testAnimatablePropertyKeyframeInterpolation() {
        val prop = AnimatableProperty(
            defaultValue = 0f,
            keyframes = listOf(
                Keyframe(timeMs = 0L, value = 0f),
                Keyframe(timeMs = 1000L, value = 100f)
            )
        )

        assertEquals(0f, prop.evaluate(0L), 0.001f)
        assertEquals(100f, prop.evaluate(1000L), 0.001f)
        assertEquals(50f, prop.evaluate(500L), 5.0f)
        assertEquals(0f, prop.evaluate(-100L), 0.001f)
        assertEquals(100f, prop.evaluate(2000L), 0.001f)
    }

    @Test
    fun testLayerEndTime() {
        val layer = Layer(
            name = "Background",
            type = LayerType.VIDEO,
            startTimeMs = 2000L,
            durationMs = 5000L
        )
        assertEquals(7000L, layer.endTimeMs)
    }
}
