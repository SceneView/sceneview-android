package io.github.sceneview.demo.demos.internal.room

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Reads a [RoomStructure] from a scan: the floor, the room's turn, the wall lines, the outline
 * they close, then each wall's openings and the figures. Same scan, same structure: nothing here
 * is random, and nothing depends on the cloud's order beyond floating-point sums over it.
 */
internal object RoomReader {
    /** The cell the full reading is done on: walls, outline and openings. */
    const val CELL_M = 0.05f

    /** The coarser cell of the live reading. */
    const val LIVE_CELL_M = 0.1f

    /** The live reading reads at most this many of the cloud's surfels. */
    const val LIVE_MAX_SURFELS = 20_000

    private const val MIN_SUPPORT = 30
    private const val LIVE_MIN_SUPPORT = 15
    private const val UNSEEN_RUN_M = 0.3f
    private const val PLANE_STEP_M = 0.1f
    private const val PLANE_RESERVE = 8_192
    private const val DOWN = -0.8f
    private const val CELL_KEY_SHIFT = 16_384
    private const val CELL_KEY_SPAN = 32_768
    private const val HALF = 0.5f
    private const val NANOS_PER_MILLI = 1e6f

    fun read(scan: RoomScan): RoomStructure {
        val normals = scan.normals
        val count = scan.positions.size / 3
        if (normals == null || normals.size < count * 3 || count < RoomLevels.MIN_SURFELS) {
            return RoomStructure.empty(RoomGap.NoDepth)
        }
        return readCloud(scan.positions, normals, scan.trail, CELL_M, MIN_SUPPORT, full = true)
    }

    /** The cheap reading: the cloud thinned to [LIVE_MAX_SURFELS], ARCore's planes added, read on [LIVE_CELL_M] cells. */
    fun live(scan: RoomScan): LiveRoom {
        val started = System.nanoTime()
        val cloudNormals = scan.normals
        val count = scan.positions.size / 3
        val usable = cloudNormals != null && cloudNormals.size >= count * 3 && count > 0
        val stride = max(1, ceil(count.toFloat() / LIVE_MAX_SURFELS).toInt())
        val kept = if (usable) (count + stride - 1) / stride else 0
        val positions = Floats(kept * 3 + PLANE_RESERVE)
        val normals = Floats(kept * 3 + PLANE_RESERVE)
        if (usable) positions.thinned(scan.positions, stride, kept)
        if (usable) normals.thinned(cloudNormals!!, stride, kept)
        planeSamples(scan, positions, normals)
        val p = positions.toArray()
        val n = normals.toArray()
        val structure = if (p.size / 3 < RoomLevels.MIN_SURFELS) {
            RoomStructure.empty(RoomGap.NoDepth)
        } else {
            readCloud(p, n, scan.trail, LIVE_CELL_M, LIVE_MIN_SUPPORT, full = false)
        }
        val walls = structure.walls.filter { !it.deduced }.map {
            LiveRoomWall(it.ax, it.az, it.bx, it.bz, it.height, it.confidence)
        }
        val outline = FloatArray(structure.walls.size * 2)
        for ((k, w) in structure.walls.withIndex()) {
            outline[k * 2] = w.ax
            outline[k * 2 + 1] = w.az
        }
        val floorY = structure.floorY.takeIf { structure.gaps.firstOrNull() != RoomGap.NoDepth }
        val millis = (System.nanoTime() - started) / NANOS_PER_MILLI
        return LiveRoom(walls, outline, structure.area?.value, floorY, millis)
    }

