package io.github.sceneview.demo.ai.scene

import io.github.sceneview.demo.demos.internal.DebugPlaneKind
import io.github.sceneview.demo.demos.internal.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A wall line the cloud shows: [from] → [to] on the floor, [height] above it (the 95th percentile
 * of its points). Candidates are what the code found; the model picks the ones that bound the room.
 */
data class WallCandidate(
    val id: String,
    val from: FloorPoint,
    val to: FloorPoint,
    val height: Float,
    /** Share of the 10 cm stretches along the line that the cloud covers, 0..1. */
    val support: Float,
    val points: Int,
    val gaps: List<GapCandidate>,
    /** The ARCore wall plane lying on this line, when there is one. */
    val planeId: Int?,
) {
    val length: Float get() = from.distanceTo(to)
}

/**
 * An empty patch of a wall candidate: [from] → [to] along it on the floor, [low] … [high] above
 * the floor. [guess] is the code's reading of its shape — the model decides.
 */
data class GapCandidate(
    val id: String,
    val wallId: String,
    val from: FloorPoint,
    val to: FloorPoint,
    val low: Float,
    val high: Float,
    /** `door`, `window`, `occluded` (something stands in front of it) or `unknown`. */
    val guess: String,
) {
    val width: Float get() = from.distanceTo(to)
}

/** A cluster of points standing on the floor away from the walls, boxed; [againstWall] names the wall it touches. */
data class ObjectCandidate(
    val id: String,
    val box: OrientedBox,
    val points: Int,
    val againstWall: String?,
)

/** Everything the code could read off the scan before the model chose anything. */
data class RoomProposal(
    val floorY: Float,
    val ceilingHeight: Float?,
    /** Yaw (degrees, about +Y) of the room's frame: walls run along its local X and Z. */
    val yawDegrees: Float,
    val walls: List<WallCandidate>,
    val objects: List<ObjectCandidate>,
    val notes: List<String>,
) {
    fun wall(id: String): WallCandidate? = walls.firstOrNull { it.id == id }

    fun gap(id: String): GapCandidate? = walls.asSequence().flatMap { it.gaps }.firstOrNull { it.id == id }

    fun cluster(id: String): ObjectCandidate? = objects.firstOrNull { it.id == id }
}

/** A wall of a closed outline: [index] is the input segment it came from. */
data class OutlineWall(val index: Int, val from: FloorPoint, val to: FloorPoint) {
    val length: Float get() = from.distanceTo(to)
}

/** Wall lines closed into a polygon when they meet; [area] only then — an open outline has no area. */
data class RoomOutline(val corners: List<FloorPoint>, val walls: List<OutlineWall>, val closed: Boolean) {
    val area: Float? = if (closed) shoelace(corners) else null
    val perimeter: Float = walls.map { it.length }.sum()

    private companion object {
        fun shoelace(corners: List<FloorPoint>): Float {
            var twice = 0f
            for (i in corners.indices) {
                val a = corners[i]
                val b = corners[(i + 1) % corners.size]
                twice += a.x * b.z - b.x * a.z
            }
            return abs(twice) / 2
        }
    }
}

/**
 * Geometry that proposes a room from the cloud: its ceiling, its frame, the wall lines, the empty
 * patches in them and the clusters standing in it. Nothing here chooses — the model does, through
 * `propose_room`, `build_room`, `accept_opening` and `accept_object` — and nothing here measures
 * from text: every number is computed from the points.
 */
object RoomReconstruction {

