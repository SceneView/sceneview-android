package io.github.sceneview.demo.demos.internal

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.normalize
import dev.romainguy.kotlin.math.rotation
import io.github.sceneview.math.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #4732: Measure on top of the live walls — wall feet, labels the right way up, refusals, actions. */
class MeasureLiveTest {
    private val floorY = -1.4f

    // ── Label orientation ──────────────────────────────────────────────────────────────────

    private fun front(q: Quaternion): Float3 = rotation(q).z.xyz
    private fun top(q: Quaternion): Float3 = rotation(q).y.xyz

    private fun assertNear(expected: Float3, actual: Float3) {
        assertEquals("x", expected.x, actual.x, 1e-4f)
        assertEquals("y", expected.y, actual.y, 1e-4f)
        assertEquals("z", expected.z, actual.z, 1e-4f)
    }

    @Test
    fun `seen straight from above, the top follows the phone, whichever way it is held`() {
        val label = Position(0f, floorY, 0f)
        val eye = Position(0f, 0f, 0f)
        // The phone's top towards -Z, then turned to face the other way: +Z, then +X.
        for (up in listOf(Float3(0f, 0f, -1f), Float3(0f, 0f, 1f), Float3(1f, 0f, 0f))) {
            val q = measureLabelQuaternion(label, eye, up)
            assertNotNull(q)
            assertNear(Float3(0f, 1f, 0f), front(q!!))
            // A world-up billboard picks a fixed top here; the label must read along the screen.
            assertNear(up, top(q))
        }
    }

    @Test
    fun `seen at an angle, the label faces the eye and keeps the screen's up`() {
        val label = Position(0.5f, floorY, -1f)
        val eye = Position(0f, 0f, 0f)
        val toEye = normalize(Float3(eye.x - label.x, eye.y - label.y, eye.z - label.z))
        val cameraUp = normalize(Float3(0f, 0.6f, -0.8f))
        val q = measureLabelQuaternion(label, eye, cameraUp)!!
        assertNear(toEye, front(q))
        // The top is the screen's up made square to the line of sight: in the up/eye plane, upward.
        assertEquals(0f, dot(top(q), front(q)), 1e-4f)
        assertTrue(dot(top(q), cameraUp) > 0.5f)
    }

    @Test
    fun `phone rolled upside down rolls the label with it`() {
        val label = Position(0f, 0f, -2f)
        val eye = Position(0f, 0f, 0f)
        val q = measureLabelQuaternion(label, eye, Float3(0f, -1f, 0f))!!
        assertNear(Float3(0f, -1f, 0f), top(q))
    }

    @Test
    fun `no orientation on the eye or with the up along the sight line`() {
        assertNull(measureLabelQuaternion(Position(1f, 1f, 1f), Position(1f, 1f, 1f), Float3(0f, 1f, 0f)))
        assertNull(measureLabelQuaternion(Position(0f, -1f, 0f), Position(0f, 0f, 0f), Float3(0f, 1f, 0f)))
    }

    // ── Wall foot ──────────────────────────────────────────────────────────────────────────

    /** A wall 3 m wide, 2 m in front of the camera, running along x. */
    private val wall = LiveWall(id = 1, ax = -1.5f, az = -2f, bx = 1.5f, bz = -2f, bornAt = 0f)

    @Test
    fun `aiming at a wall lands on its foot, under the aim`() {
        val foot = measureLiveWallFoot(
            origin = Position(0.4f, 0f, 0f),
            direction = Position(0f, -0.1f, -1f),
            walls = listOf(wall),
            floorY = floorY,
            height = 2.4f,
        )
        assertNotNull(foot)
        assertEquals(0.4f, foot!!.position.x, 1e-4f)
        assertEquals(floorY, foot.position.y, 1e-4f)
        assertEquals(-2f, foot.position.z, 1e-4f)
        assertEquals(Position(-1.5f, floorY, -2f), foot.a)
        assertEquals(Position(1.5f, floorY, -2f), foot.b)
    }

