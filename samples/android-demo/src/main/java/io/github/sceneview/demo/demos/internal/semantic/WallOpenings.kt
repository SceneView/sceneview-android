package io.github.sceneview.demo.demos.internal.semantic

import io.github.sceneview.demo.demos.internal.DenseCloud
import io.github.sceneview.demo.demos.internal.ScanBox
import io.github.sceneview.demo.demos.internal.ScanSurface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A hole in a wall: a door down to the floor or a window above it. [box] stands in the wall's
 * plane, as long as the hole is wide; [sill] is how high above the floor the hole starts.
 */
class WallOpening(
    val label: SceneLabel,
    val box: ScanBox,
    val width: Float,
    val height: Float,
    val sill: Float,
    val confidence: Float,
)

/**
 * Where a scan's walls have holes. The room survey of the plan (`internal/room/`) will know its
 * walls better than a cloud does: an adapter of it to this interface replaces [CloudWallOpenings]
 * the day it lands, and nothing else changes.
 */
fun interface WallOpenings {
    /** The openings in the walls of [cloud], whose surfels [surfaceOf] sorts. */
    fun find(cloud: DenseCloud, surfaceOf: (Int) -> ScanSurface): List<WallOpening>
}

/**
 * Openings found from the cloud alone. The room is squared up (its walls' direction, from where
 * their surfels stack up), its four walls placed where the most upright surfels are, and each is laid
 * out flat as a grid of [cell]-sized cells: those with a surfel on the wall's plane are wall, and
 * an empty patch walled in on three or four sides, as rectangular as a frame, is a hole. What
 * stands in front of a wall hides it rather than opens it: a patch behind furniture is no hole.
 * A patch down to the floor and of a door's size is a door; one higher up, a window.
 */
class CloudWallOpenings(private val cell: Float = CELL_M) : WallOpenings {
    private class Wall(val alongB: Boolean, val plane: Float, val inward: Float, val from: Float, val to: Float)

    override fun find(cloud: DenseCloud, surfaceOf: (Int) -> ScanSurface): List<WallOpening> {
        val n = cloud.count
        // Every upright surfel, not just those sorted as wall: the sorting takes the first heavy
        // plane coming in from outside for the wall, and through a door or a window that is the
        // room beyond — the holed wall itself is then sorted as an object.
        val normals = cloud.normals
        val walls = (0 until n).filter { i ->
            val surface = surfaceOf(i)
            (surface == ScanSurface.Wall || surface == ScanSurface.Object) &&
                (normals == null || abs(normals[i * 3 + 1]) <= UPRIGHT_NORMAL_Y)
        }.toIntArray()
        if (walls.size < MIN_WALL_SURFELS) return emptyList()
        val p = cloud.positions
        val floors = (0 until n).filter { surfaceOf(it) == ScanSurface.Floor }.map { p[it * 3 + 1] }
        val ceilings = (0 until n).filter { surfaceOf(it) == ScanSurface.Ceiling }.map { p[it * 3 + 1] }
        val wallYs = walls.map { p[it * 3 + 1] }.sorted()
        val floorY = floors.median() ?: wallYs[(wallYs.size * LOW_SHARE).toInt()]
        val topY = ceilings.median() ?: wallYs[(wallYs.size * HIGH_SHARE).toInt()]
        if (topY - floorY < MIN_ROOM_HEIGHT_M) return emptyList()

        val yaw = roomYaw(p, walls)
        val c = cos(yaw)
        val s = sin(yaw)
        val a = FloatArray(n) { p[it * 3] * c + p[it * 3 + 2] * s }
        val b = FloatArray(n) { -p[it * 3] * s + p[it * 3 + 2] * c }
        val (aLow, aHigh) = sides(walls.map { a[it] }.toFloatArray()) ?: return emptyList()
        val (bLow, bHigh) = sides(walls.map { b[it] }.toFloatArray()) ?: return emptyList()
        val sides = listOf(
            Wall(alongB = true, plane = aLow, inward = 1f, from = bLow, to = bHigh),
            Wall(alongB = true, plane = aHigh, inward = -1f, from = bLow, to = bHigh),
            Wall(alongB = false, plane = bLow, inward = 1f, from = aLow, to = aHigh),
            Wall(alongB = false, plane = bHigh, inward = -1f, from = aLow, to = aHigh),
        )
        return sides.flatMap { wall ->
            holes(wall, cloud, a, b, surfaceOf, floorY, topY).map { hole -> hole.placed(wall, c, s) }
        }
    }

