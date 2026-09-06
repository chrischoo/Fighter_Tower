package com.fragnetics.fightertower.app.gl

import android.content.res.AssetManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.fragnetics.fightertower.app.game.GameSession
import com.fragnetics.fightertower.core.DiamondDrop
import com.fragnetics.fightertower.core.Enemy
import com.fragnetics.fightertower.core.GameEvent
import com.fragnetics.fightertower.core.GridPos
import com.fragnetics.fightertower.core.TowerTier
import kotlin.math.sin
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val BOLT_LIFETIME_SECONDS = 0.25f

/** World-space height (before per-tower scale) of each stackable tower body piece. */
private val BODY_SEGMENT_HEIGHTS = floatArrayOf(0.6f, 0.6f, 0.5f)

/** World-space height (before scale) of each castle piece, stacked base -> mid -> mid-windows -> roof. */
private val CASTLE_SEGMENT_HEIGHTS = floatArrayOf(1.01f, 1.01f, 1.01f, 1.35f)
private const val CASTLE_SCALE = 1.3f

/**
 * Renders the board, lane, towers and enemies in true 3D (OpenGL ES 2.0), and drives the game
 * simulation forward once per frame via [GameSession.update]. The ground, base building, and
 * projectile bolts are cheap procedural meshes (flat-shaded, see [GlProgram]); towers and enemies
 * are OBJ models loaded from assets and drawn with a separate textured program (see
 * [TexturedGlProgram]).
 */
