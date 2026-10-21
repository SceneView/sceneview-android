package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The live walls (3D Scan and Measure) follow the room survey's rules: a vertical plane is a wall
 * only from near the floor to past standing height, seen long enough, along one of the room's
 * directions, and not a piece of furniture before a wall. Each scene counts the walls shown before
 * these rules ([LegacyPlaneWalls], the tracker as it was) and after ([PlaneWallSource]).
 */
class LiveWallsSurveyRulesTest {
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
    private fun turned(id: Int, cx: Float, cz: Float, length: Float, degrees: Float, top: Float = 2f): LivePlane {
        val a = degrees * PI.toFloat() / 180f
        val hx = cos(a) * length / 2f
        val hz = sin(a) * length / 2f
        return plane(id, cx - hx, cz - hz, cx + hx, cz + hz, top = top)
    }

    private fun ceiling(id: Int, height: Float) = LivePlane(
        id,
        LivePlane.Kind.Ceiling,
        floatArrayOf(
            -1f, floorY + height, -1f, 1f, floorY + height, -1f,
            1f, floorY + height, 1f, -1f, floorY + height, 1f,
        ),
    )

    // The room: a front wall 4 m long and a left wall 3.6 m long, both seen up to 2.3 m.
    private val front = plane(1, -2f, -2.6f, 2f, -2.6f, top = 2.3f)
    private val left = plane(2, -2f, -2.6f, -2f, 1f, top = 2.3f)

    private fun before(planes: List<LivePlane>): Int = LegacyPlaneWalls().update(planes, floorY).size

    private fun after(planes: List<LivePlane>): Int = PlaneWallSource().update(planes, floorY, 1f).walls.size

    /** [planes] showed [before] walls before the rules, and show [after] with them. */
    private fun assertWalls(planes: List<LivePlane>, before: Int, after: Int) {
        assertEquals("walls before the rules", before, before(planes))
        assertEquals("walls with the rules", after, after(planes))
    }

    @Test
    fun `a sofa's front is not a wall`() {
        // Pixel 9 QA: a vertical plane from 0.3 m to 0.8 m over the floor, 1.5 m long.
        assertWalls(listOf(front, left, plane(10, -1.5f, -1f, 0f, -1f, bottom = 0.3f, top = 0.8f)), before = 3, after = 2)
    }

    @Test
    fun `a kitchen worktop and a table football table are not walls`() {
        val worktop = plane(11, -1f, -2f, 1f, -2f, bottom = 0.05f, top = 0.9f)
        val table = plane(12, 0.5f, -0.5f, 1.7f, -0.5f, bottom = 0.2f, top = 0.9f)
        assertWalls(listOf(front, left, worktop, table), before = 4, after = 2)
    }

    @Test
    fun `a shelf's front is not a wall`() {
        assertWalls(listOf(front, left, plane(13, -1.6f, -1f, -1.6f, 0.2f, bottom = 0.05f, top = 0.9f)), before = 3, after = 2)
    }

    @Test
    fun `a plane that starts well above the floor is not a wall`() {
        // A cupboard's upper half, or a picture: it does not stand on the floor.
        assertWalls(listOf(front, left, plane(14, 0f, -1f, 1.5f, -1f, bottom = 0.6f, top = 2f)), before = 3, after = 2)
    }

    @Test
    fun `a tall wardrobe and its side are not walls`() {
        val wardrobe = plane(15, -1.5f, -2f, -0.5f, -2f, bottom = 0.02f, top = 1.9f)
        val side = plane(16, -0.5f, -2.6f, -0.5f, -2f, bottom = 0.02f, top = 1.9f)
        assertWalls(listOf(front, left, wardrobe, side, ceiling(3, 2.5f)), before = 4, after = 2)
    }

    @Test
    fun `a bookcase against a wall is that wall`() {
        val bookcase = plane(17, -1.7f, -1.5f, -1.7f, -0.3f, bottom = 0f, top = 2f)
        assertWalls(listOf(front, left, bookcase), before = 3, after = 2)
    }

    @Test
    fun `two planes 2 degrees apart on the same side are one wall`() {
        // Pixel 9 QA: two nearly parallel walls on one side, 30 cm apart.
        assertWalls(listOf(front, left, turned(18, 0.7f, -2.3f, 2.4f, 2f)), before = 3, after = 2)
    }

