package com.novamotion.core.preset

import com.novamotion.core.model.*
import org.junit.Assert.*
import org.junit.Test

class AlightMotionXmlTest {

    @Test
    fun testExportAndImportRoundTrip() {
        val originalProject = Project(
            title = "AM Community Preset",
            width = 1080,
            height = 1920,
            fps = 60,
            durationMs = 5000L,
            layers = listOf(
                Layer(
                    name = "Main Heading",
                    type = LayerType.TEXT,
                    startTimeMs = 0L,
                    durationMs = 3000L,
                    textContent = "KINETIC GLOW",
                    transform = LayerTransform(
                        posX = AnimatableProperty(
                            defaultValue = 0f,
                            keyframes = listOf(
                                Keyframe(timeMs = 0L, value = -200f, curve = BezierControlPoints(0.25f, 0.1f, 0.25f, 1f)),
                                Keyframe(timeMs = 1500L, value = 0f, curve = BezierControlPoints(0.42f, 0f, 0.58f, 1f))
                            )
                        ),
                        opacity = AnimatableProperty(
                            defaultValue = 1f,
                            keyframes = listOf(
                                Keyframe(timeMs = 0L, value = 0f),
                                Keyframe(timeMs = 500L, value = 1f)
                            )
                        )
                    )
                ),
                Layer(
                    name = "B-Roll Background",
                    type = LayerType.VIDEO,
                    startTimeMs = 500L,
                    durationMs = 4500L,
                    mediaUri = "content://media/video/777"
                )
            )
        )

        // 1. Export to XML
        val xmlOutput = AlightMotionXmlParser.exportToAlightMotionXml(originalProject)
        assertNotNull(xmlOutput)
        assertTrue(xmlOutput.contains("<scene"))
        assertTrue(xmlOutput.contains("width=\"1080\""))
        assertTrue(xmlOutput.contains("height=\"1920\""))
        assertTrue(xmlOutput.contains("KINETIC GLOW"))
        assertTrue(xmlOutput.contains("name=\"position\""))

        // 2. Import back from XML
        val importResult = AlightMotionXmlParser.importFromAlightMotionXml(xmlOutput)
        assertTrue(importResult.isSuccess)
        val imported = importResult.getOrThrow()

        assertEquals(1080, imported.width)
        assertEquals(1920, imported.height)
        assertEquals(60, imported.fps)
        assertEquals(5000L, imported.durationMs)
        assertEquals(2, imported.layers.size)

        // Check Layer 1 (Text)
        val textLayer = imported.layers[0]
        assertEquals("Main Heading", textLayer.name)
        assertEquals(LayerType.TEXT, textLayer.type)
        assertEquals("KINETIC GLOW", textLayer.textContent)
        assertEquals(0L, textLayer.startTimeMs)
        assertEquals(3000L, textLayer.durationMs)
        assertTrue(textLayer.transform.posX.hasKeyframes())
        assertEquals(2, textLayer.transform.posX.keyframes.size)
        assertEquals(-200f, textLayer.transform.posX.keyframes[0].value, 0.01f)
        assertEquals(0.25f, textLayer.transform.posX.keyframes[0].curve.x1, 0.01f)

        // Check Layer 2 (Video)
        val videoLayer = imported.layers[1]
        assertEquals("B-Roll Background", videoLayer.name)
        assertEquals(LayerType.VIDEO, videoLayer.type)
        assertEquals("content://media/video/777", videoLayer.mediaUri)
        assertEquals(500L, videoLayer.startTimeMs)
        assertEquals(4500L, videoLayer.durationMs)
    }

    @Test
    fun testImportMalformedXmlFailsGracefully() {
        val malformed = "<invalid><unclosed>"
        val result = AlightMotionXmlParser.importFromAlightMotionXml(malformed)
        assertTrue(result.isFailure)
    }
}
