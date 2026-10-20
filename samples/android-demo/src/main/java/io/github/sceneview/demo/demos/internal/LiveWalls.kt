package io.github.sceneview.demo.demos.internal

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * The walls 3D Scan shows while it scans (Room and Home): each wall it recognises stands on the
 * floor as a clean translucent quad, the way RoomPlan outlines a room as it is scanned. One wall
 * seen twice is one wall; a wall grows as more of it is seen, and never shrinks or leaves.
 */

/**
 * One wall the scan recognised: its floor line from ([ax], [az]) to ([bx], [bz]), and when it was
 * first recognised ([bornAt], seconds of the trace). [id] stays the same as the wall grows.
 */
data class LiveWall(
    val id: Int,
    val ax: Float,
    val az: Float,
    val bx: Float,
    val bz: Float,
    val bornAt: Float,
    /** How sure the wall is, `0..1`: a short piece is a guess, [PlaneWallSource.CONFIDENT_M] of it is a wall. */
    val confidence: Float = 1f,
) {
    val length: Float get() = hypot(bx - ax, bz - az)
}

/**
 * What the scan recognised of the room at one instant: its [walls], standing on the floor at
 * [floorY], [height] metres tall — the ceiling's height when it was seen ([ceilingSeen]), the top
 * of the highest wall seen otherwise.
 */
data class LiveWallSnapshot(
    val walls: List<LiveWall>,
    val floorY: Float,
    val height: Float,
    val ceilingSeen: Boolean = false,
) {
    companion object {
        val Empty = LiveWallSnapshot(emptyList(), 0f, 0f)
    }
}

/**
 * A plane as any AR source gives it, with nothing of 3D Scan's trace: ARCore's [id] for it, which
 * way it faces ([kind]), and its boundary as a **world-space** polygon, flat `[x0,y0,z0, …]` — an
 * ARCore `Plane`'s `polygon` put through its `centerPose`. What Measure (#4732) and a scan module
 * hand [PlaneWallSource].
 */
class LivePlane(val id: Int, val kind: Kind, val polygon: FloatArray) {
    enum class Kind { Floor, Ceiling, Wall, Other }

    val vertexCount: Int get() = polygon.size / 3
}

/**
 * Where the live walls come from. Today, ARCore's vertical planes ([PlaneWallSource]); a source
 * that builds the room's structure from the dense map can replace it without the view changing.
 */
interface LiveWallSource {
    /**
     * The walls recognised from [planes], over a floor at [floorY], at [time] seconds. Called each
     * frame: cheap when nothing changed, and the same snapshot instance while the walls do not.
     */
    fun update(planes: List<LivePlane>, floorY: Float, time: Float): LiveWallSnapshot

    /** Forgets every wall: a new capture. */
    fun reset()
}

/**
 * Walls from ARCore's vertical planes. ARCore cuts one wall into several planes (a picture, a door
 * frame, a wall seen from two places) and keeps planes of furniture: pieces on one line are joined
 * into one wall, pieces shorter than [MIN_WALL_M] are not walls, and the walls are squared to the
 * room's main axis and joined at the corners they nearly meet at.
 */
class PlaneWallSource : LiveWallSource {
    private val tracks = ArrayList<Track>()
    private var nextId = 0
    private var key = LongArray(0)
    private var last = LiveWallSnapshot.Empty

    override fun update(planes: List<LivePlane>, floorY: Float, time: Float): LiveWallSnapshot {
        // A plane is the same while its id and outline are: ARCore hands new objects every frame.
        val newKey = LongArray(planes.size) { i ->
            (planes[i].id.toLong() shl Int.SIZE_BITS) or (planes[i].polygon.contentHashCode().toLong() and MASK)
        }
        if (newKey.contentEquals(key) && last !== LiveWallSnapshot.Empty) return last
        key = newKey
        merge(clusters(pieces(planes, floorY)), time)
        val ceiling = ceilingOf(planes, floorY)
        val top = tracks.maxOfOrNull { it.top } ?: (floorY + DEFAULT_HEIGHT_M)
        val height = (ceiling ?: top) - floorY
        val next = LiveWallSnapshot(
            walls = shaped(tracks.filter { it.length >= MIN_WALL_M }),
            floorY = floorY,
            height = height.coerceIn(MIN_HEIGHT_M, MAX_HEIGHT_M),
            ceilingSeen = ceiling != null,
        )
        // A plane refit that moves no wall keeps the snapshot: whoever draws it has nothing to redo.
        if (next != last) last = next
        return last
    }

