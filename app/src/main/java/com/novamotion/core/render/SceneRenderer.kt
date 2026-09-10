package com.novamotion.core.render

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import com.novamotion.core.animation.TransformHierarchy
import com.novamotion.core.model.Layer
import com.novamotion.core.model.LayerType
import com.novamotion.core.model.Project
import com.novamotion.core.model.evaluate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Production SceneRenderer:
 *  - VIDEO layers use VideoSurfaceTextureManager (ExoPlayer → SurfaceTexture OES)
 *  - IMAGE layers use ImageTextureLoader (static Bitmap → GL_TEXTURE_2D)
 *  - TEXT / SHAPE layers use their respective texture generators
 *  - Separate OES shader program for VIDEO
 *  - No more MediaMetadataRetriever.getFrameAtTime in the render path
 */
class SceneRenderer(private val context: Context) {

    private var baseShader: ShaderProgram? = null
    private var oesShader: ShaderProgram? = null
    private var colorGradingShader: ShaderProgram? = null
    private var waveWarpShader: ShaderProgram? = null
    private var chromaticShader: ShaderProgram? = null
    private var bloomGlowShader: ShaderProgram? = null
    private var glitchShader: ShaderProgram? = null

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val texMatrix = FloatArray(16)

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    // One VideoSurfaceTextureManager per unique video URI
    private val videoManagers = mutableMapOf<String, VideoSurfaceTextureManager>()

    private val quadVertices = floatArrayOf(
        -0.5f,  0.5f, 0.0f,
        -0.5f, -0.5f, 0.0f,
         0.5f,  0.5f, 0.0f,
         0.5f, -0.5f, 0.0f
    )

    private val quadTexCoords = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    fun initialize() {
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        vertexBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(quadVertices)
                position(0)
            }

        texCoordBuffer = ByteBuffer.allocateDirect(quadTexCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(quadTexCoords)
                position(0)
            }

        baseShader = ShaderProgram(Shaders.VERTEX_QUAD, Shaders.FRAGMENT_TEXTURE_BASE)
        oesShader = ShaderProgram(Shaders.VERTEX_QUAD_OES, Shaders.FRAGMENT_OES_VIDEO)
        colorGradingShader = ShaderProgram(Shaders.VERTEX_QUAD, ColorGradingShader.FRAGMENT_COLOR_GRADING)
        waveWarpShader = ShaderProgram(Shaders.VERTEX_QUAD, VFXShaderLibrary.FRAGMENT_WAVE_WARP)
        chromaticShader = ShaderProgram(Shaders.VERTEX_QUAD, Shaders.FRAGMENT_CHROMATIC_ABERRATION)
        bloomGlowShader = ShaderProgram(Shaders.VERTEX_QUAD, Shaders.FRAGMENT_BLOOM_GLOW)
        glitchShader = ShaderProgram(Shaders.VERTEX_QUAD, VFXShaderLibrary.FRAGMENT_DIGITAL_GLITCH)

