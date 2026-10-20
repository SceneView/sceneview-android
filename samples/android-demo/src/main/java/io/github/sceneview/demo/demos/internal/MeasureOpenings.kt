package io.github.sceneview.demo.demos.internal

import io.github.sceneview.math.Position
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * Doors and windows marked on a measured wall, so the wall's net area — what a painter or a
 * plasterer prices — leaves them out. Pure, so each rule runs in a JVM test.
 */

/**
 * A door or a window on a wall: a rectangle in the wall's own frame, [from]..[to] metres along
 * the wall from its first point, [bottom]..[top] metres above its foot.
 */
data class MeasureOpening(val from: Float, val to: Float, val bottom: Float, val top: Float) {
    val width: Float get() = to - from
    val height: Float get() = top - bottom
}

/** A point on a wall, in the wall's frame: [along] its foot from the first point, [up] from the floor. */
data class MeasureWallPoint(val along: Float, val up: Float)

/**
 * Where the camera ray ([origin], [direction]) meets the wall standing [height] metres on the
 * floor line [a]–[b], in the wall's frame and kept on the wall. `null` when the ray runs along
 * the wall, points away from it, or lands more than [MEASURE_OPENING_SLACK_M] off its edges.
 */
fun measureWallPointFromRay(
    origin: Position,
    direction: Position,
    a: Position,
    b: Position,
    height: Float,
): MeasureWallPoint? {
    val ex = b.x - a.x
    val ez = b.z - a.z
    val length = sqrt(ex * ex + ez * ez)
    if (length < MIN_WALL_LENGTH_M) return null
    val nx = -ez / length
    val nz = ex / length
    val denominator = direction.x * nx + direction.z * nz
    if (abs(denominator) < RAY_PARALLEL_EPSILON) return null
    val t = ((a.x - origin.x) * nx + (a.z - origin.z) * nz) / denominator
    if (t <= 0f) return null
    val hx = origin.x + direction.x * t
    val hy = origin.y + direction.y * t
    val hz = origin.z + direction.z * t
    val along = ((hx - a.x) * ex + (hz - a.z) * ez) / length
    val up = hy - a.y
    val slack = MEASURE_OPENING_SLACK_M
    if (along < -slack || along > length + slack || up < -slack || up > height + slack) return null
    return MeasureWallPoint(along.coerceIn(0f, length), up.coerceIn(0f, height))
}

/**
 * The opening with [first] and [second] at opposite corners, or `null` when it is narrower or
 * lower than [MEASURE_OPENING_MIN_SIDE_M] — two taps on the same spot mark nothing.
 */
fun measureOpening(first: MeasureWallPoint, second: MeasureWallPoint): MeasureOpening? {
    val opening = MeasureOpening(
        from = min(first.along, second.along),
        to = max(first.along, second.along),
        bottom = min(first.up, second.up),
        top = max(first.up, second.up),
    )
    return opening.takeIf { it.width >= MEASURE_OPENING_MIN_SIDE_M && it.height >= MEASURE_OPENING_MIN_SIDE_M }
}

/**
 * The area the [openings] cover on a wall [width] × [height]: each one kept on the wall, and
 * where two overlap the overlap counted once — a window marked twice is one window.
 */
fun measureOpeningsArea(openings: List<MeasureOpening>, width: Float, height: Float): Float {
    val boxes = openings.mapNotNull {
        val from = it.from.coerceIn(0f, width)
        val to = it.to.coerceIn(0f, width)
        val bottom = it.bottom.coerceIn(0f, height)
        val top = it.top.coerceIn(0f, height)
        if (to > from && top > bottom) MeasureOpening(from, to, bottom, top) else null
    }
    if (boxes.isEmpty()) return 0f
    // Union of rectangles: in each strip between two vertical edges, the union of the spans.
    val xs = boxes.flatMap { listOf(it.from, it.to) }.distinct().sorted()
    var area = 0f
    for (i in 0 until xs.size - 1) {
        val left = xs[i]
        val right = xs[i + 1]
        val spans = boxes.filter { it.from <= left && it.to >= right }.map { it.bottom to it.top }.sortedBy { it.first }
        var covered = 0f
        var start = Float.NaN
        var end = Float.NaN
        for ((bottom, top) in spans) {
            if (start.isNaN() || bottom > end) {
                if (!start.isNaN()) covered += end - start
                start = bottom
                end = top
            } else if (top > end) {
                end = top
            }
        }
        if (!start.isNaN()) covered += end - start
        area += covered * (right - left)
    }
    return area
}

/** The four corners of [opening] on the wall [a]–[b] (its foot), in world space, for drawing. */
fun measureOpeningCorners(opening: MeasureOpening, a: Position, b: Position): List<Position> {
    val ex = b.x - a.x
    val ez = b.z - a.z
    val length = sqrt(ex * ex + ez * ez).takeIf { it >= MIN_WALL_LENGTH_M } ?: return emptyList()
    fun at(along: Float, up: Float) = Position(a.x + ex / length * along, a.y + up, a.z + ez / length * along)
    return listOf(
        at(opening.from, opening.bottom),
        at(opening.to, opening.bottom),
        at(opening.to, opening.top),
        at(opening.from, opening.top),
    )
}

/** How far off a wall's edges an aim still marks a corner on the edge itself. */
const val MEASURE_OPENING_SLACK_M = 0.1f

/** The smallest side of an opening: under it, the two taps were the same spot. */
const val MEASURE_OPENING_MIN_SIDE_M = 0.1f

private const val MIN_WALL_LENGTH_M = 0.05f
private const val RAY_PARALLEL_EPSILON = 1e-4f
