package com.novamotion.core.effects

import com.novamotion.core.model.EffectParameter
import com.novamotion.core.model.EffectType
import com.novamotion.core.model.VisualEffect

data class EffectDefinition(
    val type: EffectType,
    val description: String,
    val defaultParameters: Map<String, EffectParameter>
)

object EffectCatalog {

    val availableEffects: List<EffectDefinition> = listOf(
        EffectDefinition(
            type = EffectType.MOTION_BLUR,
            description = "Velocity-based motion blur responding to layer movement",
            defaultParameters = mapOf(
                "samples" to EffectParameter("samples", "Samples", 8f, 2f, 16f),
                "intensity" to EffectParameter("intensity", "Intensity", 1.0f, 0.1f, 3.0f)
            )
        ),
        EffectDefinition(
            type = EffectType.WAVE_WARP,
            description = "Continuous sinusoidal distortion wave",
            defaultParameters = mapOf(
                "frequency" to EffectParameter("frequency", "Frequency", 15f, 1f, 50f),
                "amplitude" to EffectParameter("amplitude", "Amplitude", 0.03f, 0.001f, 0.15f),
                "speed" to EffectParameter("speed", "Speed", 2f, 0.1f, 10f)
            )
        ),
        EffectDefinition(
            type = EffectType.CHROMATIC_ABERRATION,
            description = "RGB channel displacement and optical dispersion",
            defaultParameters = mapOf(
                "intensity" to EffectParameter("intensity", "Displacement", 0.02f, 0.001f, 0.08f)
            )
        ),
        EffectDefinition(
            type = EffectType.GLOW_BLOOM,
            description = "Cinematic Dual-Kawase Neon Bloom and soft edge glow",
            defaultParameters = mapOf(
                "threshold" to EffectParameter("threshold", "Threshold", 0.6f, 0.1f, 1.0f),
                "intensity" to EffectParameter("intensity", "Intensity", 1.5f, 0.5f, 4.0f)
            )
        ),
        EffectDefinition(
            type = EffectType.GLITCH,
            description = "Cyberpunk digital scanline tearing and color noise",
            defaultParameters = mapOf(
                "amount" to EffectParameter("amount", "Glitch Amount", 0.3f, 0.05f, 1.0f),
                "speed" to EffectParameter("speed", "Frequency", 5f, 1f, 20f)
            )
        ),
        EffectDefinition(
            type = EffectType.VIGNETTE,
            description = "Dark lens edge falloff for cinematic focus",
            defaultParameters = mapOf(
                "radius" to EffectParameter("radius", "Radius", 0.75f, 0.3f, 1.0f),
                "softness" to EffectParameter("softness", "Softness", 0.45f, 0.1f, 0.8f)
            )
        ),
        EffectDefinition(
            type = EffectType.MOTION_TILE,
            description = "Mirrors repeating edges to eliminate black borders during shakes & zooms",
            defaultParameters = mapOf(
                "scale" to EffectParameter("scale", "Scale", 1.0f, 0.2f, 3.0f),
                "mirror" to EffectParameter("mirror", "Mirror Edges (1=On)", 1.0f, 0f, 1f)
            )
        ),
        EffectDefinition(
            type = EffectType.DISPLACEMENT_MAP,
            description = "Displaces pixels based on secondary map red/green channels",
            defaultParameters = mapOf(
                "strengthX" to EffectParameter("strengthX", "Horizontal Strength", 0.05f, 0.001f, 0.2f),
                "strengthY" to EffectParameter("strengthY", "Vertical Strength", 0.05f, 0.001f, 0.2f)
            )
        ),
        EffectDefinition(
            type = EffectType.CHROMA_KEY,
            description = "Green/Blue screen color removal with spill suppression",
            defaultParameters = mapOf(
                "similarity" to EffectParameter("similarity", "Similarity Threshold", 0.4f, 0.1f, 0.8f),
                "smoothness" to EffectParameter("smoothness", "Edge Smoothness", 0.15f, 0.01f, 0.5f)
            )
        )
    )

    fun createEffect(type: EffectType): VisualEffect {
        val def = availableEffects.find { it.type == type }
        return VisualEffect(
            type = type,
            isEnabled = true,
            parameters = def?.defaultParameters ?: emptyMap()
        )
    }
}