    override fun reset() {
        tracks.clear()
        nextId = 0
        key = LongArray(0)
        last = LiveWallSnapshot.Empty
    }

    /**
     * Folds this update's [found] walls into the walls already known. Where a wall stands and which
     * way it runs is what ARCore's planes say now — a refit corrects its first rough fit — and what
     * was seen of it before keeps only its ends, so it never shrinks.
     */
    private fun merge(found: List<Segment>, time: Float) {
        val taken = HashSet<Track>()
        for (segment in found.sortedByDescending { it.length }) {
            val matches = tracks.filter { it !in taken && sameWall(it.segment, segment) }
            if (matches.isEmpty()) {
                tracks += Track(nextId++, time, segment).also { taken += it }
                continue
            }
            // The oldest keeps its name; the others were the same wall seen in pieces.
            val keep = matches.minBy { it.id }
            var grown = segment
            matches.forEach { grown = grown.spanning(it.segment) }
            keep.segment = grown
            keep.top = max(grown.top, matches.maxOf { it.top })
            tracks.removeAll { it in matches && it !== keep }
            taken += keep
        }
    }

    /** The walls as drawn: squared to the room's main axis, and joined at their corners. */
    private fun shaped(kept: List<Track>): List<LiveWall> {
        if (kept.isEmpty()) return emptyList()
        val axis = kept.maxBy { it.length }.segment.angle
        val squared = kept.map { it.segment.rotatedTo(WallMesh.square(it.segment.angle, axis)) }
        val ends = squared.map { floatArrayOf(it.ax, it.az, it.bx, it.bz) }
        for (i in squared.indices) {
            for (j in i + 1 until squared.size) joinCorner(squared[i], squared[j], ends[i], ends[j])
        }
        return kept.mapIndexed { i, track ->
            val e = ends[i]
            LiveWall(track.id, e[0], e[1], e[2], e[3], track.bornAt, (track.length / CONFIDENT_M).coerceAtMost(1f))
        }
    }

    private class Track(val id: Int, val bornAt: Float, var segment: Segment) {
        var top: Float = segment.top
        val length: Float get() = segment.length
    }