    @Test
    fun `a piece 19 degrees off a wall, along it, is part of that wall`() {
        val host = plane(1, -2f, -2.6f, 0.5f, -2.6f, top = 2.3f)
        val shard = turned(19, 1f, -2.6f, 1.2f, 19f)
        assertWalls(listOf(host, left, shard), before = 3, after = 2)
        val wall = PlaneWallSource().update(listOf(host, left, shard), floorY, 1f).walls
            .first { abs(it.az - it.bz) < 0.1f }
        assertTrue("the wall reaches the shard's far end: ${maxOf(wall.ax, wall.bx)}", maxOf(wall.ax, wall.bx) > 1.4f)
    }

    @Test
    fun `a short piece 19 degrees off the room is not a wall`() {
        assertWalls(listOf(front, left, turned(20, 0f, -0.5f, 0.8f, 19f, top = 1.8f)), before = 3, after = 2)
    }

    @Test
    fun `a wall seen only in part, a jog and a corridor are walls`() {
        val partial = plane(21, 2f, -2.6f, 2f, -1.8f, top = 1.5f)
        assertWalls(listOf(front, left, partial), before = 3, after = 3)
        val jogged = plane(1, -2f, -2.6f, 0f, -2.6f, top = 2.3f)
        val jog = plane(22, 0f, -2.3f, 2f, -2.3f, top = 2.3f)
        assertWalls(listOf(jogged, jog, left), before = 3, after = 3)
        val corridor = plane(23, -2f, -1.5f, 1.5f, -1.5f, top = 2f)
        assertWalls(listOf(front, left, corridor), before = 3, after = 3)
    }

    @Test
    fun `a wall no plane holds up any more goes`() {
        val legacy = LegacyPlaneWalls()
        val source = PlaneWallSource()
        fun counts(planes: List<LivePlane>, time: Float) =
            legacy.update(planes, floorY).size to source.update(planes, floorY, time).walls.size
        val piece = plane(24, -1f, -1f, 0.5f, -1f, bottom = 0.1f, top = 1.5f)
        assertEquals(3 to 3, counts(listOf(front, left, piece), 1f))
        // ARCore refits it to what it was, a sofa's front: no plane holds the wall up.
        val refit = plane(24, -1f, -1f, 0.5f, -1f, bottom = 0.3f, top = 0.8f)
        assertEquals(3 to 2, counts(listOf(front, left, refit), 2f))
        // And when the plane itself goes, so does the wall.
        assertEquals(3 to 3, counts(listOf(front, left, piece), 3f))
        assertEquals(3 to 2, counts(listOf(front, left), 4f))
    }

    @Test
    fun `the walls are drawn to the ceiling seen, else 2,50 m, never to the top of a plane`() {
        val low = listOf(plane(1, -2f, -2.6f, 2f, -2.6f, top = 1.6f), plane(2, -2f, -2.6f, -2f, 1f, top = 1.6f))
        val legacy = LegacyPlaneWalls().apply { update(low, floorY) }
        assertEquals("before: the top of the highest plane", 1.6f, legacy.height(floorY)!!, 0.01f)
        assertEquals(2.5f, PlaneWallSource().update(low, floorY, 1f).height, 0.01f)
        val high = listOf(plane(1, -2f, -2.6f, 2f, -2.6f, top = 2.8f), left)
        assertEquals(2.5f, PlaneWallSource().update(high, floorY, 1f).height, 0.01f)
        val seen = PlaneWallSource().update(low + ceiling(3, 2.6f), floorY, 1f)
        assertEquals(2.6f, seen.height, 0.01f)
        assertTrue(seen.ceilingSeen)
    }

    @Test
    fun `with no ceiling seen, the walls are drawn to the height measured in the session`() {
        val low = listOf(plane(1, -2f, -2.6f, 2f, -2.6f, top = 1.6f), plane(2, -2f, -2.6f, -2f, 1f, top = 1.6f))
        val source = PlaneWallSource()
        source.measuredHeight = 2.7f
        assertEquals(2.7f, source.update(low, floorY, 1f).height, 0.01f)
        // A ceiling seen wins over one measured.
        assertEquals(2.6f, source.update(low + ceiling(3, 2.6f), floorY, 2f).height, 0.01f)
        // A new capture in the same session keeps what was measured.
        source.reset()
        assertEquals(2.7f, source.update(low, floorY, 3f).height, 0.01f)
        // Cleared, with the same planes: the default.
        source.measuredHeight = null
        assertEquals(2.5f, source.update(low, floorY, 4f).height, 0.01f)
    }
}