    fun propose(session: SpatialSession): RoomProposal {
        val frame = RoomFrame(session)
        val notes = ArrayList<String>()
        if (frame.count == 0) {
            return RoomProposal(session.floorY, null, 0f, emptyList(), emptyList(), listOf("empty scan"))
        }
        val ceiling = ceilingHeight(frame)
        if (ceiling == null) notes += "ceiling not seen"
        val lines = ArrayList<Line>()
        for (axis in 0..1) lines += wallLines(frame, axis)
        var walls = lines.filter { it.height >= WALL_MIN_HEIGHT_M }
        if (walls.isEmpty() && lines.isNotEmpty()) {
            walls = lines.filter { it.height >= LOW_WALL_MIN_HEIGHT_M }
            val top = walls.maxOfOrNull { it.height }
            if (top != null) notes += "walls seen only up to ${"%.1f".format(java.util.Locale.ROOT, top)} m"
        }
        walls = walls.sortedWith(compareBy({ it.axis }, { it.coord }))
        val ids = walls.indices.map { "C${it + 1}" }
        val objects = clusters(frame, walls, ids)
        val sparse = walls.any { it.density < MIN_GAP_DENSITY }
        if (sparse) notes += "sparse scan: openings not analysed on every wall"
        var gapCount = 0
        val candidates = walls.mapIndexed { i, line ->
            val gaps = if (line.density < MIN_GAP_DENSITY) emptyList() else gaps(frame, line, ids[i], objects)
            val numbered = gaps.map { it.copy(id = "G${++gapCount}") }
            WallCandidate(
                ids[i], frame.floor(line, line.lo), frame.floor(line, line.hi), line.height, line.support,
                line.indices.size, numbered, planeOn(session, frame, line),
            )
        }
        return RoomProposal(session.floorY, ceiling, frame.yaw, candidates, objects, notes)
    }

    /**
     * Closes wall segments into a polygon: ordered around the centre of their midpoints, each
     * corner the intersection of two neighbours. Parallel neighbours, or a corner more than
     * [MAX_CORNER_REACH_M] past both walls, leave the outline open at its scanned extents.
     */
    fun close(segments: List<Pair<FloorPoint, FloorPoint>>): RoomOutline {
        val open = RoomOutline(emptyList(), segments.mapIndexed { i, (a, b) -> OutlineWall(i, a, b) }, false)
        if (segments.size < MIN_CLOSED_WALLS) return open
        val mids = segments.map { (a, b) -> FloorPoint((a.x + b.x) / 2, (a.z + b.z) / 2) }
        val cx = mids.map { it.x }.average().toFloat()
        val cz = mids.map { it.z }.average().toFloat()
        val order = segments.indices.sortedBy { atan2(mids[it].z - cz, mids[it].x - cx) }
        val n = order.size
        val corners = ArrayList<FloorPoint>(n)
        for (k in 0 until n) {
            val a = segments[order[k]]
            val b = segments[order[(k + 1) % n]]
            val corner = intersect(a, b) ?: return open
            if (reach(corner, a) > MAX_CORNER_REACH_M || reach(corner, b) > MAX_CORNER_REACH_M) return open
            corners += corner
        }
        val walls = List(n) { k -> OutlineWall(order[k], corners[(k - 1 + n) % n], corners[k]) }
        return RoomOutline(corners, walls, true)
    }

    private fun reach(p: FloorPoint, segment: Pair<FloorPoint, FloorPoint>): Float =
        min(p.distanceTo(segment.first), p.distanceTo(segment.second))

    /** The intersection of the two infinite lines, or `null` when they are parallel. */
    private fun intersect(a: Pair<FloorPoint, FloorPoint>, b: Pair<FloorPoint, FloorPoint>): FloorPoint? {
        val dax = a.second.x - a.first.x
        val daz = a.second.z - a.first.z
        val dbx = b.second.x - b.first.x
        val dbz = b.second.z - b.first.z
        val cross = dax * dbz - daz * dbx
        val la = sqrt(dax * dax + daz * daz)
        val lb = sqrt(dbx * dbx + dbz * dbz)
        if (la < 1e-6f || lb < 1e-6f || abs(cross) / (la * lb) < PARALLEL_SIN) return null
        val t = ((b.first.x - a.first.x) * dbz - (b.first.z - a.first.z) * dbx) / cross
        return FloorPoint(a.first.x + dax * t, a.first.z + daz * t)
    }

    // ---- ceiling ----

