package com.novamotion.core.text

import android.graphics.*

object TextTextureGenerator {

    /**
     * Renders kinetic styled text to a high-DPI Bitmap for OpenGL texture binding.
     */
    fun renderTextToBitmap(style: KineticTextStyle, charTransforms: List<CharTransform>): Bitmap {
        val width = 1024
        val height = 256
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = style.fontSize
            color = style.fillColor.toInt()
            isFakeBoldText = style.isBold
            letterSpacing = style.letterSpacing
            if (style.shadowRadius > 0f) {
                setShadowLayer(style.shadowRadius, style.shadowDx, style.shadowDy, style.shadowColor.toInt())
            }
        }

        // Measure base text width
        var currentX = 60f
        val baseY = height / 2f + style.fontSize / 3f

        for (t in charTransforms) {
            if (t.opacity <= 0.01f) {
                currentX += textPaint.measureText(" ")
                continue
            }

            textPaint.alpha = (t.opacity * 255).toInt().coerceIn(0, 255)
            val charStr = t.charToDisplay.toString()
            val charW = textPaint.measureText(charStr)

            canvas.save()
            canvas.translate(currentX + charW / 2f, baseY + t.offsetY)
            canvas.scale(t.scale, t.scale)
            canvas.drawText(charStr, -charW / 2f, 0f, textPaint)
            canvas.restore()

            currentX += charW
        }

        return bitmap
    }
}
