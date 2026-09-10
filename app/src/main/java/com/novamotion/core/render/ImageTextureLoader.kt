package com.novamotion.core.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import java.io.InputStream

/**
 * Loads GPU textures from image URIs using BitmapFactory.
 *
 * VIDEO layers should NOT go through this class — they need ExoPlayer +
 * SurfaceTexture (OES) for actual frame-accurate playback. This class handles:
 *   - IMAGE layers (static bitmaps)
 *   - Thumbnail extraction for import (1 frame per video, at time=0 only)
 *
 * Key production fixes:
 *  - Removed per-timeUs video frame caching (was an infinite memory leak)
 *  - No more getFrameAtTime for playback (only for import thumbnail @ t=0)
 *  - Image textures cached by URI string only (safe, bounded)
 */
object ImageTextureLoader {

    private val TAG = "ImageTextureLoader"

    // Keyed by URI string only — NOT by timeUs (safe, bounded cache)
    private val imageCache = mutableMapOf<String, TextureResult>()

    /**
     * Load a static image texture from a content/file URI.
     * Use this for LayerType.IMAGE only.
     */
    fun loadImageTexture(context: Context, uriString: String): TextureResult? {
        imageCache[uriString]?.let { return it }

        val bitmap: Bitmap? = try {
            val uri = Uri.parse(uriString)
            val stream: InputStream? = context.contentResolver.openInputStream(uri)
            stream?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode image: $uriString", e)
            null
        }

        if (bitmap == null) {
            return getOrCreateFallbackTexture()
        }

        val result = uploadBitmapToGpu(bitmap, recycleAfterUpload = true) ?: getOrCreateFallbackTexture()
        imageCache[uriString] = result
        return result
    }

    private var fallbackTexture: TextureResult? = null

    /**
     * Generates a safe fallback placeholder texture (64x64) if a media file was deleted from disk.
     */
    fun getOrCreateFallbackTexture(): TextureResult {
        fallbackTexture?.let { return it }
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint()
        paint.color = 0xFF2A2A2E.toInt()
        canvas.drawRect(0f, 0f, 64f, 64f, paint)
        paint.color = 0xFF6366F1.toInt()
        canvas.drawRect(8f, 8f, 56f, 56f, paint)
        val res = uploadBitmapToGpu(bmp, recycleAfterUpload = true) ?: TextureResult(0, 64, 64)
        fallbackTexture = res
        return res
    }

    /**
     * Extract a single thumbnail frame from a video for display in the
     * import picker / layer list. NOT for playback. Only called at t=0.
     *
     * Returns null if extraction fails (don't crash — just show placeholder).
     */
    fun extractVideoThumbnail(context: Context, uriString: String): TextureResult? {
        val cacheKey = "thumb:$uriString"
        imageCache[cacheKey]?.let { return it }

        val bitmap: Bitmap? = try {
            val uri = Uri.parse(uriString)
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            // At time=0, nearest sync — only used for thumbnail, not playback
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            frame
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract video thumbnail: $uriString", e)
            null
        }

        if (bitmap == null) return null
        val result = uploadBitmapToGpu(bitmap, recycleAfterUpload = true) ?: return null
        imageCache[cacheKey] = result
        return result
    }

    /**
     * Upload a Bitmap to a new GLES30 2D texture.
     */
    private fun uploadBitmapToGpu(bitmap: Bitmap, recycleAfterUpload: Boolean): TextureResult? {
        return try {
            val texIds = IntArray(1)
            GLES30.glGenTextures(1, texIds, 0)
            val texId = texIds[0]
            if (texId == 0) return null

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

            val w = bitmap.width
            val h = bitmap.height
            if (recycleAfterUpload) bitmap.recycle()

            TextureResult(texId, w, h)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload bitmap to GPU", e)
            null
        }
    }

    /**
     * Legacy entry point — kept for compatibility with SceneRenderer.
     * Routes IMAGE layers to loadImageTexture, VIDEO layers return null
     * (caller should use VideoSurfaceTextureManager instead).
     *
     * @param timeUs ignored for images; VIDEO should not call this
     */
    fun loadTextureFromUri(context: Context, uriString: String, timeUs: Long = 0L): TextureResult? {
        val uri = Uri.parse(uriString)
        val mime = context.contentResolver.getType(uri)
        return if (mime?.startsWith("video/") == true) {
            // VIDEO layers must be decoded by ExoPlayer + SurfaceTexture, not here.
            // Return null so SceneRenderer can fall back to VideoSurfaceTextureManager.
            null
        } else {
            loadImageTexture(context, uriString)
        }
    }

    fun clearCache() {
        val texIds = imageCache.values.map { it.textureId }.toIntArray()
        if (texIds.isNotEmpty()) {
            GLES30.glDeleteTextures(texIds.size, texIds, 0)
        }
        imageCache.clear()
    }
}
