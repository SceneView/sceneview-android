package io.github.sceneview.demo.demos.internal

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.cross
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.normalize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/**
 * [ArDebugFraming.fit] (#4492): the dollhouse's 3D view opened on a room cut by the screen's
 * edge. The projection here is written from the eye and the look-at, not from the fit's own
 * algebra, so the two can disagree.
 */
class ArDebugFramingFitTest {

    private val fov = 46.4

    /** Rooms as the 3D view stands them: centred, on the floor. Width, height, depth in metres. */
    private val rooms = listOf(
        floatArrayOf(4f, 2.4f, 3f),
        floatArrayOf(8f, 2.5f, 2f),
        floatArrayOf(1.5f, 2.1f, 1.5f),
        floatArrayOf(6f, 2.1f, 6f),
        floatArrayOf(2f, 3.2f, 9f),
    )

    /** A 5.8" phone upright, a square window, the same phone on its side. */
    private val aspects = listOf(0.46f, 1f, 2.17f)

    private val chrome = FramingInsets(left = 0.04f, top = 0.14f, right = 0.04f, bottom = 0.22f)

    private fun box(room: FloatArray) =
        floatArrayOf(-room[0] / 2f, 0f, -room[2] / 2f, room[0] / 2f, room[1], room[2] / 2f)

    /** The extent of [bounds] on screen from [pose], in NDC: `[minX, minY, maxX, maxY]`. */
    private fun projected(bounds: FloatArray, pose: OrbitPose, aspect: Float): FloatArray {
        val eye = CameraRig.eye(pose)
        val forward = normalize(pose.target - eye)
        val right = normalize(cross(forward, Float3(0f, 1f, 0f)))
        val up = cross(right, forward)
        val tanV = tan(Math.toRadians(fov / 2.0)).toFloat()
        val tanH = tanV * aspect
        val out = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (corner in 0 until 8) {
            val point = Float3(
                bounds[if (corner and 1 == 0) 0 else 3],
                bounds[if (corner and 2 == 0) 1 else 4],
                bounds[if (corner and 4 == 0) 2 else 5],
            ) - eye
            val depth = dot(point, forward)
            assertTrue("a corner is behind the lens", depth > 0f)
            val x = dot(point, right) / (depth * tanH)
            val y = dot(point, up) / (depth * tanV)
            out[0] = min(out[0], x)
            out[1] = min(out[1], y)
            out[2] = max(out[2], x)
            out[3] = max(out[3], y)
        }
        return out
    }

    private fun assertFramed(bounds: FloatArray, azimuth: Float, aspect: Float, insets: FramingInsets) {
        val pose = ArDebugFraming.fit(bounds, azimuth, 40f, fov, aspect, insets)
        val seen = projected(bounds, pose, aspect)
        val left = -1f + 2f * insets.left
        val right = 1f - 2f * insets.right
        val bottom = -1f + 2f * insets.bottom
        val top = 1f - 2f * insets.top
        val where = "room ${bounds[3] * 2}x${bounds[4]}x${bounds[5] * 2} az $azimuth aspect $aspect"
        val slack = 2e-3f
        assertTrue("$where: cut on the left (${seen[0]} < $left)", seen[0] >= left - slack)
        assertTrue("$where: cut on the right (${seen[2]} > $right)", seen[2] <= right + slack)
        assertTrue("$where: cut at the bottom (${seen[1]} < $bottom)", seen[1] >= bottom - slack)
        assertTrue("$where: cut at the top (${seen[3]} > $top)", seen[3] <= top + slack)
        // As close as it can be: the room fills the free area on one axis at least.
        val across = (seen[2] - seen[0]) / (right - left)
        val upward = (seen[3] - seen[1]) / (top - bottom)
        assertEquals("$where: not the closest framing", 1f, max(across, upward), 5e-3f)
        // And centred in it on both.
        assertEquals("$where: off centre across", (left + right) / 2f, (seen[0] + seen[2]) / 2f, 5e-3f)
        assertEquals("$where: off centre upward", (bottom + top) / 2f, (seen[1] + seen[3]) / 2f, 5e-3f)
    }

    @Test
    fun `a room of any shape opens whole in the view, from every side`() {
        for (room in rooms) for (aspect in aspects) for (azimuth in 0 until 360 step 15) {
            assertFramed(box(room), azimuth.toFloat(), aspect, FramingInsets.None)
        }
    }

    @Test
    fun `it opens whole in what the chrome leaves free`() {
        for (room in rooms) for (aspect in aspects) for (azimuth in 0 until 360 step 15) {
            assertFramed(box(room), azimuth.toFloat(), aspect, chrome)
        }
    }

    @Test
    fun `the sphere framing it replaces cut a square room on an upright phone`() {
        // The room of the issue's capture, near enough: as wide as it is deep, on a 5.8" screen.
        val bounds = box(floatArrayOf(4f, 2.4f, 4f))
        val before = ArDebugFraming.home(
            bounds, 35f, fov, 0.46f,
            elevationDegrees = 40f,
            margin = ArDebugFraming.REPLAY_MARGIN,
        )
        val seen = projected(bounds, before, 0.46f)
        assertTrue("the old framing held the room after all", seen[0] < -1f || seen[2] > 1f)

        val after = ArDebugFraming.fit(bounds, 35f, 40f, fov, 0.46f)
        assertTrue("the fit should stand further back", after.distance > before.distance)
    }

    @Test
    fun `the angles asked for are the angles kept`() {
        val pose = ArDebugFraming.fit(box(rooms[0]), 35f, 40f, fov, 0.46f, chrome)
        assertEquals(35f, pose.azimuthDegrees, 0f)
        assertEquals(40f, pose.elevationDegrees, 0f)
    }

    @Test
    fun `chrome that covers more of the view pushes the camera back`() {
        val bounds = box(rooms[0])
        val bare = ArDebugFraming.fit(bounds, 35f, 40f, fov, 0.46f)
        val covered = ArDebugFraming.fit(bounds, 35f, 40f, fov, 0.46f, chrome)
        assertTrue(covered.distance > bare.distance)
    }

    @Test
    fun `insets never take the whole view`() {
        val greedy = FramingInsets(left = 0.6f, top = 0.9f, right = 0.6f, bottom = 0.3f).bounded()
        assertEquals(1f - FramingInsets.MIN_FREE, greedy.left + greedy.right, 1e-5f)
        assertEquals(1f - FramingInsets.MIN_FREE, greedy.top + greedy.bottom, 1e-5f)
        assertEquals(greedy.left, greedy.right, 1e-6f)
        assertEquals(3f, greedy.top / greedy.bottom, 1e-4f)

        val broken = FramingInsets(left = -0.2f, top = Float.NaN, right = 0.1f, bottom = 0.2f).bounded()
        assertEquals(FramingInsets(left = 0f, top = 0f, right = 0.1f, bottom = 0.2f), broken)
        assertEquals(chrome, chrome.bounded())
        // A view not measured yet (zero wide) divides to infinity: still a framing, not a NaN.
        val unmeasured = FramingInsets(left = Float.POSITIVE_INFINITY, right = Float.POSITIVE_INFINITY)
        val pose = ArDebugFraming.fit(box(rooms[0]), 35f, 40f, fov, 1f, unmeasured)
        assertTrue(pose.distance.isFinite() && abs(pose.target.x).isFinite())
    }
}