        // Identity matrix for tex matrix default
        Matrix.setIdentityM(texMatrix, 0)
    }

    fun updateDimensions(width: Int, height: Int) {
        val aspect = width.toFloat() / height.toFloat()
        Matrix.orthoM(projectionMatrix, 0, -aspect, aspect, -1f, 1f, -100f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    /**
     * Prepare VideoSurfaceTextureManagers for any VIDEO layers in the project.
     * Call this whenever layers change (on the GL thread).
     */
    fun syncVideoManagers(project: Project) {
        val videoUris = project.layers
            .filter { it.type == LayerType.VIDEO && it.mediaUri != null }
            .mapNotNull { it.mediaUri }
            .toSet()

        // Remove managers for videos that are no longer in the project
        val iterator = videoManagers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in videoUris) {
                entry.value.release()
                iterator.remove()
            }
        }

        // Add managers for new videos
        for (uri in videoUris) {
            if (uri !in videoManagers) {
                videoManagers[uri] = VideoSurfaceTextureManager(context, uri)
                videoManagers[uri]?.initGl()
            }
        }
    }

    fun renderProject(project: Project, playheadMs: Long, width: Int, height: Int) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glViewport(0, 0, width, height)

        // Lazy-sync video managers each render frame
        syncVideoManagers(project)

        val activeLayers = project.layers
            .filter { it.isVisible && it.isActiveAt(playheadMs) }
            .sortedBy { it.startTimeMs }

        for (layer in activeLayers) {
            renderLayer(layer, playheadMs, project)
        }
    }

    private fun renderLayer(layer: Layer, playheadMs: Long, project: Project) {
        val opacity = layer.transform.opacity.evaluate(playheadMs)
        if (opacity <= 0.001f) return

        when (layer.type) {
            LayerType.VIDEO -> renderVideoLayer(layer, playheadMs, opacity, project)
            LayerType.IMAGE -> renderImageLayer(layer, playheadMs, opacity, project)
            LayerType.TEXT  -> renderTextLayer(layer, playheadMs, opacity, project)
            LayerType.SHAPE -> renderShapeLayer(layer, playheadMs, opacity, project)
            LayerType.AUDIO, LayerType.NULL_OBJECT, LayerType.ADJUSTMENT -> {
                // Audio is handled by AudioPlaybackEngine, NULL_OBJECT is non-rendering
            }
        }
    }

    private fun renderVideoLayer(
        layer: Layer,
        playheadMs: Long,
        opacity: Float,
        project: Project
    ) {
        val shader = oesShader ?: return
        val uri = layer.mediaUri ?: return
        val manager = videoManagers[uri] ?: return

        // Sync ExoPlayer seek to timeline playhead (frame-accurate scrub)
        // local time within layer = playhead - layer.start, mapped to sourceInMs
        val localMs = playheadMs - layer.startTimeMs
        val sourceMs = (layer.sourceInMs + localMs).coerceAtLeast(0L)
        // Only seek when delta > 80ms to avoid excessive seeks during smooth playback;
        // scrub jumps will trigger immediate seek.
        val needsSeek = kotlin.math.abs(manager.lastSeekMs - sourceMs) > 80L
        if (needsSeek) {
            manager.seekTo(sourceMs)
        }
        // Don't render black frame before player ready — show fallback color instead
        if (!manager.isPlayerReady) {
            // Player buffering; skip OES draw this frame to avoid black flash
            // Could draw placeholder; for now just return and let next frame retry
            return
        }

        // Update OES texture with latest decoded video frame
        if (!manager.updateTexImage()) return
        manager.getTransformMatrix(texMatrix)

        val oesTexId = manager.oesTextureId
        if (oesTexId == 0) return

        computeMvp(layer, playheadMs, 0.9f, 0.9f, project)

        shader.use()
        val mvpHandle = GLES30.glGetUniformLocation(shader.programId, "u_MVPMatrix")
        val texMatHandle = GLES30.glGetUniformLocation(shader.programId, "u_TexMatrix")
        val texHandle = GLES30.glGetUniformLocation(shader.programId, "u_VideoTexture")
        val opacityHandle = GLES30.glGetUniformLocation(shader.programId, "u_Opacity")

        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(texMatHandle, 1, false, texMatrix, 0)
        GLES30.glUniform1f(opacityHandle, opacity)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexId)
        GLES30.glUniform1i(texHandle, 0)

        drawQuad()

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    private fun renderImageLayer(
        layer: Layer,
        playheadMs: Long,
        opacity: Float,
        project: Project
    ) {
        val uri = layer.mediaUri ?: return
        val texResult = ImageTextureLoader.loadImageTexture(context, uri) ?: return

        val ratio = texResult.width.toFloat() / texResult.height.toFloat()
        computeMvp(layer, playheadMs, ratio * 0.8f, 0.8f, project)

        bindAndDrawLayer(layer, texResult.textureId, opacity, playheadMs)
    }

    private fun renderTextLayer(
        layer: Layer,
        playheadMs: Long,
        opacity: Float,
        project: Project
    ) {
        val texResult = TextTextureGenerator.getOrCreateTextTexture(
            text = layer.textContent,
            colorLong = layer.textColor,
            fontSizeSp = 36f,
            isBold = true
        )

        val ratio = texResult.width.toFloat() / texResult.height.toFloat()
        computeMvp(layer, playheadMs, ratio * 0.4f, 0.4f, project)

        bindAndDrawLayer(layer, texResult.textureId, opacity, playheadMs)
    }

    private fun renderShapeLayer(
        layer: Layer,
        playheadMs: Long,
        opacity: Float,
        project: Project
    ) {
        val texResult = ShapeTextureGenerator.getOrCreateShapeTexture(
            shapeType = layer.shapeType,
            fillColorLong = layer.fillColor,
            strokeColorLong = 0xFF00F0FF,
            strokeWidth = 6f
        )

        computeMvp(layer, playheadMs, 0.6f, 0.6f, project)
        bindAndDrawLayer(layer, texResult.textureId, opacity, playheadMs)
    }

    private fun computeMvp(
        layer: Layer,
        playheadMs: Long,
        aspectScaleX: Float,
        aspectScaleY: Float,
        project: Project
    ) {
        if (layer.parentLayerId != null) {
            TransformHierarchy.computeWorldMatrix(layer, project, playheadMs, modelMatrix)
            Matrix.scaleM(modelMatrix, 0, aspectScaleX, aspectScaleY, 1f)
        } else {
            val posX     = layer.transform.posX.evaluate(playheadMs)
            val posY     = layer.transform.posY.evaluate(playheadMs)
            val posZ     = layer.transform.posZ.evaluate(playheadMs)
            val scaleX   = layer.transform.scaleX.evaluate(playheadMs)
            val scaleY   = layer.transform.scaleY.evaluate(playheadMs)
            val rotation = layer.transform.rotation.evaluate(playheadMs)

            Matrix.setIdentityM(modelMatrix, 0)
            val normX = posX / (project.width / 2f)
            val normY = -(posY / (project.height / 2f))
            Matrix.translateM(modelMatrix, 0, normX, normY, posZ)
            if (rotation != 0f) {
                Matrix.rotateM(modelMatrix, 0, rotation, 0f, 0f, 1f)
            }
            Matrix.scaleM(modelMatrix, 0, scaleX * aspectScaleX, scaleY * aspectScaleY, 1f)
        }

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
    }

    private fun bindAndDrawLayer(layer: Layer, textureId: Int, opacity: Float, playheadMs: Long) {
        if (textureId == 0) return

        val activeEffect = layer.effects.firstOrNull { it.isEnabled }
        val shader = when (activeEffect?.type) {
            com.novamotion.core.model.EffectType.CHROMATIC_ABERRATION -> chromaticShader ?: baseShader
            com.novamotion.core.model.EffectType.WAVE_WARP -> waveWarpShader ?: baseShader
            com.novamotion.core.model.EffectType.GLOW_BLOOM -> bloomGlowShader ?: baseShader
            com.novamotion.core.model.EffectType.GLITCH -> glitchShader ?: baseShader
            else -> baseShader
        } ?: return

        shader.use()

        val mvpHandle     = GLES30.glGetUniformLocation(shader.programId, "u_MVPMatrix")
        val opacityHandle = GLES30.glGetUniformLocation(shader.programId, "u_Opacity")
        val tintHandle    = GLES30.glGetUniformLocation(shader.programId, "u_TintColor")
        val textureHandle = GLES30.glGetUniformLocation(shader.programId, "u_Texture")

        if (mvpHandle >= 0) GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        if (opacityHandle >= 0) GLES30.glUniform1f(opacityHandle, opacity)
        if (tintHandle >= 0) GLES30.glUniform4f(tintHandle, 1.0f, 1.0f, 1.0f, 1.0f)

        // Dynamic uniform configuration for active effect
        when (activeEffect?.type) {
            com.novamotion.core.model.EffectType.CHROMATIC_ABERRATION -> {
                val intensityHandle = GLES30.glGetUniformLocation(shader.programId, "u_Intensity")
                val intensity = activeEffect.parameters["intensity"]?.value ?: 0.04f
                if (intensityHandle >= 0) GLES30.glUniform1f(intensityHandle, intensity)
            }
            com.novamotion.core.model.EffectType.WAVE_WARP -> {
                val freqHandle = GLES30.glGetUniformLocation(shader.programId, "u_Frequency")
                val ampHandle = GLES30.glGetUniformLocation(shader.programId, "u_Amplitude")
                val phaseHandle = GLES30.glGetUniformLocation(shader.programId, "u_Phase")
                if (freqHandle >= 0) GLES30.glUniform1f(freqHandle, activeEffect.parameters["frequency"]?.value ?: 12f)
                if (ampHandle >= 0) GLES30.glUniform1f(ampHandle, activeEffect.parameters["amplitude"]?.value ?: 0.03f)
                if (phaseHandle >= 0) GLES30.glUniform1f(phaseHandle, (playheadMs * 0.006f) % 6.283f)
            }
            com.novamotion.core.model.EffectType.GLOW_BLOOM -> {
                val threshHandle = GLES30.glGetUniformLocation(shader.programId, "u_Threshold")
                val intensHandle = GLES30.glGetUniformLocation(shader.programId, "u_Intensity")
                if (threshHandle >= 0) GLES30.glUniform1f(threshHandle, activeEffect.parameters["threshold"]?.value ?: 0.5f)
                if (intensHandle >= 0) GLES30.glUniform1f(intensHandle, activeEffect.parameters["intensity"]?.value ?: 1.2f)
            }
            com.novamotion.core.model.EffectType.GLITCH -> {
                val timeHandle = GLES30.glGetUniformLocation(shader.programId, "u_Time")
                val amountHandle = GLES30.glGetUniformLocation(shader.programId, "u_Amount")
                if (timeHandle >= 0) GLES30.glUniform1f(timeHandle, playheadMs / 1000f)
                if (amountHandle >= 0) GLES30.glUniform1f(amountHandle, activeEffect.parameters["amount"]?.value ?: 0.2f)
            }
            else -> {}
        }

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        if (textureHandle >= 0) GLES30.glUniform1i(textureHandle, 0)

        drawQuad()

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    private fun drawQuad() {
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)

        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    fun release() {
        baseShader = null
        oesShader = null
        colorGradingShader = null
        waveWarpShader = null
        chromaticShader = null
        bloomGlowShader = null
        glitchShader = null
        videoManagers.values.forEach { it.release() }
        videoManagers.clear()
        TextTextureGenerator.clearCache()
        ShapeTextureGenerator.clearCache()
        ImageTextureLoader.clearCache()
    }
}
