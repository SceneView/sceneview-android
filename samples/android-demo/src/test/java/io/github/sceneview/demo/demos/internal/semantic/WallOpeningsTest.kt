package io.github.sceneview.demo.demos.internal.semantic

import io.github.sceneview.demo.demos.internal.DenseCloud
import io.github.sceneview.demo.demos.internal.DenseFusion
import io.github.sceneview.demo.demos.internal.DepthBackProjection
import io.github.sceneview.demo.demos.internal.RerunSyntheticRoom
import io.github.sceneview.demo.demos.internal.ScanObjects
import io.github.sceneview.demo.demos.internal.SyntheticScene
import io.github.sceneview.demo.demos.internal.SyntheticShape
import io.github.sceneview.demo.demos.internal.Vec3
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
        val (cloud, objects) = scan(holed())
        val openings = CloudWallOpenings().find(cloud) { objects.surfaceOf(it) }
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

    private fun scan(scene: SyntheticScene): Pair<DenseCloud, ScanObjects> {
        val fusion = DenseFusion()
        RerunSyntheticRoom.poses(stops = STOPS).forEach { fusion.add(DepthBackProjection.project(scene.render(it))) }
        val cloud = fusion.cloud()
        return cloud to ScanObjects.of(cloud)
    }

    /** The room: four walls 10 cm thick, a door and a window cut out, a larger room round it. */
    private fun holed(): SyntheticScene {
        fun wall(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float) =
            SyntheticShape.Box(Vec3(x0, y0, z0), Vec3(x1, y1, z1)) { _, _, _ -> WALL }
        val outer = SyntheticShape.Room(Vec3(-OUTER, 0f, -OUTER), Vec3(OUTER, HEIGHT, OUTER)) { _, y, _ ->
            if (y < SURFACE) FLOOR else OUTSIDE
        }
        val shapes = listOf(
            outer,
            // North, z = −2: a door from x 0 to 0.9, 2.05 m high.
            wall(-R - T, 0f, -R - T, 0f, HEIGHT, -R),
            wall(0.9f, 0f, -R - T, R + T, HEIGHT, -R),
            wall(0f, 2.05f, -R - T, 0.9f, HEIGHT, -R),
            // South and west, whole.
            wall(-R - T, 0f, R, R + T, HEIGHT, R + T),
            wall(-R - T, 0f, -R - T, -R, HEIGHT, R + T),
            // East, x = 2: a window from z −0.6 to 0.6, 0.9 to 2.1 m up.
            wall(R, 0f, -R - T, R + T, HEIGHT, -0.6f),
            wall(R, 0f, 0.6f, R + T, HEIGHT, R + T),
            wall(R, 0f, -0.6f, R + T, 0.9f, 0.6f),
            wall(R, 2.1f, -0.6f, R + T, HEIGHT, 0.6f),
        )
        return SyntheticScene(shapes)
    }

    private companion object {
        const val R = 2f
        const val T = 0.1f
        const val OUTER = 3.5f
        const val HEIGHT = 2.6f
        const val SURFACE = 1e-3f
        const val TOLERANCE = 0.15f
        val STOPS = listOf(Vec3(0f, 1.4f, 0.2f), Vec3(-0.6f, 1.3f, 0.6f), Vec3(0.6f, 1.5f, -0.4f))

        // Scene content, not UI.
        const val WALL = 0xFFE8E2D6.toInt()
        const val FLOOR = 0xFFB08457.toInt()
        const val OUTSIDE = 0xFF8FA58A.toInt()
    }
}
