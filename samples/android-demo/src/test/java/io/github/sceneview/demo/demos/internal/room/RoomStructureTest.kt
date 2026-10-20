package io.github.sceneview.demo.demos.internal.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The room engine on synthetic scans: depth frames rendered from a walk through a known room and
 * fused as on a phone ([SyntheticScan]). Each case's floor area must come within 1 % of the room's.
 */
class RoomStructureTest {

    private fun check(name: String, room: SyntheticRoom, walls: Int, deduced: Int = 0): RoomStructure {
        val structure = RoomStructure.read(capture(name, room).scan())
        val area = assertNotNull("$name: no area", structure.area).let { structure.area!! }
        val error = (area.value - room.area) / room.area
        println("RoomStructure $name: area ${"%.3f".format(area.value)} m² for ${room.area} (${"%+.2f".format(error * 100)} %), " +
            "${structure.walls.size} walls, ${structure.walls.count { it.deduced }} deduced, " +
            "height ${structure.ceilingHeight?.value}")
        assertTrue("$name: area ${area.value} for ${room.area}", abs(error) < MAX_ERROR)
        assertEquals("$name: walls", walls, structure.walls.size)
        assertEquals("$name: deduced walls", deduced, structure.walls.count { it.deduced })
        val perimeterError = abs(structure.perimeter!!.value - room.perimeter) / room.perimeter
        assertTrue("$name: perimeter ${structure.perimeter!!.value} for ${room.perimeter}", perimeterError < MAX_ERROR)
        val height = assertNotNull("$name: no height", structure.ceilingHeight).let { structure.ceilingHeight!!.value }
        assertEquals("$name: height", room.height, height, HEIGHT_M)
        return structure
    }

    @Test
    fun `a rectangle`() {
        check("rectangle", RECTANGLE, walls = 4)
    }

    @Test
    fun `an L`() {
        check("L", SyntheticRoom(floatArrayOf(0f, 6f, 6f, 3f, 3f, 0f), floatArrayOf(0f, 0f, 3f, 3f, 5f, 5f)), walls = 6)
    }

    @Test
    fun `a T`() {
        check(
            "T",
            SyntheticRoom(floatArrayOf(0f, 7f, 7f, 4.5f, 4.5f, 2.5f, 2.5f, 0f), floatArrayOf(0f, 0f, 3f, 3f, 6f, 6f, 3f, 3f)),
            walls = 8,
        )
    }

    @Test
    fun `a U`() {
        check(
            "U",
            SyntheticRoom(floatArrayOf(0f, 6f, 6f, 4f, 4f, 2f, 2f, 0f), floatArrayOf(0f, 0f, 5f, 5f, 2f, 2f, 5f, 5f)),
            walls = 8,
        )
    }

    @Test
    fun `an oblique wall stays oblique`() {
        val room = SyntheticRoom(floatArrayOf(0f, 5f, 5f, 3.5f, 0f), floatArrayOf(0f, 0f, 2.5f, 4f, 4f))
        val structure = check("oblique", room, walls = 5)
        val diagonal = room.wallLength(2)
        assertTrue(
            "the cut corner is a wall of its own",
            structure.walls.any { abs(it.length.value - diagonal) < LENGTH_M },
        )
    }

    @Test
    fun `a wall drawn twice 3 cm apart is one wall`() {
        check("doubled", SyntheticRoom(RECTANGLE.x, RECTANGLE.z, driftWall = 1, drift = 0.03f), walls = 4)
    }

    @Test
    fun `Gaussian depth noise`() {
        check("noise", SyntheticRoom(RECTANGLE.x, RECTANGLE.z, noise = 0.01f), walls = 4)
    }

    @Test
    fun `a wall never seen is deduced, not invented`() {
        val structure = check("unseen", SyntheticRoom(RECTANGLE.x, RECTANGLE.z, hidden = setOf(2)), walls = 4, deduced = 1)
        assertTrue("the area of a room missing a wall is an estimate", structure.area!!.estimate)
    }

    @Test
    fun `a tall wardrobe against a wall is not a wall`() {
        check(
            "furniture",
            SyntheticRoom(RECTANGLE.x, RECTANGLE.z, boxes = listOf(SyntheticBox(1f, 3f, 3.4f, 4f, 2f))),
            walls = 4,
        )
    }

    @Test
    fun `a door and a window are found with their sizes`() {
        val structure = check("openings", OPENINGS, walls = 4)
        val door = structure.openings.singleOrNull { it.kind == OpeningKind.Door }
        val window = structure.openings.singleOrNull { it.kind == OpeningKind.Window }
        println("RoomStructure openings: " + structure.openings.joinToString { "${it.kind} ${it.width}×${it.height} sill ${it.sill}" })
        assertNotNull("one door in ${structure.openings.map { it.kind }}", door)
        assertNotNull("one window in ${structure.openings.map { it.kind }}", window)
        assertEquals(0.9f, door!!.width, OPENING_M)
        assertEquals(2.05f, door.height, OPENING_M)
        assertEquals(0f, door.sill, OPENING_M)
        assertEquals(1.2f, window!!.width, OPENING_M)
        assertEquals(1.2f, window.height, OPENING_M)
        assertEquals(0.9f, window.sill, OPENING_M)
    }

