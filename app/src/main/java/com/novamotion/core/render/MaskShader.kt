package com.novamotion.core.render

object MaskShader {

    val FRAGMENT_MASK_COMPOSITE = """#version 300 es
        precision highp float;
        
        in vec2 v_TexCoord;
        uniform sampler2D u_LayerTexture;
        uniform sampler2D u_MaskTexture;
        uniform int u_MaskMode; // 0=None, 1=Alpha, 2=Alpha Invert, 3=Luma, 4=Luma Invert
        out vec4 fragColor;
        
        void main() {
            vec4 layerColor = texture(u_LayerTexture, v_TexCoord);
            vec4 maskColor = texture(u_MaskTexture, v_TexCoord);
            
            float matte = 1.0;
            if (u_MaskMode == 1) {
                // Alpha Matte
                matte = maskColor.a;
            } else if (u_MaskMode == 2) {
                // Inverted Alpha Matte
                matte = 1.0 - maskColor.a;
            } else if (u_MaskMode == 3) {
                // Luma Matte
                matte = dot(maskColor.rgb, vec3(0.299, 0.587, 0.114));
            } else if (u_MaskMode == 4) {
                // Inverted Luma Matte
                matte = 1.0 - dot(maskColor.rgb, vec3(0.299, 0.587, 0.114));
            }
            
            fragColor = vec4(layerColor.rgb, layerColor.a * matte);
        }
    """.trimIndent()
}
