package io.github.sceneview.demo.ai.scene

import io.github.sceneview.demo.demos.internal.Vec3
import kotlin.math.atan2
import kotlin.math.hypot

/** A point on the floor, world x and z, metres. */
data class FloorPoint(val x: Float, val z: Float) {
    fun distanceTo(o: FloorPoint): Float = hypot(o.x - x, o.z - z)
}

/**
 * A wall the model proposed and the cloud confirmed: [from] → [to] along the floor, [height]
 * above it. [evidence] says what confirmed it ("plane 3", "331 points").
 */
data class RoomWall(
    val id: String,
    val from: FloorPoint,
    val to: FloorPoint,
    val height: Float,
    val evidence: String,
) {
    val length: Float get() = from.distanceTo(to)

    /** Yaw (degrees, about +Y) turning local +X onto the wall's direction. */
    val yawDegrees: Float get() = Math.toDegrees(atan2(-(to.z - from.z), to.x - from.x).toDouble()).toFloat()

    /** The floor point [s] metres along the wall from [from]. */
    fun at(s: Float): FloorPoint {
        val l = length.coerceAtLeast(1e-4f)
        return FloorPoint(from.x + (to.x - from.x) * s / l, from.z + (to.z - from.z) * s / l)
    }
}

enum class OpeningKind(val wire: String) { Window("window"), Door("door") }

/** A window or a door cut in wall [wallId]: [from]…[to] metres along it, [sill]…[head] above the floor. */
data class RoomOpening(
    val id: String,
    val wallId: String,
    val kind: OpeningKind,
    val from: Float,
    val to: Float,
    val sill: Float,
    val head: Float,
) {
    val width: Float get() = to - from
    val height: Float get() = head - sill
}

/** Something the room already holds, fitted by code to the points: [box] sits on the floor. */
data class RoomObject(
    val id: String,
    val category: String,
    val box: OrientedBox,
    val evidence: String,
)

/** A catalogue item the model placed; its size is the catalogue's, never the model's. */
data class PlacedItem(
    val id: String,
    val category: String,
    val box: OrientedBox,
    val material: String,
)

enum class DimensionKind(val wire: String) { Width("width"), Depth("depth"), Height("height"), Clearance("clearance") }

/** A dimension line from [from] to [to], world metres; [value] is their distance, computed. */
data class RoomDimension(
    val id: String,
    val itemId: String,
    val kind: DimensionKind,
    val from: Vec3,
    val to: Vec3,
) {
    val value: Float get() = (to - from).length()
}

/** A floor area code found empty: [box]'s footprint, y = the floor. */
data class FreeRegion(val id: String, val box: OrientedBox)

/** A proposal the cloud did not support, kept on screen so the refusal is visible. */
data class Rejection(val what: String, val from: FloorPoint, val to: FloorPoint, val reason: String)

/**
 * The room as the model has rebuilt and furnished it so far. Immutable: every tool call returns
 * a new one, and the previous one is the undo step.
 */
/**
 * The measured floor plan: the corners the built walls meet at (empty when they do not close),
 * the area — only when closed, never a guessed number — the perimeter and the ceiling height
 * when the scan saw it. Every value is computed from the walls, none is typed by the model.
 */
data class RoomPlan(
    val corners: List<FloorPoint>,
    val area: Float?,
    val perimeter: Float,
    val ceilingHeight: Float?,
    val closed: Boolean,
)

data class RoomModel(
    val walls: List<RoomWall> = emptyList(),
    val openings: List<RoomOpening> = emptyList(),
    val objects: List<RoomObject> = emptyList(),
    val regions: List<FreeRegion> = emptyList(),
    val items: List<PlacedItem> = emptyList(),
    val dimensions: List<RoomDimension> = emptyList(),
    val rejections: List<Rejection> = emptyList(),
    /** The floor plan `build_room` measured — corners, area, perimeter — or `null` before it ran. */
    val plan: RoomPlan? = null,
    private val counter: Int = 0,
) {
    val isEmpty: Boolean get() = walls.isEmpty() && objects.isEmpty() && items.isEmpty()

    /** A fresh id with [prefix], and the model that has used it. */
    fun nextId(prefix: String): Pair<String, RoomModel> = "$prefix${counter + 1}" to copy(counter = counter + 1)

    fun wall(id: String): RoomWall? = walls.firstOrNull { it.id == id }

    /** The box of anything with a footprint: a fitted object, a placed item. */
    fun boxOf(id: String): OrientedBox? =
        objects.firstOrNull { it.id == id }?.box ?: items.firstOrNull { it.id == id }?.box

    fun remove(id: String): RoomModel? = when {
        walls.any { it.id == id } -> copy(
            walls = walls.filterNot { it.id == id },
            openings = openings.filterNot { it.wallId == id },
            plan = null,
        )
        openings.any { it.id == id } -> copy(openings = openings.filterNot { it.id == id })
        objects.any { it.id == id } -> copy(objects = objects.filterNot { it.id == id })
        items.any { it.id == id } -> copy(
            items = items.filterNot { it.id == id },
            dimensions = dimensions.filterNot { it.itemId == id },
        )
        dimensions.any { it.id == id } -> copy(dimensions = dimensions.filterNot { it.id == id })
        else -> null
    }
}
