package com.novamotion.ui.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.novamotion.core.model.Layer
import com.novamotion.core.model.LayerType

object AssetPickerHelper {

    fun createLayerFromMediaUri(
        context: Context,
        uri: Uri,
        type: LayerType,
        startTimeMs: Long = 0L
    ): Layer {
        var durationMs = 5000L
        var layerName = if (type == LayerType.VIDEO) "Video Clip" else if (type == LayerType.AUDIO) "Audio Track" else "Photo Layer"

        try {
            // Persist read access across reboots — only works with ACTION_OPEN_DOCUMENT
            // (GetContent URIs are transient). Use incoming intent flags if available.
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            // Verify provider offers persistable permission before calling
            val persisted = context.contentResolver.persistedUriPermissions
            val already = persisted.any { it.uri == uri && it.isReadPermission }
            if (!already) {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        } catch (ignored: Exception) {
            // Fallback for non-DocumentsProvider: copy to cache for durable access (best-effort)
            try {
                val input = context.contentResolver.openInputStream(uri)
                if (input != null) {
                    val cacheFile = java.io.File(context.cacheDir, "import_${System.currentTimeMillis()}_${uri.lastPathSegment ?: "media"}")
                    input.use { ins ->
                        cacheFile.outputStream().use { out -> ins.copyTo(out) }
                    }
                }
            } catch (_: Exception) {}
        }

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durStr != null) {
                durationMs = durStr.toLongOrNull() ?: 5000L
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (!title.isNullOrBlank()) {
                layerName = title
            }
            retriever.release()
        } catch (ignored: Exception) {}

        return Layer(
            name = layerName,
            type = type,
            startTimeMs = startTimeMs,
            durationMs = durationMs.coerceAtLeast(1000L),
            mediaUri = uri.toString()
        )
    }
}
