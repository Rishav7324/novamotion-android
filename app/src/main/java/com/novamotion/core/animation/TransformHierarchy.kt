package com.novamotion.core.animation

import android.opengl.Matrix
import com.novamotion.core.model.Layer
import com.novamotion.core.model.Project
import com.novamotion.core.model.evaluate

object TransformHierarchy {

    /**
     * Computes the final world transformation matrix (4x4) for a layer at timeMs,
     * resolving any parent-child hierarchy chains (Null Controllers, Rigging).
     */
    fun computeWorldMatrix(
        layer: Layer,
        project: Project,
        timeMs: Long,
        outMatrix: FloatArray
    ) {
        val localMatrix = FloatArray(16)
        computeLocalMatrix(layer, timeMs, localMatrix)

        val parentId = layer.parentLayerId
        if (parentId == null) {
            System.arraycopy(localMatrix, 0, outMatrix, 0, 16)
            return
        }

        val parentLayer = project.getLayerById(parentId)
        if (parentLayer == null) {
            System.arraycopy(localMatrix, 0, outMatrix, 0, 16)
            return
        }

        // Recursively compute parent world matrix
        val parentWorldMatrix = FloatArray(16)
        computeWorldMatrix(parentLayer, project, timeMs, parentWorldMatrix)

        // Matrix multiplication: ParentWorld * Local
        Matrix.multiplyMM(outMatrix, 0, parentWorldMatrix, 0, localMatrix, 0)
    }

    private fun computeLocalMatrix(layer: Layer, timeMs: Long, outMatrix: FloatArray) {
        Matrix.setIdentityM(outMatrix, 0)

        val posX = layer.transform.posX.evaluate(timeMs)
        val posY = layer.transform.posY.evaluate(timeMs)
        val posZ = layer.transform.posZ.evaluate(timeMs)

        val scaleX = layer.transform.scaleX.evaluate(timeMs)
        val scaleY = layer.transform.scaleY.evaluate(timeMs)
        val rotation = layer.transform.rotation.evaluate(timeMs)

        // Translate
        Matrix.translateM(outMatrix, 0, posX, posY, posZ)

        // Rotate (Z-axis)
        if (rotation != 0f) {
            Matrix.rotateM(outMatrix, 0, rotation, 0f, 0f, 1f)
        }

        // Scale
        Matrix.scaleM(outMatrix, 0, scaleX, scaleY, 1f)
    }
}
