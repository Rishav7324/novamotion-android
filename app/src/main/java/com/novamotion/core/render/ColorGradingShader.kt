package com.novamotion.core.render

object ColorGradingShader {

    val FRAGMENT_COLOR_GRADING = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        
        uniform float u_Exposure;    // Default: 0.0 (-2.0 to 2.0)
        uniform float u_Contrast;    // Default: 1.0 (0.5 to 2.0)
        uniform float u_Saturation;  // Default: 1.0 (0.0 to 2.5)
        uniform float u_Temperature; // Default: 0.0 (-1.0 cool to 1.0 warm)
        uniform float u_Tint;        // Default: 0.0 (-1.0 green to 1.0 magenta)
        
        out vec4 fragColor;
        
        vec3 adjustTemperature(vec3 color, float temp, float tint) {
            color.r += temp * 0.1;
            color.b -= temp * 0.1;
            color.g += tint * 0.1;
            return color;
        }
        
        void main() {
            vec4 col = texture(u_Texture, v_TexCoord);
            vec3 rgb = col.rgb;
            
            // 1. Exposure adjustment
            rgb *= pow(2.0, u_Exposure);
            
            // 2. Contrast adjustment
            rgb = (rgb - vec3(0.5)) * u_Contrast + vec3(0.5);
            
            // 3. Saturation adjustment
            float luma = dot(rgb, vec3(0.2126, 0.7152, 0.0722));
            rgb = mix(vec3(luma), rgb, u_Saturation);
            
            // 4. White Balance (Temperature & Tint)
            rgb = adjustTemperature(rgb, u_Temperature, u_Tint);
            
            fragColor = vec4(clamp(rgb, 0.0, 1.0), col.a);
        }
    """.trimIndent()
}
