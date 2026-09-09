package com.novamotion.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream

class CubeLutParserTest {

    @Test
    fun testProceduralPresetsGenerateValidBuffers() {
        for (preset in BuiltInLutPreset.entries) {
            val lut = CubeLutParser.generatePreset(preset, size = 16)
            assertNotNull(lut.data)
            assertEquals(16, lut.size)
            assertEquals(16 * 16 * 16 * 4, lut.data.capacity())
        }
    }

    @Test
    fun testParseCubeString() {
        val cubeContent = """
            TITLE "Test Hollywood LUT"
            LUT_3D_SIZE 2
            0.0 0.0 0.0
            1.0 0.0 0.0
            0.0 1.0 0.0
            1.0 1.0 0.0
            0.0 0.0 1.0
            1.0 0.0 1.0
            0.0 1.0 1.0
            1.0 1.0 1.0
        """.trimIndent()

        val inputStream = ByteArrayInputStream(cubeContent.toByteArray())
        val lut = CubeLutParser.parseCubeStream(inputStream)

        assertEquals("Test Hollywood LUT", lut.title)
        assertEquals(2, lut.size)
        assertEquals(2 * 2 * 2 * 4, lut.data.capacity())
    }
}