class GameRenderer(
    private val session: GameSession,
    private val assets: AssetManager
) : GLSurfaceView.Renderer {

    private lateinit var program: GlProgram
    private lateinit var cubeMesh: Mesh
    private lateinit var planeMesh: Mesh

    private lateinit var texturedProgram: TexturedGlProgram
    private var modelTextureId: Int = 0
    private lateinit var bodyMeshes: List<TexturedMesh> // index 0 = bottom, 1 = middle, 2 = top
    private lateinit var weaponMeshes: List<TexturedMesh> // turret, cannon, ballista, catapult
    private lateinit var enemyMesh: TexturedMesh

    // The base is a stacked castle keep, textured from a separate atlas than the tower/enemy models.
    private var castleTextureId: Int = 0
    private lateinit var castleMeshes: List<TexturedMesh> // base, mid, mid-windows, roof, stacked bottom-up

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val exposedLock = Any()
    private val exposedViewProjection = FloatArray(16)

    private var lastFrameNanos = 0L

    private class Bolt(val fromX: Float, val fromY: Float, val fromZ: Float, val toX: Float, val toY: Float, val toZ: Float) {
        var life = BOLT_LIFETIME_SECONDS
    }
    private val bolts = mutableListOf<Bolt>()

    private var animClock = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.53f, 0.72f, 0.86f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        program = GlProgram()
        cubeMesh = MeshFactory.unitCube()
        planeMesh = MeshFactory.unitPlane()

        texturedProgram = TexturedGlProgram()
        modelTextureId = TextureLoader.loadFromAssets(assets, "textures/colormap.png")
        bodyMeshes = listOf(
            TexturedMesh(ObjLoader.load(assets, "models/tower-round-bottom-a.obj")),
            TexturedMesh(ObjLoader.load(assets, "models/tower-round-middle-a.obj")),
            TexturedMesh(ObjLoader.load(assets, "models/tower-round-top-a.obj")),
        )
        weaponMeshes = listOf(
            TexturedMesh(ObjLoader.load(assets, "models/weapon-turret.obj")),
            TexturedMesh(ObjLoader.load(assets, "models/weapon-cannon.obj")),
            TexturedMesh(ObjLoader.load(assets, "models/weapon-ballista.obj")),
            TexturedMesh(ObjLoader.load(assets, "models/weapon-catapult.obj")),
        )
        enemyMesh = TexturedMesh(ObjLoader.load(assets, "models/enemy-ufo-a.obj"))

        castleTextureId = TextureLoader.loadFromAssets(assets, "textures/castle-colormap.png")
        castleMeshes = listOf(
            TexturedMesh(ObjLoader.load(assets, "models/castle-base.obj")),
            TexturedMesh(ObjLoader.load(assets, "models/castle-mid.obj")),
            TexturedMesh(ObjLoader.load(assets, "models/castle-mid-windows.obj")),
            TexturedMesh(ObjLoader.load(assets, "models/castle-roof.obj")),
        )

        lastFrameNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projectionMatrix, 0, 60f, aspect, 1f, 60f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000.0).coerceIn(0.0, 0.1)
        lastFrameNanos = now

        val events = session.update(dt)
        val enemies = session.snapshotEnemies()
        val diamonds = session.snapshotDiamonds()
        spawnBoltsForEvents(events, enemies)
        animClock += dt.toFloat()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 18f, 16f, 0f, 0f, 5f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        synchronized(exposedLock) {
            System.arraycopy(viewProjectionMatrix, 0, exposedViewProjection, 0, 16)
        }

        program.use()
        GLES20.glUniform3f(program.lightDirHandle, 0.4f, 0.9f, 0.5f)
        drawGround()

        texturedProgram.use()
        GLES20.glUniform3f(texturedProgram.lightDirHandle, 0.4f, 0.9f, 0.5f)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(texturedProgram.textureHandle, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, castleTextureId)
        drawBase()

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, modelTextureId)
        drawTowers()
        drawEnemies(enemies)

        program.use()
        drawBolts(dt.toFloat())
        drawDiamonds(diamonds)
    }

    /** Thread-safe: called from the touch controller on the main thread. */
    fun currentViewProjectionMatrix(): FloatArray = synchronized(exposedLock) { exposedViewProjection.copyOf() }

    private fun spawnBoltsForEvents(events: List<GameEvent>, enemies: List<Enemy>) {
        for (event in events) {
            if (event !is GameEvent.TowerAttacked) continue
            val enemy = enemies.firstOrNull { it.id == event.enemyId } ?: continue
            val towerWorld = WorldLayout.cellCenter(event.towerPos)
            val enemyWorld = enemyWorldPos(enemy)
            bolts += Bolt(towerWorld[0], 0.9f, towerWorld[2], enemyWorld[0], 0.5f, enemyWorld[2])
        }
    }

    private fun enemyWorldPos(enemy: Enemy): FloatArray =
        floatArrayOf(WorldLayout.LANE_X, 0.3f, WorldLayout.laneWorldZ(enemy.progress))

    private fun drawInstance(
        mesh: Mesh, x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(program.mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(program.modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(program.colorTintHandle, r, g, b)
        mesh.draw(program)
    }

    private fun drawTexturedInstance(
        mesh: TexturedMesh, x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(texturedProgram.mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(texturedProgram.modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(texturedProgram.colorTintHandle, r, g, b)
        mesh.draw(texturedProgram)
    }

    private fun drawGround() {
        val (columns, rows) = session.boardDimensions()
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val center = WorldLayout.cellCenter(GridPos(col, row))
                drawInstance(planeMesh, center[0], 0f, center[2], 1.2f, 1f, 1.2f, 0.29f, 0.36f, 0.24f)
            }
        }
        val laneLength = WorldLayout.LANE_SPAWN_Z - WorldLayout.LANE_BASE_Z
        drawInstance(
            planeMesh,
            WorldLayout.LANE_X, 0f, (WorldLayout.LANE_SPAWN_Z + WorldLayout.LANE_BASE_Z) / 2f,
            2.2f, 1f, laneLength,
            0.55f, 0.42f, 0.30f
        )
    }

    private fun drawBase() {
        val x = WorldLayout.LANE_X
        val z = WorldLayout.LANE_BASE_Z - 1.2f
        var y = 0f
        for ((segment, mesh) in castleMeshes.withIndex()) {
            drawTexturedInstance(mesh, x, y, z, CASTLE_SCALE, CASTLE_SCALE, CASTLE_SCALE, 1f, 1f, 1f)
            y += CASTLE_SEGMENT_HEIGHTS[segment] * CASTLE_SCALE
        }
    }

    private fun drawTowers() {
        for (tower in session.snapshotTowers()) {
            val center = WorldLayout.cellCenter(tower.position)
            val visual = TowerVisuals.forTier(tower.tier)
            val color = towerColor(tower.tier)
            val scale = 0.85f + tower.tier.ordinal * 0.03f

            var y = 0f
            for (segment in 0 until visual.bodySegments) {
                drawTexturedInstance(bodyMeshes[segment], center[0], y, center[2], scale, scale, scale, color[0], color[1], color[2])
                y += BODY_SEGMENT_HEIGHTS[segment] * scale
            }
            val weaponMesh = weaponMeshes[visual.weaponIndex]
            drawTexturedInstance(weaponMesh, center[0], y, center[2], scale, scale, scale, color[0], color[1], color[2])
        }
    }

    private fun drawEnemies(enemies: List<Enemy>) {
        for (enemy in enemies) {
            val pos = enemyWorldPos(enemy)
            val healthFrac = (enemy.health / enemy.maxHealth).toFloat().coerceIn(0f, 1f)
            drawTexturedInstance(enemyMesh, pos[0], pos[1], pos[2], 0.9f, 0.9f, 0.9f, 0.85f, 0.15f + healthFrac * 0.2f, 0.15f)
        }
    }

    private fun drawBolts(dt: Float) {
        val iterator = bolts.iterator()
        while (iterator.hasNext()) {
            val bolt = iterator.next()
            bolt.life -= dt
            if (bolt.life <= 0f) {
                iterator.remove()
                continue
            }
            val t = 1f - (bolt.life / BOLT_LIFETIME_SECONDS)
            val x = bolt.fromX + (bolt.toX - bolt.fromX) * t
            val y = bolt.fromY + (bolt.toY - bolt.fromY) * t
            val z = bolt.fromZ + (bolt.toZ - bolt.fromZ) * t
            drawInstance(cubeMesh, x, y, z, 0.15f, 0.15f, 0.15f, 1f, 0.9f, 0.3f)
        }
    }

    /** Dropped diamond piles: a small bobbing cube that flickers just before it expires. */
    private fun drawDiamonds(diamonds: List<DiamondDrop>) {
        for (diamond in diamonds) {
            val z = WorldLayout.laneWorldZ(diamond.progress)
            val bob = 0.08f * sin(animClock * 3f + diamond.id)
            val warning = diamond.timeRemaining < 2.0
            val flicker = if (warning && (animClock * 8f).toInt() % 2 == 0) 0.35f else 1f
            drawInstance(
                cubeMesh, WorldLayout.LANE_X, 0.5f + bob, z,
                0.3f, 0.3f, 0.3f,
                0.25f * flicker, 0.85f * flicker, 0.95f * flicker
            )
        }
    }

    private fun towerColor(tier: TowerTier): FloatArray = when (tier) {
        TowerTier.SCOUT -> floatArrayOf(0.55f, 0.75f, 0.55f)
        TowerTier.GUNNER -> floatArrayOf(0.35f, 0.65f, 0.85f)
        TowerTier.CANNON -> floatArrayOf(0.85f, 0.65f, 0.25f)
        TowerTier.HEAVY_CANNON -> floatArrayOf(0.85f, 0.45f, 0.20f)
        TowerTier.SIEGE_TOWER -> floatArrayOf(0.70f, 0.30f, 0.65f)
        TowerTier.RAILGUN -> floatArrayOf(0.35f, 0.85f, 0.85f)
        TowerTier.JUGGERNAUT -> floatArrayOf(0.85f, 0.25f, 0.25f)
        TowerTier.TITAN -> floatArrayOf(0.95f, 0.85f, 0.25f)
    }
}
