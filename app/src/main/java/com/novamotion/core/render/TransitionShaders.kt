package com.novamotion.core.render

object TransitionShaders {

    val FRAGMENT_ZOOM_TRANSITION = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_TexFrom;
        uniform sampler2D u_TexTo;
        uniform float u_Progress; // 0.0 to 1.0
        out vec4 fragColor;
        
        void main() {
            vec2 center = vec2(0.5);
            // Zoom out Clip A then zoom in Clip B
            float scaleFrom = 1.0 + u_Progress * 0.8;
            float scaleTo = 1.8 - u_Progress * 0.8;
            
            vec2 uvFrom = (v_TexCoord - center) / scaleFrom + center;
            vec2 uvTo = (v_TexCoord - center) / scaleTo + center;
            
            vec4 colFrom = texture(u_TexFrom, uvFrom);
            vec4 colTo = texture(u_TexTo, uvTo);
            
            fragColor = mix(colFrom, colTo, smoothstep(0.4, 0.6, u_Progress));
        }
    """.trimIndent()

    val FRAGMENT_WHIP_PAN = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_TexFrom;
        uniform sampler2D u_TexTo;
        uniform float u_Progress; // 0.0 to 1.0
        out vec4 fragColor;
        
        void main() {
            float offset = smoothstep(0.0, 1.0, u_Progress);
            vec2 uvFrom = v_TexCoord + vec2(offset, 0.0);
            vec2 uvTo = v_TexCoord - vec2(1.0 - offset, 0.0);
            
            if (v_TexCoord.x + offset < 1.0) {
                fragColor = texture(u_TexFrom, uvFrom);
            } else {
                fragColor = texture(u_TexTo, uvTo);
            }
        }
    """.trimIndent()

    val FRAGMENT_GLITCH_FLASH = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_TexFrom;
        uniform sampler2D u_TexTo;
        uniform float u_Progress;
        out vec4 fragColor;
        
        void main() {
            vec4 colFrom = texture(u_TexFrom, v_TexCoord);
            vec4 colTo = texture(u_TexTo, v_TexCoord);
            
            vec4 base = mix(colFrom, colTo, step(0.5, u_Progress));
            float flash = sin(u_Progress * 3.14159) * 0.7;
            
            fragColor = base + vec4(flash, flash, flash, 0.0);
        }
    """.trimIndent()
}
