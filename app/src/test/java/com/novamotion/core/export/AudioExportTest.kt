package com.novamotion.core.export

import com.novamotion.core.model.Layer
import com.novamotion.core.model.LayerType
import com.novamotion.core.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioExportTest {

    @Test
    fun testHasAudioReturnsFalseForVisualOnlyLayers() {
        val project = Project(
            title = "Visual Project",
            layers = listOf(
                Layer(name = "Title", type = LayerType.TEXT),
                Layer(name = "Background Rect", type = LayerType.SHAPE)
            )
        )

        assertFalse(AudioExportPipeline.hasAudio(project))
        assertNull(AudioExportPipeline.getPrimaryAudioLayer(project))
    }

    @Test
    fun testHasAudioReturnsFalseWhenUriIsBlank() {
        val project = Project(
            title = "Empty Audio Project",
            layers = listOf(
                Layer(name = "Mute Audio", type = LayerType.AUDIO, mediaUri = null, durationMs = 5000L),
                Layer(name = "Blank Audio", type = LayerType.AUDIO, mediaUri = "   ", durationMs = 5000L)
            )
        )

        assertFalse(AudioExportPipeline.hasAudio(project))
        assertNull(AudioExportPipeline.getPrimaryAudioLayer(project))
    }

    @Test
    fun testHasAudioReturnsTrueForValidAudioLayer() {
        val audioLayer = Layer(
            name = "Soundtrack",
            type = LayerType.AUDIO,
            mediaUri = "content://media/audio/123",
            startTimeMs = 0L,
            durationMs = 15000L
        )
        val project = Project(
            title = "Audio Project",
            layers = listOf(
                Layer(name = "Heading", type = LayerType.TEXT),
                audioLayer
            )
        )

        assertTrue(AudioExportPipeline.hasAudio(project))
        val primary = AudioExportPipeline.getPrimaryAudioLayer(project)
        assertNotNull(primary)
        assertEquals("Soundtrack", primary?.name)
        assertEquals(LayerType.AUDIO, primary?.type)
    }

    @Test
    fun testHasAudioReturnsTrueForVideoLayerWithMedia() {
        val videoLayer = Layer(
            name = "B-Roll Video",
            type = LayerType.VIDEO,
            mediaUri = "content://media/video/456",
            startTimeMs = 1000L,
            durationMs = 8000L
        )
        val project = Project(
            title = "Video Clip Project",
            layers = listOf(videoLayer)
        )

        assertTrue(AudioExportPipeline.hasAudio(project))
        val primary = AudioExportPipeline.getPrimaryAudioLayer(project)
        assertNotNull(primary)
        assertEquals("B-Roll Video", primary?.name)
        assertEquals(LayerType.VIDEO, primary?.type)
    }

    @Test
    fun testPrimaryAudioPrefersAudioLayerOverVideoLayer() {
        val videoLayer = Layer(
            name = "Background Video",
            type = LayerType.VIDEO,
            mediaUri = "content://media/video/100"
        )
        val audioLayer = Layer(
            name = "Primary Voiceover",
            type = LayerType.AUDIO,
            mediaUri = "content://media/audio/200"
        )

        val project = Project(
            title = "Mixed Media Project",
            layers = listOf(videoLayer, audioLayer)
        )

        val selected = AudioExportPipeline.getPrimaryAudioLayer(project)
        assertEquals("Primary Voiceover", selected?.name)
        assertEquals(LayerType.AUDIO, selected?.type)
    }
}
