package com.novamotion.core.render

object ComplexVFXShaders {

    /**
     * Motion Tile (Tiles / Mirror Edges):
     * Mirrors and repeats edges seamlessly during shakes, zooms, and rotations,
     * completely eliminating ugly black borders.
     */
    val FRAGMENT_MOTION_TILE = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform vec2 u_Offset;
        uniform float u_Scale;
        uniform int u_Mirror; // 1 = Mirror edges, 0 = Repeat
        out vec4 fragColor;
        
        void main() {
            vec2 uv = (v_TexCoord - 0.5) * u_Scale + 0.5 + u_Offset;
            
            if (u_Mirror == 1) {
                // Triangle wave reflection formula
                uv = abs(mod(uv - 1.0, 2.0) - 1.0);
            } else {
                uv = fract(uv);
            }
            
            fragColor = texture(u_Texture, uv);
        }
    """.trimIndent()

    /**
     * Displacement Map:
     * Displaces pixels along X and Y based on red and green channels of a secondary map or procedural noise.
     */
    val FRAGMENT_DISPLACEMENT_MAP = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform sampler2D u_DisplacementMap;
        uniform float u_StrengthX;
        uniform float u_StrengthY;
        out vec4 fragColor;
        
        void main() {
            vec4 mapColor = texture(u_DisplacementMap, v_TexCoord);
            float dx = (mapColor.r - 0.5) * 2.0 * u_StrengthX;
            float dy = (mapColor.g - 0.5) * 2.0 * u_StrengthY;
            
            vec2 displacedUV = v_TexCoord + vec2(dx, dy);
            // Reflect edges if out of bounds
            displacedUV = abs(mod(displacedUV - 1.0, 2.0) - 1.0);
            
            fragColor = texture(u_Texture, displacedUV);
        }
    """.trimIndent()

    /**
     * Chroma Key (Green / Blue Screen Removal with Spill Suppression):
     */
    val FRAGMENT_CHROMA_KEY = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform vec3 u_KeyColor;   // e.g. vec3(0.0, 1.0, 0.0)
        uniform float u_Similarity; // Threshold
        uniform float u_Smoothness; // Edge falloff
        out vec4 fragColor;
        
        void main() {
            vec4 col = texture(u_Texture, v_TexCoord);
            float diff = distance(col.rgb, u_KeyColor);
            float alpha = smoothstep(u_Similarity, u_Similarity + u_Smoothness, diff);
            
            // Green spill suppression
            vec3 cleanRGB = col.rgb;
            if (u_KeyColor.g > 0.5) {
                cleanRGB.g = min(cleanRGB.g, max(cleanRGB.r, cleanRGB.b));
            }
            
            fragColor = vec4(cleanRGB, col.a * alpha);
        }
    """.trimIndent()
}