    companion object {
        /** Shorter than this, a vertical plane is furniture, a door frame, a box: not a wall. */
        const val MIN_WALL_M = 0.5f

        /** A wall seen along this much, in metres, is sure: [LiveWall.confidence] is 1. */
        const val CONFIDENT_M = 1.5f

        /** A vertical piece shorter than this along the floor is left out altogether. */
        const val MIN_PIECE_M = 0.25f

        /** Two pieces this close in direction can be one wall. */
        const val SAME_WALL_DEGREES = 12f

        /** Two pieces this far apart across their line can be one wall: a wall is not a mirror. */
        const val SAME_WALL_OFFSET_M = 0.2f

        /** Two pieces on one line this far apart along it are one wall with a gap not yet seen. */
        const val SAME_WALL_GAP_M = 0.6f

        /** Two walls whose lines cross this close to their ends meet there: a corner. */
        const val CORNER_REACH_M = 0.7f

        /** How far from square two walls may be and still be joined at a corner. */
        const val CORNER_DEGREES = 20f

        const val DEFAULT_HEIGHT_M = 2.4f
        const val MIN_HEIGHT_M = 0.6f
        const val MAX_HEIGHT_M = 4f

        /** A ceiling lower than this over the floor is a table seen from below, not the ceiling. */
        const val MIN_CEILING_M = 1.8f

        /**
         * The walls in [planes], each counted once, with no tracking — one call, one answer: a
         * finished recording's planes, which no live [PlaneWallSource] followed. A live scan reads
         * its source's snapshot instead, the count the HUD shows.
         */
        fun wallCount(planes: List<LivePlane>, floorY: Float): Int =
            clusters(pieces(planes, floorY)).count { it.length >= MIN_WALL_M }

        /** Each vertical plane as a floor segment: its line, its extent along it, its top. */
        internal fun pieces(planes: List<LivePlane>, floorY: Float): List<Segment> = planes.mapNotNull { plane ->
            if (plane.kind != LivePlane.Kind.Wall || plane.vertexCount < 3) return@mapNotNull null
            Segment.ofPolygon(plane.polygon)?.takeIf { it.length >= MIN_PIECE_M && it.top > floorY }
        }

        /** Pieces on one line joined into walls (single linkage: A–B and B–C make one wall). */
        internal fun clusters(pieces: List<Segment>): List<Segment> {
            val parent = IntArray(pieces.size) { it }
            fun root(i: Int): Int {
                var r = i
                while (parent[r] != r) r = parent[r]
                return r
            }
            for (i in pieces.indices) {
                for (j in i + 1 until pieces.size) {
                    if (sameWall(pieces[i], pieces[j])) parent[root(i)] = root(j)
                }
            }
            return pieces.indices.groupBy { root(it) }.values.map { group ->
                group.map { pieces[it] }.reduce { a, b -> a.union(b) }
            }
        }

        internal fun sameWall(a: Segment, b: Segment): Boolean {
            if (abs(degrees(WallMesh.wrapHalfTurn(a.angle - b.angle))) > SAME_WALL_DEGREES) return false
            if (a.distanceAcross(b.midX, b.midZ) > SAME_WALL_OFFSET_M) return false
            if (b.distanceAcross(a.midX, a.midZ) > SAME_WALL_OFFSET_M) return false
            return a.gapAlong(b) <= SAME_WALL_GAP_M
        }

        /** The ceiling's height, from the lowest downward-facing plane high enough to be one. */
        private fun ceilingOf(planes: List<LivePlane>, floorY: Float): Float? = planes
            .filter { it.kind == LivePlane.Kind.Ceiling && it.vertexCount >= 3 }
            .map { plane -> (0 until plane.vertexCount).map { plane.polygon[it * 3 + 1] }.average().toFloat() }
            .filter { it - floorY >= MIN_CEILING_M }
            .minOrNull()

        /**
         * Joins [a] and [b] at the point their lines cross when it lies within [CORNER_REACH_M] of
         * an end of each — extended to it or trimmed back to it — and they are near square.
         */
        private fun joinCorner(a: Segment, b: Segment, ea: FloatArray, eb: FloatArray) {
            val off = abs(degrees(WallMesh.wrapHalfTurn(a.angle - b.angle)))
            if (abs(off - RIGHT_ANGLE) > CORNER_DEGREES) return
            val cross = intersection(ea, eb) ?: return
            val endA = nearestEnd(ea, cross) ?: return
            val endB = nearestEnd(eb, cross) ?: return
            ea[endA * 2] = cross.first
            ea[endA * 2 + 1] = cross.second
            eb[endB * 2] = cross.first
            eb[endB * 2 + 1] = cross.second
        }

        /** Which end of [e] (0: a, 1: b) lies within reach of [p], `null` for neither. */
        private fun nearestEnd(e: FloatArray, p: Pair<Float, Float>): Int? {
            val da = hypot(e[0] - p.first, e[1] - p.second)
            val db = hypot(e[2] - p.first, e[3] - p.second)
            val length = hypot(e[2] - e[0], e[3] - e[1])
            val end = if (da <= db) 0 else 1
            val reach = min(da, db)
            // Trimming more than half a wall away is not a corner, it is another wall crossing it.
            return end.takeIf { reach <= CORNER_REACH_M && reach < length / 2f }
        }

        private fun intersection(a: FloatArray, b: FloatArray): Pair<Float, Float>? {
            val rx = a[2] - a[0]
            val rz = a[3] - a[1]
            val sx = b[2] - b[0]
            val sz = b[3] - b[1]
            val denominator = rx * sz - rz * sx
            if (abs(denominator) < EPSILON) return null
            val t = ((b[0] - a[0]) * sz - (b[1] - a[1]) * sx) / denominator
            return (a[0] + t * rx) to (a[1] + t * rz)
        }

        private const val RIGHT_ANGLE = 90f
        private const val MASK = 0xffffffffL
        private const val EPSILON = 1e-6f
    }
}

