package com.novamotion.core.model

enum class LayerType(val label: String) {
    VIDEO("Video"),
    IMAGE("Image"),
    TEXT("Text"),
    SHAPE("Vector Shape"),
    AUDIO("Audio"),
    ADJUSTMENT("Adjustment FX"),
    NULL_OBJECT("Null Controller")
}

data class Layer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: LayerType,
    val startTimeMs: Long,
    val durationMs: Long,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val parentLayerId: String? = null,
    val transform: LayerTransform = LayerTransform(),
    val effects: List<VisualEffect> = emptyList(),
    // Content-specific properties
    val textContent: String = "NovaMotion",
    val textColor: Long = 0xFFFFFFFF,
    val kineticPreset: String = "SPRING_POP", // KineticTextPreset name
    val fontSize: Float = 64f,
    val letterSpacing: Float = 0.05f,
    val shadowRadius: Float = 8f,
    val shapeType: String = "RECTANGLE", // RECTANGLE, CIRCLE, STAR
    val fillColor: Long = 0xFF6366F1,
    val mediaUri: String? = null,
    val sourceInMs: Long = 0L
) {
    val endTimeMs: Long get() = startTimeMs + durationMs

    fun isActiveAt(timeMs: Long): Boolean {
        return isVisible && timeMs in startTimeMs..endTimeMs
    }
}
