package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2

/** #4540 live walls: the walls recognised from ARCore's planes, how they move, and the guide off them. */
class LiveWallsTest {
    private val floorY = -1.4f

    /** A vertical plane from (x0, z0) to (x1, z1), 0.3 m to 1.6 m over the floor. */
    private fun wall(id: Int, x0: Float, z0: Float, x1: Float, z1: Float, top: Float = 1.6f) = DebugPlane(
        id,
        DebugPlaneKind.Wall,
        floatArrayOf(
            x0, floorY + 0.3f, z0, x1, floorY + 0.3f, z1,
            x1, floorY + top, z1, x0, floorY + top, z0,
        ),
    )

    @Test
    fun `two planes of one wall 2 cm apart make one wall`() {
        val source = PlaneWallSource()
        val snapshot = source.update(
            listOf(wall(1, -2f, -2.6f, 0.3f, -2.6f), wall(2, 0.3f, -2.58f, 2f, -2.58f)),
            floorY,
            time = 1f,
        )
        assertEquals(1, snapshot.walls.size)
        val w = snapshot.walls.single()
        assertEquals(4f, w.length, 0.05f)
        assertEquals(-2.59f, (w.az + w.bz) / 2f, 0.02f)
    }

    @Test
    fun `a wall seen further keeps its id and only grows`() {
        val source = PlaneWallSource()
        val first = source.update(listOf(wall(1, -1f, -2f, 0f, -2f)), floorY, time = 1f).walls.single()
        val grown = source.update(listOf(wall(1, -1f, -2f, 1.5f, -2f)), floorY, time = 2f).walls.single()
        assertEquals(first.id, grown.id)
        assertEquals(1f, first.bornAt, 0f)
        assertTrue(grown.length > first.length + 1f)
        // ARCore shrinking a refit plane does not shrink the wall.
        val refit = source.update(listOf(wall(1, -0.5f, -2f, 0.5f, -2f)), floorY, time = 3f).walls.single()
        assertEquals(grown.length, refit.length, 0.01f)
        assertTrue(refit.confidence > first.confidence)
    }

    @Test
    fun `a short piece is not a wall`() {
        val snapshot = PlaneWallSource().update(listOf(wall(1, 0f, -2f, 0.3f, -2f)), floorY, time = 1f)
        assertTrue(snapshot.walls.isEmpty())
    }

    @Test
    fun `a wall 3 degrees off the room's axis is squared to it`() {
        val tilt = Math.toRadians(3.0).toFloat()
        val source = PlaneWallSource()
        val snapshot = source.update(
            listOf(
                wall(1, -2f, -2.6f, 2f, -2.6f),
                // 2 m long, 3° off square.
                wall(2, 2.2f, -2f, 2.2f + 2f * kotlin.math.sin(tilt), -2f + 2f * kotlin.math.cos(tilt)),
            ),
            floorY,
            time = 1f,
        )
        assertEquals(2, snapshot.walls.size)
        val side = snapshot.walls.first { abs(it.bz - it.az) > 1f }
        assertEquals("squared", side.ax, side.bx, 0.01f)
    }

    @Test
    fun `the synthetic turn finds two walls midway, four at the end, ids stable`() {
        val trace = ArDebugTrace()
        ScanWallWalk.events().forEach { it.applyTo(trace) }
        val source = PlaneWallSource()
        val seen = HashMap<Int, Float>()
        var mid = -1
        var t = 0f
        while (t <= ScanWallWalk.DURATION_S) {
            val walls = source.update(trace.frameAt(t)).walls
            if (abs(t - ScanWallWalk.DURATION_S / 2f) < 0.25f) mid = walls.size
            for (w in walls) {
                val before = seen[w.id]
                if (before != null) assertTrue("wall ${w.id} shrank", w.length >= before - 0.05f)
                seen[w.id] = w.length
            }
            t += 0.5f
        }
        val end = source.update(trace.frameAt(ScanWallWalk.DURATION_S)).walls
        assertTrue("walls midway: $mid", mid >= 2)
        assertEquals(4, end.size)
        // The front wall's two planes are one wall, spanning it.
        val front = end.first { abs(it.az - ScanWallWalk.MIN_Z) < 0.1f && abs(it.bz - ScanWallWalk.MIN_Z) < 0.1f }
        assertTrue("front ${front.length}", front.length > 2.5f)
    }