/**
 * A wall's floor line: through ([midX], [midZ]) along [angle] (radians on the floor, `[0, π)`),
 * from [from] to [to] metres along it, [top] the highest point seen on it. [weight] is how much of
 * it was seen, which weighs its say in a joined wall's direction.
 */
class Segment(
    val angle: Float,
    val midX: Float,
    val midZ: Float,
    val from: Float,
    val to: Float,
    val top: Float,
    val weight: Float = to - from,
) {
    private val dx = cos(angle)
    private val dz = sin(angle)

    val length: Float get() = to - from
    val ax: Float get() = midX + dx * from
    val az: Float get() = midZ + dz * from
    val bx: Float get() = midX + dx * to
    val bz: Float get() = midZ + dz * to

    /** How far ([x], [z]) lies from this line, across it. */
    fun distanceAcross(x: Float, z: Float): Float = abs(-dz * (x - midX) + dx * (z - midZ))

    private fun along(x: Float, z: Float): Float = dx * (x - midX) + dz * (z - midZ)

    /** The gap between this and [other] along this line, `0` or less where they overlap. */
    fun gapAlong(other: Segment): Float {
        val s0 = along(other.ax, other.az)
        val s1 = along(other.bx, other.bz)
        return max(min(s0, s1) - to, from - max(s0, s1))
    }

    /**
     * This and [other] as one wall: the direction they agree on, weighted by how much of each was
     * seen, through their weighted middle, spanning both.
     */
    fun union(other: Segment): Segment {
        val w = weight + other.weight
        // Directions have no sign: averaged as doubled angles.
        val c = cos(angle * 2) * weight + cos(other.angle * 2) * other.weight
        val s = sin(angle * 2) * weight + sin(other.angle * 2) * other.weight
        val joined = positive(atan2(s, c) / 2f)
        val mx = (midX * weight + other.midX * other.weight) / w
        val mz = (midZ * weight + other.midZ * other.weight) / w
        val ux = cos(joined)
        val uz = sin(joined)
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for ((x, z) in listOf(ax to az, bx to bz, other.ax to other.az, other.bx to other.bz)) {
            val t = ux * (x - mx) + uz * (z - mz)
            lo = min(lo, t)
            hi = max(hi, t)
        }
        return Segment(joined, mx, mz, lo, hi, max(top, other.top), w)
    }

    /**
     * This line as it is — its direction, its position, its weight — stretched to reach [other]'s
     * ends: what a wall seen before keeps when ARCore refits it. Unlike [union], [other] has no say
     * in where the line runs, so a wall seen again is not merged with itself.
     */
    fun spanning(other: Segment): Segment {
        var lo = from
        var hi = to
        for ((x, z) in listOf(other.ax to other.az, other.bx to other.bz)) {
            val t = along(x, z)
            lo = min(lo, t)
            hi = max(hi, t)
        }
        return Segment(angle, midX, midZ, lo, hi, max(top, other.top), weight)
    }

    /** The same span turned onto [angle] round its centre. */
    fun rotatedTo(angle: Float): Segment {
        val centre = (from + to) / 2f
        val cx = midX + dx * centre
        val cz = midZ + dz * centre
        val half = length / 2f
        return Segment(angle, cx, cz, -half, half, top, weight)
    }

    companion object {
        /**
         * A vertical plane's [polygon] (world xyz, flat) seen from above: the line its points lie
         * along (their principal axis on the floor) and their extent on it. `null` when the
         * polygon is a point.
         */
        fun ofPolygon(polygon: FloatArray): Segment? {
            val n = polygon.size / 3
            if (n < 2) return null
            var mx = 0f
            var mz = 0f
            var top = -Float.MAX_VALUE
            for (i in 0 until n) {
                mx += polygon[i * 3]
                mz += polygon[i * 3 + 2]
                top = max(top, polygon[i * 3 + 1])
            }
            mx /= n
            mz /= n
            var sxx = 0f
            var sxz = 0f
            var szz = 0f
            for (i in 0 until n) {
                val x = polygon[i * 3] - mx
                val z = polygon[i * 3 + 2] - mz
                sxx += x * x
                sxz += x * z
                szz += z * z
            }
            if (sxx + szz < TINY) return null
            val angle = positive(atan2(2f * sxz, sxx - szz) / 2f)
            val ux = cos(angle)
            val uz = sin(angle)
            var lo = Float.MAX_VALUE
            var hi = -Float.MAX_VALUE
            for (i in 0 until n) {
                val t = ux * (polygon[i * 3] - mx) + uz * (polygon[i * 3 + 2] - mz)
                lo = min(lo, t)
                hi = max(hi, t)
            }
            return Segment(angle, mx, mz, lo, hi, top)
        }

        /** A line's direction in `[0, π)`: taken before projecting onto it, so the span matches. */
        private fun positive(angle: Float): Float {
            val a = WallMesh.wrapHalfTurn(angle)
            return if (a < 0f) a + PI.toFloat() else a
        }

        private const val TINY = 1e-8f
    }
}

