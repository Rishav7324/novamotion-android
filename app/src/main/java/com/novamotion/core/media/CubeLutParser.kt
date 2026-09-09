package com.novamotion.core.media

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Professional 3D LUT (.cube) Parser and Procedural Cinema Grade Generator.
 * Parses industry standard Adobe / DaVinci Resolve .cube files and prepares
 * direct byte buffers for OpenGL ES 3.0 3D texture uploads (GL_TEXTURE_3D).
 */
data class Lut3DData(
    val title: String,
    val size: Int,
    val data: ByteBuffer
)

enum class BuiltInLutPreset(val displayName: String) {
    HOLLYWOOD_TEAL_ORANGE("Hollywood Teal & Orange"),
    KODAK_PORTRA("Kodak Portra 400 Film"),
    CYBERPUNK_NEON("Cyberpunk Neon Noir"),
    CINEMA_NOIR_BW("High Contrast Noir (B&W)"),
    BLEACH_BYPASS("Bleach Bypass Action")
}

object CubeLutParser {

    /**
     * Parses an Adobe / DaVinci Resolve .cube file from an input stream.
     */
    fun parseCubeStream(inputStream: InputStream): Lut3DData {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var title = "Custom LUT"
        var size = 0
        val rgbValues = mutableListOf<Float>()

        reader.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                return@forEachLine
            }

            val upper = line.uppercase()
            when {
                upper.startsWith("TITLE") -> {
                    title = line.substringAfter("TITLE").replace("\"", "").trim()
                }
                upper.startsWith("LUT_3D_SIZE") -> {
                    size = line.substringAfter("LUT_3D_SIZE").trim().toIntOrNull() ?: 32
                }
                upper.startsWith("LUT_1D_SIZE") -> {
                    // 1D LUT not supported in 3D sampler
                }
                else -> {
                    val tokens = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    if (tokens.size >= 3) {
                        val r = tokens[0].toFloatOrNull()
                        val g = tokens[1].toFloatOrNull()
                        val b = tokens[2].toFloatOrNull()
                        if (r != null && g != null && b != null) {
                            rgbValues.add(r.coerceIn(0f, 1f))
                            rgbValues.add(g.coerceIn(0f, 1f))
                            rgbValues.add(b.coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }

        if (size == 0) {
            val triplets = rgbValues.size / 3
            size = Math.cbrt(triplets.toDouble()).toInt().coerceAtLeast(16)
        }

        val totalBytes = size * size * size * 4
        val byteBuffer = ByteBuffer.allocateDirect(totalBytes).order(ByteOrder.nativeOrder())

        val totalPoints = size * size * size
        for (i in 0 until totalPoints) {
            val rIndex = i * 3
            if (rIndex + 2 < rgbValues.size) {
                val r = (rgbValues[rIndex] * 255f).toInt().coerceIn(0, 255).toByte()
                val g = (rgbValues[rIndex + 1] * 255f).toInt().coerceIn(0, 255).toByte()
                val b = (rgbValues[rIndex + 2] * 255f).toInt().coerceIn(0, 255).toByte()
                byteBuffer.put(r)
                byteBuffer.put(g)
                byteBuffer.put(b)
                byteBuffer.put(255.toByte()) // Alpha
            } else {
                byteBuffer.put(0.toByte())
                byteBuffer.put(0.toByte())
                byteBuffer.put(0.toByte())
                byteBuffer.put(255.toByte())
            }
        }
        byteBuffer.position(0)

        return Lut3DData(title = title, size = size, data = byteBuffer)
    }

    /**
     * Generates a procedural 3D LUT directly into memory without requiring external assets.
     */
    fun generatePreset(preset: BuiltInLutPreset, size: Int = 32): Lut3DData {
        val totalBytes = size * size * size * 4
        val buffer = ByteBuffer.allocateDirect(totalBytes).order(ByteOrder.nativeOrder())

        for (b in 0 until size) {
            val blueIn = b.toFloat() / (size - 1)
            for (g in 0 until size) {
                val greenIn = g.toFloat() / (size - 1)
                for (r in 0 until size) {
                    val redIn = r.toFloat() / (size - 1)

                    val (rOut, gOut, bOut) = applyPresetGrading(preset, redIn, greenIn, blueIn)

                    buffer.put((rOut * 255f).toInt().coerceIn(0, 255).toByte())
                    buffer.put((gOut * 255f).toInt().coerceIn(0, 255).toByte())
                    buffer.put((bOut * 255f).toInt().coerceIn(0, 255).toByte())
                    buffer.put(255.toByte())
                }
            }
        }
        buffer.position(0)
        return Lut3DData(title = preset.displayName, size = size, data = buffer)
    }

    private fun applyPresetGrading(
        preset: BuiltInLutPreset,
        r: Float,
        g: Float,
        b: Float
    ): Triple<Float, Float, Float> {
        val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b

        return when (preset) {
            BuiltInLutPreset.HOLLYWOOD_TEAL_ORANGE -> {
                val shadowFactor = (1f - luma).pow(1.5f)
                val highlightFactor = luma.pow(1.2f)

                val outR = min(1f, r * 1.15f + highlightFactor * 0.15f)
                val outG = (g * 0.95f + shadowFactor * 0.08f + highlightFactor * 0.05f).coerceIn(0f, 1f)
                val outB = (b * 0.85f + shadowFactor * 0.22f).coerceIn(0f, 1f)
                Triple(outR, outG, outB)
            }
            BuiltInLutPreset.KODAK_PORTRA -> {
                val liftedR = (r * 0.92f + 0.04f).pow(0.95f)
                val liftedG = (g * 0.90f + 0.03f).pow(0.98f)
                val liftedB = (b * 0.84f + 0.03f).pow(1.05f)
                Triple(liftedR.coerceIn(0f, 1f), liftedG.coerceIn(0f, 1f), liftedB.coerceIn(0f, 1f))
            }
            BuiltInLutPreset.CYBERPUNK_NEON -> {
                val contrastLuma = if (luma < 0.5f) 2f * luma * luma else 1f - 2f * (1f - luma) * (1f - luma)
                val outR = (r * 1.25f - g * 0.1f + contrastLuma * 0.1f).coerceIn(0f, 1f)
                val outG = (g * 0.75f + b * 0.2f).coerceIn(0f, 1f)
                val outB = (b * 1.35f + (1f - luma) * 0.2f).coerceIn(0f, 1f)
                Triple(outR, outG, outB)
            }
            BuiltInLutPreset.CINEMA_NOIR_BW -> {
                val sCurve = if (luma < 0.5f) 2f * luma.pow(1.4f) else 1f - 2f * (1f - luma).pow(1.4f)
                val finalLuma = sCurve.coerceIn(0f, 1f)
                Triple(finalLuma, finalLuma, finalLuma)
            }
            BuiltInLutPreset.BLEACH_BYPASS -> {
                val desatR = r * 0.4f + luma * 0.6f
                val desatG = g * 0.4f + luma * 0.6f
                val desatB = b * 0.4f + luma * 0.6f
                val contrastR = if (desatR < 0.5f) 2f * desatR * desatR else 1f - 2f * (1f - desatR) * (1f - desatR)
                val contrastG = if (desatG < 0.5f) 2f * desatG * desatG else 1f - 2f * (1f - desatG) * (1f - desatG)
                val contrastB = if (desatB < 0.5f) 2f * desatB * desatB else 1f - 2f * (1f - desatB) * (1f - desatB)
                Triple(contrastR.coerceIn(0f, 1f), contrastG.coerceIn(0f, 1f), contrastB.coerceIn(0f, 1f))
            }
        }
    }
}
