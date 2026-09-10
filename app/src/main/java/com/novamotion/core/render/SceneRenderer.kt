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

        // Release managers no longer needed
        val toRemove = videoManagers.keys - videoUris
        toRemove.forEach { uri ->
            videoManagers.remove(uri)?.release()
        }

        // Create managers for new video URIs
        videoUris.forEach { uri ->
            if (!videoManagers.containsKey(uri)) {
                val manager = VideoSurfaceTextureManager(context, uri)
                manager.initGl() // must be called from GL thread
                videoManagers[uri] = manager
            }
        }
    }

    fun renderProject(project: Project, playheadMs: Long, viewportWidth: Int, viewportHeight: Int) {
        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)

        // Clear with project background colour
        val bgRed = (((project.backgroundColor shr 16) and 0xFFL).toFloat()) / 255f
        val bgGreen = (((project.backgroundColor shr 8) and 0xFFL).toFloat()) / 255f
        val bgBlue = ((project.backgroundColor and 0xFFL).toFloat()) / 255f
        GLES30.glClearColor(bgRed, bgGreen, bgBlue, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Lazy-sync video managers each render frame (cheap: only acts on diff)
        syncVideoManagers(project)

        // Active visible layers in chronological Z-order
        val activeLayers = project.layers.filter { it.isActiveAt(playheadMs) }
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
            else -> return  // AUDIO, ADJUSTMENT, NULL_OBJECT — no visual output
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

        // Update OES texture with latest decoded video frame
        manager.updateTexImage()
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
        val shader = baseShader ?: return
        val uri = layer.mediaUri ?: return
        val texResult = ImageTextureLoader.loadImageTexture(context, uri) ?: return

        val ratio = texResult.width.toFloat() / texResult.height.toFloat()
        computeMvp(layer, playheadMs, ratio * 0.8f, 0.8f, project)

        bindAndDraw2DTexture(shader, texResult.textureId, opacity)
    }

    private fun renderTextLayer(
        layer: Layer,
        playheadMs: Long,
        opacity: Float,
        project: Project
    ) {
        val shader = baseShader ?: return
        val texResult = TextTextureGenerator.getOrCreateTextTexture(
            text = layer.textContent,
            colorLong = layer.textColor,
            fontSizeSp = 36f,
            isBold = true
        )

        val ratio = texResult.width.toFloat() / texResult.height.toFloat()
        computeMvp(layer, playheadMs, ratio * 0.4f, 0.4f, project)

        bindAndDraw2DTexture(shader, texResult.textureId, opacity)
    }

    private fun renderShapeLayer(
        layer: Layer,
        playheadMs: Long,
        opacity: Float,
        project: Project
    ) {
        val shader = baseShader ?: return
        val texResult = ShapeTextureGenerator.getOrCreateShapeTexture(
            shapeType = layer.shapeType,
            fillColorLong = layer.fillColor,
            strokeColorLong = 0xFF00F0FF,
            strokeWidth = 6f
        )

        computeMvp(layer, playheadMs, 0.6f, 0.6f, project)
        bindAndDraw2DTexture(shader, texResult.textureId, opacity)
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

    private fun bindAndDraw2DTexture(shader: ShaderProgram, textureId: Int, opacity: Float) {
        if (textureId == 0) return
        shader.use()

        val mvpHandle     = GLES30.glGetUniformLocation(shader.programId, "u_MVPMatrix")
        val opacityHandle = GLES30.glGetUniformLocation(shader.programId, "u_Opacity")
        val tintHandle    = GLES30.glGetUniformLocation(shader.programId, "u_TintColor")
        val textureHandle = GLES30.glGetUniformLocation(shader.programId, "u_Texture")

        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(opacityHandle, opacity)
        GLES30.glUniform4f(tintHandle, 1.0f, 1.0f, 1.0f, 1.0f)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(textureHandle, 0)

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
        videoManagers.values.forEach { it.release() }
        videoManagers.clear()
        TextTextureGenerator.clearCache()
        ShapeTextureGenerator.clearCache()
        ImageTextureLoader.clearCache()
    }
}
