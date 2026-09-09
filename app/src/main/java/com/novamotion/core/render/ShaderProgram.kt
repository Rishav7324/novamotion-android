package com.novamotion.core.render

import android.opengl.GLES30
import android.util.Log

class ShaderProgram(vertexCode: String, fragmentCode: String) {

    var programId: Int = 0
        private set

    init {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode)

        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES30.glGetProgramInfoLog(programId)
            Log.e("NovaMotionGL", "Shader linking failed: $error")
            GLES30.glDeleteProgram(programId)
            programId = 0
        }

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
    }

    fun use() {
        if (programId != 0) {
            GLES30.glUseProgram(programId)
        }
    }

    private fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            Log.e("NovaMotionGL", "Shader compilation failed ($type): $error")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
