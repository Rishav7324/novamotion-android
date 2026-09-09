package com.novamotion.core.render

object BezierMaskShader {

    /**
     * GLSL Shader for Dynamic Mask Compositing with Edge Feathering and Inversion.
     * Takes a base image and an alpha mask buffer (or SDF texture) to produce
     * clean feathered cutouts.
     */
    val FRAGMENT_MASK_COMPOSITE = """#version 300 es
        precision highp float;

        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform sampler2D u_MaskTexture;
        uniform float u_Feather; // 0.0 to 1.0 normalized
        uniform float u_Invert;  // 0.0 = normal, 1.0 = inverted
        uniform float u_Opacity; // 0.0 to 1.0

        out vec4 fragColor;

        void main() {
            vec4 baseColor = texture(u_Texture, v_TexCoord);
            float rawMask = texture(u_MaskTexture, v_TexCoord).r;

            // Apply feather smoothing
            float maskAlpha;
            if (u_Feather > 0.001) {
                float lowerBound = clamp(0.5 - u_Feather * 0.5, 0.0, 0.499);
                float upperBound = clamp(0.5 + u_Feather * 0.5, 0.501, 1.0);
                maskAlpha = smoothstep(lowerBound, upperBound, rawMask);
            } else {
                maskAlpha = step(0.5, rawMask);
            }

            // Apply invert
            if (u_Invert > 0.5) {
                maskAlpha = 1.0 - maskAlpha;
            }

            fragColor = vec4(baseColor.rgb, baseColor.a * maskAlpha * u_Opacity);
        }
    """.trimIndent()
}