    private fun ceilingHeight(f: RoomFrame): Float? {
        val bins = IntArray(((CEILING_MAX_M - CEILING_MIN_M) / CEILING_BIN_M).toInt())
        for (i in 0 until f.count) {
            val h = f.h[i]
            if (h < CEILING_MIN_M || h >= CEILING_MAX_M || !f.facesDown(i)) continue
            bins[((h - CEILING_MIN_M) / CEILING_BIN_M).toInt()]++
        }
        val smooth = smoothed(bins, 1)
        val peak = smooth.indices.maxByOrNull { smooth[it] } ?: return null
        val around = (peak - FAR_BINS..peak - NEAR_BINS) + (peak + NEAR_BINS..peak + FAR_BINS)
        val neighbours = around.filter { it in smooth.indices }.map { smooth[it] }.sorted()
        val floorOfPeak = if (neighbours.isEmpty()) 0 else neighbours[neighbours.size / 2]
        if (smooth[peak] < max(MIN_CEILING_POINTS, CEILING_PEAK_RATIO * floorOfPeak)) return null
        var weight = 0.0
        var sum = 0.0
        for (b in (peak - 2).coerceAtLeast(0)..(peak + 2).coerceAtMost(bins.lastIndex)) {
            weight += bins[b]
            sum += bins[b] * (CEILING_MIN_M + (b + 0.5f) * CEILING_BIN_M)
        }
        return if (weight == 0.0) null else (sum / weight).toFloat()
    }

    // ---- walls ----

    /** A wall line in the room frame: `across == coord`, running from [lo] to [hi] along the other axis. */
    private class Line(
        val axis: Int,
        val coord: Float,
        val lo: Float,
        val hi: Float,
        val height: Float,
        val support: Float,
        val indices: IntArray,
    ) {
        val length: Float get() = hi - lo
        val density: Float get() = indices.size / (length * height).coerceAtLeast(1e-3f)
    }

    private fun wallLines(f: RoomFrame, axis: Int): List<Line> {
        val across = if (axis == 0) f.u else f.v
        val band = IntArray(f.count)
        var m = 0
        for (i in 0 until f.count) if (f.isWallPoint(i, axis)) band[m++] = i
        if (m < MIN_WALL_POINTS) return emptyList()
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (k in 0 until m) {
            lo = min(lo, across[band[k]])
            hi = max(hi, across[band[k]])
        }
        val bins = IntArray(((hi - lo) / LINE_BIN_M).toInt() + 1)
        for (k in 0 until m) bins[((across[band[k]] - lo) / LINE_BIN_M).toInt()]++
        val smooth = smoothed(bins, 2)
        val minPeak = max(MIN_PEAK_POINTS, (m * PEAK_SHARE).toInt())
        return localMaxima(smooth, minPeak).mapNotNull { b ->
            lineAt(f, axis, band.copyOf(m), lo + (b + 0.5f) * LINE_BIN_M)
        }
    }

    private fun lineAt(f: RoomFrame, axis: Int, band: IntArray, centre: Float): Line? {
        val across = if (axis == 0) f.u else f.v
        val along = if (axis == 0) f.v else f.u
        val indices = band.filter { abs(across[it] - centre) <= WALL_BAND_M }.toIntArray()
        if (indices.size < MIN_WALL_POINTS) return null
        val coord = indices.map { across[it] }.sorted().percentile(0.5f)
        val ts = indices.map { along[it] }.sorted()
        val lo = ts.percentile(EDGE_LO)
        val hi = ts.percentile(EDGE_HI)
        if (hi - lo < MIN_WALL_LENGTH_M) return null
        val height = indices.map { f.h[it] }.sorted().percentile(HEIGHT_HI)
        val stretches = ((hi - lo) / STRETCH_M).toInt().coerceAtLeast(1)
        val seen = IntArray(stretches)
        for (t in ts) {
            val s = ((t - lo) / STRETCH_M).toInt()
            if (s in 0 until stretches) seen[s]++
        }
        val support = seen.count { it >= MIN_STRETCH_POINTS }.toFloat() / stretches
        return Line(axis, coord, lo, hi, height, support, indices)
    }

    /** The ARCore wall plane on [line]: parallel within 10°, within 15 cm, overlapping its extent. */
    private fun planeOn(session: SpatialSession, f: RoomFrame, line: Line): Int? =
        session.planes.firstOrNull { plane ->
            val n = session.planeNormal(plane)
            val vertical = plane.kind == DebugPlaneKind.Wall || abs(n.y) < VERTICAL_NORMAL_Y
            val local = f.toLocal(n.x, n.z)
            val horizontal = sqrt(n.x * n.x + n.z * n.z).coerceAtLeast(1e-6f)
            val facing = abs(if (line.axis == 0) local.first else local.second) / horizontal
            val c = session.planeCentroid(plane)
            val lc = f.toLocal(c.x, c.z)
            val across = if (line.axis == 0) lc.first else lc.second
            val along = if (line.axis == 0) lc.second else lc.first
            val onLine = abs(across - line.coord) <= WALL_PLANE_OFFSET_M
            val within = along in line.lo - WALL_PLANE_OFFSET_M..line.hi + WALL_PLANE_OFFSET_M
            vertical && facing >= PARALLEL_COS && onLine && within
        }?.id

