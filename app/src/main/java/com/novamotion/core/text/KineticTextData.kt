package com.novamotion.core.text

enum class KineticTextPreset(val displayName: String) {
    NONE("Static Text"),
    TYPEWRITER("Typewriter"),
    SPRING_POP("Spring Bounce"),
    SLIDE_UP("Slide Up"),
    GLITCH_KINETIC("Cyber Glitch"),
    SINE_WAVE("Sine Wave Float")
}

data class KineticTextStyle(
    val text: String = "NOVAMOTION ULTRA",
    val fontSize: Float = 64f,
    val fontPath: String? = null, // Path to custom .ttf/.otf
    val isBold: Boolean = true,
    val isItalic: Boolean = false,
    val letterSpacing: Float = 0.05f,
    val lineSpacing: Float = 1.2f,
    // Fill & Stroke
    val fillColor: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 0f,
    val strokeColor: Long = 0xFF000000,
    // Drop Shadow
    val shadowRadius: Float = 8f,
    val shadowDx: Float = 0f,
    val shadowDy: Float = 4f,
    val shadowColor: Long = 0x80000000,
    // Animation Preset
    val animator: KineticTextPreset = KineticTextPreset.SPRING_POP,
    val staggerDelayMs: Long = 60L // Delay between individual character animations
)
