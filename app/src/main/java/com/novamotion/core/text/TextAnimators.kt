package com.novamotion.core.text

import kotlin.math.sin

data class CharTransform(
    val offsetY: Float = 0f,
    val scale: Float = 1.0f,
    val opacity: Float = 1.0f,
    val charToDisplay: Char
)

object TextAnimators {

    /**
     * Evaluates the animated transform state of every character in the text string at timeMs.
     */
    fun evaluateCharacters(
        style: KineticTextStyle,
        timeMs: Long
    ): List<CharTransform> {
        val chars = style.text.toCharArray()
        val result = mutableListOf<CharTransform>()

        for (i in chars.indices) {
            val originalChar = chars[i]
            val charStartTime = i * style.staggerDelayMs
            val elapsed = timeMs - charStartTime

            when (style.animator) {
                KineticTextPreset.NONE -> {
                    result.add(CharTransform(charToDisplay = originalChar))
                }

                KineticTextPreset.TYPEWRITER -> {
                    if (elapsed >= 0) {
                        result.add(CharTransform(opacity = 1f, charToDisplay = originalChar))
                    } else {
                        result.add(CharTransform(opacity = 0f, charToDisplay = ' '))
                    }
                }

                KineticTextPreset.SPRING_POP -> {
                    if (elapsed < 0) {
                        result.add(CharTransform(scale = 0f, opacity = 0f, charToDisplay = originalChar))
                    } else {
                        val progress = (elapsed / 400f).coerceIn(0f, 1f)
                        // Spring overshoot curve
                        val springScale = if (progress < 0.7f) {
                            (progress / 0.7f) * 1.25f
                        } else {
                            1.25f - ((progress - 0.7f) / 0.3f) * 0.25f
                        }
                        result.add(
                            CharTransform(
                                scale = springScale,
                                opacity = progress.coerceIn(0f, 1f),
                                charToDisplay = originalChar
                            )
                        )
                    }
                }

                KineticTextPreset.SLIDE_UP -> {
                    if (elapsed < 0) {
                        result.add(CharTransform(offsetY = 40f, opacity = 0f, charToDisplay = originalChar))
                    } else {
                        val progress = (elapsed / 300f).coerceIn(0f, 1f)
                        val dy = (1f - progress) * 40f
                        result.add(CharTransform(offsetY = dy, opacity = progress, charToDisplay = originalChar))
                    }
                }

                KineticTextPreset.SINE_WAVE -> {
                    val wave = (sin(timeMs * 0.005 + i * 0.4) * 15f).toFloat()
                    result.add(CharTransform(offsetY = wave, opacity = 1f, charToDisplay = originalChar))
                }

                KineticTextPreset.GLITCH_KINETIC -> {
                    if (elapsed < 200 && elapsed > 0) {
                        // Random glitch character during transition
                        val glitchChars = "#!$%&*?@01"
                        val randomChar = glitchChars[(i + (timeMs / 50).toInt()) % glitchChars.length]
                        result.add(CharTransform(charToDisplay = randomChar, opacity = 0.9f))
                    } else if (elapsed >= 200) {
                        result.add(CharTransform(charToDisplay = originalChar, opacity = 1f))
                    } else {
                        result.add(CharTransform(opacity = 0f, charToDisplay = ' '))
                    }
                }
            }
        }

        return result
    }
}
