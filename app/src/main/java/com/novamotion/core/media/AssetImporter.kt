package com.novamotion.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.novamotion.core.model.Layer
import com.novamotion.core.model.LayerTransform
import com.novamotion.core.model.LayerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class MediaMetadata(
    val uri: Uri,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val mimeType: String?,
    val thumbnailPath: String?
)

object AssetImporter {

    suspend fun importVideo(context: Context, uri: Uri): Layer = withContext(Dispatchers.IO) {
        val metadata = extractMetadata(context, uri)
        val name = uri.lastPathSegment ?: "Video Clip"
        // Handle video rotation metadata (90/270) — apply to layer transform
        val videoRotation = metadata.rotation.toFloat()
        val transform = if (videoRotation != 0f) {
            LayerTransform(rotation = com.novamotion.core.model.AnimatableProperty(videoRotation))
        } else LayerTransform()

        Layer(
            name = name,
            type = LayerType.VIDEO,
            startTimeMs = 0L,
            durationMs = metadata.durationMs.coerceAtLeast(1000L),
            mediaUri = uri.toString(),
            transform = transform
        )
    }

    suspend fun importAudio(context: Context, uri: Uri): Layer = withContext(Dispatchers.IO) {
        val metadata = extractMetadata(context, uri)
        val name = uri.lastPathSegment ?: "Audio Track"

        Layer(
            name = name,
            type = LayerType.AUDIO,
            startTimeMs = 0L,
            durationMs = metadata.durationMs.coerceAtLeast(1000L),
            mediaUri = uri.toString(),
            transform = LayerTransform()
        )
    }

    suspend fun importImage(context: Context, uri: Uri): Layer = withContext(Dispatchers.IO) {
        val name = uri.lastPathSegment ?: "Image Layer"

        Layer(
            name = name,
            type = LayerType.IMAGE,
            startTimeMs = 0L,
            durationMs = 5000L, // Default 5s for static image
            mediaUri = uri.toString(),
            transform = LayerTransform()
        )
    }

    private fun extractMetadata(context: Context, uri: Uri): MediaMetadata {
        val retriever = MediaMetadataRetriever()
        var durationMs = 5000L
        var width = 1920
        var height = 1080
        var rotation = 0
        var mimeType: String? = null
        var thumbnailPath: String? = null

        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: 5000L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            width = widthStr?.toIntOrNull() ?: 1920
            height = heightStr?.toIntOrNull() ?: 1080

            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            rotation = rotationStr?.toIntOrNull() ?: 0

            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            // Save thumbnail bitmap
            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (frame != null) {
                val thumbFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    frame.compress(Bitmap.CompressFormat.JPEG, 75, out)
                }
                thumbnailPath = thumbFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }

        return MediaMetadata(
            uri = uri,
            durationMs = durationMs,
            width = width,
            height = height,
            rotation = rotation,
            mimeType = mimeType,
            thumbnailPath = thumbnailPath
        )
    }
}