    /** ARCore's wall and floor planes as surfels on a [PLANE_STEP_M] grid, walls facing the camera's path. */
    @Suppress("NestedBlockDepth", "CyclomaticComplexMethod")
    private fun planeSamples(scan: RoomScan, positions: Floats, normals: Floats) {
        var cx = 0.0
        var cz = 0.0
        val steps = scan.trail.size / 3
        for (k in 0 until steps) {
            cx += scan.trail[k * 3]
            cz += scan.trail[k * 3 + 2]
        }
        if (steps > 0) {
            cx /= steps
            cz /= steps
        }
        for (plane in scan.planes) {
            val poly = plane.polygon
            val m = poly.size / 3
            if (m < 3) continue
            when (plane.kind) {
                RoomPlaneKind.Wall -> {
                    // Newell's normal, levelled; turned towards where the camera went.
                    var nx = 0.0
                    var nz = 0.0
                    var mx = 0.0
                    var mz = 0.0
                    for (i in 0 until m) {
                        val j = (i + 1) % m
                        nx += (poly[i * 3 + 1] - poly[j * 3 + 1]).toDouble() * (poly[i * 3 + 2] + poly[j * 3 + 2])
                        nz += (poly[i * 3] - poly[j * 3]).toDouble() * (poly[i * 3 + 1] + poly[j * 3 + 1])
                        mx += poly[i * 3]
                        mz += poly[i * 3 + 2]
                    }
                    val len = hypot(nx, nz)
                    if (len < 1e-6) continue
                    var ux = (nx / len).toFloat()
                    var uz = (nz / len).toFloat()
                    if (steps > 0 && (cx - mx / m) * ux + (cz - mz / m) * uz < 0) {
                        ux = -ux
                        uz = -uz
                    }
                    // In the plane: along it (−uz, ux) and up.
                    val ta = FloatArray(m) { poly[it * 3] * -uz + poly[it * 3 + 2] * ux }
                    val ya = FloatArray(m) { poly[it * 3 + 1] }
                    val o = (mx / m * ux + mz / m * uz).toFloat()
                    fill(ta, ya) { t, y ->
                        positions += o * ux + t * -uz
                        positions += y
                        positions += o * uz + t * ux
                        normals += ux
                        normals += 0f
                        normals += uz
                    }
                }
                RoomPlaneKind.Floor -> {
                    val xa = FloatArray(m) { poly[it * 3] }
                    val za = FloatArray(m) { poly[it * 3 + 2] }
                    val y = (0 until m).map { poly[it * 3 + 1] }.average().toFloat()
                    fill(xa, za) { x, z ->
                        positions += x
                        positions += y
                        positions += z
                        normals += 0f
                        normals += 1f
                        normals += 0f
                    }
                }
                else -> Unit
            }
        }
    }

    /** Calls [emit] on a [PLANE_STEP_M] grid inside the polygon ([a], [b]). */
    private inline fun fill(a: FloatArray, b: FloatArray, emit: (Float, Float) -> Unit) {
        val a0 = a.min()
        val a1 = a.max()
        val b0 = b.min()
        val b1 = b.max()
        var p = floor(a0 / PLANE_STEP_M) * PLANE_STEP_M + PLANE_STEP_M / 2
        while (p < a1) {
            var q = floor(b0 / PLANE_STEP_M) * PLANE_STEP_M + PLANE_STEP_M / 2
            while (q < b1) {
                if (inside(a, b, p, q)) emit(p, q)
                q += PLANE_STEP_M
            }
            p += PLANE_STEP_M
        }
    }

