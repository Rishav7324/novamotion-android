package com.novamotion.core.render

object ProceduralNoiseShader {

    /**
     * GLSL Shader for Multi-Octave Fractal Brownian Motion (FBM) & Simplex Noise.
     * Generates turbulent smoke, electric arcs, cosmic nebulas, and liquid displacement
     * procedurally entirely on the GPU without requiring external texture assets.
     */
    val FRAGMENT_FBM_NOISE = """#version 300 es
        precision highp float;

        in vec2 v_TexCoord;
        uniform float u_Time;
        uniform float u_Scale;
        uniform int u_Octaves;
        uniform float u_Roughness;
        uniform vec4 u_Color1;
        uniform vec4 u_Color2;
        out vec4 fragColor;

        // Hash function for pseudo-random gradient vectors
        vec2 hash2(vec2 p) {
            p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
            return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
        }

        // 2D Perlin / Simplex gradient noise
        float perlinNoise(vec2 p) {
            vec2 pi = floor(p);
            vec2 pf = fract(p);

            // Quintic Hermite interpolation curve
            vec2 w = pf * pf * pf * (pf * (pf * 6.0 - 15.0) + 10.0);

            return mix(
                mix(dot(hash2(pi + vec2(0.0, 0.0)), pf - vec2(0.0, 0.0)),
                    dot(hash2(pi + vec2(1.0, 0.0)), pf - vec2(1.0, 0.0)), w.x),
                mix(dot(hash2(pi + vec2(0.0, 1.0)), pf - vec2(0.0, 1.0)),
                    dot(hash2(pi + vec2(1.0, 1.0)), pf - vec2(1.0, 1.0)), w.x),
                w.y
            );
        }

        // Fractal Brownian Motion (FBM)
        float fbm(vec2 p, int octaves, float roughness) {
            float value = 0.0;
            float amplitude = 0.5;
            float frequency = 1.0;

            for (int i = 0; i < octaves; i++) {
                value += amplitude * perlinNoise(p * frequency);
                frequency *= 2.0;
                amplitude *= roughness;
            }
            return value;
        }

        void main() {
            vec2 p = v_TexCoord * u_Scale;
            // Add time evolution drift
            vec2 drift = vec2(u_Time * 0.2, u_Time * 0.15);

            // Domain warping for fluid swirling motion
            float q = fbm(p + drift, u_Octaves, u_Roughness);
            vec2 r = vec2(
                fbm(p + 1.0 * q + vec2(1.7, 9.2) + 0.15 * u_Time, u_Octaves, u_Roughness),
                fbm(p + 1.0 * q + vec2(8.3, 2.8) + 0.126 * u_Time, u_Octaves, u_Roughness)
            );

            float f = fbm(p + 4.0 * r, u_Octaves, u_Roughness);

            // Map [-1, 1] to [0, 1]
            float normalized = clamp(f * 0.5 + 0.5, 0.0, 1.0);

            // Color gradient blend
            fragColor = mix(u_Color1, u_Color2, normalized);
        }
    """.trimIndent()
}
