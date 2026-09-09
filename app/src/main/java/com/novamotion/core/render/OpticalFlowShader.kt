package com.novamotion.core.render

object OpticalFlowShader {

    /**
     * GLSL Shader for Dense Motion Vector Estimation between two video frames.
     * Computes spatial gradients (Ix, Iy) and temporal difference (It)
     * using a regularized Horn-Schunck / Lucas-Kanade optical flow formulation.
     */
    val FRAGMENT_OPTICAL_FLOW_VECTORS = """#version 300 es
        precision highp float;

        in vec2 v_TexCoord;
        uniform sampler2D u_FrameCurrent;
        uniform sampler2D u_FrameNext;
        uniform vec2 u_TexelSize;
        out vec4 fragColor;

        void main() {
            vec4 curr = texture(u_FrameCurrent, v_TexCoord);
            vec4 next = texture(u_FrameNext, v_TexCoord);

            float currLuma = dot(curr.rgb, vec3(0.299, 0.587, 0.114));
            float nextLuma = dot(next.rgb, vec3(0.299, 0.587, 0.114));

            // Spatial gradients
            float rightLuma = dot(texture(u_FrameCurrent, v_TexCoord + vec2(u_TexelSize.x, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
            float leftLuma  = dot(texture(u_FrameCurrent, v_TexCoord - vec2(u_TexelSize.x, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
            float upLuma    = dot(texture(u_FrameCurrent, v_TexCoord + vec2(0.0, u_TexelSize.y)).rgb, vec3(0.299, 0.587, 0.114));
            float downLuma  = dot(texture(u_FrameCurrent, v_TexCoord - vec2(0.0, u_TexelSize.y)).rgb, vec3(0.299, 0.587, 0.114));

            float Ix = (rightLuma - leftLuma) * 0.5;
            float Iy = (upLuma - downLuma) * 0.5;
            float It = nextLuma - currLuma;

            // Optical flow equation: Ix * u + Iy * v + It = 0
            // Regularized gradient descent step:
            float gradMagSq = Ix * Ix + Iy * Iy + 0.001; // Epsilon regularizer
            vec2 motionVec = -It * vec2(Ix, Iy) / gradMagSq;

            // Clamp motion vector to reasonable displacement (in UV space)
            motionVec = clamp(motionVec, vec2(-0.1), vec2(0.1));

            // Encode motion vector into RG channels: map [-0.1, 0.1] to [0, 1]
            vec2 encoded = motionVec * 5.0 + 0.5;
            fragColor = vec4(encoded.x, encoded.y, abs(It), 1.0);
        }
    """.trimIndent()

    /**
     * GLSL Shader for Bidirectional Motion-Compensated Frame Synthesis.
     * Warps Frame A forward by (t * V) and Frame B backward by ((1-t) * -V)
     * and blends them smoothly to synthesize high-frame-rate slow-motion frames.
     */
    val FRAGMENT_OPTICAL_FLOW_WARP = """#version 300 es
        precision highp float;

        in vec2 v_TexCoord;
        uniform sampler2D u_FrameA;
        uniform sampler2D u_FrameB;
        uniform sampler2D u_MotionVectors; // Encoded (u, v) in RG
        uniform float u_TimeRatio;         // 0.0 = Frame A, 1.0 = Frame B
        out vec4 fragColor;

        void main() {
            vec4 rawVec = texture(u_MotionVectors, v_TexCoord);
            vec2 flowVector = (rawVec.rg - 0.5) / 5.0;

            // Forward warp from Frame A
            vec2 uvA = v_TexCoord - flowVector * u_TimeRatio;
            vec4 colA = texture(u_FrameA, clamp(uvA, 0.0, 1.0));

            // Backward warp from Frame B
            vec2 uvB = v_TexCoord + flowVector * (1.0 - u_TimeRatio);
            vec4 colB = texture(u_FrameB, clamp(uvB, 0.0, 1.0));

            // Blend based on time ratio
            fragColor = mix(colA, colB, u_TimeRatio);
        }
    """.trimIndent()
}