    // ---- gaps ----

    private fun gaps(f: RoomFrame, line: Line, wallId: String, objects: List<ObjectCandidate>): List<GapCandidate> {
        val along = if (line.axis == 0) f.v else f.u
        val cols = (line.length / CELL_M).toInt().coerceAtLeast(1)
        val rows = (line.height / CELL_M).toInt().coerceAtLeast(1)
        val filled = BooleanArray(cols * rows)
        for (i in line.indices) {
            val c = ((along[i] - line.lo) / CELL_M).toInt()
            val r = (f.h[i] / CELL_M).toInt()
            if (c in 0 until cols && r in 0 until rows) filled[r * cols + c] = true
        }
        val runs = Array(cols) { c -> emptyRun(filled, cols, rows, c) }
        val out = ArrayList<GapCandidate>()
        var start = -1
        for (c in 0..cols) {
            val run = runs.getOrNull(c)
            val joins = start >= 0 && run != null && overlap(runs[c - 1], run) >= MIN_RUN_OVERLAP_M
            if (joins) continue
            if (start >= 0) gapOf(f, line, wallId, start, c - 1, runs, objects)?.let { out += it }
            start = if (run != null) c else -1
        }
        return out
    }

    /** The longest empty vertical run of column [c], as (low, high) metres, when it is at least [MIN_RUN_M]. */
    private fun emptyRun(filled: BooleanArray, cols: Int, rows: Int, c: Int): Pair<Float, Float>? {
        var best: Pair<Int, Int>? = null
        var runStart = -1
        for (r in 0..rows) {
            val empty = r < rows && !filled[r * cols + c]
            if (empty && runStart < 0) runStart = r
            if (!empty && runStart >= 0) {
                if (best == null || r - runStart > best.second - best.first) best = runStart to r
                runStart = -1
            }
        }
        val run = best ?: return null
        if ((run.second - run.first) * CELL_M < MIN_RUN_M) return null
        return run.first * CELL_M to run.second * CELL_M
    }

    private fun overlap(a: Pair<Float, Float>?, b: Pair<Float, Float>?): Float =
        if (a == null || b == null) 0f else min(a.second, b.second) - max(a.first, b.first)

    private fun gapOf(
        f: RoomFrame,
        line: Line,
        wallId: String,
        c0: Int,
        c1: Int,
        runs: Array<Pair<Float, Float>?>,
        objects: List<ObjectCandidate>,
    ): GapCandidate? {
        val width = (c1 - c0 + 1) * CELL_M
        if (width < MIN_GAP_M) return null
        val group = (c0..c1).mapNotNull { runs[it] }
        val low = group.map { it.first }.sorted().percentile(0.5f)
        val high = group.map { it.second }.sorted().percentile(0.5f)
        val t0 = line.lo + c0 * CELL_M
        val t1 = line.lo + (c1 + 1) * CELL_M
        val hidden = objects.any { o -> f.standsBefore(o.box, line, t0, t1, low) }
        val guess = when {
            hidden -> "occluded"
            low <= DOOR_SILL_MAX_M && high >= DOOR_HEAD_MIN_M -> "door"
            low > WINDOW_SILL_MIN_M && high < line.height - WINDOW_HEAD_MARGIN_M -> "window"
            low <= DOOR_SILL_MAX_M -> "occluded"
            else -> "unknown"
        }
        return GapCandidate("", wallId, f.floor(line, t0), f.floor(line, t1), low, high, guess)
    }

    // ---- objects ----

