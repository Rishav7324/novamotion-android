package com.novamotion.core.audio

import org.junit.Assert.*
import org.junit.Test

class AudioReactiveRigTest {

    @Test
    fun testDampedSpringRestsAtDefaultPosition() {
        val spring = DampedSpring(SpringParameters(restPosition = 1.0f))
        assertEquals(1.0f, spring.position, 0.001f)
        assertEquals(0f, spring.velocity, 0.001f)

        // Without external force, position remains at rest
        spring.update(0f, 0.016f)
        assertEquals(1.0f, spring.position, 0.001f)
    }

    @Test
    fun testDampedSpringDisplacementAndSettling() {
        val spring = DampedSpring(SpringParameters(restPosition = 1.0f, stiffness = 200f, damping = 20f))

        // Apply external force impulse
        spring.update(100f, 0.016f)
        assertTrue("Spring should displace above rest position on impulse", spring.position > 1.0f)

        // Simulate 60 frames without force — should decay back towards rest position
        for (i in 0 until 60) {
            spring.update(0f, 0.016f)
        }

        assertEquals(1.0f, spring.position, 0.05f)
    }

    @Test
    fun testAudioReactiveRigBassImpulse() {
        val rig = AudioReactiveRig(
            band = AudioFrequencyBand.BASS,
            threshold = 0.1f,
            sensitivity = 2.0f
        )

        // Synthetic spectrum: 128 bins up to 22050 Hz (bin width ~172 Hz)
        // Bin 0 (0..172 Hz) covers bass
        val spectrum = FloatArray(128) { 0f }
        spectrum[0] = 0.9f // High bass energy

        val modulated = rig.evaluate(spectrum, sampleRate = 44100f, dtSeconds = 0.016f)
        assertTrue("High bass energy should pulse value above 1.0", modulated > 1.0f)

        // Silent spectrum should settle back
        val silentSpectrum = FloatArray(128) { 0f }
        var finalValue = modulated
        for (i in 0 until 90) {
            finalValue = rig.evaluate(silentSpectrum, sampleRate = 44100f, dtSeconds = 0.016f)
        }
        assertEquals(1.0f, finalValue, 0.05f)
    }
}
