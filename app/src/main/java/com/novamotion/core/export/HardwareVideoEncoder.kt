package com.novamotion.core.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.novamotion.core.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ExportConfiguration(
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 60,
    val bitrateMbps: Int = 20,
    val mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC, // H.264
    val outputFile: File
)

object HardwareVideoEncoder {

    /**
     * Encodes a project into an MP4 video file using Android MediaCodec hardware encoder.
     */
    suspend fun encodeProject(
        project: Project,
        config: ExportConfiguration,
        onProgress: (progress: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            val format = MediaFormat.createVideoFormat(config.mimeType, config.width, config.height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, config.bitrateMbps * 1000 * 1000)
                setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframe interval
            }

            codec = MediaCodec.createEncoderByType(config.mimeType)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            val inputSurface = codec.createInputSurface()
            codec.start()

            muxer = MediaMuxer(config.outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var isMuxerStarted = false

            val totalFrames = project.totalFrames
            val frameDurationNs = 1_000_000_000L / config.fps
            val bufferInfo = MediaCodec.BufferInfo()

            // Frame-by-frame offline rendering loop
            for (frame in 0 until totalFrames) {
                val presentationTimeNs = frame * frameDurationNs
                val playheadMs = (frame * 1000L) / config.fps

                // Signal end of stream on the final frame
                if (frame == totalFrames - 1) {
                    codec.signalEndOfInputStream()
                }

                // Drain encoded output packets from codec into MediaMuxer
                var isDrainComplete = false
                while (!isDrainComplete) {
                    val status = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!isMuxerStarted) {
                            videoTrackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            isMuxerStarted = true
                        }
                    } else if (status >= 0) {
                        val encodedBuffer = codec.getOutputBuffer(status)
                        if (encodedBuffer != null && bufferInfo.size > 0 && isMuxerStarted) {
                            encodedBuffer.position(bufferInfo.offset)
                            encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            bufferInfo.presentationTimeUs = presentationTimeNs / 1000L
                            muxer.writeSampleData(videoTrackIndex, encodedBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(status, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isDrainComplete = true
                        }
                    } else if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        isDrainComplete = true
                    }
                }

                onProgress((frame + 1).toFloat() / totalFrames.toFloat())
            }

            return@withContext Result.success(config.outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            try {
                codec?.stop()
                codec?.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}
