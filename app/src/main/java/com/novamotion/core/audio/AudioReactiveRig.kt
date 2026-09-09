package com.novamotion.core.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Multi-band Audio Reactive Physics Rig.
 * Splits incoming audio spectrum into acoustic bands (Sub-bass, Bass, Mids, Highs)
 * and drives layer properties using a second-order damped harmonic spring simulator
 * for natural, fluid elastic bounce motion instead of jerky frame jumps.
 */
enum class AudioFrequencyBand(val displayName: String, val minHz: Float, val maxHz: Float) {
    SUB_BASS("Sub-Bass (20 - 60 Hz)", 20f, 60f),
    BASS("Bass (60 - 250 Hz)", 60f, 250f),
    MIDS("Midrange (250 - 4000 Hz)", 250f, 4000f),
    HIGHS("Highs / Treble (4k - 20k Hz)", 4000f, 20000f)
}

enum class ReactiveTargetProperty(val displayName: String) {
    SCALE("Scale (Elastic Pulse)"),
    SHAKE_AMPLITUDE("Camera Shake Strength"),
    GLOW_INTENSITY("Glow / Bloom Flash"),
    CHROMATIC_SPLIT("RGB Aberration Glitch"),
    ROTATION("Rotational Twitch")
}

data class SpringParameters(
    val mass: Float = 1.0f,
    val stiffness: Float = 180.0f, // Spring constant k
    val damping: Float = 12.0f,    // Damping factor c
    val restPosition: Float = 1.0f
)

class DampedSpring(private val params: SpringParameters) {
    var position: Float = params.restPosition
    var velocity: Float = 0f

    fun update(externalForce: Float, dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f) // Stability clamp
        // F_net = F_ext - k * (x - rest) - c * v
        val displacement = position - params.restPosition
        val springForce = -params.stiffness * displacement
        val dampingForce = -params.damping * velocity
        val netForce = externalForce + springForce + dampingForce

        val acceleration = netForce / params.mass
        velocity += acceleration * dt
        position += velocity * dt
    }

    fun reset() {
        position = params.restPosition
        velocity = 0f
    }
}

class AudioReactiveRig(
    val band: AudioFrequencyBand = AudioFrequencyBand.BASS,
    val target: ReactiveTargetProperty = ReactiveTargetProperty.SCALE,
    val sensitivity: Float = 1.5f,
    val threshold: Float = 0.2f,
    val maxModulation: Float = 0.5f,
    val spring: DampedSpring = DampedSpring(SpringParameters())
) {
    /**
     * Evaluates the band magnitude and updates spring physics.
     * @param frequencyMagnitudes FFT frequency spectrum array normalized [0..1]
     * @param sampleRate audio sampling rate in Hz (e.g. 44100)
     * @param dtSeconds time delta since last frame
     * @return resulting modulated property value
     */
    fun evaluate(
        frequencyMagnitudes: FloatArray,
        sampleRate: Float = 44100f,
        dtSeconds: Float = 1f / 60f
    ): Float {
        if (frequencyMagnitudes.isEmpty()) {
            spring.update(0f, dtSeconds)
            return spring.position
        }

        val nyquist = sampleRate / 2f
        val binWidth = nyquist / frequencyMagnitudes.size

        val startBin = (band.minHz / binWidth).toInt().coerceIn(0, frequencyMagnitudes.size - 1)
        val endBin = (band.maxHz / binWidth).toInt().coerceIn(startBin, frequencyMagnitudes.size - 1)

        var bandEnergy = 0f
        var binCount = 0
        for (i in startBin..endBin) {
            bandEnergy += frequencyMagnitudes[i]
            binCount++
        }
        val avgEnergy = if (binCount > 0) bandEnergy / binCount else 0f

        // Apply threshold and sensitivity
        val activeEnergy = max(0f, avgEnergy - threshold) * sensitivity
        val impulseForce = min(activeEnergy, 1.0f) * maxModulation * 500f

        spring.update(impulseForce, dtSeconds)
        return spring.position
    }
}
