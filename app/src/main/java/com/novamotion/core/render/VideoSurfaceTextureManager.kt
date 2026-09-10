package com.novamotion.core.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manages ExoPlayer → SurfaceTexture → OES GPU texture pipeline for each
 * VIDEO layer in the project.
 *
 * Architecture:
 *   ExoPlayer (decoder) → Surface → SurfaceTexture (OES GL_TEXTURE_EXTERNAL_OES)
 *   → SceneRenderer samples from OES texture each frame via updateTexImage()
 *
 * One instance per video layer URI. Lifecycle matches EditorViewModel.
 *
 * Production design:
 *  - ExoPlayer decodes on its own thread pool
 *  - updateTexImage() is called from GL thread every frame the layer is active
 *  - seekTo() routes through ExoPlayer for frame-accurate decode
 *  - No MediaMetadataRetriever.getFrameAtTime() used at all
 */
class VideoSurfaceTextureManager(
    private val context: Context,
    private val uriString: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** OES texture ID (GL_TEXTURE_EXTERNAL_OES), valid after initGl() */
    var oesTextureId: Int = 0
        private set

    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null

    private var player: ExoPlayer? = null
    private var isPlayerReady = false

    /**
     * Must be called from the GL thread after an EGL context is active.
     * Creates the OES texture and SurfaceTexture, then builds ExoPlayer.
     */
    fun initGl() {
        // 1. Generate OES texture
        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        oesTextureId = texIds[0]

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        // 2. Create SurfaceTexture targeting that OES texture
        surfaceTexture = SurfaceTexture(oesTextureId).also { st ->
            st.setDefaultBufferSize(1920, 1080) // Adjusted by ExoPlayer output
            surface = Surface(st)
        }

        // 3. Create ExoPlayer on main thread (Media3 requirement)
        scope.launch {
            buildPlayer()
        }
    }

    private fun buildPlayer() {
        val sur = surface ?: return
        val exo = ExoPlayer.Builder(context).build().apply {
            setVideoSurface(sur)
            setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
            playWhenReady = false
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        isPlayerReady = true
                    }
                }
            })
            prepare()
        }
        player = exo
    }

    /**
     * Called from GL thread each frame when this layer is active.
     * Updates the OES texture with the latest decoded video frame.
     *
     * @return true if a new frame was available
     */
    fun updateTexImage(): Boolean {
        return try {
            surfaceTexture?.updateTexImage()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get the OES texture transformation matrix for use in the shader.
     * Must be called after updateTexImage().
     */
    fun getTransformMatrix(out: FloatArray) {
        surfaceTexture?.getTransformMatrix(out)
    }

    /** Seek ExoPlayer to the given position in milliseconds. */
    fun seekTo(positionMs: Long) {
        scope.launch {
            player?.seekTo(positionMs)
        }
    }

    /** Start ExoPlayer playback. */
    fun play() {
        scope.launch {
            player?.play()
        }
    }

    /** Pause ExoPlayer playback. */
    fun pause() {
        scope.launch {
            player?.pause()
        }
    }

    /** Release all resources. Call from the owner's onCleared() or onDestroy(). */
    fun release() {
        scope.launch {
            player?.release()
            player = null
        }
        surface?.release()
        surface = null
        surfaceTexture?.release()
        surfaceTexture = null
        if (oesTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
            oesTextureId = 0
        }
    }
}