    private fun clusters(f: RoomFrame, walls: List<Line>, ids: List<String>): List<ObjectCandidate> {
        val selected = (0 until f.count).filter { i ->
            f.h[i] in OBJECT_LOW_M..OBJECT_HIGH_M && walls.none { abs(f.across(it, i) - it.coord) <= WALL_SKIRT_M }
        }
        if (selected.isEmpty()) return emptyList()
        val grid = CellGrid(f, selected)
        val components = grid.components()
        val boxes = components.mapNotNull { cells ->
            val points = cells.flatMap { grid.pointsOf(it) }
            val extentCells = grid.extentCells(cells)
            if (cells.size < MIN_CLUSTER_CELLS || points.size < MIN_CLUSTER_POINTS) return@mapNotNull null
            if (extentCells * GRID_M < MIN_CLUSTER_M) return@mapNotNull null
            boxOf(f, points, walls, ids)
        }
        return boxes.sortedByDescending { it.second }.take(MAX_CLUSTERS)
            .mapIndexed { i, (box, points, against) -> ObjectCandidate("K${i + 1}", box, points, against) }
    }

    /** The smallest rectangle around [points], searched over yaw offsets; axis-aligned ones snap to a near wall. */
    private fun boxOf(
        f: RoomFrame,
        points: List<Int>,
        walls: List<Line>,
        ids: List<String>,
    ): Triple<OrientedBox, Int, String?> {
        var best: Rect? = null
        for (offset in 0 until QUARTER_TURN step YAW_STEP) {
            val rect = Rect.around(f, points, offset.toFloat())
            if (best == null || rect.area < best.area) best = rect
        }
        var rect = checkNotNull(best)
        var against: String? = null
        if (rect.offset == 0f) {
            for ((k, line) in walls.withIndex()) {
                val snapped = rect.snapTo(line) ?: continue
                rect = snapped
                against = ids[k]
            }
        }
        val height = points.map { f.h[it] }.sorted().percentile(HEIGHT_HI)
        val box = rect.toBox(f, height)
        return Triple(box, points.size, against)
    }

    /** A rectangle in the room frame turned by [offset]: [a] across its local X, [b] across its local Z. */
    private class Rect(val a0: Float, val a1: Float, val b0: Float, val b1: Float, val offset: Float) {
        val area: Float get() = (a1 - a0) * (b1 - b0)

        /** Pulls the face within [SNAP_M] of [line] onto it; `null` when none is. Axis-aligned rects only. */
        fun snapTo(line: Line): Rect? {
            val (lo, hi) = if (line.axis == 0) a0 to a1 else b0 to b1
            val (other0, other1) = if (line.axis == 0) b0 to b1 else a0 to a1
            if (other1 < line.lo || other0 > line.hi) return null
            val newLo = if (lo - line.coord in 0f..SNAP_M) line.coord else lo
            val newHi = if (line.coord - hi in 0f..SNAP_M) line.coord else hi
            if (newLo == lo && newHi == hi) return null
            return if (line.axis == 0) Rect(newLo, newHi, b0, b1, offset) else Rect(a0, a1, newLo, newHi, offset)
        }

        fun toBox(f: RoomFrame, height: Float): OrientedBox {
            val c = cos(Math.toRadians(offset.toDouble())).toFloat()
            val s = sin(Math.toRadians(offset.toDouble())).toFloat()
            val ac = (a0 + a1) / 2
            val bc = (b0 + b1) / 2
            val centre = f.toWorld(ac * c + bc * s, f.floorY + height / 2, -ac * s + bc * c)
            return OrientedBox(centre, Vec3(a1 - a0, height, b1 - b0), f.yaw + offset)
        }

        companion object {
            fun around(f: RoomFrame, points: List<Int>, offset: Float): Rect {
                val c = cos(Math.toRadians(offset.toDouble())).toFloat()
                val s = sin(Math.toRadians(offset.toDouble())).toFloat()
                var a0 = Float.MAX_VALUE
                var a1 = -Float.MAX_VALUE
                var b0 = Float.MAX_VALUE
                var b1 = -Float.MAX_VALUE
                for (i in points) {
                    val a = f.u[i] * c - f.v[i] * s
                    val b = f.u[i] * s + f.v[i] * c
                    a0 = min(a0, a)
                    a1 = max(a1, a)
                    b0 = min(b0, b)
                    b1 = max(b1, b)
                }
                return Rect(a0, a1, b0, b1, offset)
            }
        }
    }