/**
 * The walls as they are drawn: each eases towards what was recognised, so a new wall rises from
 * the floor and a growing one stretches, calmly — no wall jumps or flashes when ARCore refits a plane.
 */
class LiveWallMotion(
    /** How long a wall takes to rise, in seconds, to within 5 % of its height. */
    private val riseSeconds: Float = RISE_SECONDS,
    /** How long a wall's ends take to follow a refit, to within 5 %. */
    private val stretchSeconds: Float = STRETCH_SECONDS,
) {
    /** One wall as drawn now: its floor line, and how far it has risen, `0` to `1`. */
    class Shown(val id: Int, var ax: Float, var az: Float, var bx: Float, var bz: Float, var rise: Float)

    val shown = ArrayList<Shown>()

    /** The height drawn, eased like the walls. */
    var height = 0f
        private set

    var floorY = 0f
        private set

    /**
     * Steps the walls drawn [dt] seconds towards [target]. Returns `true` when anything drawn
     * moved — when the meshes need rebuilding.
     */
    fun step(target: LiveWallSnapshot, dt: Float): Boolean {
        var moved = false
        val ease = settle(dt, stretchSeconds)
        val rise = settle(dt, riseSeconds)
        val ids = target.walls.mapTo(HashSet()) { it.id }
        // A wall merged into another goes with no fade: the one it joined grows over it.
        if (shown.removeAll { it.id !in ids }) moved = true
        for (wall in target.walls) {
            val s = shown.firstOrNull { it.id == wall.id }
            if (s == null) {
                // A new wall stands on its whole floor line at once and rises.
                shown += Shown(wall.id, wall.ax, wall.az, wall.bx, wall.bz, 0f)
                moved = true
                continue
            }
            moved = s.approach(wall, ease) or moved
            if (s.rise < 1f) {
                s.rise = if (1f - s.rise < DONE) 1f else s.rise + (1f - s.rise) * rise
                moved = true
            }
        }
        if (floorY != target.floorY || height == 0f) {
            if (height == 0f || abs(floorY - target.floorY) > JUMP_M) floorY = target.floorY else {
                floorY += (target.floorY - floorY) * ease
                if (abs(floorY - target.floorY) < DONE) floorY = target.floorY
            }
            moved = true
        }
        if (height != target.height) {
            height = if (height == 0f || abs(height - target.height) < DONE) {
                target.height
            } else {
                height + (target.height - height) * ease
            }
            moved = true
        }
        return moved
    }

    fun reset() {
        shown.clear()
        height = 0f
        floorY = 0f
    }

    private fun Shown.approach(wall: LiveWall, ease: Float): Boolean {
        val far = abs(ax - wall.ax) + abs(az - wall.az) + abs(bx - wall.bx) + abs(bz - wall.bz)
        if (far == 0f) return false
        if (far < DONE) {
            ax = wall.ax
            az = wall.az
            bx = wall.bx
            bz = wall.bz
        } else {
            ax += (wall.ax - ax) * ease
            az += (wall.az - az) * ease
            bx += (wall.bx - bx) * ease
            bz += (wall.bz - bz) * ease
        }
        return true
    }

    companion object {
        /** DESIGN.md `duration-long`: a wall rises as the eye can follow it, not as a flash. */
        const val RISE_SECONDS = 0.7f
        const val STRETCH_SECONDS = 0.35f

        /** Closer than this (metres, or of a rise), the wall is where it is going. */
        private const val DONE = 1e-3f

        /** A floor that moves more than this was refound, not refined: it is taken at once. */
        private const val JUMP_M = 0.3f

        /** ln(20): the step that brings a value within 5 % of its target in the given time. */
        private const val LN_20 = 2.9957323f

        private fun settle(dt: Float, seconds: Float): Float =
            if (seconds <= 0f) 1f else 1f - exp(-dt.coerceIn(0f, MAX_STEP_S) * LN_20 / seconds)

        /** A stalled frame does not make the walls jump: at most this much time per step. */
        private const val MAX_STEP_S = 0.1f
    }
}

