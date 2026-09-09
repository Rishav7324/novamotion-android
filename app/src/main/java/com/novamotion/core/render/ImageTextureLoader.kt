package com.novamotion.core.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.opengl.GLES30
import android.opengl.GLUtils
import java.io.InputStream

object ImageTextureLoader {

    private val mediaCache = mutableMapOf<String, TextureResult>()

    fun loadTextureFromUri(context: Context, uriString: String, timeUs: Long = 0L): TextureResult? {
        val cacheKey = "$uriString-$timeUs"
        val existing = mediaCache[cacheKey]
        if (existing != null) {
            return existing
        }

        val bitmap: Bitmap? = try {
            val uri = Uri.parse(uriString)
            val scheme = uri.scheme
            if (uriString.endsWith(".mp4", ignoreCase = true) ||
                uriString.endsWith(".mov", ignoreCase = true) ||
                uriString.endsWith(".mkv", ignoreCase = true) ||
                (scheme != null && scheme.startsWith("content"))) {
                // Try video frame retriever first
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    // Fall back to image stream
                    val stream: InputStream? = context.contentResolver.openInputStream(uri)
                    stream?.use { BitmapFactory.decodeStream(it) }
                } finally {
                    try { retriever.release() } catch (ignored: Exception) {}
                }
            } else {
                val stream: InputStream? = context.contentResolver.openInputStream(uri)
                stream?.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            null
        }

        if (bitmap == null) return null

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

        val result = TextureResult(texId, bitmap.width, bitmap.height)
        mediaCache[cacheKey] = result
        bitmap.recycle()
        return result
    }

    fun clearCache() {
        for (tex in mediaCache.values) {
            GLES30.glDeleteTextures(1, intArrayOf(tex.textureId), 0)
        }
        mediaCache.clear()
    }
}
