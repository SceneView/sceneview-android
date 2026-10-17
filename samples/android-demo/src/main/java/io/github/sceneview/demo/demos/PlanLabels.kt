package io.github.sceneview.demo.demos

import kotlin.math.abs

/** Pixel geometry only: no Android, Compose, density or font assumptions. */
internal data class PlanPoint(val x: Float, val y: Float)

internal data class PlanRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val center get() = PlanPoint((left + right) / 2, (top + bottom) / 2)
    val corners get() = listOf(
        PlanPoint(left, top), PlanPoint(right, top), PlanPoint(right, bottom), PlanPoint(left, bottom),
    )
    fun contains(other: PlanRect) = other.left >= left && other.right <= right &&
        other.top >= top && other.bottom <= bottom
    fun overlaps(other: PlanRect, gap: Float = 0f) = left < other.right + gap &&
        right + gap > other.left && top < other.bottom + gap && bottom + gap > other.top

    companion object {
        fun around(center: PlanPoint, width: Float, height: Float) = PlanRect(
            center.x - width / 2, center.y - height / 2, center.x + width / 2, center.y + height / 2,
        )
        fun enclosing(points: List<PlanPoint>) = PlanRect(
            points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y },
        )
    }
}

internal data class PlanLabel(
    val footprint: List<PlanPoint>,
    val width: Float,
    val height: Float,
    val markerWidth: Float,
    val markerHeight: Float,
) {
    val center get() = PlanRect.enclosing(footprint).center
}

internal data class PlanLabelPlacement(
    val bounds: PlanRect?,
    val leader: Boolean,
    /** Stable object index, shared by the marker and legend. */
    val number: Int? = null,
)

/**
 * Keep horizontal labels inside their actual (possibly rotated) footprint when they fit.
 * Otherwise find nearby empty screen space, keeping clear of dimensions, other footprints
 * and labels. Reserve small numbered positions first so a long label cannot starve a later
 * object's fallback. A viewport too small even for a marker retains its full legend entry.
 */
internal fun placePlanLabels(
    labels: List<PlanLabel>,
    viewport: PlanRect,
    obstacles: List<PlanRect>,
    gap: Float,
): List<PlanLabelPlacement> {
    require(gap > 0f)
    val footprints = labels.map { PlanRect.enclosing(it.footprint) }
    val markers = mutableListOf<PlanRect?>()
    labels.forEach { label ->
        markers += nearestPlanSpace(
            label.center, label.markerWidth, label.markerHeight, viewport,
            obstacles + markers.filterNotNull(), gap,
        )
    }
    val placed = arrayOfNulls<PlanLabelPlacement>(labels.size)
    fun occupied(index: Int) = obstacles + labels.indices.filter { it != index }.mapNotNull {
        placed[it]?.bounds ?: markers[it]
    }
    // Protect fitting labels before searching for displaced ones.
    labels.forEachIndexed { index, label ->
        val bounds = PlanRect.around(label.center, label.width, label.height)
        if (viewport.contains(bounds) && bounds.corners.all { insideFootprint(it, label.footprint) } &&
            occupied(index).none { bounds.overlaps(it, gap) }
        ) {
            placed[index] = PlanLabelPlacement(bounds, leader = false)
        }
    }
    labels.forEachIndexed { index, label ->
        if (placed[index] != null) return@forEachIndexed
        val bounds = nearestPlanSpace(
            label.center, label.width, label.height, viewport, occupied(index) + footprints, gap,
        )
        placed[index] = if (bounds != null) {
            PlanLabelPlacement(bounds, leader = true)
        } else {
            PlanLabelPlacement(markers[index], leader = markers[index]?.center != label.center, number = index + 1)
        }
    }
    return placed.map { checkNotNull(it) }
}

private fun insideFootprint(point: PlanPoint, polygon: List<PlanPoint>): Boolean {
    if (polygon.size < 3) return false
    var positive = false
    var negative = false
    var area = 0f
    polygon.indices.forEach { index ->
        val a = polygon[index]
        val b = polygon[(index + 1) % polygon.size]
        val cross = (b.x - a.x) * (point.y - a.y) - (b.y - a.y) * (point.x - a.x)
        positive = positive || cross > 0f
        negative = negative || cross < 0f
        area += a.x * b.y - b.x * a.y
    }
    return abs(area) > 0f && !(positive && negative)
}

private fun nearestPlanSpace(
    anchor: PlanPoint,
    width: Float,
    height: Float,
    viewport: PlanRect,
    occupied: List<PlanRect>,
    gap: Float,
): PlanRect? {
    if (width > viewport.right - viewport.left || height > viewport.bottom - viewport.top) return null
    val minX = viewport.left + width / 2
    val maxX = viewport.right - width / 2
    val minY = viewport.top + height / 2
    val maxY = viewport.bottom - height / 2
    // Include exact obstacle edges as well as a font-sized grid; narrow free slots matter.
    val xs = mutableListOf(anchor.x.coerceIn(minX, maxX), minX, maxX)
    val ys = mutableListOf(anchor.y.coerceIn(minY, maxY), minY, maxY)
    occupied.forEach {
        xs += it.left - gap - width / 2
        xs += it.right + gap + width / 2
        ys += it.top - gap - height / 2
        ys += it.bottom + gap + height / 2
    }
    val step = maxOf(gap, height / 2)
    var x = minX
    while (x <= maxX) {
        xs += x
        x += step
    }
    var y = minY
    while (y <= maxY) {
        ys += y
        y += step
    }
    var best: PlanRect? = null
    var distance = Float.POSITIVE_INFINITY
    val candidatesX = xs.distinct().filter { it in minX..maxX }
    val candidatesY = ys.distinct().filter { it in minY..maxY }
    for (cx in candidatesX) {
        for (cy in candidatesY) {
            val d = (cx - anchor.x) * (cx - anchor.x) + (cy - anchor.y) * (cy - anchor.y)
            if (d >= distance) continue
            val candidate = PlanRect.around(PlanPoint(cx, cy), width, height)
            if (occupied.none { candidate.overlaps(it, gap) }) {
                best = candidate
                distance = d
            }
        }
    }
    return best
}