    /** A hole of [wall]: columns along it, rows up from the floor, and what was seen in it. */
    private class Hole(
        val label: SceneLabel,
        val from: Float,
        val to: Float,
        val bottom: Float,
        val top: Float,
        val floorY: Float,
        val confidence: Float,
    ) {
        fun placed(wall: Wall, c: Float, s: Float): WallOpening {
            val along = (from + to) / 2
            val ra = if (wall.alongB) wall.plane else along
            val rb = if (wall.alongB) along else wall.plane
            val yaw = if (wall.alongB) atan2(s, c) + (PI / 2).toFloat() else atan2(s, c)
            val width = to - from
            val box = ScanBox(ra * c - rb * s, ra * s + rb * c, bottom, yaw, width, OPENING_DEPTH_M, top - bottom)
            return WallOpening(label, box, width, top - bottom, bottom - floorY, confidence)
        }
    }

    @Suppress("LongParameterList", "CyclomaticComplexMethod", "NestedBlockDepth")
    private fun holes(
        wall: Wall,
        cloud: DenseCloud,
        a: FloatArray,
        b: FloatArray,
        surfaceOf: (Int) -> ScanSurface,
        floorY: Float,
        topY: Float,
    ): List<Hole> {
        val t0 = min(wall.from, wall.to)
        val cols = ceil((max(wall.from, wall.to) - t0) / cell).toInt()
        val rows = ceil((topY - floorY) / cell).toInt()
        if (cols < 3 || rows < 3) return emptyList()
        val seen = BooleanArray(cols * rows)
        val beyond = BooleanArray(cols * rows)
        val front = BooleanArray(cols * rows)
        for (i in 0 until cloud.count) {
            val across = if (wall.alongB) a[i] else b[i]
            val along = if (wall.alongB) b[i] else a[i]
            val col = ((along - t0) / cell).toInt()
            val row = ((cloud.positions[i * 3 + 1] - floorY) / cell).toInt()
            if (col !in 0 until cols || row !in 0 until rows) continue
            val d = (across - wall.plane) * wall.inward
            val at = row * cols + col
            when {
                abs(d) <= ON_WALL_M -> seen[at] = true
                d < -BEYOND_M -> beyond[at] = true
                d in BEYOND_M..FRONT_M && surfaceOf(i) == ScanSurface.Object -> front[at] = true
            }
        }
        // A wall a scan saw is still speckled with gaps a cell wide: closed before looking for holes.
        val wallCell = BooleanArray(cols * rows)
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                var any = false
                for (dr in -1..1) for (dc in -1..1) {
                    val r = row + dr
                    val k = col + dc
                    if (r in 0 until rows && k in 0 until cols && seen[r * cols + k]) any = true
                }
                wallCell[row * cols + col] = any
            }
        }
        val visited = BooleanArray(cols * rows)
        val found = ArrayList<Hole>()
        val queue = IntArray(cols * rows)
        for (start in 0 until cols * rows) {
            if (wallCell[start] || visited[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true
            var c0 = cols
            var c1 = -1
            var r0 = rows
            var r1 = -1
            var shadowed = 0
            var through = 0
            while (head < tail) {
                val at = queue[head++]
                val row = at / cols
                val col = at % cols
                c0 = min(c0, col)
                c1 = max(c1, col)
                r0 = min(r0, row)
                r1 = max(r1, row)
                if (front[at]) shadowed++
                if (beyond[at]) through++
                for ((dr, dc) in NEIGHBOURS) {
                    val r = row + dr
                    val k = col + dc
                    if (r !in 0 until rows || k !in 0 until cols) continue
                    val next = r * cols + k
                    if (!wallCell[next] && !visited[next]) {
                        visited[next] = true
                        queue[tail++] = next
                    }
                }
            }
            val count = tail
            // Running into a corner or up to the ceiling: the scan's edge, not a hole.
            if (c0 == 0 || c1 == cols - 1 || r1 >= rows - 1) continue
            val fill = count.toFloat() / ((c1 - c0 + 1) * (r1 - r0 + 1))
            if (fill < MIN_FILL || shadowed > count * MAX_SHADOW) continue
            var border = 0
            var closed = 0
            fun edge(row: Int, col: Int) {
                if (row !in 0 until rows || col !in 0 until cols) return
                border++
                if (wallCell[row * cols + col]) closed++
            }
            for (row in r0..r1) {
                edge(row, c0 - 1)
                edge(row, c1 + 1)
            }
            for (col in c0..c1) {
                edge(r1 + 1, col)
                if (r0 > 0) edge(r0 - 1, col)
            }
            val enclosed = if (border == 0) 0f else closed.toFloat() / border
            if (enclosed < MIN_ENCLOSED) continue
            // The closing above took a cell off each side of the hole: given back.
            val from = t0 + (c0 - 1) * cell
            val to = t0 + (c1 + 2) * cell
            val bottom = if (r0 <= 2) floorY else floorY + (r0 - 1) * cell
            val top = floorY + (r1 + 2) * cell
            val width = to - from
            val height = top - bottom
            val sill = bottom - floorY
            val throughShare = through.toFloat() / count
            val label = when {
                sill <= DOOR_SILL_M && width in DOOR_WIDTH && height in DOOR_HEIGHT -> SceneLabel.Door
                sill >= WINDOW_SILL_M && width in WINDOW_WIDTH && height in WINDOW_HEIGHT &&
                    (throughShare > 0f || fill >= WINDOW_FILL) -> SceneLabel.Window
                else -> null
            } ?: continue
            val confidence = (BASE_CONFIDENCE + ENCLOSED_WEIGHT * enclosed + THROUGH_WEIGHT * throughShare)
                .coerceAtMost(MAX_CONFIDENCE)
            found += Hole(label, from, to, bottom, top, floorY, confidence)
        }
        return found
    }

    /** The turn about +Y that squares the room up: where the wall surfels stack up most sharply. */
    private fun roomYaw(p: FloatArray, walls: IntArray): Float {
        val step = max(1, walls.size / YAW_SAMPLES)
        val xs = (walls.indices step step).map { p[walls[it] * 3] }
        val zs = (walls.indices step step).map { p[walls[it] * 3 + 2] }
        val reach = max(xs.maxOf { abs(it) }, zs.maxOf { abs(it) }) + cell
        val bins = ceil(2 * reach / cell).toInt() + 1
        val ha = IntArray(bins)
        val hb = IntArray(bins)
        var best = 0f
        var bestScore = -1L
        for (deg in 0 until QUARTER_DEG) {
            val angle = (deg * PI / 180).toFloat()
            val c = cos(angle)
            val s = sin(angle)
            ha.fill(0)
            hb.fill(0)
            for (k in xs.indices) {
                ha[((xs[k] * c + zs[k] * s + reach) / cell).toInt().coerceIn(0, bins - 1)]++
                hb[((-xs[k] * s + zs[k] * c + reach) / cell).toInt().coerceIn(0, bins - 1)]++
            }
            var score = 0L
            for (i in 0 until bins) score += ha[i].toLong() * ha[i] + hb[i].toLong() * hb[i]
            if (score > bestScore) {
                bestScore = score
                best = angle
            }
        }
        return best
    }

    /** The two walls across one axis: the most wall surfels on each side of the room's middle. */
    private fun sides(values: FloatArray): Pair<Float, Float>? {
        val sorted = values.sortedArray()
        val lo = sorted[(sorted.size * LOW_SHARE).toInt()]
        val hi = sorted[(sorted.size * HIGH_SHARE).toInt()]
        if (hi - lo < MIN_ROOM_SIDE_M) return null
        val mid = (lo + hi) / 2
        val low = peak(sorted.filter { it < mid }) ?: return null
        val high = peak(sorted.filter { it >= mid }) ?: return null
        return low to high
    }

    /** Where [values] stack up most, refined to the median of those near it. */
    private fun peak(values: List<Float>): Float? {
        if (values.isEmpty()) return null
        val counts = HashMap<Int, Int>()
        values.forEach { counts.merge(floor(it / cell).toInt(), 1, Int::plus) }
        val bin = counts.maxBy { it.value }.key
        val centre = (bin + 0.5f) * cell
        return values.filter { abs(it - centre) <= ON_WALL_M }.median()
    }

    private fun List<Float>.median(): Float? = if (isEmpty()) null else sorted()[size / 2]

    companion object {
        const val CELL_M = 0.05f

        /** A surfel this close to a wall's plane is the wall. */
        const val ON_WALL_M = 0.1f

        /** Further than this behind the wall: seen through a hole. */
        const val BEYOND_M = 0.15f

        /** Between [BEYOND_M] and this in front of the wall, an object hides it. */
        const val FRONT_M = 1.2f
        const val OPENING_DEPTH_M = 0.12f

        const val DOOR_SILL_M = 0.15f
        val DOOR_WIDTH = 0.6f..1.6f
        val DOOR_HEIGHT = 1.7f..2.5f
        const val WINDOW_SILL_M = 0.3f
        val WINDOW_WIDTH = 0.3f..3f
        val WINDOW_HEIGHT = 0.3f..2.2f

        /** A window nothing was seen through must be this rectangular to be one. */
        const val WINDOW_FILL = 0.75f
        const val MIN_FILL = 0.6f
        const val MIN_ENCLOSED = 0.6f
        const val MAX_SHADOW = 0.5f

        private const val BASE_CONFIDENCE = 0.4f
        private const val ENCLOSED_WEIGHT = 0.3f
        private const val THROUGH_WEIGHT = 0.3f
        private const val MAX_CONFIDENCE = 0.95f
        private const val MIN_WALL_SURFELS = 200

        /** A surfel whose normal leans less than this towards ±Y faces sideways: a wall's. */
        private const val UPRIGHT_NORMAL_Y = 0.5f
        private const val MIN_ROOM_HEIGHT_M = 1.5f
        private const val MIN_ROOM_SIDE_M = 1f
        private const val LOW_SHARE = 0.02f
        private const val HIGH_SHARE = 0.98f
        private const val YAW_SAMPLES = 4000
        private const val QUARTER_DEG = 90
        private val NEIGHBOURS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    }
}