    /** The selected points binned on a [GRID_M] grid of the room frame, with 8-connected components. */
    private class CellGrid(f: RoomFrame, selected: List<Int>) {
        private val u0 = selected.minOf { f.u[it] }
        private val v0 = selected.minOf { f.v[it] }
        private val cols = ((selected.maxOf { f.u[it] } - u0) / GRID_M).toInt() + 1
        private val rows = ((selected.maxOf { f.v[it] } - v0) / GRID_M).toInt() + 1
        private val cells: Map<Int, List<Int>> = selected.groupBy { i ->
            ((f.u[i] - u0) / GRID_M).toInt() + ((f.v[i] - v0) / GRID_M).toInt() * cols
        }

        fun pointsOf(cell: Int): List<Int> = cells[cell].orEmpty()

        fun extentCells(component: List<Int>): Int {
            val cs = component.map { it % cols }
            val rs = component.map { it / cols }
            return max(cs.max() - cs.min() + 1, rs.max() - rs.min() + 1)
        }

        fun components(): List<List<Int>> {
            val seen = HashSet<Int>()
            return cells.keys.mapNotNull { start -> if (seen.add(start)) flood(start, seen) else null }
        }

        private fun flood(start: Int, seen: MutableSet<Int>): List<Int> {
            val component = ArrayList<Int>()
            val queue = ArrayDeque(listOf(start))
            while (queue.isNotEmpty()) {
                val cell = queue.removeFirst()
                component += cell
                for (next in neighbours(cell)) if (cells.containsKey(next) && seen.add(next)) queue.addLast(next)
            }
            return component
        }

        private fun neighbours(cell: Int): List<Int> {
            val c = cell % cols
            val r = cell / cols
            val out = ArrayList<Int>(8)
            for (dr in -1..1) for (dc in -1..1) {
                val nc = c + dc
                val nr = r + dr
                val self = dr == 0 && dc == 0
                val inside = nc in 0 until cols && nr in 0 until rows
                if (!self && inside) out += nr * cols + nc
            }
            return out
        }
    }

    // ---- the room frame ----

    /**
     * The cloud in the room's frame: [u] across (local X), [v] along (local Z), [h] above the floor,
     * and the normals turned the same way when the scan has them. The frame's yaw is the dominant
     * direction of the vertical normals — or, without normals, the yaw whose 5 cm histograms of u
     * and v are the spikiest.
     */
    private class RoomFrame(session: SpatialSession) {
        val count = session.pointCount
        val floorY = session.floorY
        val h = FloatArray(count) { session.points[it * 3 + 1] - floorY }
        private val normals: FloatArray? = session.normals?.takeIf { it.size == session.points.size }
        val yaw: Float = if (normals != null) yawFromNormals(normals, h) else yawFromHistograms(session.points, h)
        private val c = cos(Math.toRadians(yaw.toDouble())).toFloat()
        private val s = sin(Math.toRadians(yaw.toDouble())).toFloat()
        val u = FloatArray(count) { session.points[it * 3] * c - session.points[it * 3 + 2] * s }
        val v = FloatArray(count) { session.points[it * 3] * s + session.points[it * 3 + 2] * c }
        private val nu: FloatArray? = normals?.let { n -> FloatArray(count) { n[it * 3] * c - n[it * 3 + 2] * s } }
        private val nv: FloatArray? = normals?.let { n -> FloatArray(count) { n[it * 3] * s + n[it * 3 + 2] * c } }
        private val ny: FloatArray? = normals?.let { n -> FloatArray(count) { n[it * 3 + 1] } }

        fun toLocal(x: Float, z: Float): Pair<Float, Float> = (x * c - z * s) to (x * s + z * c)

        fun toWorld(lu: Float, y: Float, lv: Float): Vec3 = Vec3(lu * c + lv * s, y, -lu * s + lv * c)

        /** The world floor point [t] metres along [line]'s axis. */
        fun floor(line: Line, t: Float): FloorPoint {
            val w = if (line.axis == 0) toWorld(line.coord, floorY, t) else toWorld(t, floorY, line.coord)
            return FloorPoint(w.x, w.z)
        }

        fun across(line: Line, i: Int): Float = if (line.axis == 0) u[i] else v[i]