/**
 * The edges of the floor seen so far ([extent], a polygon on the floor) that no recognised wall
 * stands on: where the room still opens onto what was not scanned. Each is a pair of floor points.
 */
fun openFloorEdges(extent: List<Pair<Float, Float>>, walls: List<LiveWall>): List<FloatArray> {
    if (extent.size < 3) return emptyList()
    val open = ArrayList<FloatArray>()
    for (i in extent.indices) {
        val (ax, az) = extent[i]
        val (bx, bz) = extent[(i + 1) % extent.size]
        val length = hypot(bx - ax, bz - az)
        if (length < MIN_OPEN_EDGE_M) continue
        // Cut into steps, so the part of a long edge a wall covers is not drawn as open.
        val steps = max(1, (length / OPEN_STEP_M).toInt())
        var runStart = -1
        for (k in 0..steps) {
            val t = k.toFloat() / steps
            val x = ax + (bx - ax) * t
            val z = az + (bz - az) * t
            val covered = walls.any { distanceToSegment(x, z, it) <= WALL_COVERS_M }
            val last = k == steps
            if (!covered && runStart < 0) runStart = k
            if (runStart >= 0 && (covered || last)) {
                val end = if (covered) k - 1 else k
                if (end > runStart) {
                    val t0 = runStart.toFloat() / steps
                    val t1 = end.toFloat() / steps
                    open += floatArrayOf(ax + (bx - ax) * t0, az + (bz - az) * t0, ax + (bx - ax) * t1, az + (bz - az) * t1)
                }
                runStart = -1
            }
        }
    }
    return open
}

private fun distanceToSegment(x: Float, z: Float, wall: LiveWall): Float {
    val vx = wall.bx - wall.ax
    val vz = wall.bz - wall.az
    val l2 = vx * vx + vz * vz
    val t = if (l2 < 1e-9f) 0f else (((x - wall.ax) * vx + (z - wall.az) * vz) / l2).coerceIn(0f, 1f)
    return hypot(x - (wall.ax + vx * t), z - (wall.az + vz * t))
}

private fun hypot(x: Float, z: Float): Float = sqrt(x * x + z * z)

/** [radians] in degrees. */
private fun degrees(radians: Float): Float = radians * (HALF_TURN_DEGREES / PI.toFloat())

private const val HALF_TURN_DEGREES = 180f

/** An open floor edge shorter than this is the grid's own raggedness, not an opening. */
private const val MIN_OPEN_EDGE_M = 0.3f

/** The steps an open edge is cut into to leave out the part a wall stands on. */
private const val OPEN_STEP_M = 0.1f

/** A floor point this close to a wall's line is the wall's foot. */
private const val WALL_COVERS_M = 0.35f
