package io.github.sceneview.demo.demos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import io.github.sceneview.demo.demos.internal.ArDebugFrame
import io.github.sceneview.demo.demos.internal.DebugMesh
import io.github.sceneview.demo.demos.internal.DebugPose
import io.github.sceneview.demo.demos.internal.LiveWallMotion
import io.github.sceneview.demo.demos.internal.LiveWallSnapshot
import io.github.sceneview.demo.demos.internal.LiveWallSource
import io.github.sceneview.demo.demos.internal.PlaneWallSource
import io.github.sceneview.demo.demos.internal.ScanGuide
import io.github.sceneview.demo.demos.internal.ScanTurn
import io.github.sceneview.demo.demos.internal.WallMesh
import io.github.sceneview.demo.demos.internal.openFloorEdges
import io.github.sceneview.demo.demos.internal.update
import io.github.sceneview.demo.theme.SceneViewTokens.ArOverlay
import io.github.sceneview.loaders.MaterialLoader
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The walls 3D Scan recognises while it scans a room (#4540 live walls), fed frame by frame from
 * the live view's `observe` after [ScanCoverageState]: the [source]'s walls, eased by [motion], the
 * floor edges no wall stands on yet, and what the HUD reads off them — [wallCount], the
 * [completion] gauge, [done], and the [turn] to the next gap. Compose state changes only when its
 * value does; the rest is read by [ScanWallLayer] on the render thread's frame.
 */
internal class ScanWallsState(private val source: LiveWallSource = PlaneWallSource()) {
    val motion = LiveWallMotion()

    var snapshot: LiveWallSnapshot = LiveWallSnapshot.Empty
        private set

    /** The floor edges seen with no wall on them: where the room still opens onto the unscanned. */
    var openEdges: List<FloatArray> = emptyList()
        private set

    /** The gap the cue points at, `null` without one. */
    var cueEdge: FloatArray? = null
        private set

    var wallCount by mutableIntStateOf(0)
        private set

    /** The room's scan done so far, `0..1` ([ScanGuide.completion]); it only grows. */
    var completion by mutableFloatStateOf(0f)
        private set

    /** The walls close the room, or the gauge is full: the HUD invites Finish. */
    var done by mutableStateOf(false)
        private set

    var turn by mutableStateOf<ScanTurn?>(null)
        private set

    private var extentFor: List<Pair<Float, Float>>? = null
    private var gridVersion = -1

    fun observe(frame: ArDebugFrame, coverage: ScanCoverageState, pose: DebugPose?) {
        val next = source.update(frame)
        val wallsChanged = next !== snapshot
        snapshot = next
        if (wallsChanged || coverage.extent !== extentFor || coverage.grid.version != gridVersion) {
            extentFor = coverage.extent
            gridVersion = coverage.grid.version
            openEdges = openFloorEdges(coverage.extent, next.walls)
            val closed = ScanGuide.closed(next.walls.size, openEdges)
            val share = coverage.grid.coveredShareOf(coverage.extent)
            completion = max(completion, ScanGuide.completion(next.walls.size, share, closed))
            if (next.walls.size != wallCount) wallCount = next.walls.size
            val finished = closed || completion >= 1f
            if (finished != done) done = finished
        }
        val cue = if (done) null else ScanGuide.cue(pose, openEdges)
        cueEdge = cue?.edge
        if (cue?.turn != turn) turn = cue?.turn
    }

    fun reset() {
        source.reset()
        motion.reset()
        snapshot = LiveWallSnapshot.Empty
        openEdges = emptyList()
        cueEdge = null
        extentFor = null
        gridVersion = -1
        wallCount = 0
        completion = 0f
        done = false
        turn = null
    }
}

/**
 * Draws a [ScanWallsState] in the live view: each wall a translucent quad in Measure's colour
 * ([WallMesh.FILL_ALPHA] of `accentProgress`) rising from its floor line, outlined; the floor edges
 * no wall stands on yet dashed in `accentGuidance` with a low ghost wall, the one the cue points at
 * standing a little taller. The layers go under the points, so the points stay on top.
 */
internal class ScanWallLayer(engine: Engine, private val materialLoader: MaterialLoader) {
    private val fillMaterial = layerMaterial(ArOverlay.accentProgress.copy(alpha = WallMesh.FILL_ALPHA))
    private val lineMaterial = layerMaterial(ArOverlay.accentProgress)
    private val openMaterial = layerMaterial(ArOverlay.accentGuidance)
    private val ghostMaterial = layerMaterial(ArOverlay.accentGuidance.copy(alpha = GHOST_ALPHA))

    private val fill = DebugLayerNode(engine, fillMaterial, priority = FILL_PRIORITY)
    private val lines = DebugLayerNode(engine, lineMaterial, priority = LINE_PRIORITY)
    private val open = DebugLayerNode(engine, openMaterial, priority = LINE_PRIORITY)
    private val ghost = DebugLayerNode(engine, ghostMaterial, priority = FILL_PRIORITY)

    val nodes = listOf(ghost, fill, open, lines)

    private val fillMesh = DebugMesh()
    private val lineMesh = DebugMesh()
    private val openMesh = DebugMesh()
    private val ghostMesh = DebugMesh()
    private var openFor: List<FloatArray>? = null
    private var cueFor: FloatArray? = null
    private var openFloorY = Float.NaN

    /**
     * Eases the walls [dt] seconds on and re-uploads what moved, or hides them all while not
     * [shown] — the save's orbit shows the cloud alone. On the main thread.
     */
    fun sync(state: ScanWallsState, dt: Float, shown: Boolean = true) {
        nodes.forEach { if (it.isVisible != shown) it.isVisible = shown }
        if (!shown) return
        val motion = state.motion
        if (motion.step(state.snapshot, dt)) {
            fillMesh.clear()
            lineMesh.clear()
            for (wall in motion.shown) {
                val height = motion.height * wall.rise
                if (height <= 0f) continue
                val y = motion.floorY
                val base = fillMesh.vertexCount
                WallMesh.quad(wall.ax, y, wall.az, wall.bx, y, wall.bz, height) { x, vy, z -> fillMesh.vertex(x, vy, z) }
                fillMesh.quad(base, base + 1, base + 2, base + 3)
                outline(lineMesh, wall.ax, wall.az, wall.bx, wall.bz, y, height)
            }
            fill.upload(fillMesh)
            lines.upload(lineMesh)
        }
        val floorY = motion.floorY
        if (state.openEdges !== openFor || state.cueEdge !== cueFor || floorY != openFloorY) {
            openFor = state.openEdges
            cueFor = state.cueEdge
            openFloorY = floorY
            openMesh.clear()
            ghostMesh.clear()
            if (motion.height > 0f) {
                for (edge in state.openEdges) {
                    dashes(openMesh, edge, floorY + LIFT_M)
                    val tall = if (edge === state.cueEdge) CUE_GHOST_M else GHOST_M
                    val base = ghostMesh.vertexCount
                    WallMesh.quad(edge[0], floorY, edge[1], edge[2], floorY, edge[3], tall) { x, y, z ->
                        ghostMesh.vertex(x, y, z)
                    }
                    ghostMesh.quad(base, base + 1, base + 2, base + 3)
                }
            }
            open.upload(openMesh)
            ghost.upload(ghostMesh)
        }
    }

    /** On the main thread, after [nodes] left the scene. */
    fun destroy() {
        nodes.forEach { it.destroy() }
        listOf(fillMaterial, lineMaterial, openMaterial, ghostMaterial).forEach { materialLoader.destroyMaterialInstance(it) }
    }

    private fun layerMaterial(color: Color): MaterialInstance =
        materialLoader.createUnlitColorInstance(color).apply { setCullingMode(Material.CullingMode.NONE) }

    private companion object {
        /** Under the points (the debug layers' default 4): the cloud reads through the walls. */
        const val FILL_PRIORITY = 2
        const val LINE_PRIORITY = 3

        /** A wall's outline, in metres: a hairline from across the room, not a bar up close. */
        const val LINE_M = 0.012f

        /** The open edge's dashes, lifted off the floor so the floor's points do not hide them. */
        const val DASH_M = 0.12f
        const val GAP_M = 0.08f
        const val LIFT_M = 0.01f

        /** The ghost of a wall not seen yet: knee-high, and waist-high where the cue points. */
        const val GHOST_M = 0.35f
        const val CUE_GHOST_M = 0.9f
        const val GHOST_ALPHA = 0.18f

        /** The wall's base, top and two sides as thin ribbons in its own plane. */
        fun outline(mesh: DebugMesh, ax: Float, az: Float, bx: Float, bz: Float, y: Float, height: Float) {
            val length = sqrt((bx - ax) * (bx - ax) + (bz - az) * (bz - az))
            if (length < 1e-4f) return
            val ux = (bx - ax) / length * LINE_M
            val uz = (bz - az) / length * LINE_M
            val half = LINE_M / 2f
            // Base and top: ribbons along the wall, LINE_M tall.
            ribbon(mesh, ax, y, az, bx, y, bz, 0f, LINE_M, 0f)
            ribbon(mesh, ax, y + height - half, az, bx, y + height - half, bz, 0f, LINE_M, 0f)
            // Sides: ribbons up the wall, LINE_M wide, inside its ends.
            ribbon(mesh, ax + ux / 2f, y, az + uz / 2f, ax + ux / 2f, y + height, az + uz / 2f, ux, 0f, uz)
            ribbon(mesh, bx - ux / 2f, y, bz - uz / 2f, bx - ux / 2f, y + height, bz - uz / 2f, ux, 0f, uz)
        }

        /** A ribbon from a to b, [wx]/[wy]/[wz] wide across it, centred on the line. */
        @Suppress("LongParameterList") // two points and a width vector, flat for a render loop
        fun ribbon(
            mesh: DebugMesh,
            ax: Float, ay: Float, az: Float,
            bx: Float, by: Float, bz: Float,
            wx: Float, wy: Float, wz: Float,
        ) {
            val hx = wx / 2f
            val hy = wy / 2f
            val hz = wz / 2f
            val a = mesh.vertex(ax - hx, ay - hy, az - hz)
            val b = mesh.vertex(bx - hx, by - hy, bz - hz)
            val c = mesh.vertex(bx + hx, by + hy, bz + hz)
            val d = mesh.vertex(ax + hx, ay + hy, az + hz)
            mesh.quad(a, b, c, d)
        }

        /** The open [edge] dashed on the floor at [y], each dash a flat ribbon. */
        fun dashes(mesh: DebugMesh, edge: FloatArray, y: Float) {
            val dx = edge[2] - edge[0]
            val dz = edge[3] - edge[1]
            val length = sqrt(dx * dx + dz * dz)
            if (length < 1e-4f) return
            val ux = dx / length
            val uz = dz / length
            // Across the edge, on the floor: twice a wall line, so it reads from standing height.
            val wx = -uz * LINE_M * 2f
            val wz = ux * LINE_M * 2f
            var t = 0f
            while (t < length) {
                val end = minOf(t + DASH_M, length)
                ribbon(
                    mesh,
                    edge[0] + ux * t, y, edge[1] + uz * t,
                    edge[0] + ux * end, y, edge[1] + uz * end,
                    wx, 0f, wz,
                )
                t += DASH_M + GAP_M
            }
        }
    }
}
