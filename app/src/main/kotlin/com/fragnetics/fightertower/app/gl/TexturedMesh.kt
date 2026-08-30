package com.fragnetics.fightertower.app.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val FLOATS_PER_VERTEX = 8 // position(3) + normal(3) + uv(2)
private const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4

/** Like [Mesh] but samples a texture instead of a baked vertex color, for loaded OBJ models. */
class TexturedMesh(vertexData: FloatArray) {
    private val vertexCount = vertexData.size / FLOATS_PER_VERTEX
    private val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertexData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertexData)
            position(0)
        }

    fun draw(program: TexturedGlProgram) {
        buffer.position(0)
        GLES20.glVertexAttribPointer(program.positionHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, buffer)
        GLES20.glEnableVertexAttribArray(program.positionHandle)

        buffer.position(3)
        GLES20.glVertexAttribPointer(program.normalHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, buffer)
        GLES20.glEnableVertexAttribArray(program.normalHandle)

        buffer.position(6)
        GLES20.glVertexAttribPointer(program.uvHandle, 2, GLES20.GL_FLOAT, false, STRIDE_BYTES, buffer)
        GLES20.glEnableVertexAttribArray(program.uvHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }
}
