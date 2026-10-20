package io.github.sceneview.demo.demos.internal.room

import io.github.sceneview.demo.demos.internal.Vec3
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.floor

/**
 * The room engine on synthetic scans shaped like the phone scans that defeated it: walls seen only
 * up to 0.9 m, floor seen only ahead of the camera, a doorway the floor runs on through, one wall
 * seen twice, two walls of four, an L and a T seen on three quarters of their length. When a
 * figure is given it is within [MAX_ERROR] of the room's; where the walls seen do not close the
 * room, none is, and the plan is still drawn.
 */
class RoomStructureRealScanTest {

    /** A case: the room, the camera's walk through it, and whether its walls close it. */
    private class Case(val name: String, val room: SyntheticRoom, val figures: Boolean, val walk: List<Vec3>? = null)

    private fun capture(case: Case): SyntheticCapture = synchronized(captures) {
        captures.getOrPut(case.name) {
            if (case.walk == null) SyntheticScan.capture(case.room) else SyntheticScan.capture(case.room, walk = case.walk)
        }
    }

    /** One line a failure prints: what was read against the room. */
    private fun summary(case: Case, structure: RoomStructure): String {
        val area = structure.area?.value
        val error = area?.let { "%+.1f %%".format((it - case.room.area) / case.room.area * 100) } ?: "no area"
        return "${case.name}: area ${area?.let { "%.2f".format(it) }} for ${"%.2f".format(case.room.area)} ($error), " +
            "${structure.walls.size} walls, ${structure.seenWalls} seen, ${structure.walls.count { it.deduced }} deduced, " +
            "perimeter ${structure.perimeter?.value?.let { "%.2f".format(it) }} for ${"%.2f".format(case.room.perimeter)}, " +
            "height ${structure.ceilingHeight?.value?.let { "%.2f".format(it) }}, gaps ${structure.gaps}"
    }

    /** What is wrong with [case]'s reading, or `null`. */
    private fun problem(case: Case, structure: RoomStructure): String? {
        val area = structure.area
        return when {
            structure.walls.isEmpty() -> "no plan"
            !case.figures && area != null -> "a figure where the walls seen do not close the room"
            !case.figures && (structure.perimeter != null || structure.length != null || structure.width != null) -> "a length"
            !case.figures && RoomGap.MissingWall !in structure.gaps -> "no missing wall"
            case.figures && area == null -> "no area"
            case.figures && abs(area!!.value - case.room.area) / case.room.area >= MAX_ERROR -> "area off"
            else -> null
        }
    }

    private fun check(case: Case) {
        val structure = RoomStructure.read(capture(case).scan())
        val problem = problem(case, structure)
        assertNull("${summary(case, structure)}: $problem", problem)
    }

    @Test
    fun `walls seen only up to 0,9 m are walls, a sofa standing free is not`() {
        check(LOW_WALLS)
    }

    @Test
    fun `floor seen only ahead of the camera still holds the room`() {
        val scan = capture(FLOOR_AHEAD).scan()
        val share = trailOverFloor(scan)
        assertTrue("the walk stands over seen floor ${"%.0f".format(share * 100)} % of the time", share < MAX_TRAIL_OVER_FLOOR)
        check(FLOOR_AHEAD)
    }

    @Test
    fun `floor seen a metre on through a doorway stays out of the room`() {
        check(DOOR)
    }

    @Test
    fun `one wall seen twice 3 cm apart gives a plan and no figure`() {
        check(TWICE)
    }

    @Test
    fun `two walls of four give a plan and no figure`() {
        check(TWO_LOW)
        check(TWO)
    }

    @Test
    fun `an L seen on three quarters of its walls is read between them`() {
        check(L_PART)
    }

    @Test
    fun `a T seen on three quarters of its walls is read between them`() {
        check(T_PART)
    }

    @Test
    fun `every case at once, with its timings`() {
        val lines = ArrayList<String>()
        var failed = false
        for (case in CASES) {
            val scan = capture(case).scan()
            repeat(WARMUP) { RoomStructure.read(scan) }
            val full = (0 until RUNS).map { timed { RoomStructure.read(scan) } }.sorted()
            repeat(WARMUP) { RoomStructure.live(scan) }
            val live = (0 until RUNS).map { timed { RoomStructure.live(scan) } }.sorted()
            val structure = RoomStructure.read(scan)
            val problem = problem(case, structure)
            if (problem != null || full[RUNS / 2] >= FULL_BUDGET_MS || live.first() >= LIVE_BUDGET_MS) failed = true
            lines += "${summary(case, structure)}, full ${"%.1f".format(full[RUNS / 2])} ms, " +
                "live ${"%.2f".format(live.first())} ms, ${scan.positions.size / 3} points" + (problem?.let { " — $it" } ?: "")
        }
        assertTrue(lines.joinToString(" | "), !failed)
    }