        /** Whether point [i] can belong to a wall facing [axis]: a vertical normal that way, or a mid height. */
        fun isWallPoint(i: Int, axis: Int): Boolean {
            val facing = if (axis == 0) nu else nv
            return if (facing != null && ny != null) {
                abs(ny[i]) < VERTICAL_NY && abs(facing[i]) > FACING_MIN
            } else {
                h[i] in BAND_LOW_M..BAND_HIGH_M
            }
        }

        fun facesDown(i: Int): Boolean = ny == null || ny[i] < -DOWN_NY

        /** Whether [box] stands against [line] over the gap [t0]..[t1] and rises above [low]. */
        fun standsBefore(box: OrientedBox, line: Line, t0: Float, t1: Float, low: Float): Boolean {
            if (box.center.y - floorY + box.size.y / 2 < low + OCCLUDER_RISE_M) return false
            val corners = box.footprint()
            var near = false
            var tLo = Float.MAX_VALUE
            var tHi = -Float.MAX_VALUE
            for (k in 0 until 4) {
                val (lu, lv) = toLocal(corners[k * 2], corners[k * 2 + 1])
                val across = if (line.axis == 0) lu else lv
                val along = if (line.axis == 0) lv else lu
                if (abs(across - line.coord) <= OCCLUDER_REACH_M) near = true
                tLo = min(tLo, along)
                tHi = max(tHi, along)
            }
            return near && min(tHi, t1) - max(tLo, t0) > 0f
        }

        private companion object {
            fun yawFromNormals(normals: FloatArray, h: FloatArray): Float {
                var sin4 = 0.0
                var cos4 = 0.0
                for (i in h.indices) {
                    val nx = normals[i * 3]
                    val ny = normals[i * 3 + 1]
                    val nz = normals[i * 3 + 2]
                    if (abs(ny) >= VERTICAL_NY) continue
                    val theta = 4 * atan2(nz.toDouble(), nx.toDouble())
                    sin4 += sin(theta)
                    cos4 += cos(theta)
                }
                if (sin4 == 0.0 && cos4 == 0.0) return 0f
                val theta0 = Math.toDegrees(atan2(sin4, cos4) / 4).toFloat()
                return fold(-theta0)
            }

            fun yawFromHistograms(points: FloatArray, h: FloatArray): Float {
                var radius = 0f
                for (i in h.indices) radius = max(radius, abs(points[i * 3]) + abs(points[i * 3 + 2]))
                val bins = ((2 * radius) / HISTOGRAM_BIN_M).toInt() + 2
                var bestYaw = 0f
                var bestScore = -1L
                for (yaw in -HALF_QUARTER until HALF_QUARTER) {
                    val score = spikiness(points, h, yaw.toFloat(), radius, bins)
                    if (score > bestScore) {
                        bestScore = score
                        bestYaw = yaw.toFloat()
                    }
                }
                return bestYaw
            }

            /** Σ bin² of the u and v histograms of the mid-height points at [yaw]: spikiest when walls align. */
            fun spikiness(points: FloatArray, h: FloatArray, yaw: Float, radius: Float, bins: Int): Long {
                val c = cos(Math.toRadians(yaw.toDouble())).toFloat()
                val s = sin(Math.toRadians(yaw.toDouble())).toFloat()
                val hu = IntArray(bins)
                val hv = IntArray(bins)
                for (i in h.indices) {
                    if (h[i] !in BAND_LOW_M..BAND_HIGH_M) continue
                    val x = points[i * 3]
                    val z = points[i * 3 + 2]
                    hu[((x * c - z * s + radius) / HISTOGRAM_BIN_M).toInt().coerceIn(0, bins - 1)]++
                    hv[((x * s + z * c + radius) / HISTOGRAM_BIN_M).toInt().coerceIn(0, bins - 1)]++
                }
                var score = 0L
                for (b in 0 until bins) score += hu[b].toLong() * hu[b] + hv[b].toLong() * hv[b]
                return score
            }

            /** [deg] folded into -45..45: the frame is the same every quarter turn. */
            fun fold(deg: Float): Float = deg - QUARTER_TURN * (deg / QUARTER_TURN).roundToInt()
        }
    }

    // ---- shared helpers ----

    private fun smoothed(bins: IntArray, radius: Int): IntArray = IntArray(bins.size) { b ->
        var sum = 0
        for (k in (b - radius).coerceAtLeast(0)..(b + radius).coerceAtMost(bins.lastIndex)) sum += bins[k]
        sum
    }

