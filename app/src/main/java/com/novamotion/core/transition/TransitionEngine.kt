package com.novamotion.core.transition

enum class TransitionType(val displayName: String, val category: String) {
    ZOOM_BLUR("Smooth Zoom", "Motion"),
    WHIP_PAN("Whip Pan", "Motion"),
    GLITCH_FLASH("Cyber Flash", "Glitch & Flash"),
    SPIN_ROLL("Spin Roll", "Motion"),
    CROSS_DISSOLVE("Cross Dissolve", "Classic")
}

data class ClipTransition(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: TransitionType = TransitionType.ZOOM_BLUR,
    val durationMs: Long = 500L,
    val boundaryTimeMs: Long
) {
    val startTimeMs: Long get() = boundaryTimeMs - durationMs / 2
    val endTimeMs: Long get() = boundaryTimeMs + durationMs / 2

    fun calculateProgress(timeMs: Long): Float {
        if (timeMs <= startTimeMs) return 0f
        if (timeMs >= endTimeMs) return 1f
        return (timeMs - startTimeMs).toFloat() / durationMs.toFloat()
    }
}
