package io.github.sceneview.demo.demos

import io.github.sceneview.demo.ai.scene.FloorPoint
import io.github.sceneview.demo.ai.scene.OrientedBox
import io.github.sceneview.demo.ai.scene.PlacedItem
import io.github.sceneview.demo.ai.scene.RoomModel
import io.github.sceneview.demo.ai.scene.RoomObject
import io.github.sceneview.demo.ai.scene.RoomWall
import io.github.sceneview.demo.demos.internal.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How the `ai-scene` demo dresses the room the AI rebuilt: which way each recognised object faces,
 * how a catalogue model is turned and scaled into the box the scan measured, and the oak floor
 * laid on the room's side of the first wall.
 */
class AiSceneLayoutTest {

    // A wall along +X at z = 0; the room lies on −Z (where the furniture is).
    private val wall = RoomWall("w1", FloorPoint(0f, 0f), FloorPoint(4f, 0f), 2.5f, "plane 1")

    private fun box(x: Float, z: Float, w: Float, h: Float, d: Float, yaw: Float = 0f) =
        OrientedBox(Vec3(x, h / 2, z), Vec3(w, h, d), yaw)

    @Test
    fun `a model is turned a quarter at a time so its front meets the box's front`() {
        assertEquals(0, quarterTurnsFor(0f to 1f))
        assertEquals(1, quarterTurnsFor(1f to 0f))
        assertEquals(2, quarterTurnsFor(0f to -1f))
        assertEquals(3, quarterTurnsFor(-1f to 0f))
        // Off-axis: the closest quarter wins.
        assertEquals(1, quarterTurnsFor(0.9f to 0.3f))
    }

    @Test
    fun `a model fills its box's footprint at one scale, never squashed, and keeps its height by default`() {
        // A 1.5 × 0.75 × 0.75 desk into a 1.2 × 0.75 × 0.6 box, facing +Z.
        val fit = fitModel(floatArrayOf(0.75f, 0.375f, 0.375f), box(0f, 0f, 1.2f, 0.75f, 0.6f), 0f to 1f, false)
        assertEquals(0, fit.quarterTurns)
        assertEquals(0.8f, fit.sx, 1e-4f)
        assertEquals(0.8f, fit.sz, 1e-4f)
        assertEquals(0.8f, fit.sy, 1e-4f)
    }

    @Test
    fun `a model turned a quarter swaps its width and depth before it is scaled`() {
        // Facing +X: the model's depth (0.5) runs along the box's x, its width (1.0) along z.
        val fit = fitModel(floatArrayOf(0.5f, 0.5f, 0.25f), box(0f, 0f, 0.5f, 1f, 1f), 1f to 0f, false)
        assertEquals(1, fit.quarterTurns)
        assertEquals(1f, fit.sx, 1e-4f)
        assertEquals(1f, fit.sz, 1e-4f)
    }

    @Test
    fun `a model may stretch a little to fill a longer box, and match the box's height when asked`() {
        // A 1 m cube into a 1.1 × 2 × 1 box: x stretches to 1.1, height follows up to the cap.
        val fit = fitModel(floatArrayOf(0.5f, 0.5f, 0.5f), box(0f, 0f, 1.1f, 2f, 1f), 0f to 1f, true)
        assertEquals(1.1f, fit.sx, 1e-4f)
        assertEquals(1f, fit.sz, 1e-4f)
        assertTrue("height is capped", fit.sy < 2f && fit.sy > 1f)
    }

    @Test
    fun `a chair faces its desk, and the desk faces its chair`() {
        val desk = RoomObject("desk", "desk", box(2f, -0.4f, 1.2f, 0.75f, 0.6f), "")
        val chair = RoomObject("chair", "chair", box(2f, -1.2f, 0.5f, 0.8f, 0.5f), "")
        val model = RoomModel(walls = listOf(wall), objects = listOf(desk, chair))
        val chairFront = frontOf(chair, model)!!
        assertEquals(0f, chairFront.first, 1e-4f)
        assertEquals(1f, chairFront.second, 1e-4f) // towards +Z, the desk
        val deskFront = frontOf(desk, model)!!
        assertEquals(-1f, deskFront.second, 1e-4f) // towards −Z, the chair
    }

    @Test
    fun `a radiator faces away from its wall, into the room`() {
        val radiator = RoomObject("rad", "radiator", box(1f, -0.1f, 1f, 0.6f, 0.1f), "")
        val front = frontOf(radiator, RoomModel(walls = listOf(wall), objects = listOf(radiator)))!!
        assertEquals(-1f, front.second, 1e-4f)
    }

    @Test
    fun `a bed's head goes against the wall it touches`() {
        // Long side along z, its +z end 0.1 m from the wall: the foot points to −Z.
        val bed = RoomObject("bed", "bed", box(1f, -1.1f, 1.4f, 0.5f, 2f), "")
        val front = frontOf(bed, RoomModel(walls = listOf(wall), objects = listOf(bed)))!!
        assertEquals(0f to -1f, front)
    }

    @Test
    fun `the floor lies on the furniture's side of the wall and reaches past all of it`() {
        val sofa = PlacedItem("a", "armchair", box(3f, -2.5f, 0.7f, 0.8f, 0.7f), "oak")
        val floor = roomFloor(wall, RoomModel(walls = listOf(wall), items = listOf(sofa)))
        // The wall runs +X; its local +z is world +Z, so the room is on local −z.
        assertEquals(-1f, floor.inward)
        assertEquals(WALL_THICKNESS_M, floor.z1, 1e-4f)
        assertTrue("covers the armchair", floor.z0 < -2.85f)
        assertTrue("covers the wall's length", floor.x0 < 0f && floor.x1 > 4f)
    }

    @Test
    fun `the planks tile the floor in staggered rows of three cuts`() {
        val floor = RoomFloor(1f, 0f, 3f, 0f, 0.64f)
        val planks = plankLayout(floor)
        assertEquals(4, planks.map { it.z0 }.distinct().size)
        planks.forEach { p ->
            assertTrue(p.x0 >= floor.x0 && p.x1 <= floor.x1 && p.z1 <= floor.z1 + 1e-4f)
            assertTrue(p.cut in 0..2)
        }
        // Each row covers the whole length.
        planks.groupBy { it.z0 }.values.forEach { row ->
            assertEquals(3f, row.sumOf { (it.x1 - it.x0).toDouble() }.toFloat(), 1e-3f)
        }
        // Neighbouring rows do not line their first joint up.
        val firstJoints = planks.groupBy { it.z0 }.values.map { row -> row.minBy { it.x0 }.x1 }
        assertTrue(firstJoints.zipWithNext().all { (a, b) -> kotlin.math.abs(a - b) > 0.05f })
    }
}
