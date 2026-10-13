package io.github.sceneview.demo.demos.internal

import io.github.sceneview.math.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ArDebugFraming.reveal] (#4505): a picked object's box sat partly under the replay's Camera
 * card in the 3D view, and under the stats card in the Map view. Where each corner is drawn is
 * asked of [scanCamera] — the pinhole the view draws and taps through, stage lift included — not
 * of the reveal's own algebra, so the two can disagree.
 */
class ArDebugFramingRevealTest {

    @Test
    fun `a box drawn clear of the chrome leaves the camera where it is`() {
        val pose = OrbitPose(Position(0f, 0.4f, 0f), azimuthDegrees = 35f, elevationDegrees = 32f, distance = 6f)
        val box = ScanBox(0f, 0f, baseY = 0f, yaw = 0.4f, length = 0.9f, depth = 0.5f, height = 0.7f)
        assertTrue("clear to begin with", isClear(pose, box, CHROME))
        assertSame(pose, reveal(pose, box, listOf(CHROME)))
    }

    @Test
    fun `a box under the map's stats card is slid out from under it, at the same height over the room`() {
        val pose = OrbitPose(Position(0f, 0f, 0f), 0f, ArDebugFraming.MAP_ELEVATION, distance = 9f)
        // The far end of the room: drawn at the top of a map, where the card is.
        val box = ScanBox(0.3f, -3.2f, baseY = 0f, yaw = 0f, length = 1.4f, depth = 0.6f, height = 0.8f)
        assertFalse("under the card to begin with", isClear(pose, box, CHROME))

        val revealed = reveal(pose, box, listOf(CHROME))
        assertTrue("still under the card", isClear(revealed, box, CHROME))
        assertEquals("no closer, no further", pose.distance, revealed.distance, 1e-4f)
        assertEquals(pose.azimuthDegrees, revealed.azimuthDegrees, 0f)
        assertEquals(pose.elevationDegrees, revealed.elevationDegrees, 0f)
        // Slid by what it took and no more: the box now touches the free area's top edge.
        val top = corners(box).minOf { camera(revealed).project(it)!!.second }
        assertEquals(CHROME.top * HEIGHT, top, PIXELS)
    }

    @Test
    fun `beside the camera card or under it, whichever moves the room least`() {
        val pose = OrbitPose(Position(0f, 0.4f, 0f), azimuthDegrees = 35f, elevationDegrees = 32f, distance = 6f)
        // Drawn in the view's top right corner, where the 3D view keeps its Camera card.
        val box = boxDrawnAt(pose, 0.8f * WIDTH, 0.3f * HEIGHT)
        assertFalse(isClear(pose, box, BESIDE_CARD))
        assertFalse(isClear(pose, box, UNDER_CARD))

        val revealed = reveal(pose, box, listOf(BESIDE_CARD, UNDER_CARD))
        assertTrue(isClear(revealed, box, BESIDE_CARD) || isClear(revealed, box, UNDER_CARD))
        val moved = moved(pose, revealed)
        assertTrue("moved $moved m", moved > 0f)
        assertTrue(moved <= moved(pose, reveal(pose, box, listOf(BESIDE_CARD))) + 1e-4f)
        assertTrue(moved <= moved(pose, reveal(pose, box, listOf(UNDER_CARD))) + 1e-4f)

        // A box drawn left of the card, level with it, is not under it: nothing moves.
        val beside = boxDrawnAt(pose, 0.3f * WIDTH, 0.3f * HEIGHT)
        assertFalse("in the card's band", isClear(pose, beside, UNDER_CARD))
        assertSame(pose, reveal(pose, beside, listOf(BESIDE_CARD, UNDER_CARD)))
    }

    @Test
    fun `a box the free area is too small for steps the camera back, never closer`() {
        val close = OrbitPose(Position(0f, 0.5f, 0f), azimuthDegrees = 20f, elevationDegrees = 30f, distance = 1.2f)
        val sofa = ScanBox(centerX = 0f, centerZ = 0f, baseY = 0f, yaw = 0f, length = 2f, depth = 0.9f, height = 0.8f)
        val revealed = reveal(close, sofa, listOf(CHROME))
        assertTrue(revealed.distance > close.distance)
        assertTrue(isClear(revealed, sofa, CHROME))

        // From far away the same sofa is small and clear: the camera does not come to it.
        val far = close.copy(distance = 12f)
        assertSame(far, reveal(far, sofa, listOf(CHROME)))
    }

    @Test
    fun `the free camera glides to the pose it is given, and a finger stops it`() {
        val start = OrbitPose(Position(0f, 0.4f, 0f), azimuthDegrees = 35f, elevationDegrees = 32f, distance = 6f)
        val to = start.copy(target = Position(0.5f, 0.1f, -0.4f), distance = 7f)
        val orbit = ArDebugOrbitCamera(initialPose = start, drift = false).apply { setViewport(WIDTH, HEIGHT) }

        // Following, the view frames through home: the glide is not taken.
        orbit.home = start
        orbit.glideTo(to)
        repeat(120) { orbit.update(1f / 60f) }
        assertEquals(start, orbit.pose)

        orbit.hold()
        orbit.glideTo(to)
        orbit.update(1f / 60f)
        assertTrue("it eases, it does not jump", orbit.pose != to && orbit.pose != start)
        repeat(240) { orbit.update(1f / 60f) }
        assertEquals(to, orbit.pose)

        orbit.glideTo(start)
        repeat(6) { orbit.update(1f / 60f) }
        orbit.fingerDown()
        val held = orbit.pose
        repeat(120) { orbit.update(1f / 60f) }
        assertEquals("stopped under the finger", held, orbit.pose)
        assertTrue(held != start)
    }

    private fun reveal(pose: OrbitPose, box: ScanBox, insets: List<FramingInsets>): OrbitPose {
        val orbit = orbit(pose)
        val corners = box.corners().map { Position(it.x, it.y, it.z) }
        return ArDebugFraming.reveal(pose, corners, orbit.verticalFovDegrees, orbit.aspect, insets, LIFT)
    }

    private fun orbit(pose: OrbitPose) =
        ArDebugOrbitCamera(initialPose = pose, drift = false, lift = LIFT).apply { setViewport(WIDTH, HEIGHT) }

    private fun camera(pose: OrbitPose) = orbit(pose).scanCamera()

    private fun corners(box: ScanBox) = box.corners()

    /** Whether every corner of [box] is drawn from [pose] inside what [insets] leave free. */
    private fun isClear(pose: OrbitPose, box: ScanBox, insets: FramingInsets): Boolean {
        val camera = camera(pose)
        return corners(box).all { corner ->
            val (x, y) = camera.project(corner) ?: return false
            x >= insets.left * WIDTH - PIXELS && x <= (1f - insets.right) * WIDTH + PIXELS &&
                y >= insets.top * HEIGHT - PIXELS && y <= (1f - insets.bottom) * HEIGHT + PIXELS
        }
    }

    /** A stool standing on the floor where [pose] draws pixel ([x], [y]). */
    private fun boxDrawnAt(pose: OrbitPose, x: Float, y: Float): ScanBox {
        val camera = camera(pose)
        val ray = camera.ray(x, y)
        val at = camera.eye + ray * (-camera.eye.y / ray.y)
        return ScanBox(at.x, at.z, baseY = 0f, yaw = 0.3f, length = 0.5f, depth = 0.4f, height = 0.45f)
    }

    private fun moved(from: OrbitPose, to: OrbitPose): Float {
        val slide = to.target - from.target
        return Vec3(slide.x, slide.y, slide.z).length() + (to.distance - from.distance)
    }

    private companion object {
        const val WIDTH = 1080
        const val HEIGHT = 2340
        const val LIFT = 0.07f
        const val PIXELS = 1.5f

        /** A header and a stats card above, a filmstrip and a dock below. */
        val CHROME = FramingInsets(left = 0.03f, top = 0.2f, right = 0.03f, bottom = 0.3f)

        /** The 3D view's two ways round its Camera card, which hangs under the stats card. */
        val BESIDE_CARD = CHROME.copy(right = 0.4f)
        val UNDER_CARD = CHROME.copy(top = 0.42f)
    }
}
