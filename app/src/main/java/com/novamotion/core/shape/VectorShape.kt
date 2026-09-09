package com.novamotion.core.shape

enum class ShapeType(val displayName: String) {
    RECTANGLE("Rectangle"),
    CIRCLE("Circle / Ellipse"),
    STAR("Star"),
    POLYGON("Polygon"),
    ARROW("Arrow"),
    HEART("Heart"),
    CUSTOM_PATH("Custom Bézier Path")
}

enum class FillType {
    SOLID,
    LINEAR_GRADIENT,
    RADIAL_GRADIENT
}

enum class MaskMode(val label: String) {
    NONE("No Mask"),
    ALPHA_MATTE("Alpha Mask"),
    ALPHA_INVERT("Invert Alpha Mask"),
    LUMA_MATTE("Luma Matte"),
    LUMA_INVERT("Invert Luma Matte")
}

data class VectorShapeData(
    val type: ShapeType = ShapeType.RECTANGLE,
    val width: Float = 300f,
    val height: Float = 300f,
    val cornerRadius: Float = 24f,
    // Star & Polygon specifics
    val pointsCount: Int = 5,
    val innerRadiusRatio: Float = 0.5f,
    // Fill & Stroke
    val fillType: FillType = FillType.SOLID,
    val primaryColor: Long = 0xFF6366F1, // Electric Indigo
    val secondaryColor: Long = 0xFF06B6D4, // Neon Cyan
    val strokeWidth: Float = 0f,
    val strokeColor: Long = 0xFFFFFFFF,
    // Masking
    val maskMode: MaskMode = MaskMode.NONE
)
