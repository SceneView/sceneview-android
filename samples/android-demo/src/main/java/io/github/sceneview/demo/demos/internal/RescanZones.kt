package io.github.sceneview.demo.demos.internal

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * The parts of a saved scan worth scanning again (#4736): a wall the scan never saw, a piece of
 * floor it never saw, a corner it only glimpsed. Each one is a box in the scan's own frame, so a
 * later scan can be aimed at it and its result merged back.
 *
 * The finder reads three plain inputs, whatever produced them:
 * - the room's walls, each flagged when it was inferred rather than seen, with the stretches of
 *   it the scan saw nothing on ([RescanWall]);
 * - the holes in the floor, as outlines ([RescanHole]);
 * - how densely the scan covered each patch of the room's footprint ([RescanCoverage]).
 *
 * [RescanSources] builds these from what the app has today (the room survey, the ground under the
 * model, the dense cloud); a structure engine or a cleaned model can hand its own in their place.
 *
 * Frame: the scan's, metres, +Y up, the floor in the XZ plane.
 */

/** What a [RescanZone] is missing. */
enum class RescanKind {
    /** A wall, or a stretch of one, the scan saw nothing on: it is drawn from its neighbours. */
    MissingWall,

    /** A piece of floor the scan never saw, with nothing standing on it to explain it. */
    FloorHole,

    /** A part of the room the scan saw, but only sparsely. */
    LowDensity,
}

/**
 * A part of a scan worth scanning again.
 *
 * @property kind what it is missing.
 * @property box the axis-aligned box it spans in the scan frame: `[minX, minY, minZ, maxX, maxY, maxZ]`,
 *   metres. A wall's box stands from the floor to the room's height and is [RescanZones.WALL_HALF_M]
 *   thick on each side of the wall; a floor zone's box is a flat slab [RescanZones.FLOOR_ZONE_H_M] high.
 * @property size how much of it there is: metres of wall for [RescanKind.MissingWall], square
 *   metres of floor for the other two.
 * @property reason why it is listed, one English sentence, for logs and reports. The app words
 *   it from [kind] and [size] in its own strings.
 */
class RescanZone(
    val kind: RescanKind,
    val box: FloatArray,
    val size: Float,
    val reason: String,
) {
    init {
        require(box.size == BOX_SIZE) { "a box is 6 floats" }
    }

    /** The middle of [box] on the floor plane, `[x, z]`. */
    val centre: FloatArray get() = floatArrayOf((box[0] + box[3]) / 2f, (box[2] + box[5]) / 2f)

    /** The part of [box]'s floor footprint that lies in [other]'s, in square metres. */
    fun overlapM2(other: FloatArray): Float {
        val w = min(box[3], other[3]) - max(box[0], other[0])
        val d = min(box[5], other[5]) - max(box[2], other[2])
        return if (w <= 0f || d <= 0f) 0f else w * d
    }

    /** [box]'s floor footprint, square metres. */
    val footprintM2: Float get() = (box[3] - box[0]) * (box[5] - box[2])

    override fun toString(): String = String.format(
        Locale.ROOT,
        "%s %.2f [%.2f %.2f %.2f .. %.2f %.2f %.2f] %s",
        kind, size, box[0], box[1], box[2], box[3], box[4], box[5], reason,
    )

    companion object {
        /** Floats in a [box]. */
        const val BOX_SIZE = 6
    }
}

/**
 * One wall of the room, from `(ax, az)` to `(bx, bz)` on the floor plane.
 *
 * @property inferred `true` when the scan saw no wall there and it was drawn to close the room:
 *   the whole wall is then a zone.
 * @property unseen the stretches of a seen wall the scan saw nothing on, in metres from `(ax, az)`
 *   toward `(bx, bz)`. Those of [RescanZones.MIN_WALL_RUN_M] or more are zones.
 */
class RescanWall(
    val ax: Float,
    val az: Float,
    val bx: Float,
    val bz: Float,
    val inferred: Boolean,
    val unseen: List<ClosedFloatingPointRange<Float>> = emptyList(),
) {
    /** Its length, metres. */
    val length: Float get() = sqrt((bx - ax) * (bx - ax) + (bz - az) * (bz - az))
}

/**
 * A hole in the floor: its [outline] on the floor plane, `[x0, z0, x1, z1, …]`, metres, and its
 * [area] in square metres (the outline's own by default; a coarser outline passes the true one).
 */
class RescanHole(
    val outline: FloatArray,
    val area: Float = shoelace(outline),
) {
    init {
        require(outline.size >= MIN_OUTLINE && outline.size % 2 == 0) { "an outline is 3 points or more" }
    }

    /** [outline]'s bounds, `[minX, minZ, maxX, maxZ]`. */
    fun bounds(): FloatArray {
        val b = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (i in outline.indices step 2) {
            b[0] = min(b[0], outline[i])
            b[1] = min(b[1], outline[i + 1])
            b[2] = max(b[2], outline[i])
            b[3] = max(b[3], outline[i + 1])
        }
        return b
    }

    /** Whether `(x, z)` lies inside [outline] (even-odd rule). */
    fun contains(x: Float, z: Float): Boolean = insidePolygon(outline, x, z)

    companion object {
        private const val MIN_OUTLINE = 6

        /** The area a closed [outline] encloses, square metres. */
        fun shoelace(outline: FloatArray): Float {
            var sum = 0.0
            val n = outline.size / 2
            for (i in 0 until n) {
                val j = (i + 1) % n
                sum += outline[i * 2].toDouble() * outline[j * 2 + 1] - outline[j * 2].toDouble() * outline[i * 2 + 1]
            }
            return abs(sum / 2.0).toFloat()
        }
    }
}

/**
 * How densely the scan covered the room's footprint: a grid of [columns] × [rows] square cells of
 * [cellM] on the floor plane, the first one's corner at `(originX, originZ)`, row-major (`row *
 * columns + column`, rows along +Z). [density] is in any unit that grows with what the scan saw
 * over a cell — surface seen per square metre, say — from the floor up to the furniture, ceiling
 * left out; `0` is a cell nothing was seen over. Only ratios to the room's median are read.
 */
class RescanCoverage(
    val cellM: Float,
    val originX: Float,
    val originZ: Float,
    val columns: Int,
    val rows: Int,
    val density: FloatArray,
) {
    init {
        require(cellM > 0f) { "cells have a size" }
        require(density.size == columns * rows) { "one density per cell" }
    }

    /** The density of the cell at [column], [row]. */
    fun at(column: Int, row: Int): Float = density[row * columns + column]

    /** The cell `[column, row]` holding `(x, z)`, `null` off the grid. */
    fun cellOf(x: Float, z: Float): IntArray? {
        val c = floor((x - originX) / cellM).toInt()
        val r = floor((z - originZ) / cellM).toInt()
        return if (c in 0 until columns && r in 0 until rows) intArrayOf(c, r) else null
    }
}

/**
 * What [RescanZones.find] reads.
 *
 * @property floorY the floor's height in the scan frame.
 * @property height the room's height above it (the ceiling's, else [RescanZones.DEFAULT_HEIGHT_M]).
 * @property walls the room's walls. When they chain into a closed outline, only the coverage
 *   inside it is read; otherwise the covered area's own outline is.
 * @property holes the holes in the floor.
 * @property coverage how densely the scan covered the room; `null` when there is none to read:
 *   only walls and holes give zones then.
 */
class RescanInput(
    val floorY: Float,
    val height: Float = RescanZones.DEFAULT_HEIGHT_M,
    val walls: List<RescanWall> = emptyList(),
    val holes: List<RescanHole> = emptyList(),
    val coverage: RescanCoverage? = null,
)

/**
 * Finds the [RescanZone]s of a scan.
 *
 * - **Walls.** An inferred wall is a zone end to end; a seen wall gives one per unseen stretch of
 *   [MIN_WALL_RUN_M] or more.
 * - **Floor.** The coverage cells inside the room are compared to the room's median: under
 *   [LOW_SHARE] of it a cell is short, under [EMPTY_SHARE] it is empty. A hole marks the cells
 *   it covers as empty, unless they are covered at [LOW_SHARE] or more — something stands on it,
 *   so the scan could not have seen that floor and was not meant to, and none of its cells is
 *   short: the back of a tall cabinet, its top never seen, hides the floor as its front does.
 *   Short cells that touch make
 *   one zone: a [RescanKind.FloorHole] when [HOLE_EMPTY_SHARE] or more of it is empty (its box is
 *   then its holes' when it has any), a [RescanKind.LowDensity] otherwise. A floor hole needs
 *   [MIN_HOLE_M2], a sparse patch [MIN_LOW_M2]. A hole outside every patch (no coverage, or by a
 *   wall) is a zone of its own.
 * - A floor zone [ABSORB_SHARE] or more of whose footprint lies in a wall zone's is left to it:
 *   a missing wall also leaves its foot unseen.
 *
 * Walls come first in the result, then holes, then sparse patches, each the largest first.
 * Pure arithmetic: any thread.
 */
object RescanZones {
    /** The shortest unseen stretch of a seen wall that is a zone, metres. */
    const val MIN_WALL_RUN_M = 0.3f

    /** How far a wall zone's box reaches on each side of the wall, metres. */
    const val WALL_HALF_M = 0.15f

    /** A floor zone's box height, metres: a slab on the floor. */
    const val FLOOR_ZONE_H_M = 0.05f

    /** A cell under this share of the room's median density is short. */
    const val LOW_SHARE = 0.35f

    /** A cell under this share of the room's median density is empty. */
    const val EMPTY_SHARE = 0.05f

    /** The share of a patch's cells that have to be empty for it to be a hole. */
    const val HOLE_EMPTY_SHARE = 0.5f

    /** The smallest floor hole that is a zone, square metres (30 cm × 30 cm). */
    const val MIN_HOLE_M2 = 0.09f

    /** The smallest sparse patch that is a zone, square metres (50 cm × 50 cm). */
    const val MIN_LOW_M2 = 0.25f

    /** The share of a floor zone's footprint in a wall zone's that leaves it to the wall. */
    const val ABSORB_SHARE = 0.5f

    /** The room's height when the ceiling was not measured, metres. */
    const val DEFAULT_HEIGHT_M = 2.4f

    /** How far two wall ends may be apart and still close the outline, metres. */
    const val CHAIN_GAP_M = 0.05f

    /** How far inside the outline a cell's corners are tested, metres: a cell flush with a wall is inside. */
    const val EDGE_TOLERANCE_M = 0.02f

    /** The zones of [input]. */
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun find(input: RescanInput): List<RescanZone> = emptyList()
}

/** Whether `(x, z)` lies inside the closed [polygon] `[x0, z0, x1, z1, …]` (even-odd rule). */
internal fun insidePolygon(polygon: FloatArray, x: Float, z: Float): Boolean {
    var inside = false
    val n = polygon.size / 2
    var j = n - 1
    for (i in 0 until n) {
        val xi = polygon[i * 2]
        val zi = polygon[i * 2 + 1]
        val xj = polygon[j * 2]
        val zj = polygon[j * 2 + 1]
        if ((zi > z) != (zj > z) && x < (xj - xi) * (z - zi) / (zj - zi) + xi) inside = !inside
        j = i
    }
    return inside
}
