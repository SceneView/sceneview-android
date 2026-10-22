package io.github.sceneview.demo.demos.internal.semantic

import io.github.sceneview.demo.demos.internal.DenseCloud
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Doors and windows found as holes in the walls of a scanned cloud: a 4 × 4 m room with a door
 * in its north wall and a window in its east one, a larger room beyond them seen through.
 */
class WallOpeningsTest {
    @Test
    fun `a door down to the floor and a window up the wall are found where they are`() {
        val scan = HoledRoomScan
        val openings = CloudWallOpenings().find(scan.cloud) { scan.objects.surfaceOf(it) }
        val summary = openings.joinToString {
            "${it.label} w=%.2f h=%.2f sill=%.2f at (%.2f, %.2f) %.2f".format(
                it.width, it.height, it.sill, it.box.centerX, it.box.centerZ, it.confidence,
            )
        }
        val door = openings.singleOrNull { it.label == SceneLabel.Door } ?: error("no single door: $summary")
        assertEquals("door width: $summary", 0.9f, door.width, TOLERANCE)
        assertEquals("door height: $summary", 2.05f, door.height, TOLERANCE)
        assertTrue("door sill: $summary", door.sill <= CloudWallOpenings.DOOR_SILL_M)
        assertEquals("door along the north wall: $summary", 0.45f, door.box.centerX, TOLERANCE)
        assertEquals("door in the north wall: $summary", -2f, door.box.centerZ, TOLERANCE)

        val window = openings.singleOrNull { it.label == SceneLabel.Window } ?: error("no single window: $summary")
        assertEquals("window width: $summary", 1.2f, window.width, TOLERANCE)
        assertEquals("window height: $summary", 1.2f, window.height, TOLERANCE)
        assertEquals("window sill: $summary", 0.9f, window.sill, TOLERANCE)
        assertEquals("window in the east wall: $summary", 2f, window.box.centerX, TOLERANCE)
        assertEquals("window along the east wall: $summary", 0f, window.box.centerZ, TOLERANCE)
        assertEquals("nothing else: $summary", 2, openings.size)
    }

    @Test
    fun `the closed synthetic room has no openings`() {
        val scan = SyntheticRoomScan
        val openings = CloudWallOpenings().find(scan.cloud) { scan.objects.surfaceOf(it) }
        assertTrue(openings.joinToString { "${it.label} ${it.box.describe()}" }, openings.isEmpty())
    }

    @Test
    fun `a cloud with no walls has no openings`() {
        assertTrue(CloudWallOpenings().find(DenseCloud(FloatArray(0), IntArray(0))) { error("no surfel") }.isEmpty())
    }

    private companion object {
        const val TOLERANCE = 0.15f
    }
}
