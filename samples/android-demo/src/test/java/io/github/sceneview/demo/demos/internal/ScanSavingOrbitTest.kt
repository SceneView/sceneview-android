package io.github.sceneview.demo.demos.internal

import io.github.sceneview.math.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** While a scan is saved (#4719): the view leaves the phone for a slow orbit round the whole cloud. */
class ScanSavingOrbitTest {
    private val phone = OrbitPose(
        target = Position(1f, 1.4f, 2f),
        azimuthDegrees = 70f,
        elevationDegrees = ScanFollow.ELEVATION_DEGREES,
        distance = ScanFollow.DISTANCE_M,
    )
    private val centre = Position(0f, 1f, 0f)

    @Test
    fun `the orbit starts on the follow pose, with no cut`() {
        val start = ScanFollow.savingPose(phone, centre, radius = 3f, seconds = 0f, verticalFovDegrees = 45.0)
        assertEquals(phone, start)
    }

    @Test
    fun `once on it, the orbit frames the whole cloud round its centre`() {
        val on = ScanFollow.savingPose(phone, centre, radius = 3f, seconds = 2f, verticalFovDegrees = 45.0)
        assertEquals(centre, on.target)
        assertEquals(ScanFollow.SAVING_ELEVATION_DEGREES, on.elevationDegrees, 1e-4f)
        // 3 m of reach in half a 45° view, at 85 % of it: about 9.2 m out.
        assertEquals(9.22f, on.distance, 0.05f)
        // A small cloud is never looked at from closer than the live view.
        val small = ScanFollow.savingPose(phone, centre, radius = 0.2f, seconds = 2f, verticalFovDegrees = 45.0)
        assertEquals(ScanFollow.DISTANCE_M, small.distance, 1e-4f)
    }

    @Test
    fun `it turns slowly and one way, from the first frame`() {
        var previous = phone.azimuthDegrees
        var t = 0.05f
        while (t < 10f) {
            val pose = ScanFollow.savingPose(phone, centre, radius = 3f, seconds = t, verticalFovDegrees = 45.0)
            assertTrue(pose.azimuthDegrees > previous)
            previous = pose.azimuthDegrees
            t += 0.05f
        }
        assertEquals(phone.azimuthDegrees + ScanFollow.SAVING_DEGREES_PER_SECOND * 10f, previous, 0.5f)
    }

    @Test
    fun `the cloud's reach ignores a few stray far points`() {
        val points = FloatArray(3 * 1_000) { i ->
            val p = i / 3
            when (i % 3) {
                0 -> if (p < 10) 40f else (p % 7 - 3).toFloat()
                1 -> 1f
                else -> 0f
            }
        }
        assertEquals(3f, ScanFollow.cloudRadius(points, Position(0f, 1f, 0f)), 1e-4f)
        assertEquals(0f, ScanFollow.cloudRadius(FloatArray(0), centre), 0f)
    }

    @Test
    fun `the saving card says what it fuses`() {
        assertEquals("Fusing 261k points…", ScanCopy.fusing(261_400))
        assertEquals("Fusing 1 point…", ScanCopy.fusing(1))
        assertEquals(ScanCopy.SAVING, ScanCopy.fusing(0))
    }
}
