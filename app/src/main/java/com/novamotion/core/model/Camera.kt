package com.novamotion.core.model

import android.opengl.Matrix

data class Camera3D(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Camera 1",
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = -1000f,
    val targetX: Float = 0f,
    val targetY: Float = 0f,
    val targetZ: Float = 0f,
    val rollAngle: Float = 0f,
    val fovDegrees: Float = 60f,
    // Depth of Field (DoF)
    val enableDoF: Boolean = false,
    val focusDistance: Float = 1000f,
    val apertureSize: Float = 2.8f
) {
    /**
     * Computes the 4x4 View Matrix for this camera.
     */
    fun computeViewMatrix(outMatrix: FloatArray) {
        Matrix.setLookAtM(
            outMatrix, 0,
            posX, posY, posZ,
            targetX, targetY, targetZ,
            0f, 1f, 0f // Up vector
        )
        if (rollAngle != 0f) {
            Matrix.rotateM(outMatrix, 0, rollAngle, 0f, 0f, 1f)
        }
    }

    /**
     * Computes the 4x4 Perspective Projection Matrix.
     */
    fun computeProjectionMatrix(aspectRatio: Float, near: Float = 1f, far: Float = 5000f, outMatrix: FloatArray) {
        Matrix.perspectiveM(outMatrix, 0, fovDegrees, aspectRatio, near, far)
    }
}
