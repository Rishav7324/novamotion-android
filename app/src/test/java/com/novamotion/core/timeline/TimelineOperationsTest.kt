package com.novamotion.core.timeline

import com.novamotion.core.model.*
import org.junit.Assert.*
import org.junit.Test

class TimelineOperationsTest {

    @Test
    fun testSplitLayerBoundaryConditions() {
        val layer = Layer(
            name = "Test Clip",
            type = LayerType.VIDEO,
            startTimeMs = 1000L,
            durationMs = 4000L // ends at 5000L
        )

        // Split outside or within MIN_LAYER_DURATION_MS (50ms) buffer should return null
        assertNull(TimelineOperations.splitLayer(layer, 500L))
        assertNull(TimelineOperations.splitLayer(layer, 1000L))
        assertNull(TimelineOperations.splitLayer(layer, 1020L)) // 20ms into clip < 50ms
        assertNull(TimelineOperations.splitLayer(layer, 4980L)) // 20ms from end < 50ms
        assertNull(TimelineOperations.splitLayer(layer, 5000L))
        assertNull(TimelineOperations.splitLayer(layer, 6000L))

        // Split inside range
        val result = TimelineOperations.splitLayer(layer, 2500L)
        assertNotNull(result)
        val (part1, part2) = result!!

        assertEquals(1000L, part1.startTimeMs)
        assertEquals(1500L, part1.durationMs)
        assertEquals(2500L, part1.endTimeMs)

        assertEquals(2500L, part2.startTimeMs)
        assertEquals(2500L, part2.durationMs)
        assertEquals(5000L, part2.endTimeMs)
        assertEquals(1500L, part2.sourceInMs)
    }

    @Test
    fun testSplitAnimatableFloatPreservesContinuity() {
        val prop = AnimatableProperty(
            defaultValue = 0f,
            keyframes = listOf(
                Keyframe(timeMs = 0L, value = 0f),
                Keyframe(timeMs = 1000L, value = 100f)
            )
        )

        // Split at 500ms
        val (p1, p2) = TimelineOperations.splitAnimatableFloat(prop, 500L)

        // Value at 500ms should be roughly 50f
        val valAtCut = prop.evaluate(500L)

        // Part 1 ends at cut with the evaluated value
        assertEquals(valAtCut, p1.keyframes.last().value, 0.01f)
        assertEquals(500L, p1.keyframes.last().timeMs)

        // Part 2 starts at 0ms with the evaluated value
        assertEquals(valAtCut, p2.keyframes.first().value, 0.01f)
        assertEquals(0L, p2.keyframes.first().timeMs)

        // Part 2 second keyframe is shifted (1000 - 500 = 500ms)
        assertEquals(500L, p2.keyframes[1].timeMs)
        assertEquals(100f, p2.keyframes[1].value, 0.01f)
    }

    @Test
    fun testTrimLayerHeadAndTail() {
        val layer = Layer(
            name = "Trimmable",
            type = LayerType.TEXT,
            startTimeMs = 2000L,
            durationMs = 5000L // ends at 7000L
        )

        // Trim head to 3000L (shifted by +1000ms)
        val trimmedHead = TimelineOperations.trimLayerHead(layer, 3000L)
        assertEquals(3000L, trimmedHead.startTimeMs)
        assertEquals(4000L, trimmedHead.durationMs)
        assertEquals(1000L, trimmedHead.sourceInMs)

        // Trim tail to 6000L (ends 1000ms earlier)
        val trimmedTail = TimelineOperations.trimLayerTail(layer, 6000L)
        assertEquals(2000L, trimmedTail.startTimeMs)
        assertEquals(4000L, trimmedTail.durationMs)
    }

    @Test
    fun testDuplicateLayer() {
        val layer = Layer(
            name = "Original",
            type = LayerType.SHAPE,
            startTimeMs = 1000L,
            durationMs = 2000L
        )

        val dup = TimelineOperations.duplicateLayer(layer, offsetMs = 500L)
        assertNotEquals(layer.id, dup.id)
        assertEquals("Original Copy", dup.name)
        assertEquals(1500L, dup.startTimeMs)
        assertEquals(2000L, dup.durationMs)
    }
}
