package com.novamotion.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * Production audio playback engine wrapping Android MediaPlayer.
 *
 * Key fixes:
 *  - seekTo uses Long overload (API 26+) to avoid Int truncation on long files
 *  - prepare() failure is logged, not silently swallowed
 *  - isPlaying guard prevents IllegalStateException on stop/release calls
 */
class AudioPlaybackEngine(private val context: Context) {

    private val TAG = "AudioPlaybackEngine"

    private var mediaPlayer: MediaPlayer? = null
    private var currentUri: String? = null
    private var isPrepared = false

    fun loadAudio(uriString: String) {
        if (currentUri == uriString && mediaPlayer != null && isPrepared) return
        release()
        try {
            currentUri = uriString
            isPrepared = false
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uriString))
                prepare()
                isPrepared = true
                // Ensure volume max
                setVolume(1f, 1f)
            }
            Log.i(TAG, "Audio loaded: $uriString isPrepared=$isPrepared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load audio: $uriString", e)
            mediaPlayer = null
            isPrepared = false
        }
    }

    /**
     * Start playback from the specified position.
     * @param fromMs Position in milliseconds (Long, no Int overflow)
     */
    fun play(fromMs: Long) {
        val player = mediaPlayer ?: return
        if (!isPrepared) return
        try {
            // Use Long-based seekTo to avoid truncation for files > ~35 minutes
            player.seekTo(fromMs, MediaPlayer.SEEK_CLOSEST)
            player.start()
        } catch (e: Exception) {
            Log.e(TAG, "play() failed", e)
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "pause() failed", e)
        }
    }

    /**
     * Seek to position without changing play state.
     * @param ms Position in milliseconds (Long, no Int overflow)
     */
    fun seekTo(ms: Long) {
        val player = mediaPlayer ?: return
        if (!isPrepared) return
        try {
            player.seekTo(ms, MediaPlayer.SEEK_CLOSEST)
        } catch (e: Exception) {
            Log.e(TAG, "seekTo($ms) failed", e)
        }
    }

    /**
     * Checks drift against authoritative playhead clock.
     * If drift exceeds 200ms, resyncs audio to authoritative position.
     */
    fun correctDriftIfNeeded(authoritativeMs: Long) {
        val player = mediaPlayer ?: return
        if (!isPrepared) return
        try {
            if (!player.isPlaying) return
            val audioMs = player.currentPosition.toLong()
            val drift = kotlin.math.abs(audioMs - authoritativeMs)
            if (drift > 200L) {
                Log.d(TAG, "Audio drift: audio=$audioMs authoritative=$authoritativeMs (drift=${drift}ms) — re-syncing")
                player.seekTo(authoritativeMs, MediaPlayer.SEEK_CLOSEST)
            }
        } catch (e: Exception) {
            Log.e(TAG, "correctDriftIfNeeded failed", e)
        }
    }

    /** Returns current playback position in milliseconds. */
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
        try {
            mediaPlayer?.stop()
        } catch (ignored: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (ignored: Exception) {}
        mediaPlayer = null
        currentUri = null
        isPrepared = false
    }
}
