package com.fragnetics.fightertower.app.gl

import kotlin.math.sqrt

/**
 * Builds small unit meshes (roughly 1 world-unit across) with per-face brightness baked into the
 * color channel for a cheap pseudo-shaded look. Actual hue is supplied per-instance via the
 * uColorTint uniform at draw time, so these three meshes are built once and reused for every
 * tower tier and enemy on screen. Back-face culling is intentionally left off (see GameRenderer)
 * so any face-winding slip just costs a slightly duller face rather than a visible hole.
 */
object MeshFactory {

    fun unitCube(): Mesh = Mesh(cubeVertexData())
    fun unitPyramid(): Mesh = Mesh(pyramidVertexData())
    fun unitPlane(): Mesh = Mesh(planeVertexData())

    private fun cubeVertexData(): FloatArray {
        val h = 0.5f
        val nnn = floatArrayOf(-h, -h, -h); val pnn = floatArrayOf(h, -h, -h)
        val ppn = floatArrayOf(h, h, -h); val npn = floatArrayOf(-h, h, -h)
        val nnp = floatArrayOf(-h, -h, h); val pnp = floatArrayOf(h, -h, h)
        val ppp = floatArrayOf(h, h, h); val npp = floatArrayOf(-h, h, h)

        val out = mutableListOf<Float>()
        quad(nnp, pnp, ppp, npp, 0.95f, out) // front  (+Z)
        quad(pnn, nnn, npn, ppn, 0.65f, out) // back   (-Z)
        quad(npp, ppp, ppn, npn, 1.00f, out) // top    (+Y)
        quad(nnn, pnn, pnp, nnp, 0.50f, out) // bottom (-Y)
        quad(pnp, pnn, ppn, ppp, 0.80f, out) // right  (+X)
        quad(nnn, nnp, npp, npn, 0.75f, out) // left   (-X)
        return out.toFloatArray()
    }

    private fun pyramidVertexData(): FloatArray {
        val half = 0.5f
        val apex = floatArrayOf(0f, 0.5f, 0f)
        val bottom = -0.5f
        val a = floatArrayOf(-half, bottom, -half)
        val b = floatArrayOf(half, bottom, -half)
        val c = floatArrayOf(half, bottom, half)
        val d = floatArrayOf(-half, bottom, half)

        val out = mutableListOf<Float>()
        tri(a, b, apex, 0.85f, out)
        tri(b, c, apex, 0.70f, out)
        tri(c, d, apex, 0.90f, out)
        tri(d, a, apex, 0.60f, out)
        quad(d, c, b, a, 0.50f, out) // base, facing down
        return out.toFloatArray()
    }

    private fun planeVertexData(): FloatArray {
        val h = 0.5f
        val a = floatArrayOf(-h, 0f, -h)
        val b = floatArrayOf(h, 0f, -h)
        val c = floatArrayOf(h, 0f, h)
        val d = floatArrayOf(-h, 0f, h)
        val out = mutableListOf<Float>()
        quad(a, b, c, d, 1.0f, out)
        return out.toFloatArray()
    }

    private fun quad(
        v0: FloatArray, v1: FloatArray, v2: FloatArray, v3: FloatArray,
        shade: Float, out: MutableList<Float>
    ) {
        tri(v0, v1, v2, shade, out)
        tri(v0, v2, v3, shade, out)
    }

    private fun tri(v0: FloatArray, v1: FloatArray, v2: FloatArray, shade: Float, out: MutableList<Float>) {
        val normal = faceNormal(v0, v1, v2)
        for (v in listOf(v0, v1, v2)) {
            out.add(v[0]); out.add(v[1]); out.add(v[2])
            out.add(normal[0]); out.add(normal[1]); out.add(normal[2])
            out.add(shade); out.add(shade); out.add(shade)
        }
    }

    private fun faceNormal(p0: FloatArray, p1: FloatArray, p2: FloatArray): FloatArray {
        val ux = p1[0] - p0[0]; val uy = p1[1] - p0[1]; val uz = p1[2] - p0[2]
        val vx = p2[0] - p0[0]; val vy = p2[1] - p0[1]; val vz = p2[2] - p0[2]
        val nx = uy * vz - uz * vy
        val ny = uz * vx - ux * vz
        val nz = ux * vy - uy * vx
        val len = sqrt(nx * nx + ny * ny + nz * nz).let { if (it < 1e-6f) 1f else it }
        return floatArrayOf(nx / len, ny / len, nz / len)
    }
}
