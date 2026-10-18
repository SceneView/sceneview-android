package io.github.sceneview.demo.demos.internal

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.normalize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pins the heading-up follow view of 3D Scan's live capture (#4628): the heading read off the
 * phone's pose, the view turned so that heading is the top of the screen, and the Recenter state
 * a drag leaves and a recenter restores.
 */
class ScanFollowTest {

    /** A phone at [position], turned [yawDegrees] about the vertical, pitched [pitchDegrees]. */
    private fun phone(yawDegrees: Float, pitchDegrees: Float = 0f, position: Float3 = Float3()): DebugPose {
        // q = yaw(Y) * pitch(X): ARCore's world is Y-up, the camera looks down its −Z.
        val yaw = Math.toRadians(yawDegrees.toDouble()) / 2
        val pitch = Math.toRadians(pitchDegrees.toDouble()) / 2
        val cy = cos(yaw).toFloat()
        val sy = sin(yaw).toFloat()
        val cp = cos(pitch).toFloat()
        val sp = sin(pitch).toFloat()
        return DebugPose(
            position.x, position.y, position.z,
            qx = cy * sp, qy = sy * cp, qz = -sy * sp, qw = cy * cp,
        )
    }

    private fun assertAngle(expected: Float, actual: Float?) {
        assertNotNull(actual)
        assertEquals(0f, CameraRig.shortestDelta(expected, actual!!), 1e-3f)
    }

    /** The orbit camera's horizontal forward, read off the transform it hands the renderer. */
    private fun viewForward(orbit: ArDebugOrbitCamera): Float3 {
        val m = orbit.getTransform()
        // A camera looks down its −Z: minus the third column.
        val f = Float3(-m.z.x, 0f, -m.z.z)
        return normalize(f)
    }

    @Test
    fun `a phone facing down -Z has heading 0, the azimuth an orbit camera looks down -Z at`() {
        assertAngle(0f, ScanFollow.headingOf(phone(0f)))
    }

    @Test
    fun `turning the phone left by 90 degrees turns the heading by 90 degrees`() {
        // Yaw +90° about Y turns −Z onto −X: the phone now faces −X.
        val (fx, _, fz) = ScanFollow.forwardOf(phone(90f))
        assertEquals(-1f, fx, 1e-4f)
        assertEquals(0f, fz, 1e-4f)
        assertAngle(90f, ScanFollow.headingOf(phone(90f)))
        assertAngle(-135f, ScanFollow.headingOf(phone(-135f)))
    }

    @Test
    fun `the heading ignores pitch, the phone tilted down at the floor still has its yaw`() {
        assertAngle(30f, ScanFollow.headingOf(phone(30f, pitchDegrees = -60f)))
        assertAngle(30f, ScanFollow.headingOf(phone(30f, pitchDegrees = 45f)))
    }

    @Test
    fun `a phone pointing straight down keeps the last heading instead of a noisy one`() {
        val down = phone(170f, pitchDegrees = -89f)
        assertEquals(42f, ScanFollow.headingOf(down, previous = 42f)!!, 0f)
        assertNull(ScanFollow.headingOf(down, previous = null))

        val follow = ScanFollow()
        assertNotNull(follow.poseFor(phone(20f)))
        val held = follow.poseFor(phone(170f, pitchDegrees = -89f))
        assertAngle(20f, held?.azimuthDegrees)
    }

    @Test
    fun `heading-up - the view looks the way the phone faces, whatever the yaw`() {
        for (yaw in listOf(0f, 37f, 90f, 180f, -100f)) {
            val orbit = ArDebugOrbitCamera(drift = false, lift = ScanFollow.LIFT)
            orbit.setViewport(1080, 2400)
            val pose = ScanFollow().poseFor(phone(yaw, pitchDegrees = -20f))!!
            orbit.snapTo(pose)

            val (fx, _, fz) = ScanFollow.forwardOf(phone(yaw, pitchDegrees = -20f))
            val horizontal = sqrt(fx * fx + fz * fz)
            val view = viewForward(orbit)
            assertEquals("yaw $yaw", fx / horizontal, view.x, 1e-3f)
            assertEquals("yaw $yaw", fz / horizontal, view.z, 1e-3f)
        }
    }

    @Test
    fun `the view stands behind and above the phone, on it`() {
        val at = Float3(1f, 1.4f, -2f)
        val pose = ScanFollow.poseAt(phone(0f, position = at), 0f)
        assertEquals(at.x, pose.target.x, 0f)
        assertEquals(at.y, pose.target.y, 0f)
        assertEquals(at.z, pose.target.z, 0f)
        val eye = CameraRig.eye(pose)
        // Facing −Z, behind is +Z.
        assertTrue(eye.z > at.z)
        assertTrue(eye.y > at.y)
        assertEquals(ScanFollow.DISTANCE_M, pose.distance, 0f)
    }

    @Test
    fun `turning the phone turns the followed view the short way, damped`() {
        val orbit = ArDebugOrbitCamera(drift = false, approachRate = ScanFollow.APPROACH_RATE)
        val follow = ScanFollow()
        orbit.snapTo(follow.poseFor(phone(170f))!!)
        orbit.home = follow.poseFor(phone(-170f))!!

        orbit.update(1f / 60f)
        val step = CameraRig.shortestDelta(170f, orbit.pose.azimuthDegrees)
        // 20° the short way across ±180°, not 340° the long way, and only part of it per frame.
        assertTrue("step $step", step > 0f && step < 20f)

        repeat(60) { orbit.update(1f / 60f) }
        assertAngle(-170f, orbit.pose.azimuthDegrees)
    }

    @Test
    fun `a drag leaves the follow view, Recenter returns to it`() {
        val orbit = ArDebugOrbitCamera(drift = false)
        val changes = mutableListOf<Boolean>()
        orbit.onFollowingChange = { changes += it }
        assertTrue(orbit.following)

        orbit.grabBegin(10, 10, strafe = false)
        orbit.grabUpdate(80, 10)
        orbit.grabEnd()
        assertFalse(orbit.following)

        // Free, it no longer eases onto the phone.
        val before = orbit.pose
        orbit.home = ScanFollow.poseAt(phone(90f), 90f)
        orbit.update(0.1f)
        assertEquals(before, orbit.pose)

        orbit.recenter()
        assertTrue(orbit.following)
        orbit.update(0.1f)
        assertTrue(abs(CameraRig.shortestDelta(before.azimuthDegrees, orbit.pose.azimuthDegrees)) > 0f)
        assertEquals(listOf(false, true), changes)
    }

    @Test
    fun `a pinch leaves the follow view too, and only a change is reported`() {
        val orbit = ArDebugOrbitCamera(drift = false)
        val changes = mutableListOf<Boolean>()
        orbit.onFollowingChange = { changes += it }
        orbit.recenter()
        orbit.scrollBegin(0, 0, 100f)
        orbit.scrollBegin(0, 0, 100f)
        assertEquals(listOf(false), changes)
    }
}
