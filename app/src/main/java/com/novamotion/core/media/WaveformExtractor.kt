package com.novamotion.core.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max

object WaveformExtractor {

    /**
     * Extracts normalized audio amplitude peaks (0.0 to 1.0) for visual waveform rendering.
     */
    suspend fun extractWaveform(
        context: Context,
        uri: Uri,
        samplesCount: Int = 100
    ): FloatArray = withContext(Dispatchers.IO) {
        val peaks = FloatArray(samplesCount) { 0.1f } // Default minimum baseline

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) return@withContext peaks

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext peaks
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val rawSamples = mutableListOf<Float>()
            var isEOS = false

            while (!isEOS && rawSamples.size < 50000) {
                val inIndex = codec.dequeueInputBuffer(5000)
                if (inIndex >= 0) {
                    val buffer = codec.getInputBuffer(inIndex)
                    if (buffer != null) {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 5000)
                if (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && info.size > 0) {
                        val shortBuffer = outBuffer.asShortBuffer()
                        var maxPeak = 0f
                        while (shortBuffer.hasRemaining()) {
                            val sample = abs(shortBuffer.get().toFloat() / Short.MAX_VALUE)
                            maxPeak = max(maxPeak, sample)
                        }
                        rawSamples.add(maxPeak)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                }
            }

            // Downsample raw samples into target samplesCount
            if (rawSamples.isNotEmpty()) {
                val step = rawSamples.size.toFloat() / samplesCount.toFloat()
                for (i in 0 until samplesCount) {
                    val idx = (i * step).toInt().coerceIn(0, rawSamples.lastIndex)
                    peaks[i] = rawSamples[idx].coerceIn(0.05f, 1.0f)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }

        return@withContext peaks
    }
}
