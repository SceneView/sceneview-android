package io.github.sceneview.demo.demos

import io.github.sceneview.demo.ai.scene.OrientedBox
import io.github.sceneview.demo.ai.scene.RoomModel
import io.github.sceneview.demo.ai.scene.RoomObject
import io.github.sceneview.demo.ai.scene.RoomWall
import kotlin.math.hypot
import kotlin.math.min

/*
 * How the AI Scene's rebuilt room is laid out, as plain geometry the JVM tests read: which way a
 * recognised object faces, and how a catalogue model is turned and scaled into the box the scan
 * measured.
 */

/** A unit direction in [OrientedBox]'s own frame (x, z), from its centre towards a world point. */
internal fun OrientedBox.localDirectionTo(x: Float, z: Float): Pair<Float, Float> {
    val l = toLocal(x, center.y, z)
    val n = hypot(l.x, l.z).takeIf { it > EPSILON } ?: return 0f to 1f
    return l.x / n to l.z / n
}

/** Distance on the floor from (x, z) to [wall]'s segment. */
internal fun RoomWall.floorDistanceTo(x: Float, z: Float): Float {
    val dx = to.x - from.x
    val dz = to.z - from.z
    val len2 = dx * dx + dz * dz
    val t = if (len2 < EPSILON) 0f else (((x - from.x) * dx + (z - from.z) * dz) / len2).coerceIn(0f, 1f)
    return hypot(x - (from.x + t * dx), z - (from.z + t * dz))
}

/**
 * Which way a recognised object faces, in its box's frame, or `null` when it has no front: a
 * desk faces its chair, a chair its desk, a radiator the room; a bed's foot points away from the
 * wall its head is against.
 */
internal fun frontOf(obj: RoomObject, model: RoomModel): Pair<Float, Float>? {
    val box = obj.box
    fun nearest(categories: Set<String>) = model.objects
        .filter { it.id != obj.id && it.category in categories }
        .minByOrNull { hypot(it.box.center.x - box.center.x, it.box.center.z - box.center.z) }
        ?.takeIf { hypot(it.box.center.x - box.center.x, it.box.center.z - box.center.z) < PARTNER_RANGE_M }
    fun awayFromWall(): Pair<Float, Float>? {
        val wall = model.walls.minByOrNull { it.floorDistanceTo(box.center.x, box.center.z) } ?: return null
        val t = projectionOn(wall, box.center.x, box.center.z)
        val (fx, fz) = box.localDirectionTo(
            wall.from.x + t * (wall.to.x - wall.from.x),
            wall.from.z + t * (wall.to.z - wall.from.z),
        )
        return -fx to -fz
    }
    return when (obj.category) {
        "desk", "table" -> nearest(SEATS)
            ?.let { box.localDirectionTo(it.box.center.x, it.box.center.z) }
            ?: awayFromWall()
        "chair", "office_chair" -> nearest(WORKTOPS)?.let { box.localDirectionTo(it.box.center.x, it.box.center.z) }
        "radiator", "sofa", "shelf", "cabinet" -> awayFromWall()
        "bed" -> bedFoot(box, model.walls)
        else -> null
    }
}

/** The bed's foot, along its long side: away from the end against a wall, else towards the nearest wall. */
private fun bedFoot(box: OrientedBox, walls: List<RoomWall>): Pair<Float, Float> {
    val alongZ = box.size.z >= box.size.x
    val half = (if (alongZ) box.size.z else box.size.x) / 2
    val ends = listOf(-half, half).map { e ->
        val p = if (alongZ) box.toWorld(0f, 0f, e) else box.toWorld(e, 0f, 0f)
        e to (walls.minOfOrNull { it.floorDistanceTo(p.x, p.z) } ?: Float.MAX_VALUE)
    }
    val headAgainst = ends.minBy { it.second }.takeIf { it.second < HEAD_TO_WALL_M }
    // Against a wall, the head is there. Otherwise the scan only saw the room's open side, and the
    // head goes to the far end, against the wall it could not confirm.
    val head = headAgainst?.first ?: ends.maxBy { it.second }.first
    val foot = -head
    return if (alongZ) 0f to (if (foot > 0) 1f else -1f) else (if (foot > 0) 1f else -1f) to 0f
}

private fun projectionOn(wall: RoomWall, x: Float, z: Float): Float {
    val dx = wall.to.x - wall.from.x
    val dz = wall.to.z - wall.from.z
    val len2 = dx * dx + dz * dz
    return if (len2 < EPSILON) 0f else (((x - wall.from.x) * dx + (z - wall.from.z) * dz) / len2).coerceIn(0f, 1f)
}

/**
 * How a model fits [box]: [ModelFit.quarterTurns] × 90° about Y so its front (+Z) is the box's
 * front, then a scale per box axis. The footprint sets one uniform scale; a side may stretch up to
 * [MAX_STRETCH] beyond it to fill the box. Height follows the footprint, or — [matchHeight] —
 * reaches for the box's height within the same stretch.
 */
internal fun fitModel(
    halfExtent: FloatArray,
    box: OrientedBox,
    front: Pair<Float, Float>,
    matchHeight: Boolean,
): ModelFit {
    val turns = quarterTurnsFor(front)
    val odd = turns % 2 == 1
    val ex = 2f * (if (odd) halfExtent[2] else halfExtent[0])
    val ey = 2f * halfExtent[1]
    val ez = 2f * (if (odd) halfExtent[0] else halfExtent[2])
    val rx = box.size.x / ex.coerceAtLeast(EPSILON)
    val ry = box.size.y / ey.coerceAtLeast(EPSILON)
    val rz = box.size.z / ez.coerceAtLeast(EPSILON)
    val s = min(rx, rz)
    val sy = if (matchHeight) ry.coerceIn(s, s * MAX_STRETCH) else s
    return ModelFit(turns, min(rx, s * MAX_STRETCH), sy, min(rz, s * MAX_STRETCH))
}

/** The quarter turn that brings a model's +Z closest to [front] (box frame x, z). */
internal fun quarterTurnsFor(front: Pair<Float, Float>): Int {
    // A turn of k × 90° about Y sends +Z to (sin, cos): +Z, +X, −Z, −X.
    val dirs = listOf(0f to 1f, 1f to 0f, 0f to -1f, -1f to 0f)
    return dirs.indices.maxBy { dirs[it].first * front.first + dirs[it].second * front.second }
}

private const val EPSILON = 1e-4f
private const val PARTNER_RANGE_M = 1.6f
private val SEATS = setOf("chair", "office_chair")
private val WORKTOPS = setOf("desk", "table")
private const val HEAD_TO_WALL_M = 0.5f
private const val MAX_STRETCH = 1.25f
