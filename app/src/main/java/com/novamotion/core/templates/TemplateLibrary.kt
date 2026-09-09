package com.novamotion.core.templates

import com.novamotion.core.model.*

data class ProjectTemplate(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val project: Project
)

object TemplateLibrary {

    val templates: List<ProjectTemplate> = listOf(
        ProjectTemplate(
            id = "tpl_cyberpunk_glitch",
            title = "Cyberpunk Glitch Stinger",
            category = "Intros & Stingers",
            description = "High-energy RGB split, wave warp, and kinetic glitch text",
            project = Project(
                title = "Cyberpunk Glitch Stinger",
                width = 1080,
                height = 1920,
                fps = 60,
                durationMs = 6000L,
                layers = listOf(
                    Layer(
                        name = "Cyberpunk City Background",
                        type = LayerType.VIDEO,
                        startTimeMs = 0L,
                        durationMs = 6000L,
                        effects = listOf(
                            VisualEffect(type = EffectType.GLITCH),
                            VisualEffect(type = EffectType.CHROMATIC_ABERRATION)
                        )
                    ),
                    Layer(
                        name = "Neon Hexagon Mask",
                        type = LayerType.SHAPE,
                        startTimeMs = 500L,
                        durationMs = 5500L,
                        shapeType = "POLYGON",
                        fillColor = 0xFF06B6D4
                    ),
                    Layer(
                        name = "Main Kinetic Title",
                        type = LayerType.TEXT,
                        startTimeMs = 800L,
                        durationMs = 4500L,
                        textContent = "CYBER // GLITCH"
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "tpl_neon_bass_drop",
            title = "Neon Audio Bass Reactive",
            category = "Music & Beats",
            description = "Dual-Kawase glow bloom pulsing to EDM kicks with velocity motion blur",
            project = Project(
                title = "Neon Audio Bass Reactive",
                width = 1080,
                height = 1920,
                fps = 60,
                durationMs = 8000L,
                layers = listOf(
                    Layer(
                        name = "Electronic Synth Beat",
                        type = LayerType.AUDIO,
                        startTimeMs = 0L,
                        durationMs = 8000L
                    ),
                    Layer(
                        name = "Pulsing Glow Star",
                        type = LayerType.SHAPE,
                        startTimeMs = 0L,
                        durationMs = 8000L,
                        shapeType = "STAR",
                        fillColor = 0xFF6366F1,
                        effects = listOf(
                            VisualEffect(type = EffectType.GLOW_BLOOM),
                            VisualEffect(type = EffectType.MOTION_BLUR)
                        )
                    ),
                    Layer(
                        name = "Drop Title Text",
                        type = LayerType.TEXT,
                        startTimeMs = 1500L,
                        durationMs = 6000L,
                        textContent = "FEEL THE BASS"
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "tpl_cinematic_minimal",
            title = "Cinematic Minimalist Story",
            category = "Cinematic & Vlog",
            description = "Smooth ease-in-out zooms, letterbox bars, and cinematic vignette",
            project = Project(
                title = "Cinematic Minimalist Story",
                width = 1080,
                height = 1920,
                fps = 60,
                durationMs = 10000L,
                layers = listOf(
                    Layer(
                        name = "Cinematic Footage",
                        type = LayerType.VIDEO,
                        startTimeMs = 0L,
                        durationMs = 10000L,
                        effects = listOf(
                            VisualEffect(type = EffectType.VIGNETTE)
                        )
                    ),
                    Layer(
                        name = "Aesthetic Subtitle",
                        type = LayerType.TEXT,
                        startTimeMs = 1000L,
                        durationMs = 7000L,
                        textContent = "Somewhere in Tokyo"
                    )
                )
            )
        )
    )
}
