package com.novamotion.core.export

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.novamotion.core.model.Project
import com.novamotion.core.render.EglSurfaceRenderer
import com.novamotion.core.render.SceneRenderer
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
     * Encodes a project into an MP4 video file using Android MediaCodec hardware encoder
     * and real EGL OpenGL ES 3.0 frame rendering.
     */
    suspend fun encodeProject(
        context: Context,
        project: Project,
        config: ExportConfiguration,
        onProgress: (progress: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var eglRenderer: EglSurfaceRenderer? = null
        var sceneRenderer: SceneRenderer? = null
        var isMuxerStarted = false

        var tempRenderFile: File? = null

        try {
            // Ensure width and height are divisible by 16 for universal MediaCodec hardware compatibility across all SoCs
            val alignedWidth = (config.width + 15) / 16 * 16
            val alignedHeight = (config.height + 15) / 16 * 16

            val format = MediaFormat.createVideoFormat(config.mimeType, alignedWidth, alignedHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, config.bitrateMbps * 1000 * 1000)
                setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframe interval
            }

            codec = MediaCodec.createEncoderByType(config.mimeType)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            val inputSurface = codec.createInputSurface()
            codec.start()

            // Bind EGL 1.4 context to encoder input surface
            eglRenderer = EglSurfaceRenderer(inputSurface)
            eglRenderer.makeCurrent()

            // Initialize OpenGL Scene Renderer for offline export
            sceneRenderer = SceneRenderer(context).apply {
                initialize()
                updateDimensions(alignedWidth, alignedHeight)
            }

            val hasAudio = AudioExportPipeline.hasAudio(project)
            tempRenderFile = if (hasAudio) {
                File(config.outputFile.parentFile ?: context.cacheDir, "temp_render_${System.currentTimeMillis()}.mp4")
            } else {
                config.outputFile
            }

            muxer = MediaMuxer(tempRenderFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            isMuxerStarted = false

            val totalFrames = project.totalFrames
            val frameDurationNs = 1_000_000_000L / config.fps
            val bufferInfo = MediaCodec.BufferInfo()

            // Frame-by-frame offline rendering loop
            for (frame in 0 until totalFrames) {
                val presentationTimeNs = frame * frameDurationNs
                val playheadMs = (frame * 1000L) / config.fps

                // 1. Render project frame directly into encoder EGL Surface
                eglRenderer.makeCurrent()
                sceneRenderer.renderProject(project, playheadMs, alignedWidth, alignedHeight)
                eglRenderer.setPresentationTime(presentationTimeNs)
                eglRenderer.swapBuffers()

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

            // Flush and stop video muxer before audio multiplexing stage
            if (isMuxerStarted) {
                muxer.stop()
                isMuxerStarted = false
            }
            muxer.release()
            muxer = null

            // Stage 2: Merge audio tracks if present
            if (hasAudio && tempRenderFile != null) {
                val mergeResult = AudioExportPipeline.mergeAudioAndVideo(
                    context = context,
                    project = project,
                    tempVideoFile = tempRenderFile,
                    outputFile = config.outputFile
                )
                if (mergeResult.isFailure) {
                    return@withContext mergeResult
                }
            }

            return@withContext Result.success(config.outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            try { sceneRenderer?.release() } catch (ignored: Exception) {}
            try { eglRenderer?.release() } catch (ignored: Exception) {}
            try { codec?.stop() } catch (ignored: Exception) {}
            try { codec?.release() } catch (ignored: Exception) {}
            try {
                if (isMuxerStarted) {
                    muxer?.stop()
                }
            } catch (ignored: Exception) {}
            try { muxer?.release() } catch (ignored: Exception) {}
            if (tempRenderFile != null && tempRenderFile != config.outputFile && tempRenderFile.exists()) {
                try { tempRenderFile.delete() } catch (ignored: Exception) {}
            }
        }
    }
}
