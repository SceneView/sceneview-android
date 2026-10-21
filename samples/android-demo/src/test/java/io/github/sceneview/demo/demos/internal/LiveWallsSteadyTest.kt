package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The live walls hold still while ARCore refits its planes. A wall a plane stops holding up for a
 * few frames stays, under the same id, and goes only after half a second with no plane under it;
 * and a plane refit back and forth across one of the rules' thresholds keeps the verdict it had,
 * wall or not, instead of the wall going and coming back at each refit.
 */
class LiveWallsSteadyTest {
    private val floorY = -1.4f

    /** A vertical plane from (x0, z0) to (x1, z1), [bottom] to [top] metres above the floor. */
    private fun plane(
        id: Int,
        x0: Float,
        z0: Float,
        x1: Float,
        z1: Float,
        bottom: Float = 0.1f,
        top: Float = 2f,
    ) = LivePlane(
        id,
        LivePlane.Kind.Wall,
        floatArrayOf(
            x0, floorY + bottom, z0, x1, floorY + bottom, z1,
            x1, floorY + top, z1, x0, floorY + top, z0,
        ),
    )

    /** A vertical plane [length] long, centred on ([cx], [cz]), [degrees] off the x axis. */
    private fun turned(id: Int, cx: Float, cz: Float, length: Float, degrees: Float): LivePlane {
        val a = degrees * PI.toFloat() / 180f
        val hx = cos(a) * length / 2f
        val hz = sin(a) * length / 2f
        return plane(id, cx - hx, cz - hz, cx + hx, cz + hz)
    }

    // The room: a front wall 4 m long and a left wall 3.6 m long, both seen up to 2.3 m.
    private val front = plane(1, -2f, -2.6f, 2f, -2.6f, top = 2.3f)
    private val left = plane(2, -2f, -2.6f, -2f, 1f, top = 2.3f)

    /** [a] then [b], back and forth, [times] times each: a plane refit across a threshold and back. */
    private fun alternate(a: List<LivePlane>, b: List<LivePlane>, times: Int = 6) =
        List(times * 2) { if (it % 2 == 0) a else b }

    /** The ids of the walls one source shows after each of [frames], fed [step] seconds apart. */
    private fun idsOver(frames: List<List<LivePlane>>, step: Float): List<Set<Int>> {
        val source = PlaneWallSource()
        return frames.mapIndexed { i, planes ->
            source.update(planes, floorY, time = 1f + i * step).walls.mapTo(HashSet()) { it.id }
        }
    }

    /** [frames], [step] seconds apart, show [walls] walls from the first and never another set. */
    private fun assertSteady(what: String, frames: List<List<LivePlane>>, step: Float, walls: Int) {
        val ids = idsOver(frames, step)
        assertEquals("$what: walls at first", walls, ids.first().size)
        ids.forEachIndexed { i, shown -> assertEquals("$what: walls after refit $i", ids.first(), shown) }
    }

    @Test
    fun `a wall no plane holds up for a few frames stays, under the same id`() {
        val piece = plane(24, -1f, -1f, 0.5f, -1f, top = 1.8f)
        val held = listOf(front, left, piece)
        val dropped = listOf(front, left)
        // At 30 frames a second: ARCore drops the plane for 5 frames, then has it again.
        val frames = List(10) { held } + List(5) { dropped } + List(10) { held }
        assertSteady("a plane dropped for 5 frames", frames, step = 1f / 30f, walls = 3)
        // Dropped for good: the wall goes, even with no plane changing after it went.
        val ids = idsOver(List(10) { held } + List(30) { dropped }, step = 1f / 30f)
        assertEquals("a second after its plane went", 2, ids.last().size)
    }

    @Test
    fun `a plane refit across the foot's threshold keeps its wall`() {
        val foot = WallRules.MAX_FOOT_M
        fun piece(bottom: Float) = listOf(front, left, plane(24, -1f, -1f, 0.5f, -1f, bottom = bottom, top = 1.8f))
        // A refit a second apart, longer than a wall waits for its plane.
        assertSteady("foot", alternate(piece(foot - 0.03f), piece(foot + 0.03f)), step = 1f, walls = 3)
    }

    @Test
    fun `a plane refit across the top's threshold keeps its wall`() {
        val top = WallRules.MIN_TOP_M
        fun piece(at: Float) = listOf(front, left, plane(24, -1f, -1f, 0.5f, -1f, top = at))
        assertSteady("top", alternate(piece(top + 0.03f), piece(top - 0.03f)), step = 1f, walls = 3)
    }

    @Test
    fun `a plane refit across the length's threshold keeps its wall`() {
        val seen = WallRules.MIN_SEEN_M
        fun piece(length: Float) = listOf(front, left, plane(24, -1f, -1f, -1f + length, -1f, top = 1.8f))
        assertSteady("length", alternate(piece(seen + 0.03f), piece(seen - 0.03f)), step = 1f, walls = 3)
    }

    @Test
    fun `a wall refit across square with the room keeps its verdict`() {
        // Alone along its direction, 0.8 m long: a wall on the room's square, short furniture off it.
        val snap = WallRules.SNAP_DEGREES
        fun piece(off: Float) = listOf(front, turned(30, 1.5f, -1.5f, 0.8f, 90f + off))
        assertSteady("square first", alternate(piece(snap - 1f), piece(snap + 1f)), step = 1f, walls = 2)
        assertSteady("askew first", alternate(piece(snap + 1f), piece(snap - 1f)), step = 1f, walls = 1)
    }

    @Test
    fun `a plane refit across how far furniture stands before a wall keeps its verdict`() {
        // 1.5 m long, up to 1.6 m: furniture when it stands close enough before the front wall.
        val reach = WallRules.BEHIND_MAX_M
        fun piece(across: Float) = listOf(front, left, plane(24, -1f, -2.6f + across, 0.5f, -2.6f + across, top = 1.6f))
        assertSteady("a wall first", alternate(piece(reach + 0.02f), piece(reach - 0.02f)), step = 1f, walls = 3)
        assertSteady("furniture first", alternate(piece(reach - 0.02f), piece(reach + 0.02f)), step = 1f, walls = 2)
    }

    @Test
    fun `the walls counted in a recording follow its planes`() {
        val piece = plane(24, -1f, -1f, 0.5f, -1f, top = 1.8f)
        assertEquals(2, PlaneWallSource.wallCount(listOf(front, left), floorY))
        assertEquals(2, PlaneWallSource.wallCount(listOf(front, left), floorY))
        assertEquals(3, PlaneWallSource.wallCount(listOf(front, left, piece), floorY))
        // The same planes over a floor half a metre lower stand on nothing.
        assertEquals(0, PlaneWallSource.wallCount(listOf(front, left), floorY - 0.5f))
        assertEquals(2, PlaneWallSource.wallCount(listOf(front, left), floorY))
    }
}