    @Test
    fun `aiming at the floor in front of the wall is not the wall`() {
        // Steeply down: the floor 1 m ahead comes before the wall 2 m ahead.
        val foot = measureLiveWallFoot(Position(0f, 0f, 0f), Position(0f, -1.4f, -1f), listOf(wall), floorY, 2.4f)
        assertNull(foot)
    }

    @Test
    fun `aiming past the wall's end, or above it, misses`() {
        assertNull(measureLiveWallFoot(Position(0f, 0f, 0f), Position(1.5f, 0f, -1f), listOf(wall), floorY, 2.4f))
        assertNull(measureLiveWallFoot(Position(0f, 0f, 0f), Position(0f, 1f, -1f), listOf(wall), floorY, 2.4f))
        assertNull(measureLiveWallFoot(Position(0f, 0f, 0f), Position(0f, 0f, 1f), listOf(wall), floorY, 2.4f))
    }

    @Test
    fun `the nearest of two walls wins, dropped to the given level`() {
        val far = LiveWall(id = 2, ax = -1.5f, az = -4f, bx = 1.5f, bz = -4f, bornAt = 0f)
        val foot = measureLiveWallFoot(
            Position(0f, 0f, 0f), Position(0f, 0f, -1f), listOf(far, wall), floorY, 2.4f, level = -1.38f,
        )!!
        assertEquals(-2f, foot.position.z, 1e-4f)
        assertEquals(-1.38f, foot.position.y, 1e-4f)
    }

    @Test
    fun `wall feet are snap edges and their ends corners`() {
        val targets = measureLiveWallTargets(listOf(wall), floorY)
        assertEquals(listOf(Position(-1.5f, floorY, -2f), Position(1.5f, floorY, -2f)), targets.corners)
        assertEquals(listOf(Position(-1.5f, floorY, -2f) to Position(1.5f, floorY, -2f)), targets.edges)
    }

    // ── Refusals and the add button's action ───────────────────────────────────────────────

    @Test
    fun `a refused tap always has a reason`() {
        assertEquals(MeasureRefusal.NotTracking, measureRefusal(false, MeasurePhase.First, true, false))
        assertEquals(MeasureRefusal.NoSurface, measureRefusal(true, MeasurePhase.Next, false, false))
        assertEquals(MeasureRefusal.OffWall, measureRefusal(true, MeasurePhase.Raising, true, false))
        assertNull(measureRefusal(true, MeasurePhase.Raising, false, true))
        assertNull(measureRefusal(true, MeasurePhase.First, true, false))
    }

    @Test
    fun `the button says start, continue, close, set height, then new measure`() {
        var board = MeasureBoard<Int>(mode = MeasureMode.Room)
        assertEquals(MeasureAction.Start, measureAction(board, closes = false))
        board = board.tap(1, false).tap(2, false).tap(3, false)
        assertEquals(MeasureAction.Continue, measureAction(board, closes = false))
        assertEquals(MeasureAction.Close, measureAction(board, closes = true))
        board = board.tap(1, closesOutline = true)
        assertEquals(MeasureAction.SetHeight, measureAction(board, closes = false))
        board = board.lockHeight(2.4f)
        assertEquals(MeasureAction.StartNew, measureAction(board, closes = false))
    }

    @Test
    fun `a distance chain ended with Done makes the next point a new measure`() {
        var board = MeasureBoard<Int>().tap(1, false).tap(2, false)
        assertEquals(MeasureAction.Continue, measureAction(board, closes = false))
        board = board.finish()
        assertEquals(MeasureAction.StartNew, measureAction(board, closes = false))
        // Another mode has no measure of its own yet: a plain start.
        assertEquals(MeasureAction.Start, measureAction(board.withMode(MeasureMode.Wall), closes = false))
    }
}
