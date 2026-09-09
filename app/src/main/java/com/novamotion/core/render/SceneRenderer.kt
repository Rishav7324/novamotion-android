package com.novamotion.core.render

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import com.novamotion.core.model.Layer
import com.novamotion.core.model.LayerType
import com.novamotion.core.model.Project
import com.novamotion.core.model.evaluate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class SceneRenderer(private val context: Context) {

    private var baseShader: ShaderProgram? = null
    private var colorGradingShader: ShaderProgram? = null
    private var waveWarpShader: ShaderProgram? = null
    private var chromaticShader: ShaderProgram? = null

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

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
        colorGradingShader = ShaderProgram(Shaders.VERTEX_QUAD, ColorGradingShader.FRAGMENT_COLOR_GRADING)
        waveWarpShader = ShaderProgram(Shaders.VERTEX_QUAD, VFXShaderLibrary.FRAGMENT_WAVE_WARP)
        chromaticShader = ShaderProgram(Shaders.VERTEX_QUAD, Shaders.FRAGMENT_CHROMATIC_ABERRATION)
    }

    fun updateDimensions(width: Int, height: Int) {
        val aspect = width.toFloat() / height.toFloat()
        // Orthographic projection matching coordinate space
        Matrix.orthoM(projectionMatrix, 0, -aspect, aspect, -1f, 1f, -100f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    fun renderProject(project: Project, playheadMs: Long, viewportWidth: Int, viewportHeight: Int) {
        GLES30.glViewport(0, 0, viewportWidth, height = viewportHeight)

        // Clear with dark studio background
        val bgRed = ((project.backgroundColor shr 16) and 0xFF) / 255f
        val bgGreen = ((project.backgroundColor shr 8) and 0xFF) / 255f
        val bgBlue = (project.backgroundColor and 0xFF) / 255f
        GLES30.glClearColor(bgRed, bgGreen, bgBlue, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Active visible layers in chronological Z-order
        val activeLayers = project.layers.filter { it.isActiveAt(playheadMs) }

        for (layer in activeLayers) {
            renderLayer(layer, playheadMs, project)
        }
    }

    private fun renderLayer(layer: Layer, playheadMs: Long, project: Project) {
        val shader = baseShader ?: return

        // 1. Evaluate keyframed transform properties
        val posX = layer.transform.posX.evaluate(playheadMs)
        val posY = layer.transform.posY.evaluate(playheadMs)
        val posZ = layer.transform.posZ.evaluate(playheadMs)
        val scaleX = layer.transform.scaleX.evaluate(playheadMs)
        val scaleY = layer.transform.scaleY.evaluate(playheadMs)
        val rotation = layer.transform.rotation.evaluate(playheadMs)
        val opacity = layer.transform.opacity.evaluate(playheadMs)

        if (opacity <= 0.001f) return

        // 2. Obtain real texture based on layer type
        var textureId = 0
        var aspectWidth = 1.0f
        var aspectHeight = 1.0f

        when (layer.type) {
            LayerType.TEXT -> {
                val texResult = TextTextureGenerator.getOrCreateTextTexture(
                    text = layer.textContent,
                    colorLong = layer.textColor,
                    fontSizeSp = 36f,
                    isBold = true
                )
                textureId = texResult.textureId
                val ratio = texResult.width.toFloat() / texResult.height.toFloat()
                aspectWidth = ratio * 0.4f
                aspectHeight = 0.4f
            }
            LayerType.SHAPE -> {
                val texResult = ShapeTextureGenerator.getOrCreateShapeTexture(
                    shapeType = layer.shapeType,
                    fillColorLong = layer.fillColor,
                    strokeColorLong = 0xFF00F0FF,
                    strokeWidth = 6f
                )
                textureId = texResult.textureId
                aspectWidth = 0.6f
                aspectHeight = 0.6f
            }
            LayerType.IMAGE, LayerType.VIDEO -> {
                val uri = layer.mediaUri
                if (uri != null) {
                    val texResult = ImageTextureLoader.loadTextureFromUri(context, uri, playheadMs * 1000L)
                    if (texResult != null) {
                        textureId = texResult.textureId
                        val ratio = texResult.width.toFloat() / texResult.height.toFloat()
                        aspectWidth = ratio * 0.8f
                        aspectHeight = 0.8f
                    }
                }
            }
            else -> {
                // Non-visual layers (AUDIO, NULL_OBJECT)
                return
            }
        }

        if (textureId == 0) return

        // 3. Compute Layer Model Matrix
        Matrix.setIdentityM(modelMatrix, 0)
        // Convert canvas pixel offsets to normalized orthographic space
        val normX = (posX / (project.width / 2f))
        val normY = -(posY / (project.height / 2f)) // Invert Y for standard 2D Cartesian
        Matrix.translateM(modelMatrix, 0, normX, normY, posZ)
        Matrix.rotateM(modelMatrix, 0, rotation, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, scaleX * aspectWidth, scaleY * aspectHeight, 1f)

        // 4. Multiply MVP matrix
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        // 5. Bind shader & draw
        shader.use()

        val mvpHandle = GLES30.glGetUniformLocation(shader.programId, "u_MVPMatrix")
        val opacityHandle = GLES30.glGetUniformLocation(shader.programId, "u_Opacity")
        val tintHandle = GLES30.glGetUniformLocation(shader.programId, "u_TintColor")
        val textureHandle = GLES30.glGetUniformLocation(shader.programId, "u_Texture")

        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(opacityHandle, opacity)
        GLES30.glUniform4f(tintHandle, 1.0f, 1.0f, 1.0f, 1.0f) // Neutral tint

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(textureHandle, 0)

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)

        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun release() {
        baseShader = null
        colorGradingShader = null
        waveWarpShader = null
        chromaticShader = null
        TextTextureGenerator.clearCache()
        ShapeTextureGenerator.clearCache()
        ImageTextureLoader.clearCache()
    }
}
