package com.novamotion.core.render

import android.opengl.GLES30
import com.novamotion.core.media.Lut3DData

object Lut3DShader {

    /**
     * True 3D Texture sampling shader for OpenGL ES 3.0+.
     * Uses hardware trilinear filtering across the 3D RGB color volume.
     */
    val FRAGMENT_LUT_3D = """#version 300 es
        precision mediump float;
        precision mediump sampler3D;

        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform sampler3D u_Lut3D;
        uniform float u_Intensity; // 0.0 to 1.0 blend
        
        out vec4 fragColor;

        void main() {
            vec4 baseColor = texture(u_Texture, v_TexCoord);
            
            // Texture coordinates in 3D LUT space [0..1] with half-texel offset for perfect precision
            float lutSize = float(textureSize(u_Lut3D, 0).x);
            vec3 lutCoord = (baseColor.rgb * (lutSize - 1.0) + 0.5) / lutSize;
            
            vec3 gradedColor = texture(u_Lut3D, lutCoord).rgb;
            fragColor = vec4(mix(baseColor.rgb, gradedColor, u_Intensity), baseColor.a);
        }
    """.trimIndent()

    /**
     * Fallback 2D Color Atlas LUT shader (e.g. 512x512 with 64x64 slices).
     */
    val FRAGMENT_LUT_2D_ATLAS = """#version 300 es
        precision highp float;

        in vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform sampler2D u_LutAtlas;
        uniform float u_Intensity;
        
        out vec4 fragColor;

        void main() {
            vec4 baseColor = texture(u_Texture, v_TexCoord);
            
            float blueColor = baseColor.b * 63.0;
            
            vec2 quad1;
            quad1.y = floor(floor(blueColor) / 8.0);
            quad1.x = floor(blueColor) - (quad1.y * 8.0);
            
            vec2 quad2;
            quad2.y = floor(ceil(blueColor) / 8.0);
            quad2.x = ceil(blueColor) - (quad2.y * 8.0);
            
            vec2 texPos1;
            texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * baseColor.r);
            texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * baseColor.g);
            
            vec2 texPos2;
            texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * baseColor.r);
            texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * baseColor.g);
            
            vec4 newColor1 = texture(u_LutAtlas, texPos1);
            vec4 newColor2 = texture(u_LutAtlas, texPos2);
            
            vec4 graded = mix(newColor1, newColor2, fract(blueColor));
            fragColor = vec4(mix(baseColor.rgb, graded.rgb, u_Intensity), baseColor.a);
        }
    """.trimIndent()

    /**
     * Uploads Lut3DData into a native OpenGL ES 3.0 3D texture handle.
     */
    fun create3DTexture(lutData: Lut3DData): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val texId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, texId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glTexImage3D(
            GLES30.GL_TEXTURE_3D,
            0,
            GLES30.GL_RGBA8,
            lutData.size,
            lutData.size,
            lutData.size,
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            lutData.data
        )

        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, 0)
        return texId
    }
}
