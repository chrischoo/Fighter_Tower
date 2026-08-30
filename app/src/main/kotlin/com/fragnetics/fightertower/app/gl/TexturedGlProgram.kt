package com.fragnetics.fightertower.app.gl

import android.opengl.GLES20

private object TexturedShaderSource {
    const val VERTEX = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        attribute vec3 aPosition;
        attribute vec3 aNormal;
        attribute vec2 aUV;
        varying vec2 vUV;
        varying vec3 vNormal;
        void main() {
            gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            vNormal = mat3(uModelMatrix) * aNormal;
            vUV = aUV;
        }
    """

    const val FRAGMENT = """
        precision mediump float;
        varying vec2 vUV;
        varying vec3 vNormal;
        uniform sampler2D uTexture;
        uniform vec3 uLightDir;
        uniform vec3 uColorTint;
        void main() {
            vec3 n = normalize(vNormal);
            float diffuse = max(dot(n, normalize(uLightDir)), 0.0);
            float lighting = 0.45 + diffuse * 0.65;
            vec4 texColor = texture2D(uTexture, vUV);
            gl_FragColor = vec4(texColor.rgb * uColorTint * lighting, texColor.a);
        }
    """
}

/** Shader program for textured OBJ models (towers, enemies) — see [GlProgram] for the flat-shaded one. */
class TexturedGlProgram {
    val handle: Int = GLES20.glCreateProgram().also { program ->
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, TexturedShaderSource.VERTEX)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, TexturedShaderSource.FRAGMENT)
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        check(linkStatus[0] == GLES20.GL_TRUE) {
            "Shader link failed: ${GLES20.glGetProgramInfoLog(program)}"
        }
    }

    val positionHandle: Int = GLES20.glGetAttribLocation(handle, "aPosition")
    val normalHandle: Int = GLES20.glGetAttribLocation(handle, "aNormal")
    val uvHandle: Int = GLES20.glGetAttribLocation(handle, "aUV")
    val mvpMatrixHandle: Int = GLES20.glGetUniformLocation(handle, "uMVPMatrix")
    val modelMatrixHandle: Int = GLES20.glGetUniformLocation(handle, "uModelMatrix")
    val lightDirHandle: Int = GLES20.glGetUniformLocation(handle, "uLightDir")
    val colorTintHandle: Int = GLES20.glGetUniformLocation(handle, "uColorTint")
    val textureHandle: Int = GLES20.glGetUniformLocation(handle, "uTexture")

    fun use() {
        GLES20.glUseProgram(handle)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) {
            "Shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}"
        }
        return shader
    }
}
