package com.novamotion.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlin.math.max

data class TextureResult(
    val textureId: Int,
    val width: Int,
    val height: Int
)

object TextTextureGenerator {

    private val textureCache = mutableMapOf<String, TextureResult>()

    /**
     * Renders a text string with styling into an OpenGL ES 2D texture.
     */
    fun getOrCreateTextTexture(
        text: String,
        colorLong: Long = 0xFFFFFFFF,
        fontSizeSp: Float = 48f,
        isBold: Boolean = true
    ): TextureResult {
        val cacheKey = "$text-$colorLong-$fontSizeSp-$isBold"
        val existing = textureCache[cacheKey]
        if (existing != null) {
            return existing
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizeSp * 2.0f // Scale for crisp high-DPI rendering
            color = colorLong.toInt()
            typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
            // Subtle drop shadow for readability
            setShadowLayer(4f, 2f, 2f, 0x88000000.toInt())
        }

        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val padding = 20
        val bitmapWidth = max(64, bounds.width() + padding * 2)
        val fontMetrics = paint.fontMetrics
        val textHeight = (fontMetrics.bottom - fontMetrics.top).toInt()
        val bitmapHeight = max(64, textHeight + padding * 2)

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        bitmap.eraseColor(Color.TRANSPARENT)

        val y = padding - fontMetrics.top
        canvas.drawText(text, padding.toFloat(), y, paint)

        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        val texId = texIds[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

        bitmap.recycle()

        val result = TextureResult(texId, bitmapWidth, bitmapHeight)
        textureCache[cacheKey] = result
        return result
    }

    fun clearCache() {
        for (tex in textureCache.values) {
            GLES30.glDeleteTextures(1, intArrayOf(tex.textureId), 0)
        }
        textureCache.clear()
    }
}