    @Test
    fun `a new wall rises from the floor and a dropped one goes`() {
        val motion = LiveWallMotion()
        val one = LiveWall(1, 0f, 0f, 2f, 0f, bornAt = 0f)
        val snapshot = LiveWallSnapshot(listOf(one), floorY, 2.4f)
        assertTrue(motion.step(snapshot, 0.016f))
        assertEquals(0f, motion.shown.single().rise, 0f)
        repeat(3) { motion.step(snapshot, 0.016f) }
        val early = motion.shown.single().rise
        assertTrue(early > 0f && early < 0.5f)
        repeat(120) { motion.step(snapshot, 0.016f) }
        assertEquals(1f, motion.shown.single().rise, 0f)
        assertFalse("settled", motion.step(snapshot, 0.016f))
        assertTrue(motion.step(LiveWallSnapshot(emptyList(), floorY, 2.4f), 0.016f))
        assertTrue(motion.shown.isEmpty())
    }

    @Test
    fun `a wall's refit stretches calmly instead of jumping`() {
        val motion = LiveWallMotion()
        motion.step(LiveWallSnapshot(listOf(LiveWall(1, 0f, 0f, 1f, 0f, 0f)), floorY, 2.4f), 0.016f)
        motion.step(LiveWallSnapshot(listOf(LiveWall(1, 0f, 0f, 3f, 0f, 0f)), floorY, 2.4f), 0.016f)
        val bx = motion.shown.single().bx
        assertTrue("eased: $bx", bx > 1f && bx < 2f)
    }

    private val square = listOf(-2f to -2f, 2f to -2f, 2f to 2f, -2f to 2f)

    @Test
    fun `the floor's edges with no wall on them are open`() {
        assertEquals(4, openFloorEdges(square, emptyList()).size)
        val front = LiveWall(1, -2f, -2f, 2f, -2f, 0f)
        val open = openFloorEdges(square, listOf(front))
        assertEquals(3, open.size)
        assertTrue(open.none { abs(it[1] + 2f) < 0.01f && abs(it[3] + 2f) < 0.01f })
        val all = listOf(front, LiveWall(2, 2f, -2f, 2f, 2f, 0f), LiveWall(3, 2f, 2f, -2f, 2f, 0f), LiveWall(4, -2f, 2f, -2f, -2f, 0f))
        assertTrue(openFloorEdges(square, all).isEmpty())
        assertTrue(ScanGuide.closed(4, openFloorEdges(square, all)))
        assertFalse(ScanGuide.closed(1, open))
    }

    @Test
    fun `the cue turns to the open edge`() {
        val phone = DebugPose(0f, 0f, 0f) // looks down -z, right is +x
        assertEquals(ScanTurn.Ahead, ScanGuide.turnTo(phone, 0f, -2f))
        assertEquals(ScanTurn.Right, ScanGuide.turnTo(phone, 2f, 0f))
        assertEquals(ScanTurn.Left, ScanGuide.turnTo(phone, -2f, 0f))
        assertEquals(ScanTurn.Behind, ScanGuide.turnTo(phone, 0f, 2f))
        val rightEdge = floatArrayOf(2f, -2f, 2f, 2f)
        val cue = ScanGuide.cue(phone, listOf(floatArrayOf(-0.5f, -2f, 0f, -2f), rightEdge))
        assertNotNull(cue)
        assertEquals(ScanTurn.Right, cue!!.turn)
        assertTrue(cue.edge === rightEdge)
        assertNull(ScanGuide.cue(null, listOf(rightEdge)))
        assertNull(ScanGuide.cue(phone, emptyList()))
        // Turned right a quarter, the same edge is ahead.
        val q = Math.toRadians(-90.0) / 2.0
        val turned = DebugPose(0f, 0f, 0f, qy = kotlin.math.sin(q).toFloat(), qw = kotlin.math.cos(q).toFloat())
        val f = turned.rotate(0f, 0f, -1f)
        assertEquals(0.0, Math.toDegrees(atan2(f.z, f.x).toDouble()), 1.0)
        assertEquals(ScanTurn.Ahead, ScanGuide.turnTo(turned, 2f, 0f))
    }

