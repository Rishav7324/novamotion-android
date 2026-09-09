package com.novamotion.core.render

import android.opengl.Matrix
import kotlin.math.abs

/**
 * 3D Camera System with 3D Layer Transformations,
 * Perspective Projection, and Depth of Field (DoF) Circle of Confusion (CoC).
 */
data class Transform3D(
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val rotationX: Float = 0f, // Pitch
    val rotationY: Float = 0f, // Yaw
    val rotationZ: Float = 0f, // Roll
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f,
    val anchorX: Float = 0f,
    val anchorY: Float = 0f,
    val anchorZ: Float = 0f
) {
    fun computeModelMatrix(outMatrix: FloatArray) {
        Matrix.setIdentityM(outMatrix, 0)
        // 1. Translation in 3D
        Matrix.translateM(outMatrix, 0, posX, posY, posZ)
        // 2. 3D Rotations (Euler order: Z -> Y -> X)
        if (rotationZ != 0f) Matrix.rotateM(outMatrix, 0, rotationZ, 0f, 0f, 1f)
        if (rotationY != 0f) Matrix.rotateM(outMatrix, 0, rotationY, 0f, 1f, 0f)
        if (rotationX != 0f) Matrix.rotateM(outMatrix, 0, rotationX, 1f, 0f, 0f)
        // 3. 3D Scaling
        Matrix.scaleM(outMatrix, 0, scaleX, scaleY, scaleZ)
        // 4. Anchor point offset
        if (anchorX != 0f || anchorY != 0f || anchorZ != 0f) {
            Matrix.translateM(outMatrix, 0, -anchorX, -anchorY, -anchorZ)
        }
    }
}

object Camera3DSystem {

    /**
     * Calculates the Circle of Confusion (blur radius) in pixels for Depth of Field.
     * @param layerZ Distance of layer along camera optical axis
     * @param focusDistance Distance where the lens is focused
     * @param focalLength Lens focal length in mm (e.g. 50mm)
     * @param aperture F-stop number (e.g. 2.8, 1.4)
     */
    fun calculateCircleOfConfusion(
        layerZ: Float,
        focusDistance: Float,
        focalLength: Float = 50f,
        aperture: Float = 2.8f
    ): Float {
        val dist = abs(layerZ).coerceAtLeast(10f)
        val focus = abs(focusDistance).coerceAtLeast(10f)
        
        // Lens aperture diameter A = f / N
        val apertureDiameter = focalLength / aperture.coerceAtLeast(0.5f)
        
        // Thin lens equation CoC: c = A * |dist - focus| / dist * (f / (focus - f))
        val factor = focalLength / (focus - focalLength).coerceAtLeast(1f)
        val coc = apertureDiameter * (abs(dist - focus) / dist) * factor
        
        return (coc * 0.5f).coerceIn(0f, 30f) // Clamp to reasonable bokeh pixel radius
    }

    /**
     * GLSL Shader for Cinematic Bokeh Depth of Field Blur.
     */
    val FRAGMENT_DEPTH_OF_FIELD = """#version 300 es
        precision highp float;

        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform float u_BlurRadius; // CoC blur radius in UV space
        out vec4 fragColor;

        const int SAMPLES = 16;
        const float GOLDEN_ANGLE = 2.39996323;

        void main() {
            if (u_BlurRadius < 0.001) {
                fragColor = texture(u_Texture, v_TexCoord);
                return;
            }

            vec4 colorAcc = vec4(0.0);
            float totalWeight = 0.0;

            // Fermat's spiral / Vogel disc distribution for realistic circular bokeh
            for (int i = 0; i < SAMPLES; i++) {
                float theta = float(i) * GOLDEN_ANGLE;
                float r = sqrt(float(i) / float(SAMPLES)) * u_BlurRadius;
                vec2 offset = vec2(cos(theta), sin(theta)) * r;

                vec4 tap = texture(u_Texture, v_TexCoord + offset);
                // Weight highlights slightly higher for natural bokeh discs
                float luma = dot(tap.rgb, vec3(0.299, 0.587, 0.114));
                float weight = 1.0 + luma * 1.5;

                colorAcc += tap * weight;
                totalWeight += weight;
            }

            fragColor = colorAcc / totalWeight;
        }
    """.trimIndent()
}