    private fun inside(a: FloatArray, b: FloatArray, p: Float, q: Float): Boolean {
        var hit = false
        var j = a.size - 1
        for (i in a.indices) {
            if ((b[i] > q) != (b[j] > q) && p < (a[j] - a[i]) * (q - b[i]) / (b[j] - b[i]) + a[i]) hit = !hit
            j = i
        }
        return hit
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount", "LongParameterList")
    private fun readCloud(
        positions: FloatArray,
        normals: FloatArray,
        trail: FloatArray,
        cell: Float,
        minSupport: Int,
        full: Boolean,
    ): RoomStructure {
        val floor = RoomLevels.floorOf(positions, normals, trail) ?: return RoomStructure.empty(RoomGap.NoWalls)
        val yaw = RoomLevels.yawOf(positions, normals, floor.y)
        val c = cos(yaw)
        val s = sin(yaw)
        val cloud = FrameCloud.of(positions, normals, floor.y, yaw)
        val steps = trail.size / 3
        val trailU = FloatArray(steps) { c * trail[it * 3] + s * trail[it * 3 + 2] }
        val trailV = FloatArray(steps) { -s * trail[it * 3] + c * trail[it * 3 + 2] }
        // The ceiling's surfels, each over a cell of the plan.
        val ceilingH = FloatArray(cloud.count)
        val ceilingCell = IntArray(cloud.count)
        var ceilings = 0
        if (full) {
            for (i in 0 until cloud.count) {
                val h = cloud.h[i]
                if (cloud.ny[i] > DOWN || h < RoomLevels.MIN_CEILING_M || h > RoomLevels.CEILING_MAX_M) continue
                ceilingH[ceilings] = h
                ceilingCell[ceilings] = cellKey(cloud.u[i], cloud.v[i], cell)
                ceilings++
            }
        }
        fun ceiling(inside: (Float, Float) -> Boolean): RoomFigure? = if (!full) {
            null
        } else {
            RoomLevels.ceilingOf(ceilingH, ceilingCell, ceilings, cell * cell, floor) { key ->
                inside(keyU(key, cell), keyV(key, cell))
            }
        }
        val lines = WallLines.of(cloud, cell, minSupport)
        if (lines.isEmpty()) {
            return RoomStructure.empty(RoomGap.NoWalls, floor.y, floor.thickness, ceiling { _, _ -> true })
        }
        WallLines.connect(lines)
        val members = BooleanArray(cloud.count)
        for (line in lines) for (i in line.members) members[i] = true
        val outline = RoomOutline.of(cloud, lines, trailU, trailV, floor.thickness, cell, members)
            ?: return RoomStructure.empty(RoomGap.NoWalls, floor.y, floor.thickness)
        val raster = outline.raster
        val height = if (full) {
            ceiling { u, v ->
                val k = raster.index(u, v)
                k >= 0 && outline.region[k]
            }
        } else {
            null
        }
        val n = outline.sides.size
        val seenTops = outline.sides.mapNotNull { it.line?.top }.sorted()
        val wallHeight = height?.value ?: if (seenTops.isEmpty()) 0f else seenTops[seenTops.size / 2]
        class Read(
            val length: Float,
            val share: Float,
            val unseen: List<ClosedFloatingPointRange<Float>>,
            val openings: List<RoomOpening>,
        )
        val reads = (0 until n).map { i ->
            val next = (i + 1) % n
            val side = outline.sides[i]
            val au = outline.u[i]
            val av = outline.v[i]
            val bu = outline.u[next]
            val bv = outline.v[next]
            val length = hypot(bu - au, bv - av)
            val line = side.line ?: return@map Read(length, 0f, listOf(0f..length), emptyList())
            val ta = line.tOf(au, av)
            val tb = line.tOf(bu, bv)
            // Openings: their place counted from the wall's start, whichever way the line runs.
            val found = if (full) WallOpenings.of(cloud, line, min(ta, tb), max(ta, tb), wallHeight) else emptyList()
            val openings = found.map { o ->
                val from = if (ta >= tb) ta - o.t1 else o.t0 - ta
                RoomOpening(o.kind, from, o.t1 - o.t0, o.bottom, o.top - o.bottom, o.confidence)
            }
            val samples = max(1, (length / CELL_M + HALF).toInt())
            var hit = 0
            val runs = ArrayList<ClosedFloatingPointRange<Float>>()
            var runFrom = -1
            for (k in 0..samples) {
                val at = (k + HALF) * length / samples
                var seen = false
                if (k < samples) {
                    val t = ta + (tb - ta) * (k + HALF) / samples
                    seen = line.seenAt(t) || openings.any { at >= it.from && at <= it.from + it.width }
                    if (seen) hit++
                    if (!seen && runFrom < 0) runFrom = k
                }
                if ((seen || k == samples) && runFrom >= 0) {
                    if ((k - runFrom) * length / samples >= UNSEEN_RUN_M) {
                        runs += (runFrom * length / samples)..min(length, k * length / samples)
                    }
                    runFrom = -1
                }
            }
            Read(length, hit.toFloat() / samples, runs, openings)
        }
        val seen = BooleanArray(n) { outline.sides[it].line != null && reads[it].share >= RoomStructure.MIN_SEEN_SHARE }
        if (seen.count { it } < if (full) RoomStructure.MIN_SEEN_WALLS else 1) {
            // An outline (almost) no wall was seen along is only the floor's extent: not drawn.
            return RoomStructure.empty(RoomGap.NoWalls, floor.y, floor.thickness, height)
        }
        // No figure is given tighter than the thinnest layer a cloud is read to.
        val spread = FloatArray(n) { if (seen[it]) max(outline.sides[it].line!!.spread, RoomLevels.MIN_SPREAD_M) else 0f }
        val walls = (0 until n).map { i ->
            val next = (i + 1) % n
            val before = (i + n - 1) % n
            val side = outline.sides[i]
            val read = reads[i]
            RoomWall(
                ax = c * outline.u[i] - s * outline.v[i],
                az = s * outline.u[i] + c * outline.v[i],
                bx = c * outline.u[next] - s * outline.v[next],
                bz = s * outline.u[next] + c * outline.v[next],
                nx = c * side.nu - s * side.nv,
                nz = s * side.nu + c * side.nv,
                // A wall runs from one neighbour to the other: it is measured when all three were seen.
                length = RoomFigure.of(
                    read.length, spread[before] + spread[next], !seen[i] || !seen[before] || !seen[next],
                ),
                seenShare = read.share,
                seen = seen[i],
                deduced = side.line == null,
                unseen = if (seen[i]) read.unseen else listOf(0f..read.length),
                height = side.line?.top ?: wallHeight,
                openings = read.openings,
            )
        }
        val rough = seen.any { !it }
        val measured = seen.count { it } >= RoomStructure.MIN_SEEN_WALLS
        val area = abs(RoomOutline.signedArea(outline.u, outline.v))
        var areaSpread = 0f
        for (i in 0 until n) areaSpread += reads[i].length * spread[i]
        val perimeter = RoomFigure.of(
            walls.sumOf { it.length.value.toDouble() }.toFloat(),
            walls.sumOf { it.length.plusMinus.toDouble() }.toFloat(),
            rough,
        )
        val areaFigure = RoomFigure.of(area, areaSpread, rough)
        val alongU = extentOf(outline.u, seen, spread)
        val alongV = extentOf(outline.v, seen, spread)
        val turned = alongV.value > alongU.value
        val total = reads.sumOf { it.length.toDouble() }.toFloat()
        var seenLength = 0f
        for (i in 0 until n) if (outline.sides[i].line != null) seenLength += reads[i].length * reads[i].share
        return RoomStructure(
            walls = walls,
            floorY = floor.y,
            floorThickness = floor.thickness,
            yaw = yaw,
            ceilingHeight = height,
            area = areaFigure.takeIf { measured },
            perimeter = perimeter.takeIf { measured },
            volume = volumeOf(areaFigure, height).takeIf { measured },
            length = (if (turned) alongV else alongU).takeIf { measured },
            width = (if (turned) alongU else alongV).takeIf { measured },
            confidence = if (total > 0f) seenLength / total else 0f,
            gaps = listOfNotNull(
                RoomGap.MissingWall.takeIf { rough },
                RoomGap.Ceiling.takeIf { height == null },
            ),
        )
    }

    /** The volume under the ceiling: the area times the height, each one's scatter carried. */
    fun volumeOf(area: RoomFigure?, height: RoomFigure?): RoomFigure? {
        if (area == null || height == null) return null
        return RoomFigure.of(
            area.value * height.value,
            area.value * height.plusMinus + height.value * area.plusMinus,
            area.estimate || height.estimate,
        )
    }

    /**
     * The outline's extent along one axis of the room's frame ([coords] per corner): measured when
     * every side at its two ends was seen, its scatter the widest of theirs at each end.
     */
    private fun extentOf(coords: FloatArray, seen: BooleanArray, spread: FloatArray): RoomFigure {
        val n = coords.size
        val lo = coords.min()
        val hi = coords.max()
        var estimate = false
        var spreadLo = 0f
        var spreadHi = 0f
        fun at(i: Int, edge: Float) = abs(coords[i] - edge) < EDGE_M
        // The sides that lie on an extreme hold it; where none does — a corner — the two that meet there.
        val lyingLo = (0 until n).any { at(it, lo) && at((it + 1) % n, lo) }
        val lyingHi = (0 until n).any { at(it, hi) && at((it + 1) % n, hi) }
        for (i in 0 until n) {
            val next = (i + 1) % n
            val atLo = if (lyingLo) at(i, lo) && at(next, lo) else at(i, lo) || at(next, lo)
            val atHi = if (lyingHi) at(i, hi) && at(next, hi) else at(i, hi) || at(next, hi)
            if (!atLo && !atHi) continue
            if (!seen[i]) estimate = true
            if (atLo) spreadLo = max(spreadLo, spread[i])
            if (atHi) spreadHi = max(spreadHi, spread[i])
        }
        return RoomFigure.of(hi - lo, spreadLo + spreadHi, estimate)
    }

    private fun cellKey(u: Float, v: Float, cell: Float): Int {
        val i = (floor(u / cell).toInt() + CELL_KEY_SHIFT).coerceIn(0, CELL_KEY_SPAN - 1)
        val j = (floor(v / cell).toInt() + CELL_KEY_SHIFT).coerceIn(0, CELL_KEY_SPAN - 1)
        return i * CELL_KEY_SPAN + j
    }

    private fun keyU(key: Int, cell: Float): Float = (key / CELL_KEY_SPAN - CELL_KEY_SHIFT + HALF) * cell
    private fun keyV(key: Int, cell: Float): Float = (key % CELL_KEY_SPAN - CELL_KEY_SHIFT + HALF) * cell

    private const val EDGE_M = 0.01f
}

/** A growing float array: the live reading's cloud, unboxed. */
internal class Floats(capacity: Int) {
    private var data = FloatArray(max(capacity, 1))
    var size = 0
        private set

    operator fun plusAssign(value: Float) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    fun toArray(): FloatArray = data.copyOf(size)

    /** Appends every [stride]th of [source]'s `x, y, z` triples, [kept] of them. */
    fun thinned(source: FloatArray, stride: Int, kept: Int) {
        if (size + kept * 3 > data.size) data = data.copyOf(size + kept * 3)
        for (k in 0 until kept) {
            val from = k * stride * 3
            data[size++] = source[from]
            data[size++] = source[from + 1]
            data[size++] = source[from + 2]
        }
    }
}

