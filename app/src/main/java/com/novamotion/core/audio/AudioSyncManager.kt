package com.novamotion.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

class AudioSyncManager(private val context: Context) {

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
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play(fromMs: Long) {
        mediaPlayer?.let { player ->
            if (isReady) {
                player.seekTo(fromMs.toInt())
                player.start()
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
    }

    fun seekTo(timeMs: Long) {
        mediaPlayer?.let { player ->
            if (isReady) {
                player.seekTo(timeMs.toInt())
            }
        }
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
        isReady = false
    }
}
