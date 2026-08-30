package com.fragnetics.fightertower.app.gl

import android.content.res.AssetManager

/**
 * Minimal Wavefront OBJ parser for the pre-triangulated, single-texture models bundled under
 * assets/models (Kenney's Tower Defense Kit, CC0). Only handles the subset those files use:
 * v/vn/vt/f lines with triangulated f entries in v/vt/vn form. Expands straight into a flat,
 * non-indexed vertex list (position3 + normal3 + uv2) matching [TexturedMesh]'s layout, since
 * that's what glDrawArrays wants and these meshes are small enough that duplication is cheap.
 */
object ObjLoader {
    fun load(assets: AssetManager, path: String): FloatArray {
        val positions = mutableListOf<FloatArray>()
        val normals = mutableListOf<FloatArray>()
        val uvs = mutableListOf<FloatArray>()
        val out = mutableListOf<Float>()

        assets.open(path).bufferedReader().useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line[0] == '#') continue
                val tokens = line.split(Regex("\\s+"))
                when (tokens[0]) {
                    "v" -> positions += floatArrayOf(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat())
                    "vn" -> normals += floatArrayOf(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat())
                    // OBJ v=0 is the bottom of the texture; Android's decoded Bitmap has row 0 at
                    // the top, so flip here once rather than flipping every uploaded texture.
                    "vt" -> uvs += floatArrayOf(tokens[1].toFloat(), 1f - tokens[2].toFloat())
                    "f" -> {
                        for (i in 1..3) {
                            val parts = tokens[i].split("/")
                            val vi = resolveIndex(parts[0], positions.size)
                            val ti = parts.getOrNull(1)?.let { resolveIndex(it, uvs.size) } ?: -1
                            val ni = parts.getOrNull(2)?.let { resolveIndex(it, normals.size) } ?: -1
                            val p = positions[vi]
                            val n = if (ni >= 0) normals[ni] else FALLBACK_NORMAL
                            val uv = if (ti >= 0) uvs[ti] else FALLBACK_UV
                            out.add(p[0]); out.add(p[1]); out.add(p[2])
                            out.add(n[0]); out.add(n[1]); out.add(n[2])
                            out.add(uv[0]); out.add(uv[1])
                        }
                    }
                }
            }
        }
        return out.toFloatArray()
    }

    private val FALLBACK_NORMAL = floatArrayOf(0f, 1f, 0f)
    private val FALLBACK_UV = floatArrayOf(0f, 0f)

    private fun resolveIndex(token: String, count: Int): Int {
        val idx = token.toInt()
        return if (idx > 0) idx - 1 else count + idx
    }
}