    /** Bins at least [minPeak] high that beat their neighbours, the ones within [MERGE_BINS] merged into the highest. */
    private fun localMaxima(smooth: IntArray, minPeak: Int): List<Int> {
        val peaks = smooth.indices.filter { b ->
            val left = if (b > 0) smooth[b - 1] else -1
            val right = if (b < smooth.lastIndex) smooth[b + 1] else -1
            smooth[b] >= minPeak && smooth[b] > left && smooth[b] >= right
        }
        val merged = ArrayList<Int>()
        for (b in peaks) {
            val last = merged.lastOrNull()
            if (last != null && b - last <= MERGE_BINS) {
                if (smooth[b] > smooth[last]) merged[merged.lastIndex] = b
            } else {
                merged += b
            }
        }
        return merged
    }

    private fun List<Float>.percentile(p: Float): Float =
        if (isEmpty()) 0f else this[((size - 1) * p).roundToInt().coerceIn(0, size - 1)]

    const val WALL_MIN_HEIGHT_M = 1.9f
    const val LOW_WALL_MIN_HEIGHT_M = 1.2f
    const val MIN_WALL_POINTS = 40
    const val MIN_WALL_LENGTH_M = 0.6f
    const val MAX_CLUSTERS = 12
    const val MAX_CORNER_REACH_M = 1.0f
    private const val MIN_CLOSED_WALLS = 3
    private const val PARALLEL_SIN = 0.05f
    private const val CEILING_MIN_M = 1.8f
    private const val CEILING_MAX_M = 6f
    private const val CEILING_BIN_M = 0.02f
    private const val NEAR_BINS = 5
    private const val FAR_BINS = 15
    private const val MIN_CEILING_POINTS = 30
    private const val CEILING_PEAK_RATIO = 3
    private const val DOWN_NY = 0.5f
    private const val VERTICAL_NY = 0.35f
    private const val FACING_MIN = 0.7f
    private const val BAND_LOW_M = 0.25f
    private const val BAND_HIGH_M = 2.3f
    private const val LINE_BIN_M = 0.02f
    private const val MIN_PEAK_POINTS = 20
    private const val PEAK_SHARE = 0.03f
    private const val MERGE_BINS = 3
    private const val WALL_BAND_M = 0.08f
    private const val EDGE_LO = 0.01f
    private const val EDGE_HI = 0.99f
    private const val HEIGHT_HI = 0.95f
    private const val STRETCH_M = 0.1f
    private const val MIN_STRETCH_POINTS = 2
    private const val VERTICAL_NORMAL_Y = 0.3f
    private const val PARALLEL_COS = 0.985f
    private const val WALL_PLANE_OFFSET_M = 0.15f
    private const val MIN_GAP_DENSITY = 200f
    private const val CELL_M = 0.1f
    private const val MIN_RUN_M = 0.5f
    private const val MIN_RUN_OVERLAP_M = 0.4f
    private const val MIN_GAP_M = 0.5f
    private const val DOOR_SILL_MAX_M = 0.15f
    private const val DOOR_HEAD_MIN_M = 1.8f
    private const val WINDOW_SILL_MIN_M = 0.3f
    private const val WINDOW_HEAD_MARGIN_M = 0.1f
    private const val OCCLUDER_RISE_M = 0.1f
    private const val OCCLUDER_REACH_M = 0.25f
    private const val OBJECT_LOW_M = 0.04f
    private const val OBJECT_HIGH_M = 2.0f
    private const val WALL_SKIRT_M = 0.15f
    private const val GRID_M = 0.05f
    private const val MIN_CLUSTER_CELLS = 12
    private const val MIN_CLUSTER_POINTS = 15
    private const val MIN_CLUSTER_M = 0.2f
    /**
     * A flush face sits exactly one skirt away from its wall once the skirt points are gone, so
     * the snap reach is the skirt plus a little; a 20 cm standoff must stay where it was scanned.
     */
    private const val SNAP_M = WALL_SKIRT_M + 0.03f
    private const val QUARTER_TURN = 90
    private const val YAW_STEP = 5
    private const val HALF_QUARTER = 45
    private const val HISTOGRAM_BIN_M = 0.05f
}
