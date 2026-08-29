package com.fragnetics.fightertower.app.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val FLOATS_PER_VERTEX = 9 // position(3) + normal(3) + baked-shade color(3)
private const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4

/** A static, non-indexed triangle mesh uploaded once and redrawn every frame via glDrawArrays. */
class Mesh(vertexData: FloatArray) {
    private val vertexCount = vertexData.size / FLOATS_PER_VERTEX
    private val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertexData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertexData)
            position(0)
        }

    fun draw(program: GlProgram) {
        buffer.position(0)
        GLES20.glVertexAttribPointer(program.positionHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, buffer)
        GLES20.glEnableVertexAttribArray(program.positionHandle)

        buffer.position(3)
        GLES20.glVertexAttribPointer(program.normalHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, buffer)
        GLES20.glEnableVertexAttribArray(program.normalHandle)

        buffer.position(6)
        GLES20.glVertexAttribPointer(program.colorHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, buffer)
        GLES20.glEnableVertexAttribArray(program.colorHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }
}
