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
        ),
        EffectDefinition(
            type = EffectType.LUT_3D,
            description = "Professional .cube 3D Look-Up Table cinema grading with trilinear hardware interpolation",
            defaultParameters = mapOf(
                "intensity" to EffectParameter("intensity", "Blend Intensity", 1.0f, 0f, 1f),
                "presetIndex" to EffectParameter("presetIndex", "Preset (0:Teal, 1:Portra, 2:Neon, 3:Noir, 4:Bleach)", 0f, 0f, 4f)
            )
        ),
        EffectDefinition(
            type = EffectType.BEZIER_MASK,
            description = "Multi-point Bezier pen mask outline with edge feathering and inversion",
            defaultParameters = mapOf(
                "feather" to EffectParameter("feather", "Edge Feather Radius", 0.15f, 0f, 1f),
                "invert" to EffectParameter("invert", "Invert Mask (1=On)", 0f, 0f, 1f),
                "opacity" to EffectParameter("opacity", "Mask Opacity", 1.0f, 0f, 1f)
            )
        ),
        EffectDefinition(
            type = EffectType.AUDIO_REACTIVE_PULSE,
            description = "Drives scale, shake or glow using damped spring physics driven by audio frequency bands",
            defaultParameters = mapOf(
                "sensitivity" to EffectParameter("sensitivity", "Sensitivity", 1.5f, 0.1f, 5.0f),
                "threshold" to EffectParameter("threshold", "Noise Threshold", 0.2f, 0.0f, 0.8f),
                "maxModulation" to EffectParameter("maxModulation", "Max Modulation", 0.4f, 0.05f, 1.0f)
            )
        ),
        EffectDefinition(
            type = EffectType.CAMERA_3D_PERSPECTIVE,
            description = "3D depth positioning, 3-axis rotation, and cinematic bokeh Depth of Field blur",
            defaultParameters = mapOf(
                "posZ" to EffectParameter("posZ", "Depth Z Position", 0f, -2000f, 2000f),
                "rotX" to EffectParameter("rotX", "Pitch Rotation", 0f, -180f, 180f),
                "rotY" to EffectParameter("rotY", "Yaw Rotation", 0f, -180f, 180f),
                "aperture" to EffectParameter("aperture", "Lens Aperture (DoF)", 2.8f, 1.2f, 16.0f)
            )
        ),
        EffectDefinition(
            type = EffectType.TIME_REMAP,
            description = "Non-linear time remapping, velocity speed ramping, and optical slow-motion blending",
            defaultParameters = mapOf(
                "speed" to EffectParameter("speed", "Speed Multiplier", 1.0f, 0.1f, 10.0f),
                "frameBlend" to EffectParameter("frameBlend", "Frame Blending (1=On)", 1.0f, 0f, 1f)
            )
        ),
        EffectDefinition(
            type = EffectType.OPTICAL_FLOW_SLOWMO,
            description = "GPU Lucas-Kanade optical flow vector estimation and bidirectional frame synthesis for 120fps/240fps slow-motion",
            defaultParameters = mapOf(
                "speedRatio" to EffectParameter("speedRatio", "Slow-Mo Speed", 0.25f, 0.05f, 1.0f),
                "smoothness" to EffectParameter("smoothness", "Motion Smoothness", 0.8f, 0.1f, 1.0f)
            )
        ),
        EffectDefinition(
            type = EffectType.INVERSE_KINEMATICS,
            description = "Analytical 2-bone inverse kinematics character rigging with pole vector knee/elbow constraint",
            defaultParameters = mapOf(
                "length1" to EffectParameter("length1", "Upper Bone Length", 150f, 20f, 600f),
                "length2" to EffectParameter("length2", "Lower Bone Length", 150f, 20f, 600f),
                "flipBend" to EffectParameter("flipBend", "Flip Bend Direction (1=On)", 0f, 0f, 1f)
            )
        ),
        EffectDefinition(
            type = EffectType.FBM_TURBULENCE,
            description = "Procedural multi-octave Fractal Brownian Motion and Simplex noise for fluid smoke, electricity, and nebulae",
            defaultParameters = mapOf(
                "scale" to EffectParameter("scale", "Noise Scale", 4.0f, 0.5f, 20.0f),
                "roughness" to EffectParameter("roughness", "Roughness", 0.5f, 0.1f, 0.9f),
                "speed" to EffectParameter("speed", "Drift Speed", 1.0f, 0.1f, 5.0f)
            )
        ),
        EffectDefinition(
            type = EffectType.MSDF_BEVEL_3D,
            description = "Vector Multi-channel Signed Distance Field with pseudo-3D bevel lighting, stroke outline, and glow",
            defaultParameters = mapOf(
                "bevelStrength" to EffectParameter("bevelStrength", "3D Bevel Depth", 0.5f, 0.0f, 1.0f),
                "strokeWidth" to EffectParameter("strokeWidth", "Stroke Width", 0.05f, 0.0f, 0.3f),
                "lightAngle" to EffectParameter("lightAngle", "Light Direction Angle", 45f, 0f, 360f)
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
