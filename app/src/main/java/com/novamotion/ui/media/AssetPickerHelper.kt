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
            // Persist read access across app restarts for saved project reload
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (ignored: Exception) {}

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
