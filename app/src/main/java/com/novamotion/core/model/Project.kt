package com.novamotion.core.model

data class Project(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "Untitled Project",
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 60,
    val durationMs: Long = 10000L, // 10 seconds default
    val layers: List<Layer> = emptyList()
) {
    val totalFrames: Long get() = (durationMs * fps) / 1000L

    fun getLayerById(id: String): Layer? = layers.find { it.id == id }
}
