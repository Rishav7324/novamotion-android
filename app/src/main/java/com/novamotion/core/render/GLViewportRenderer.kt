package com.novamotion.core.render

import android.content.Context
import android.opengl.GLSurfaceView
import com.novamotion.core.model.Project
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GLSurfaceView.Renderer that drives SceneRenderer.
 *
 * Production changes:
 *  - Uses RENDERMODE_WHEN_DIRTY (set by caller) — no wasted GPU frames
 *  - Project and playhead are @Volatile for correct GL thread visibility
 *  - Caller must call glSurfaceView.requestRender() when state changes
 */
class GLViewportRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private val sceneRenderer = SceneRenderer(context)
    private var viewportWidth = 1080
    private var viewportHeight = 1920

    @Volatile
    var currentProject: Project? = null

    @Volatile
    var currentPlayheadMs: Long = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        sceneRenderer.initialize()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        sceneRenderer.updateDimensions(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val project = currentProject ?: return
        sceneRenderer.renderProject(project, currentPlayheadMs, viewportWidth, viewportHeight)
    }

    fun release() {
        sceneRenderer.release()
    }
}
