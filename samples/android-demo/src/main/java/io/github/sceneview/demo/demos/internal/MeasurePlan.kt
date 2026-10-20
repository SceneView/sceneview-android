package io.github.sceneview.demo.demos.internal

import io.github.sceneview.math.Position
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * A room traced in Measure, as the same plan 3D Scan draws and shares: a [RoomSurvey] made from
 * the floor outline, so `RerunPlanView` and its PDF need nothing of their own for it.
 */

/**
 * The plan of a room traced corner by corner: its floor [outline] (world space, levelled, in
 * the order it was traced, either way round) with the walls [height] metres high.
 *
 * In the plan's frame, as [RoomSurvey] wants it: the outline is turned so its walls square up
 * with the page (the turn that boxes it tightest, one of its own walls along x), its longer
 * side runs along x, y goes down the page, and the walls run clockwise from the corner nearest
 * the top-left — wall A along the top. Every wall was measured end to end, so every wall is
 * seen, and no figure is an estimate. A tape-measured figure has no scatter: its `±` is 0,
 * which the plan prints as its own rounding step.
 *
 * `null` under three corners, or for an outline with no area.
 */
fun measureRoomSurvey(outline: List<Position>, height: Float?): RoomSurvey? {
    if (outline.size < MeasureBoard.MIN_OUTLINE_POINTS) return null
    val turn = squaringTurn(outline)
    val c = cos(turn)
    val s = sin(turn)
    // Seen from above, world x to the right puts world z down the page.
    var xs = outline.map { it.x * c + it.z * s }
    var ys = outline.map { -it.x * s + it.z * c }
    var yaw = turn
    if (ys.max() - ys.min() > xs.max() - xs.min()) {
        // A quarter turn more: the longer side runs along x.
        val turnedX = ys.map { -it }
        ys = xs
        xs = turnedX
        yaw += HALF_PI
    }
    val minX = xs.min()
    val minY = ys.min()
    var corners = xs.indices.map { (xs[it] - minX) to (ys[it] - minY) }
    val signed = signedArea(corners)
    if (abs(signed) < MIN_AREA_M2) return null
    // Positive on a page whose y goes down is clockwise: the room on the right of the walk.
    if (signed < 0f) corners = corners.reversed()
    val first = corners.indices.minBy { corners[it].first + corners[it].second }
    corners = corners.drop(first) + corners.take(first)

    val walls = corners.indices.map { i ->
        val (ax, ay) = corners[i]
        val (bx, by) = corners[(i + 1) % corners.size]
        val dx = bx - ax
        val dy = by - ay
        SurveyWall(
            tag = ('A' + i).toString(),
            ax = ax,
            ay = ay,
            bx = bx,
            by = by,
            length = measured(sqrt(dx * dx + dy * dy)),
            seenShare = 1f,
            seen = true,
            unseen = emptyList(),
        )
    }
    val planLength = xs.max() - minX
    val planWidth = ys.max() - minY
    return RoomSurvey(
        walls = walls,
        planLength = planLength,
        planWidth = planWidth,
        length = measured(planLength),
        width = measured(planWidth),
        area = measured(abs(signed)),
        perimeter = measured(walls.sumOf { it.length.value.toDouble() }.toFloat()),
        ceilingHeight = height?.let(::measured),
        gaps = if (height == null) listOf(SurveyGap.Ceiling) else emptyList(),
        floorY = outline.first().y,
        floorThickness = 0f,
        yaw = yaw,
    )
}

private fun measured(value: Float) = SurveyFigure(value, plusMinus = 0f, estimate = false)

/**
 * The turn about +Y that boxes the [outline] tightest, among the turns that lay one of its own
 * walls along x — the longest such wall on a tie. A rectangle squares up exactly, an L as
 * well, and a room with one leaning wall keeps its other walls square to the page.
 */
private fun squaringTurn(outline: List<Position>): Float {
    var best = 0f
    var bestArea = Float.MAX_VALUE
    var bestLength = 0f
    for (i in outline.indices) {
        val a = outline[i]
        val b = outline[(i + 1) % outline.size]
        val dx = b.x - a.x
        val dz = b.z - a.z
        val length = sqrt(dx * dx + dz * dz)
        if (length < MIN_WALL_M) continue
        val turn = atan2(dz, dx)
        val c = cos(turn)
        val s = sin(turn)
        val xs = outline.map { it.x * c + it.z * s }
        val ys = outline.map { -it.x * s + it.z * c }
        val area = (xs.max() - xs.min()) * (ys.max() - ys.min())
        val tighter = area < bestArea - AREA_TIE_M2
        val tie = !tighter && area <= bestArea + AREA_TIE_M2 && length > bestLength
        if (tighter || tie) {
            best = turn
            bestArea = area
            bestLength = length
        }
    }
    return best
}

/** Shoelace area on the page: positive when the corners run clockwise there (y down). */
private fun signedArea(corners: List<Pair<Float, Float>>): Float {
    var sum = 0f
    for (i in corners.indices) {
        val (ax, ay) = corners[i]
        val (bx, by) = corners[(i + 1) % corners.size]
        sum += ax * by - bx * ay
    }
    return sum / 2f
}

private const val HALF_PI = (Math.PI / 2).toFloat()
private const val MIN_WALL_M = 0.05f
private const val MIN_AREA_M2 = 0.01f
private const val AREA_TIE_M2 = 1e-3f
