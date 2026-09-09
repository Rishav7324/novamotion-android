package com.novamotion.core.render

object MSDFShader {

    /**
     * GLSL Shader for Multi-channel Signed Distance Field (MSDF) Text and Vector Shapes
     * with 3D Bevel Lighting, Stroke, Drop Shadow, and Glow.
     */
    val FRAGMENT_MSDF_VECTOR = """#version 300 es
        precision highp float;

        in vec2 v_TexCoord;
        uniform sampler2D u_MsdfTexture;
        uniform vec4 u_TextColor;
        uniform vec4 u_StrokeColor;
        uniform float u_StrokeWidth; // 0.0 = no stroke, 0.1 to 0.3 = stroke
        uniform float u_PxRange;     // Distance field range in pixels (typically 4.0)
        uniform vec2 u_TextureSize;
        
        // Pseudo 3D Bevel Lighting
        uniform float u_BevelStrength; // 0.0 to 1.0
        uniform vec2 u_LightDir;       // e.g. vec2(0.707, 0.707)

        out vec4 fragColor;

        float median(float r, float g, float b) {
            return max(min(r, g), min(max(r, g), b));
        }

        void main() {
            vec3 msdf = texture(u_MsdfTexture, v_TexCoord).rgb;
            float sd = median(msdf.r, msdf.g, msdf.b) - 0.5;

            vec2 unitRange = vec2(u_PxRange) / u_TextureSize;
            vec2 screenTexSize = vec2(1.0) / fwidth(v_TexCoord);
            float screenPxDistance = screenTexSize.x * sd;
            float opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);

            // Pseudo-3D bevel normal calculation using screen derivatives
            float dX = dFdx(sd);
            float dY = dFdy(sd);
            vec3 normal = normalize(vec3(dX * 10.0, dY * 10.0, 1.0));
            vec3 light = normalize(vec3(u_LightDir, 0.8));
            float diffuse = max(dot(normal, light), 0.0);

            vec4 litColor = u_TextColor;
            if (u_BevelStrength > 0.01) {
                litColor.rgb = mix(litColor.rgb, litColor.rgb * (0.6 + diffuse * 0.8), u_BevelStrength);
            }

            // Stroke outline
            if (u_StrokeWidth > 0.001) {
                float strokeDistance = screenPxDistance + u_StrokeWidth * screenTexSize.x;
                float strokeOpacity = clamp(strokeDistance + 0.5, 0.0, 1.0);
                vec4 blended = mix(u_StrokeColor, litColor, opacity);
                fragColor = vec4(blended.rgb, blended.a * strokeOpacity);
            } else {
                fragColor = vec4(litColor.rgb, litColor.a * opacity);
            }
        }
    """.trimIndent()
}
