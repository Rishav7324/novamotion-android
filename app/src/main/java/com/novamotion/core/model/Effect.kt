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
    CHROMA_KEY("Chroma Key (Green Screen)", "Matte & Mask"),
    LUT_3D("3D LUT Cinema Grade (.cube)", "Color"),
    BEZIER_MASK("Vector Pen Mask (Feathered)", "Matte & Mask"),
    AUDIO_REACTIVE_PULSE("Audio Pulse & Beat Physics", "Physics & Dynamics"),
    CAMERA_3D_PERSPECTIVE("3D Camera Depth & DoF", "3D"),
    TIME_REMAP("Time Remapping & Speed Ramp", "Time"),
    OPTICAL_FLOW_SLOWMO("Optical Flow Slow-Mo (AI Warp)", "Time"),
    INVERSE_KINEMATICS("2-Bone Character IK Rig", "Physics & Dynamics"),
    FBM_TURBULENCE("Fractal Simplex Noise & Liquid", "Distortion"),
    MSDF_BEVEL_3D("Vector 3D Bevel & Emboss", "Stylize")
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
