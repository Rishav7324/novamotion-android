package com.novamotion.core.model

enum class EffectType(val displayName: String, val category: String) {
    MOTION_BLUR("Velocity Motion Blur", "Blur & Sharpen"),
    DIRECTIONAL_BLUR("Directional Blur", "Blur & Sharpen"),
    CHROMATIC_ABERRATION("RGB Split / Aberration", "Distortion"),
    GLOW_BLOOM("Dual-Kawase Glow", "Light & Glow"),
    COLOR_GRADING("3D LUT & Grade", "Color"),
    WAVE_WARP("Wave Warp", "Distortion"),
    VIGNETTE("Cinematic Vignette", "Light & Glow"),
    GLITCH("Digital Glitch", "Stylize"),
    MOTION_TILE("Motion Tile / Mirror Edges", "Distortion"),
    DISPLACEMENT_MAP("Displacement Map", "Distortion"),
    CHROMA_KEY("Chroma Key (Green Screen)", "Matte & Mask")
}

data class EffectParameter(
    val key: String,
    val name: String,
    val value: Float,
    val min: Float,
    val max: Float
)

data class VisualEffect(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: EffectType,
    val isEnabled: Boolean = true,
    val parameters: Map<String, EffectParameter> = emptyMap()
)