    /** The share of the camera's path that stands over floor the scan saw. */
    private fun trailOverFloor(scan: RoomScan): Float {
        val normals = scan.normals ?: return 0f
        val floor = HashSet<Long>()
        for (i in 0 until scan.positions.size / 3) {
            if (normals[i * 3 + 1] < UP || abs(scan.positions[i * 3 + 1]) > FLOOR_BAND_M) continue
            floor += cellOf(scan.positions[i * 3], scan.positions[i * 3 + 2])
        }
        val steps = scan.trail.size / 3
        if (steps == 0) return 0f
        val over = (0 until steps).count { floor.contains(cellOf(scan.trail[it * 3], scan.trail[it * 3 + 2])) }
        return over.toFloat() / steps
    }

    private fun cellOf(x: Float, z: Float): Long =
        (floor(x / TRAIL_CELL_M).toLong() shl CELL_BITS) xor (floor(z / TRAIL_CELL_M).toLong() and CELL_MASK)

    private inline fun timed(block: () -> Unit): Double {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1e6
    }

    private companion object {
        const val MAX_ERROR = 0.03f
        const val MAX_TRAIL_OVER_FLOOR = 0.2f
        const val UP = 0.8f
        const val FLOOR_BAND_M = 0.05f
        const val TRAIL_CELL_M = 0.1f
        const val CELL_BITS = 32
        const val CELL_MASK = 0xffffffffL
        const val WARMUP = 3
        const val RUNS = 7

        /** The budgets of [RoomStructureTest]: a full reading's median, a live reading's best run. */
        const val FULL_BUDGET_MS = 1000.0
        const val LIVE_BUDGET_MS = 20.0

        /** How high a phone held towards the floor sees the walls. */
        const val LOW_M = 0.9f

        val RECTANGLE_X = floatArrayOf(0f, 5f, 5f, 0f)
        val RECTANGLE_Z = floatArrayOf(0f, 0f, 4f, 4f)

        val LOW_WALLS = Case(
            "low walls",
            SyntheticRoom(RECTANGLE_X, RECTANGLE_Z, seenUpTo = LOW_M, boxes = listOf(SyntheticBox(1.5f, 3.5f, 2.6f, 3.4f, 0.85f))),
            figures = true,
        )

        /** The camera never leaves the room's middle: the floor under it is never seen, the east wall never looked at. */
        val FLOOR_AHEAD = Case(
            "floor ahead",
            SyntheticRoom(RECTANGLE_X, RECTANGLE_Z, hidden = setOf(1)),
            figures = true,
            walk = listOf(Vec3(2.3f, 1.4f, 1.8f), Vec3(2.7f, 1.4f, 1.8f), Vec3(2.7f, 1.4f, 2.2f), Vec3(2.3f, 1.4f, 2.2f)),
        )

        val DOOR = Case(
            "door",
            SyntheticRoom(
                RECTANGLE_X,
                RECTANGLE_Z,
                openings = listOf(SyntheticOpening(wall = 0, from = 1f, width = 0.9f, sill = 0f, height = 2.05f)),
                seenUpTo = LOW_M,
                nextDoor = listOf(SyntheticFloor(-1f, 6f, -1f, 0f)),
            ),
            figures = true,
        )

        val TWICE = Case(
            "one wall twice",
            SyntheticRoom(RECTANGLE_X, RECTANGLE_Z, hidden = setOf(1, 2, 3), driftWall = 0, drift = 0.03f, seenUpTo = LOW_M),
            figures = false,
        )

        val TWO_LOW = Case("two walls", SyntheticRoom(RECTANGLE_X, RECTANGLE_Z, hidden = setOf(2, 3), seenUpTo = LOW_M), figures = false)

        val TWO = Case("two full walls", SyntheticRoom(RECTANGLE_X, RECTANGLE_Z, hidden = setOf(2, 3)), figures = false)

        /** The L's 5 m west wall never seen: 17 m of its 22. */
        val L_PART = Case(
            "L three quarters",
            SyntheticRoom(floatArrayOf(0f, 6f, 6f, 3f, 3f, 0f), floatArrayOf(0f, 0f, 3f, 3f, 5f, 5f), hidden = setOf(5), seenUpTo = LOW_M),
            figures = true,
        )

        /** The T's west wall and its stem's end never seen: 21 m of its 26. */
        val T_PART = Case(
            "T three quarters",
            SyntheticRoom(
                floatArrayOf(0f, 7f, 7f, 4.5f, 4.5f, 2.5f, 2.5f, 0f),
                floatArrayOf(0f, 0f, 3f, 3f, 6f, 6f, 3f, 3f),
                hidden = setOf(4, 7),
                seenUpTo = LOW_M,
            ),
            figures = true,
        )

        val CASES = listOf(LOW_WALLS, FLOOR_AHEAD, DOOR, TWICE, TWO_LOW, TWO, L_PART, T_PART)

        val captures = HashMap<String, SyntheticCapture>()
    }
}
