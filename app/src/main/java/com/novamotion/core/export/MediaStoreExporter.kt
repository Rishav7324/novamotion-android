package com.novamotion.core.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream

/**
 * Saves an MP4 export file to the system MediaStore so it appears in the
 * device Gallery and is accessible via DCIM/Movies or Movies folder.
 *
 * API 29+ uses MediaStore.Video.Media with PENDING flag (no legacy permission needed).
 * API 26-28 copies to getExternalFilesDir(DIRECTORY_MOVIES) and inserts into MediaStore.
 */
object MediaStoreExporter {

    private const val TAG = "MediaStoreExporter"

    /**
     * Saves the given [sourceFile] to the device's Movies media store.
     *
     * @param context Application context
     * @param sourceFile The temporary MP4 file written by HardwareVideoEncoder
     * @param displayName Filename to show in Gallery (e.g. "NovaMotion_12345.mp4")
     * @return The content URI of the saved file, or null on failure
     */
    fun saveToGallery(
        context: Context,
        sourceFile: File,
        displayName: String = "NovaMotion_${System.currentTimeMillis()}.mp4"
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStoreQ(context, sourceFile, displayName)
        } else {
            saveToMediaStoreLegacy(context, sourceFile, displayName)
        }
    }

    // ─── API 29+ ──────────────────────────────────────────────────────────────

    private fun saveToMediaStoreQ(context: Context, sourceFile: File, displayName: String): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NovaMotion")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return null

            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }

            // Mark as no longer pending — makes it visible to gallery apps
            val updateValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(uri, updateValues, null, null)

            Log.i(TAG, "Saved to MediaStore (API 29+): $uri")
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to MediaStore (API 29+)", e)
            null
        }
    }

    // ─── API 26-28 ────────────────────────────────────────────────────────────

    private fun saveToMediaStoreLegacy(context: Context, sourceFile: File, displayName: String): Uri? {
        return try {
            // Copy to public Movies directory
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val novaDir = File(moviesDir, "NovaMotion").also { it.mkdirs() }
            val destFile = File(novaDir, displayName)
            sourceFile.copyTo(destFile, overwrite = true)

            // Insert into MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.TITLE, displayName)
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATA, destFile.absolutePath)
                put(MediaStore.Video.Media.SIZE, destFile.length())
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            Log.i(TAG, "Saved to MediaStore (legacy): ${destFile.absolutePath}")
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to MediaStore (legacy)", e)
            null
        }
    }
}
