package com.novamotion.core.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.novamotion.core.model.Project
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLViewportRenderer : GLSurfaceView.Renderer {

    private var baseShader: ShaderProgram? = null
    private var motionBlurShader: ShaderProgram? = null

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    var currentProject: Project? = null
    var currentPlayheadMs: Long = 0L

    private val quadVertices = floatArrayOf(
        -1.0f,  1.0f, 0.0f,
        -1.0f, -1.0f, 0.0f,
         1.0f,  1.0f, 0.0f,
         1.0f, -1.0f, 0.0f
    )

    private val quadTexCoords = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.04f, 0.045f, 0.06f, 1.0f) // Studio Dark Background
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
        motionBlurShader = ShaderProgram(Shaders.VERTEX_QUAD, Shaders.FRAGMENT_MOTION_BLUR)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.orthoM(projectionMatrix, 0, -aspect, aspect, -1f, 1f, -1f, 1f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Evaluate and render layers for currentPlayheadMs
        val project = currentProject ?: return
        val activeLayers = project.layers.filter { it.isActiveAt(currentPlayheadMs) }

        for (layer in activeLayers) {
            drawLayerQuad(layer)
        }
    }

    private fun drawLayerQuad(layer: com.novamotion.core.model.Layer) {
        val shader = baseShader ?: return
        shader.use()

        Matrix.setIdentityM(modelMatrix, 0)
        // Combine view and projection
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        val mvpHandle = GLES30.glGetUniformLocation(shader.programId, "u_MVPMatrix")
        val opacityHandle = GLES30.glGetUniformLocation(shader.programId, "u_Opacity")
        val tintHandle = GLES30.glGetUniformLocation(shader.programId, "u_TintColor")

        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(opacityHandle, 1.0f)
        GLES30.glUniform4f(tintHandle, 0.39f, 0.40f, 0.95f, 1.0f) // Neon Indigo tint for preview

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)

        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }
}
