package com.novamotion.core.render

object Shaders {

    val VERTEX_QUAD = """#version 300 es
        layout (location = 0) in vec4 a_Position;
        layout (location = 1) in vec2 a_TexCoord;
        
        uniform mat4 u_MVPMatrix;
        out vec2 v_TexCoord;
        
        void main() {
            gl_Position = u_MVPMatrix * a_Position;
            v_TexCoord = a_TexCoord;
        }
    """.trimIndent()

    val FRAGMENT_TEXTURE_BASE = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform float u_Opacity;
        uniform vec4 u_TintColor;
        out vec4 fragColor;
        
        void main() {
            vec4 texColor = texture(u_Texture, v_TexCoord);
            fragColor = texColor * u_TintColor * u_Opacity;
        }
    """.trimIndent()

    val FRAGMENT_MOTION_BLUR = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform vec2 u_Velocity;
        uniform int u_Samples;
        out vec4 fragColor;
        
        void main() {
            vec4 sum = vec4(0.0);
            int samples = max(u_Samples, 1);
            vec2 step = u_Velocity / float(samples);
            
            for (int i = 0; i < samples; i++) {
                vec2 offset = step * (float(i) - float(samples) * 0.5);
                sum += texture(u_Texture, v_TexCoord + offset);
            }
            fragColor = sum / float(samples);
        }
    """.trimIndent()

    val FRAGMENT_CHROMATIC_ABERRATION = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform float u_Intensity;
        out vec4 fragColor;
        
        void main() {
            vec2 dir = v_TexCoord - vec2(0.5);
            vec2 offset = dir * u_Intensity;
            
            float r = texture(u_Texture, v_TexCoord + offset).r;
            float g = texture(u_Texture, v_TexCoord).g;
            float b = texture(u_Texture, v_TexCoord - offset).b;
            float a = texture(u_Texture, v_TexCoord).a;
            
            fragColor = vec4(r, g, b, a);
        }
    """.trimIndent()

    val FRAGMENT_BLOOM_GLOW = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform float u_Threshold;
        uniform float u_Intensity;
        out vec4 fragColor;
        
        void main() {
            vec4 col = texture(u_Texture, v_TexCoord);
            float brightness = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));
            vec3 glow = (brightness > u_Threshold) ? col.rgb * u_Intensity : vec3(0.0);
            fragColor = vec4(col.rgb + glow, col.a);
        }
    """.trimIndent()
}
