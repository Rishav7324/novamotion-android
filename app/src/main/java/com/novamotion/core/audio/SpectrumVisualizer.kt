package com.novamotion.core.audio

import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerType(val label: String) {
    BARS("Vertical Frequency Bars"),
    CIRCULAR("Radial Circular Ring"),
    WAVEFORM("Smooth Oscilloscope Line")
}

data class SpectrumPoint(val x: Float, val y: Float)

object SpectrumVisualizer {

    /**
     * Computes the visual coordinates of an audio spectrum at timeMs.
     */
    fun computeSpectrum(
        audioPeaks: FloatArray,
        timeMs: Long,
        totalDurationMs: Long,
        type: VisualizerType = VisualizerType.BARS,
        bandsCount: Int = 32,
        centerX: Float = 540f,
        centerY: Float = 960f,
        radius: Float = 200f
    ): List<SpectrumPoint> {
        val points = mutableListOf<SpectrumPoint>()
        if (audioPeaks.isEmpty()) return points

        val progress = (timeMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        val centerIdx = (progress * (audioPeaks.size - 1)).toInt()

        when (type) {
            VisualizerType.BARS -> {
                val barWidth = 16f
                val spacing = 8f
                val totalWidth = bandsCount * (barWidth + spacing)
                val startX = centerX - totalWidth / 2f

                for (i in 0 until bandsCount) {
                    val sampleIdx = (centerIdx + i - bandsCount / 2).coerceIn(0, audioPeaks.lastIndex)
                    val height = audioPeaks[sampleIdx] * 250f
                    val x = startX + i * (barWidth + spacing)
                    val y = centerY - height
                    points.add(SpectrumPoint(x, y))
                }
            }

            VisualizerType.CIRCULAR -> {
                val angleStep = (2 * Math.PI) / bandsCount
                for (i in 0 until bandsCount) {
                    val sampleIdx = (centerIdx + i).coerceIn(0, audioPeaks.lastIndex)
                    val r = radius + audioPeaks[sampleIdx] * 120f
                    val angle = i * angleStep
                    val x = centerX + (r * cos(angle)).toFloat()
                    val y = centerY + (r * sin(angle)).toFloat()
                    points.add(SpectrumPoint(x, y))
                }
            }

            VisualizerType.WAVEFORM -> {
                val stepX = 800f / bandsCount
                val startX = centerX - 400f
                for (i in 0 until bandsCount) {
                    val sampleIdx = (centerIdx + i - bandsCount / 2).coerceIn(0, audioPeaks.lastIndex)
                    val waveY = centerY + (audioPeaks[sampleIdx] - 0.5f) * 160f
                    val x = startX + i * stepX
                    points.add(SpectrumPoint(x, waveY))
                }
            }
        }

        return points
    }
}
