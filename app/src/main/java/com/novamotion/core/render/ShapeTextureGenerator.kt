package com.novamotion.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlin.math.cos
import kotlin.math.sin

object ShapeTextureGenerator {

    private val shapeCache = mutableMapOf<String, TextureResult>()

    fun getOrCreateShapeTexture(
        shapeType: String,
        fillColorLong: Long = 0xFF6366F1,
        strokeColorLong: Long = 0xFF00F0FF,
        strokeWidth: Float = 4f,
        width: Int = 256,
        height: Int = 256
    ): TextureResult {
        val cacheKey = "$shapeType-$fillColorLong-$strokeColorLong-$strokeWidth-$width-$height"
        val existing = shapeCache[cacheKey]
        if (existing != null) {
            return existing
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        bitmap.eraseColor(Color.TRANSPARENT)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColorLong.toInt()
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = strokeColorLong.toInt()
        }

        val pad = strokeWidth + 4f
        val rect = RectF(pad, pad, width.toFloat() - pad, height.toFloat() - pad)

        when (shapeType.uppercase()) {
            "CIRCLE" -> {
                val cx = width / 2f
                val cy = height / 2f
                val radius = (width - pad * 2) / 2f
                canvas.drawCircle(cx, cy, radius, fillPaint)
                if (strokeWidth > 0) canvas.drawCircle(cx, cy, radius, strokePaint)
            }
            "STAR" -> {
                val starPath = createStarPath(width / 2f, height / 2f, 5, (width - pad * 2) / 2f, (width - pad * 2) / 4.5f)
                canvas.drawPath(starPath, fillPaint)
                if (strokeWidth > 0) canvas.drawPath(starPath, strokePaint)
            }
            "POLYGON" -> {
                val polyPath = createPolygonPath(width / 2f, height / 2f, 6, (width - pad * 2) / 2f)
                canvas.drawPath(polyPath, fillPaint)
                if (strokeWidth > 0) canvas.drawPath(polyPath, strokePaint)
            }
            "HEART" -> {
                val heartPath = createHeartPath(rect)
                canvas.drawPath(heartPath, fillPaint)
                if (strokeWidth > 0) canvas.drawPath(heartPath, strokePaint)
            }
            else -> { // RECTANGLE / ROUNDED_RECTANGLE
                val radius = 24f
                canvas.drawRoundRect(rect, radius, radius, fillPaint)
                if (strokeWidth > 0) canvas.drawRoundRect(rect, radius, radius, strokePaint)
            }
        }

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

        val result = TextureResult(texId, width, height)
        shapeCache[cacheKey] = result
        return result
    }

    private fun createStarPath(cx: Float, cy: Float, points: Int, outerRadius: Float, innerRadius: Float): Path {
        val path = Path()
        val angleStep = Math.PI / points
        var angle = -Math.PI / 2.0

        path.moveTo(
            (cx + outerRadius * cos(angle)).toFloat(),
            (cy + outerRadius * sin(angle)).toFloat()
        )

        for (i in 0 until points * 2) {
            val r = if (i % 2 == 1) outerRadius else innerRadius
            angle += angleStep
            path.lineTo(
                (cx + r * cos(angle)).toFloat(),
                (cy + r * sin(angle)).toFloat()
            )
        }
        path.close()
        return path
    }

    private fun createPolygonPath(cx: Float, cy: Float, sides: Int, radius: Float): Path {
        val path = Path()
        val angleStep = 2.0 * Math.PI / sides
        var angle = -Math.PI / 2.0

        path.moveTo(
            (cx + radius * cos(angle)).toFloat(),
            (cy + radius * sin(angle)).toFloat()
        )

        for (i in 1 until sides) {
            angle += angleStep
            path.lineTo(
                (cx + radius * cos(angle)).toFloat(),
                (cy + radius * sin(angle)).toFloat()
            )
        }
        path.close()
        return path
    }

    private fun createHeartPath(rect: RectF): Path {
        val path = Path()
        val w = rect.width()
        val h = rect.height()
        val x0 = rect.left
        val y0 = rect.top

        path.moveTo(x0 + 0.5f * w, y0 + 0.85f * h)
        path.cubicTo(
            x0 + 0.1f * w, y0 + 0.55f * h,
            x0, y0 + 0.25f * h,
            x0 + 0.25f * w, y0 + 0.1f * h
        )
        path.cubicTo(
            x0 + 0.45f * w, y0 + 0.05f * h,
            x0 + 0.5f * w, y0 + 0.25f * h,
            x0 + 0.5f * w, y0 + 0.25f * h
        )
        path.cubicTo(
            x0 + 0.5f * w, y0 + 0.25f * h,
            x0 + 0.55f * w, y0 + 0.05f * h,
            x0 + 0.75f * w, y0 + 0.1f * h
        )
        path.cubicTo(
            x0 + w, y0 + 0.25f * h,
            x0 + 0.9f * w, y0 + 0.55f * h,
            x0 + 0.5f * w, y0 + 0.85f * h
        )
        path.close()
        return path
    }

    fun clearCache() {
        for (tex in shapeCache.values) {
            GLES30.glDeleteTextures(1, intArrayOf(tex.textureId), 0)
        }
        shapeCache.clear()
    }
}
