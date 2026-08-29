package com.fragnetics.fightertower.app.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.fragnetics.fightertower.app.game.GameSession
import com.fragnetics.fightertower.core.Enemy
import com.fragnetics.fightertower.core.GameEvent
import com.fragnetics.fightertower.core.GridPos
import com.fragnetics.fightertower.core.TowerTier
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val BOLT_LIFETIME_SECONDS = 0.25f

/**
 * Renders the board, lane, towers and enemies in true 3D (OpenGL ES 2.0), and drives the game
 * simulation forward once per frame via [GameSession.update].
 */
class GameRenderer(private val session: GameSession) : GLSurfaceView.Renderer {

    private lateinit var program: GlProgram
    private lateinit var cubeMesh: Mesh
    private lateinit var pyramidMesh: Mesh
    private lateinit var planeMesh: Mesh

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

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.53f, 0.72f, 0.86f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        program = GlProgram()
        cubeMesh = MeshFactory.unitCube()
        pyramidMesh = MeshFactory.unitPyramid()
        planeMesh = MeshFactory.unitPlane()
        lastFrameNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projectionMatrix, 0, 55f, aspect, 1f, 60f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000.0).coerceIn(0.0, 0.1)
        lastFrameNanos = now

        val events = session.update(dt)
        val enemies = session.snapshotEnemies()
        spawnBoltsForEvents(events, enemies)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        program.use()

        Matrix.setLookAtM(viewMatrix, 0, -1f, 16f, 14f, -1f, 0f, 5f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        synchronized(exposedLock) {
            System.arraycopy(viewProjectionMatrix, 0, exposedViewProjection, 0, 16)
        }

        GLES20.glUniform3f(program.lightDirHandle, 0.4f, 0.9f, 0.5f)

        drawGround()
        drawBase()
        drawTowers()
        drawEnemies(enemies)
        drawBolts(dt.toFloat())
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
        floatArrayOf(WorldLayout.LANE_X, 0.45f, WorldLayout.laneWorldZ(enemy.progress))

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

    private fun drawGround() {
        val (columns, rows) = session.boardDimensions()
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val center = WorldLayout.cellCenter(GridPos(col, row))
                drawInstance(planeMesh, center[0], 0f, center[2], 1.4f, 1f, 1.4f, 0.29f, 0.36f, 0.24f)
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
        drawInstance(
            cubeMesh, WorldLayout.LANE_X, 1f, WorldLayout.LANE_BASE_Z - 1.2f,
            2.4f, 2f, 2.4f,
            0.25f, 0.45f, 0.85f
        )
    }

    private fun drawTowers() {
        for (tower in session.snapshotTowers()) {
            val center = WorldLayout.cellCenter(tower.position)
            val tierIndex = tower.tier.ordinal
            val height = 0.6f + tierIndex * 0.22f
            val width = 0.55f + tierIndex * 0.05f
            val color = towerColor(tower.tier)
            drawInstance(cubeMesh, center[0], height / 2f, center[2], width, height, width, color[0], color[1], color[2])
        }
    }

    private fun drawEnemies(enemies: List<Enemy>) {
        for (enemy in enemies) {
            val pos = enemyWorldPos(enemy)
            val healthFrac = (enemy.health / enemy.maxHealth).toFloat().coerceIn(0f, 1f)
            drawInstance(pyramidMesh, pos[0], 0.4f, pos[2], 0.7f, 0.8f, 0.7f, 0.85f, 0.15f + healthFrac * 0.2f, 0.15f)
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
