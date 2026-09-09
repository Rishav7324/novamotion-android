package com.novamotion.core.render

object VFXShaderLibrary {

    val FRAGMENT_WAVE_WARP = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform float u_Frequency;
        uniform float u_Amplitude;
        uniform float u_Phase;
        out vec4 fragColor;
        
        void main() {
            vec2 uv = v_TexCoord;
            uv.x += sin(uv.y * u_Frequency + u_Phase) * u_Amplitude;
            fragColor = texture(u_Texture, uv);
        }
    """.trimIndent()

    val FRAGMENT_MIRROR = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform int u_Mode; // 0=Horizontal, 1=Vertical, 2=Quad
        out vec4 fragColor;
        
        void main() {
            vec2 uv = v_TexCoord;
            if (u_Mode == 0) {
                uv.x = abs(uv.x - 0.5) * 2.0;
            } else if (u_Mode == 1) {
                uv.y = abs(uv.y - 0.5) * 2.0;
            } else {
                uv = abs(uv - vec2(0.5)) * 2.0;
            }
            fragColor = texture(u_Texture, uv);
        }
    """.trimIndent()

    val FRAGMENT_RADIAL_ZOOM_BLUR = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform vec2 u_Center;
        uniform float u_Intensity;
        out vec4 fragColor;
        
        void main() {
            vec2 dir = v_TexCoord - u_Center;
            vec4 sum = vec4(0.0);
            const int samples = 10;
            
            for (int i = 0; i < samples; i++) {
                float scale = 1.0 - u_Intensity * (float(i) / float(samples));
                sum += texture(u_Texture, u_Center + dir * scale);
            }
            fragColor = sum / float(samples);
        }
    """.trimIndent()

    val FRAGMENT_DIGITAL_GLITCH = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform float u_Time;
        uniform float u_Amount;
        out vec4 fragColor;
        
        float hash(vec2 p) {
            return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
        }
        
        void main() {
            vec2 uv = v_TexCoord;
            float lineNoise = hash(vec2(floor(uv.y * 30.0), floor(u_Time * 15.0)));
            if (lineNoise < u_Amount) {
                uv.x += (hash(vec2(u_Time, uv.y)) - 0.5) * 0.08 * u_Amount;
            }
            
            float r = texture(u_Texture, uv + vec2(0.005 * u_Amount, 0.0)).r;
            float g = texture(u_Texture, uv).g;
            float b = texture(u_Texture, uv - vec2(0.005 * u_Amount, 0.0)).b;
            float a = texture(u_Texture, uv).a;
            
            fragColor = vec4(r, g, b, a);
        }
    """.trimIndent()

    val FRAGMENT_VIGNETTE = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform float u_Radius;
        uniform float u_Softness;
        out vec4 fragColor;
        
        void main() {
            vec4 col = texture(u_Texture, v_TexCoord);
            float dist = distance(v_TexCoord, vec2(0.5));
            float vignette = smoothstep(u_Radius, u_Radius - u_Softness, dist);
            fragColor = vec4(col.rgb * vignette, col.a);
        }
    """.trimIndent()
}