    @Test
    fun `the gauge weighs the walls and the floor, and is full once the room is closed`() {
        assertEquals(0f, ScanGuide.completion(0, 0f, closed = false), 1e-6f)
        assertEquals(0.3f, ScanGuide.completion(2, 0f, closed = false), 1e-6f)
        // The floor seen with two walls is half a room at most.
        assertEquals(0.5f, ScanGuide.completion(2, 1f, closed = false), 1e-6f)
        assertEquals(0.8f, ScanGuide.completion(4, 0.5f, closed = false), 1e-6f)
        assertEquals(1f, ScanGuide.completion(6, 2f, closed = false), 1e-6f)
        assertEquals(1f, ScanGuide.completion(3, 0.1f, closed = true), 0f)
    }

    @Test
    fun `a scan with no depth is stuck, not low on texture`() {
        assertEquals(ScanActivity.Working, ScanActivity.of(ScanPhase.Scanning, trackingLost = false, coach = null))
        assertEquals(
            ScanActivity.Stuck,
            ScanActivity.of(ScanPhase.Scanning, trackingLost = false, coach = null, depthStarved = true),
        )
        assertEquals(
            ScanActivity.Paused,
            ScanActivity.of(ScanPhase.Scanning, trackingLost = true, coach = null, depthStarved = true),
        )
        assertEquals(
            ScanCopy.COACH_NO_DEPTH,
            ScanCopy.coaching(ScanPhase.Scanning, false, false, lowTexture = true, depthStarved = true),
        )
        assertEquals(
            ScanCopy.COACH_STILL,
            ScanCopy.coaching(ScanPhase.Scanning, false, false, coach = ScanCoach.Line.Still),
        )
    }

    @Test
    fun `a room's coaching goes from wall to wall`() {
        val room = ScanScope.Room
        assertEquals(
            ScanCopy.turnHint(ScanTurn.Left),
            ScanScopeCopy.coaching(room, ScanCopy.COACH_SCANNING, walls = 2, turn = ScanTurn.Left),
        )
        assertEquals(
            ScanCopy.ROOM_DONE,
            ScanScopeCopy.coaching(room, ScanCopy.COACH_STILL, walls = 4, done = true),
        )
        assertEquals(
            ScanCopy.COACH_BLANK_WALL,
            ScanScopeCopy.coaching(room, ScanCopy.COACH_NOTHING_NEW, walls = 2),
        )
        // Tracking lost still says so, whatever the walls.
        assertEquals(
            ScanCopy.TRACKING_LOST,
            ScanScopeCopy.coaching(room, ScanCopy.TRACKING_LOST, walls = 2, turn = ScanTurn.Left),
        )
    }

    @Test
    fun `turning on the spot with nothing new is not standing still`() {
        val coach = ScanCoach()
        var t = 0f
        while (t < 8f) {
            // 20 degrees a second about +Y, on the spot.
            val half = Math.toRadians(-20.0 * t).toFloat() / 2f
            coach.update(t, DebugPose(0f, 0f, 0f, qy = kotlin.math.sin(half), qw = kotlin.math.cos(half)), grew = false)
            t += 1f / 30f
        }
        assertEquals(ScanCoach.Line.NothingNew, coach.line)
        assertTrue(coach.turnRate > 15f)
    }

    @Test
    fun `the synthetic turn stays under the feature map's cap`() {
        val trace = ArDebugTrace()
        ScanWallWalk.events().forEach { it.applyTo(trace) }
        val count = trace.frameAt(ScanWallWalk.DURATION_S).mapPointCount
        assertTrue("$count points", count in 2_000 until ArDebugTrace.MAX_MAP_POINTS)
    }

    @Test
    fun `a new dense snapshot covers cells, and a covered cell stays covered`() {
        val grid = CoverageGrid(minPoints = 1)
        assertTrue(grid.cover(floatArrayOf(0.1f, 0f, 0.1f, 1.1f, 0f, 0.1f)))
        assertEquals(2, grid.covered)
        // A later snapshot elsewhere: the first cells stay.
        assertTrue(grid.cover(floatArrayOf(2.1f, 0f, 0.1f)))
        assertEquals(3, grid.covered)
        assertFalse(grid.cover(floatArrayOf(0.1f, 0f, 0.1f)))
        assertEquals(3, grid.covered)
    }
}