    @Test
    fun `the structure mesh is closed, the openings cut out`() {
        for ((name, room) in listOf("rectangle" to RECTANGLE, "openings" to OPENINGS)) {
            val mesh = RoomStructure.read(capture(name, room).scan()).mesh()
            assertTrue("$name: a mesh", mesh.triangleCount > 0)
            assertEquals("$name: no edge shared by more than two faces", 2, mesh.maxEdgeUse())
            assertEquals("$name: no open edge off a hole", 0, mesh.leaks())
            assertTrue(mesh.watertight)
        }
    }

    @Test
    fun `fewer than two walls seen gives no figure`() {
        // Three walls out of four never seen: one wall alone measures nothing.
        val structure = RoomStructure.read(capture("one wall", SyntheticRoom(RECTANGLE.x, RECTANGLE.z, hidden = setOf(1, 2, 3))).scan())
        assertTrue("seen walls ${structure.seenWalls}", structure.seenWalls < RoomStructure.MIN_SEEN_WALLS)
        assertNull(structure.area)
        assertNull(structure.perimeter)
        assertNull(structure.volume)
    }

    @Test
    fun `the whole room reads in well under a second`() {
        val scan = capture("openings", OPENINGS).scan()
        repeat(FULL_WARMUP) { RoomStructure.read(scan) }
        val millis = (0 until RUNS).map {
            val start = System.nanoTime()
            RoomStructure.read(scan)
            (System.nanoTime() - start) / 1e6
        }.sorted()
        println("RoomStructure full read: median ${"%.1f".format(millis[RUNS / 2])} ms, max ${"%.1f".format(millis.last())} ms, ${scan.positions.size / 3} points")
        assertTrue("median ${millis[RUNS / 2]} ms", millis[RUNS / 2] < FULL_BUDGET_MS)
    }

    @Test
    fun `the live reading sees the walls of a scan in progress, in a few milliseconds`() {
        val partial = SyntheticScan.capture(RECTANGLE, share = 0.4f)
        val scan = partial.scan()
        repeat(WARMUP) { RoomStructure.live(scan) }
        val millis = (0 until RUNS).map {
            val start = System.nanoTime()
            RoomStructure.live(scan)
            (System.nanoTime() - start) / 1e6
        }.sorted()
        val live = RoomStructure.live(scan)
        println("RoomStructure live read: median ${"%.2f".format(millis[RUNS / 2])} ms, max ${"%.2f".format(millis.last())} ms, " +
            "${scan.positions.size / 3} points, ${live.walls.size} walls, area ${live.area}")
        assertTrue("median ${millis[RUNS / 2]} ms", millis[RUNS / 2] < LIVE_BUDGET_MS)
        assertTrue("walls seen", live.walls.isNotEmpty())
        assertTrue("an outline", live.outline.size >= 6)
        for (wall in live.walls) {
            assertTrue(wall.height > 1f)
            assertTrue(wall.confidence in 0f..1f)
        }
    }

    @Test
    fun `the live reading runs off the main thread`() {
        val scan = capture("rectangle", RECTANGLE).scan()
        var live: LiveRoom? = null
        val thread = Thread { live = RoomStructure.live(scan) }
        thread.start()
        thread.join()
        assertNotNull(live)
        assertFalse(live!!.walls.isEmpty())
    }

    companion object {
        private const val MAX_ERROR = 0.01f
        private const val HEIGHT_M = 0.05f
        private const val LENGTH_M = 0.05f
        private const val OPENING_M = 0.1f
        private const val WARMUP = 100
        private const val RUNS = 21
        private const val FULL_BUDGET_MS = 1000.0
        private const val FULL_WARMUP = 10

        /** The target is 5 ms on a laptop's JVM (see the printed figure); twice that leaves room for a shared CI runner. */
        private const val LIVE_BUDGET_MS = 10.0

        val RECTANGLE = SyntheticRoom(floatArrayOf(0f, 5f, 5f, 0f), floatArrayOf(0f, 0f, 4f, 4f))
        val OPENINGS = SyntheticRoom(
            RECTANGLE.x,
            RECTANGLE.z,
            openings = listOf(
                SyntheticOpening(wall = 0, from = 1f, width = 0.9f, sill = 0f, height = 2.05f),
                SyntheticOpening(wall = 1, from = 1.4f, width = 1.2f, sill = 0.9f, height = 1.2f),
            ),
        )

        private val captures = HashMap<String, SyntheticCapture>()

        /** Each room is rendered and fused once per run. */
        @Synchronized
        fun capture(name: String, room: SyntheticRoom): SyntheticCapture = captures.getOrPut(name) { SyntheticScan.capture(room) }
    }
}
