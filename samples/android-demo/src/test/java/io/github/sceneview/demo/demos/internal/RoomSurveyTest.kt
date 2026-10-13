package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pins the room survey on synthetic rooms (#4540): walls read where the surfels stand, never on
 * the extent of what was seen; a floor that is the densest layer and not the lowest one; no
 * figure for what the scan did not see; the same cloud read twice, the same figures.
 */
class RoomSurveyTest {

    /** A cloud of surfels built wall by wall, in a room turned [yaw] and moved off the origin. */
    private class Room(private val yaw: Float = 0f, private val ox: Float = 0.37f, private val oz: Float = -1.23f) {
        private val positions = ArrayList<Float>()
        private val normals = ArrayList<Float>()
        private val trail = ArrayList<Float>()

        private fun add(a: Float, y: Float, b: Float, na: Float, ny: Float, nb: Float) {
            val c = cos(yaw)
            val s = sin(yaw)
            positions += ox + a * c - b * s
            positions += y
            positions += oz + a * s + b * c
            normals += na * c - nb * s
            normals += ny
            normals += na * s + nb * c
        }

        /** A horizontal layer over [a0]..[a1] × [b0]..[b1] at [y], [thickness] thick, facing up or down. */
        fun slab(
            a0: Float,
            a1: Float,
            b0: Float,
            b1: Float,
            y: Float,
            thickness: Float = 0f,
            up: Boolean = true,
        ) = apply {
            var i = 0
            var a = a0 + STEP / 2
            while (a < a1) {
                var j = 0
                var b = b0 + STEP / 2
                while (b < b1) {
                    // A fixed pattern spreads the layer over its thickness: no random source.
                    val spread = if (thickness == 0f) 0f else ((i * 7 + j * 13) % 11 - 5) / 10f * thickness
                    add(a, y + spread, b, 0f, if (up) 1f else -1f, 0f)
                    b += STEP
                    j++
                }
                a += STEP
                i++
            }
        }

        /** A wall standing on `a = at`, from [b0] to [b1], facing [facing] along a, floor to [top]. */
        fun wallAcross(
            at: Float,
            b0: Float,
            b1: Float,
            facing: Float,
            top: Float = HEIGHT,
            bottom: Float = 0f,
        ) = apply {
            var b = b0 + STEP / 2
            while (b < b1) {
                var y = bottom + STEP / 2
                while (y < top) {
                    add(at, y, b, facing, 0f, 0f)
                    y += RISE
                }
                b += STEP
            }
        }

        /** A wall standing on `b = at`, from [a0] to [a1], facing [facing] along b. */
        fun wallAlong(at: Float, a0: Float, a1: Float, facing: Float, top: Float = HEIGHT, bottom: Float = 0f) = apply {
            var a = a0 + STEP / 2
            while (a < a1) {
                var y = bottom + STEP / 2
                while (y < top) {
                    add(a, y, at, 0f, 0f, facing)
                    y += RISE
                }
                a += STEP
            }
        }

        /** The camera walking a loop around ([a], [b]). */
        fun camera(a: Float, b: Float, radius: Float = 0.7f) = apply {
            val c = cos(yaw)
            val s = sin(yaw)
            for (k in 0 until 40) {
                val t = k / 40f * 2 * Math.PI.toFloat()
                val pa = a + radius * cos(t)
                val pb = b + radius * sin(t)
                trail += ox + pa * c - pb * s
                trail += 1.4f
                trail += oz + pa * s + pb * c
            }
        }

        /** The four walls, floor and ceiling of a [length] × [width] room, each optional. */
        @Suppress("LongParameterList")
        fun box(
            length: Float = 5f,
            width: Float = 4f,
            floorThickness: Float = 0f,
            ceiling: Boolean = true,
            west: Boolean = true,
            east: Boolean = true,
        ) = apply {
            slab(0f, length, 0f, width, 0f, floorThickness)
            if (ceiling) slab(0f, length, 0f, width, HEIGHT, up = false)
            if (west) wallAcross(0f, 0f, width, 1f)
            if (east) wallAcross(length, 0f, width, -1f)
            wallAlong(0f, 0f, length, 1f)
            wallAlong(width, 0f, length, -1f)
            camera(length / 2, width / 2)
        }

        fun input() = RoomSurveyInput(positions.toFloatArray(), normals.toFloatArray(), trail.toFloatArray())

        fun survey() = RoomSurveyor.survey(input())
    }

    private fun assertNear(expected: Float, actual: Float?, tolerance: Float = 0.06f) {
        assertNotNull(actual)
        assertEquals(expected, actual!!, tolerance)
    }

    @Test
    fun `a rectangular room reads its four walls, area, perimeter and height`() {
        val survey = Room().box().survey()

        assertEquals(listOf("A", "B", "C", "D"), survey.walls.map { it.tag })
        assertEquals(listOf(5f, 4f, 5f, 4f), survey.walls.map { Math.round(it.length.value * 10) / 10f })
        assertTrue(survey.walls.all { it.seen && !it.length.estimate })
        assertNear(5f, survey.length?.value)
        assertNear(4f, survey.width?.value)
        assertNear(20f, survey.area?.value, 0.4f)
        assertNear(18f, survey.perimeter?.value, 0.2f)
        assertNear(2.5f, survey.ceilingHeight?.value)
        assertTrue(survey.gaps.isEmpty())
        assertFalse(survey.hasEstimates)
    }

    @Test
    fun `the outline starts top-left and walks clockwise on the page`() {
        val walls = Room().box().survey().walls

        assertEquals(0f, walls[0].ax, 0.06f)
        assertEquals(0f, walls[0].ay, 0.06f)
        assertEquals(5f, walls[0].bx, 0.06f)
        assertTrue(walls[0].horizontal)
        assertEquals(4f, walls[1].by, 0.06f)
        assertFalse(walls[1].horizontal)
        // Each wall starts where the one before it ends.
        for (i in walls.indices) {
            val next = walls[(i + 1) % walls.size]
            assertEquals(walls[i].bx, next.ax, 1e-4f)
            assertEquals(walls[i].by, next.ay, 1e-4f)
        }
    }

    @Test
    fun `a room turned on the scan's axes reads the same figures`() {
        val survey = Room(yaw = 0.52f).box().survey()

        assertNear(5f, survey.length?.value)
        assertNear(4f, survey.width?.value)
        assertNear(20f, survey.area?.value, 0.4f)
        assertEquals(4, survey.walls.size)
    }

    @Test
    fun `a thick floor and a wall drawn twice are one floor and one wall, with their spread`() {
        // The floor's layer is 20 cm thick; the west wall is there twice, 22 cm apart.
        val survey = Room().box(floorThickness = 0.2f).wallAcross(-0.22f, 0f, 4f, 1f).survey()

        assertEquals(0f, survey.floorY, 0.03f)
        assertEquals(0.2f, survey.floorThickness, 0.06f)
        assertEquals(4, survey.walls.size)
        // The doubled wall stands between its two layers, and says so.
        val length = survey.length!!
        assertEquals(5.11f, length.value, 0.05f)
        assertTrue("± ${length.plusMinus}", length.plusMinus in 0.1f..0.2f)
        // The clean pair of walls is read tight.
        assertTrue(survey.width!!.plusMinus < 0.06f)
        assertNear(2.5f, survey.ceilingHeight?.value)
        assertTrue(survey.ceilingHeight!!.plusMinus >= 0.08f)
    }

    @Test
    fun `the survey stays on the walls when planes and stray surfaces overflow them`() {
        // A terrace seen through a doorway in the east wall, and a far façade beyond it.
        val room = Room()
            .slab(0f, 5f, 0f, 4f, 0f)
            .slab(0f, 5f, 0f, 4f, HEIGHT, up = false)
            .wallAcross(0f, 0f, 4f, 1f)
            .wallAcross(5f, 0f, 1.5f, -1f)
            .wallAcross(5f, 2.4f, 4f, -1f)
            .wallAlong(0f, 0f, 5f, 1f)
            .wallAlong(4f, 0f, 5f, -1f)
            .slab(5f, 8.5f, 0.5f, 3.5f, 0f)
            .wallAcross(8.5f, 0f, 4f, -1f, top = 1.1f)
            .camera(2.5f, 2f)
        val survey = room.survey()

        assertNear(20f, survey.area?.value, 0.4f)
        assertNear(5f, survey.length?.value)
        assertNear(4f, survey.width?.value)

        // The same room as ARCore's planes give it: the floor plane runs out onto the terrace.
        val floor = DebugPlane(
            1,
            DebugPlaneKind.Floor,
            floatArrayOf(0.37f, 0f, -1.23f, 8.87f, 0f, -1.23f, 8.87f, 0f, 2.77f, 0.37f, 0f, 2.77f),
        )
        val rectangle = RoomMeasure.of(listOf(floor), 0f)!!
        assertEquals(1.7f, rectangle.area / survey.area!!.value, 0.05f)
    }

    @Test
    fun `a patch 40 cm under the floor is not the floor`() {
        val survey = Room().box(floorThickness = 0.2f).slab(1f, 2.6f, 1f, 2.6f, -0.4f).survey()

        assertEquals(0f, survey.floorY, 0.03f)
        assertNear(2.5f, survey.ceilingHeight?.value)
    }

    @Test
    fun `three walls give a plan and no area`() {
        val survey = Room().box(east = false).survey()

        assertNull(survey.area)
        assertNull(survey.perimeter)
        assertNull(survey.length)
        // The pair that was seen is measured.
        assertNear(4f, survey.width?.value)
        assertTrue(SurveyGap.MissingWall in survey.gaps)
        assertTrue(survey.hasPlan)
        val unseen = survey.walls.filter { !it.seen }
        assertEquals(1, unseen.size)
        assertTrue(unseen[0].length.estimate)
        assertEquals(0f, unseen[0].seenShare, 0f)
        // The walls that run up to the missing one end where the floor was seen to end: estimates.
        assertEquals(2, survey.walls.count { it.seen && it.length.estimate })
        assertTrue(survey.hasEstimates)
    }

    @Test
    fun `two walls give no area either`() {
        val survey = Room().box(east = false, west = false).survey()

        assertNull(survey.area)
        assertNull(survey.length)
        assertNear(4f, survey.width?.value)
        assertTrue(SurveyGap.MissingWall in survey.gaps)
    }

    @Test
    fun `no ceiling seen, no height`() {
        val survey = Room().box(ceiling = false).survey()

        assertNull(survey.ceilingHeight)
        assertEquals(listOf(SurveyGap.Ceiling), survey.gaps)
        assertNear(20f, survey.area?.value, 0.4f)
    }

    @Test
    fun `a corner of ceiling is not a height`() {
        // 0.8 x 0.8 m of ceiling: under the area a height is read from.
        val room = Room().box(ceiling = false).slab(0f, 0.8f, 0f, 0.8f, HEIGHT, up = false)

        assertNull(room.survey().ceilingHeight)
    }

    @Test
    fun `a wall seen on less than half its length is dashed and its length an estimate`() {
        // 1.8 m of the 4 m east wall.
        val survey = Room().box(east = false).wallAcross(5f, 0f, 1.8f, -1f).survey()

        val east = survey.walls.single { !it.horizontal && it.ax > 2.5f }
        assertEquals(0.45f, east.seenShare, 0.05f)
        assertFalse(east.seen)
        assertTrue(east.length.estimate)
        assertNull(survey.area)
        assertTrue(RoomSurveyor.MIN_SEEN_SHARE > east.seenShare)
    }

    @Test
    fun `a wall seen on most of its length is measured, and its gap is known`() {
        // The east wall but for 1 m of it.
        val survey = Room().box(east = false).wallAcross(5f, 0f, 2f, -1f).wallAcross(5f, 3f, 4f, -1f).survey()

        val east = survey.walls.single { !it.horizontal && it.ax > 2.5f }
        assertEquals(0.75f, east.seenShare, 0.05f)
        assertTrue(east.seen)
        assertEquals(1, east.unseen.size)
        assertEquals(2f, east.unseen[0].start, 0.1f)
        assertEquals(3f, east.unseen[0].endInclusive, 0.1f)
        assertNear(20f, survey.area?.value, 0.4f)
    }

    @Test
    fun `furniture is not a wall`() {
        // A sideboard 0.9 m high along the room's middle.
        val survey = Room().box()
            .wallAcross(2f, 1f, 3f, 1f, top = 0.9f)
            .wallAcross(1.6f, 1f, 3f, -1f, top = 0.9f)
            .survey()

        assertEquals(4, survey.walls.size)
        assertNear(20f, survey.area?.value, 0.4f)
    }

    @Test
    fun `an L-shaped room reads its six walls`() {
        // 5 x 4 m with a 2 x 1.5 m corner taken out of it.
        val room = Room()
            .slab(0f, 5f, 0f, 2.5f, 0f)
            .slab(0f, 3f, 2.5f, 4f, 0f)
            .wallAlong(0f, 0f, 5f, 1f)
            .wallAcross(5f, 0f, 2.5f, -1f)
            .wallAlong(2.5f, 3f, 5f, -1f)
            .wallAcross(3f, 2.5f, 4f, -1f)
            .wallAlong(4f, 0f, 3f, -1f)
            .wallAcross(0f, 0f, 4f, 1f)
            .camera(2f, 1.6f)
        val survey = room.survey()

        assertEquals(6, survey.walls.size)
        assertNear(17f, survey.area?.value, 0.4f)
        assertNear(18f, survey.perimeter?.value, 0.3f)
        assertEquals(listOf(5f, 2.5f, 2f, 1.5f, 3f, 4f), survey.walls.map { Math.round(it.length.value * 10) / 10f })
    }

    @Test
    fun `the same cloud read twice gives the same figures`() {
        val room = Room(yaw = 0.3f).box(floorThickness = 0.2f).wallAcross(-0.22f, 0f, 4f, 1f)
        val first = room.survey()
        val second = RoomSurveyor.survey(room.input())

        assertEquals(first.area, second.area)
        assertEquals(first.perimeter, second.perimeter)
        assertEquals(first.ceilingHeight, second.ceilingHeight)
        assertEquals(first.floorY, second.floorY, 0f)
        assertEquals(first.yaw, second.yaw, 0f)
        assertEquals(first.walls.map { it.length }, second.walls.map { it.length })
        assertEquals(first.walls.map { it.seenShare }, second.walls.map { it.seenShare })
    }

    @Test
    fun `a scan with no depth or no wall says so and gives nothing`() {
        val noDepth = RoomSurveyor.survey(RoomSurveyInput(FloatArray(0), null))
        assertEquals(SurveyGap.NoDepth, noDepth.gaps.first())
        assertFalse(noDepth.hasPlan)

        val floorOnly = Room().slab(0f, 5f, 0f, 4f, 0f).camera(2.5f, 2f).survey()
        assertEquals(SurveyGap.NoWalls, floorOnly.gaps.first())
        assertNull(floorOnly.area)
        assertFalse(floorOnly.hasPlan)
    }

    @Test
    fun `an outline no wall was seen along is the floor's extent, and is not drawn`() {
        // The floor, and a short piece of one wall: under half of that side.
        val survey = Room().slab(0f, 5f, 0f, 4f, 0f).wallAlong(0f, 0f, 1.5f, 1f).camera(2.5f, 2f).survey()

        assertFalse(survey.hasPlan)
        assertEquals(SurveyGap.NoWalls, survey.gaps.first())
        assertNull(survey.area)
        assertNull(survey.length)
        assertNull(survey.width)
    }

    @Test
    fun `walls are named A to Z, then AA`() {
        assertEquals("A", RoomSurveyor.tagOf(0))
        assertEquals("Z", RoomSurveyor.tagOf(25))
        assertEquals("AA", RoomSurveyor.tagOf(26))
    }

    private companion object {
        const val STEP = 0.025f
        const val RISE = 0.04f
        const val HEIGHT = 2.5f
    }
}
