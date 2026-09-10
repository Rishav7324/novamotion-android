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

    // LRU cache bounded by ~32 entries to prevent OOM on long sessions.
    // Evicted entries delete their GL textures. Uses LinkedHashMap access-order.
    private const val MAX_IMAGE_CACHE_SIZE = 32
    private val imageCache = object : LinkedHashMap<String, TextureResult>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, TextureResult>?): Boolean {
            if (size > MAX_IMAGE_CACHE_SIZE) {
                eldest?.value?.let { evicted ->
                    try { GLES30.glDeleteTextures(1, intArrayOf(evicted.textureId), 0) } catch (_: Exception) {}
                }
                return true
            }
            return false
        }
    }

    private const val MAX_TEXTURE_DIMENSION = 2048 // Cap to avoid GPU OOM on 48MP images

    /**
     * Load a static image texture from a content/file URI.
     * Use this for LayerType.IMAGE only.
     */
    fun loadImageTexture(context: Context, uriString: String): TextureResult? {
        synchronized(imageCache) { imageCache[uriString]?.let { return it } }

        val bitmap: Bitmap? = try {
            decodeSampledBitmap(context, uriString, MAX_TEXTURE_DIMENSION, MAX_TEXTURE_DIMENSION)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode image: $uriString", e)
            null
        }

        if (bitmap == null) {
            return getOrCreateFallbackTexture()
        }

        val result = uploadBitmapToGpu(bitmap, recycleAfterUpload = true) ?: getOrCreateFallbackTexture()
        synchronized(imageCache) { imageCache[uriString] = result }
        return result
    }

    /**
     * Efficient sampled decode — 2-pass: first justBounds to get dimensions,
     * then calculate power-of-2 inSampleSize capped to MAX_TEXTURE_DIMENSION.
     * Prevents OOM on 48MP photos (official docs: developer.android.com/topic/performance/graphics/load-bitmap).
     */
    private fun decodeSampledBitmap(context: Context, uriString: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val uri = Uri.parse(uriString)
        // Pass 1: bounds only
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
        } catch (_: Exception) {}
        // If bounds failed, fallback to direct decode
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
        val sampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        // Need fresh stream for second decode (inputStream is consumed)
        var bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null
        // Fix EXIF rotation (photo ulta) — read orientation and rotate bitmap
        try {
            context.contentResolver.openInputStream(uri)?.use { exifStream ->
                val exif = androidx.exifinterface.media.ExifInterface(exifStream)
                val orientation = exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                )
                val matrix = android.graphics.Matrix()
                when (orientation) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    else -> {}
                }
                if (!matrix.isIdentity) {
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rotated != bitmap) {
                        bitmap.recycle()
                        bitmap = rotated
                    }
                }
            }
        } catch (_: Exception) {}
        return bitmap
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfH = height / 2
            val halfW = width / 2
            while (halfH / inSampleSize >= reqHeight && halfW / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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
        synchronized(imageCache) { imageCache[cacheKey]?.let { return it } }

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
        synchronized(imageCache) { imageCache[cacheKey] = result }
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
        synchronized(imageCache) {
            val texIds = imageCache.values.map { it.textureId }.toIntArray()
            if (texIds.isNotEmpty()) {
                try { GLES30.glDeleteTextures(texIds.size, texIds, 0) } catch (_: Exception) {}
            }
            imageCache.clear()
        }
        fallbackTexture?.let { fb ->
            try { GLES30.glDeleteTextures(1, intArrayOf(fb.textureId), 0) } catch (_: Exception) {}
            fallbackTexture = null
        }
    }
}
