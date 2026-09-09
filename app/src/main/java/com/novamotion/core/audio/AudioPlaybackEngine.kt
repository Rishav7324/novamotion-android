package com.novamotion.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class AudioPlaybackEngine(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentUri: String? = null

    fun loadAudio(uriString: String) {
        if (currentUri == uriString && mediaPlayer != null) return
        release()
        try {
            currentUri = uriString
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uriString))
                prepare()
            }
        } catch (e: Exception) {
            mediaPlayer = null
        }
    }

    fun play(fromMs: Long) {
        val player = mediaPlayer ?: return
        try {
            player.seekTo(fromMs.toInt())
            player.start()
        } catch (ignored: Exception) {}
    }

    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            }
        } catch (ignored: Exception) {}
    }

    fun seekTo(ms: Long) {
        val player = mediaPlayer ?: return
        try {
            player.seekTo(ms.toInt())
        } catch (ignored: Exception) {}
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (ignored: Exception) {}
        mediaPlayer = null
        currentUri = null
    }
}
