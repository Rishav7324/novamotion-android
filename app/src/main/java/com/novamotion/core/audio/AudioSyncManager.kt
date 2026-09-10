package com.novamotion.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * Production AudioSyncManager.
 *
 * Key fixes from original stub:
 *  - seekTo(Long) instead of seekTo(Int) — avoids overflow on files > 35 min
 *  - Drift correction: if audio position drifts > DRIFT_THRESHOLD_MS from
 *    the authoritative playhead clock, it re-seeks to realign
 *  - prepareAsync + isReady guard (original was correct, kept)
 *  - getCurrentPositionMs() exposed for drift checking
 */
class AudioSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioSyncManager"
        /** Maximum allowed drift before forcing audio re-seek (in ms) */
        private const val DRIFT_THRESHOLD_MS = 150L
    }

    private var mediaPlayer: MediaPlayer? = null
    var isReady: Boolean = false
        private set

    fun loadAudio(uri: Uri, onPrepared: () -> Unit = {}) {
        release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                setOnPreparedListener {
                    isReady = true
                    onPrepared()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load audio: $uri", e)
        }
    }

    fun play(fromMs: Long) {
        val player = mediaPlayer ?: return
        if (!isReady) return
        try {
            player.seekTo(fromMs, MediaPlayer.SEEK_CLOSEST)
            player.start()
        } catch (e: Exception) {
            Log.e(TAG, "play() failed", e)
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) player.pause()
        } catch (e: Exception) {
            Log.e(TAG, "pause() failed", e)
        }
    }

    /**
     * Seek to timeMs. Use Long overload to avoid Int truncation.
     */
    fun seekTo(timeMs: Long) {
        val player = mediaPlayer ?: return
        if (!isReady) return
        try {
            player.seekTo(timeMs, MediaPlayer.SEEK_CLOSEST)
        } catch (e: Exception) {
            Log.e(TAG, "seekTo($timeMs) failed", e)
        }
    }

    /**
     * Called each frame by the playback clock to detect and correct drift.
     * If the gap between audio and the authoritative playhead exceeds
     * DRIFT_THRESHOLD_MS, forces a re-seek.
     *
     * @param authoritativeMs The Choreographer-based playhead position (ground truth)
     */
    fun correctDriftIfNeeded(authoritativeMs: Long) {
        val player = mediaPlayer ?: return
        if (!isReady) return
        try {
            if (!player.isPlaying) return
            val audioMs = player.currentPosition.toLong()
            val drift = kotlin.math.abs(audioMs - authoritativeMs)
            if (drift > DRIFT_THRESHOLD_MS) {
                Log.d(TAG, "Audio drift detected: audio=$audioMs authoritative=$authoritativeMs drift=${drift}ms — correcting")
                player.seekTo(authoritativeMs, MediaPlayer.SEEK_CLOSEST)
            }
        } catch (e: Exception) {
            Log.e(TAG, "drift correction failed", e)
        }
    }

    /** Returns the current audio playback position in ms, or 0 if not ready. */
    fun getCurrentPositionMs(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    val isPlaying: Boolean
        get() = try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }

    fun release() {
        try { mediaPlayer?.stop() } catch (ignored: Exception) {}
        try { mediaPlayer?.release() } catch (ignored: Exception) {}
        mediaPlayer = null
        isReady = false
    }
}
