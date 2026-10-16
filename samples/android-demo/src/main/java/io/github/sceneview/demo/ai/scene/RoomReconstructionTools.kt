package io.github.sceneview.demo.ai.scene

import io.github.sceneview.demo.ai.scene.SceneToolExecutor.Companion.OBJECT_CATEGORIES
import io.github.sceneview.demo.ai.scene.SceneToolExecutor.Companion.distanceToWall
import io.github.sceneview.demo.ai.scene.SceneToolExecutor.Companion.fmt
import io.github.sceneview.demo.ai.scene.SceneToolExecutor.Companion.r2
import io.github.sceneview.demo.demos.internal.Vec3
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.math.max
import kotlin.math.min

/**
 * The reconstruction tools: the code proposes what it reads off the cloud (`propose_room`), the
 * model chooses (`build_room`, `accept_opening`, `accept_object`), the code measures. The proposal
 * is computed once per session and kept, so ids (C1, G2, K3) stay valid across tool rounds.
 */
internal class RoomReconstructionTools(private val session: SpatialSession) {

    private var cached: RoomProposal? = null

    val proposal: RoomProposal
        get() = cached ?: RoomReconstruction.propose(session).also { cached = it }

    fun propose(model: RoomModel): ToolOutcome {
        val p = proposal
        val content = buildJsonObject {
            put("floor_height", r2(p.floorY))
            put("ceiling_height_m", p.ceilingHeight?.let { JsonPrimitive(r2(it)) } ?: JsonNull)
            put("orientation_deg", r2(p.yawDegrees))
            putJsonArray("walls") { p.walls.forEach { addJsonObject { wall(it) } } }
            putJsonArray("objects") { p.objects.forEach { addJsonObject { cluster(it) } } }
            putJsonArray("notes") { p.notes.forEach { add(it) } }
        }.toString()
        val looks = p.walls.map { segment(it.from, it.to, session.floorY) } + p.objects.map { Look.Probe(it.box, true) }
        val gaps = p.walls.sumOf { it.gaps.size }
        return ToolOutcome(
            content, false, model, looks,
            caption = "Proposed ${p.walls.size} walls, $gaps gaps, ${p.objects.size} objects",
        )
    }

    fun build(model: RoomModel, input: JsonObject): ToolOutcome {
        val ids = input.strings("wall_ids")
        if (ids.isEmpty()) return fail(model, "wall_ids is empty", "No walls to build")
        val unknown = ids.firstOrNull { proposal.wall(it) == null }
        if (unknown != null) return fail(model, "unknown wall id '$unknown' — propose_room lists them", "Unknown wall")
        val chosen = ids.distinct().map { checkNotNull(proposal.wall(it)) }
        val outline = RoomReconstruction.close(chosen.map { it.from to it.to })
        var next = model.copy(walls = emptyList(), openings = emptyList())
        val walls = outline.walls.map { w ->
            val candidate = chosen[w.index]
            val (id, after) = next.nextId("W")
            next = after
            val plane = candidate.planeId?.let { ", plane $it" } ?: ""
            RoomWall(id, w.from, w.to, candidate.height, "${candidate.id}: ${candidate.points} points$plane")
        }
        val plan = RoomPlan(outline.corners, outline.area, outline.perimeter, proposal.ceilingHeight, outline.closed)
        next = next.copy(walls = walls, plan = plan)
        val content = buildJsonObject {
            putJsonArray("walls") {
                walls.forEach { w ->
                    addJsonObject {
                        put("id", w.id)
                        put("from", arr(w.from.x, w.from.z))
                        put("to", arr(w.to.x, w.to.z))
                        put("length_m", r2(w.length))
                        put("height_m", r2(w.height))
                    }
                }
            }
            put("closed", outline.closed)
            if (outline.closed) putJsonArray("corners") { outline.corners.forEach { add(arr(it.x, it.z)) } }
            outline.area?.let { put("area_m2", r2(it)) }
            put("perimeter_m", r2(outline.perimeter))
            put("ceiling_height_m", proposal.ceilingHeight?.let { JsonPrimitive(r2(it)) } ?: JsonNull)
            if (!outline.closed) put("note", "these walls do not close a polygon; kept at their scanned extents")
        }.toString()
        val caption = outline.area?.let { "Room built: ${walls.size} walls, ${fmt(it)} m²" }
            ?: "Room built: ${walls.size} walls, open outline"
        return ToolOutcome(
            content, false, next,
            looks = walls.map { segment(it.from, it.to, session.floorY) },
            caption = caption,
        )
    }

    fun acceptOpening(model: RoomModel, input: JsonObject): ToolOutcome {
        val gapId = input.str("gap_id")
        val kindWire = input.str("kind")
        val kind = OpeningKind.entries.firstOrNull { it.wire == kindWire }
            ?: return fail(model, "kind must be one of ${OpeningKind.entries.map { it.wire }}", "Unknown kind")
        val gap = proposal.gap(gapId) ?: return fail(model, "unknown gap id '$gapId'", "Unknown gap")
        val wall = model.walls.firstOrNull { near(it, gap.from) && near(it, gap.to) }
        val refusal = refusal(gap, wall)
        if (refusal != null) return fail(model, refusal.first, refusal.second)
        checkNotNull(wall)
        val s0 = along(wall, gap.from)
        val s1 = along(wall, gap.to)
        val from = min(s0, s1)
        val to = max(s0, s1)
        val sill = max(0f, gap.low)
        val head = min(wall.height, gap.high)
        if (to - from < MIN_OPENING_M || head - sill < MIN_OPENING_M) {
            return fail(model, "$gapId is too small for an opening once clipped to ${wall.id}", "Too small")
        }
        val (id, next) = model.nextId(if (kind == OpeningKind.Window) "N" else "D")
        val opening = RoomOpening(id, wall.id, kind, from, to, sill, head)
        val content = buildJsonObject {
            put("id", id)
            put("wall_id", wall.id)
            put("kind", kind.wire)
            put("from_m", r2(from))
            put("to_m", r2(to))
            put("width_m", r2(opening.width))
            put("sill_m", r2(sill))
            put("head_m", r2(head))
            put("height_m", r2(opening.height))
        }.toString()
        val label = kind.wire.replaceFirstChar { it.uppercase() }
        return ToolOutcome(
            content, false, next.copy(openings = next.openings + opening),
            looks = listOf(segment(wall.at(from), wall.at(to), session.floorY + sill)),
            caption = "$label $id cut in ${wall.id} — ${fmt(opening.width)} m wide",
        )
    }

    fun acceptObject(model: RoomModel, input: JsonObject): ToolOutcome {
        val clusterId = input.str("cluster_id")
        val category = input.str("category")
        val cluster = proposal.cluster(clusterId)
            ?: return fail(model, "unknown cluster id '$clusterId'", "Unknown cluster")
        if (category !in OBJECT_CATEGORIES) {
            return fail(model, "category must be one of $OBJECT_CATEGORIES", "Unknown category")
        }
        val evidence = "cluster ${cluster.id}, ${cluster.points} points"
        val existing = model.objects.firstOrNull { it.evidence == evidence }
        val (id, next) = if (existing != null) {
            existing.id to model.copy(
                objects = model.objects.map { if (it.id == existing.id) it.copy(category = category) else it },
            )
        } else {
            val (fresh, after) = model.nextId("O")
            fresh to after.copy(objects = after.objects + RoomObject(fresh, category, cluster.box, evidence))
        }
        val box = cluster.box
        val content = buildJsonObject {
            put("id", id)
            put("category", category)
            put("center", arr(box.center.x, box.center.z))
            put("size", arr(box.size.x, box.size.z))
            put("height_m", r2(box.size.y))
            put("yaw_deg", r2(box.yawDegrees))
            put("points", cluster.points)
            put("against_wall", cluster.againstWall?.let { JsonPrimitive(it) } ?: JsonNull)
            put("evidence", evidence)
        }.toString()
        val size = "${fmt(box.size.x)} × ${fmt(box.size.z)} m, ${fmt(box.size.y)} m high"
        return ToolOutcome(
            content, false, next,
            looks = listOf(Look.Probe(box, true)),
            caption = "${category.replace('_', ' ')} $id — $size",
        )
    }

    /** Why [gap] cannot become an opening in [wall] — message and caption — or `null` when it can. */
    private fun refusal(gap: GapCandidate, wall: RoomWall?): Pair<String, String>? = when {
        gap.guess == "occluded" ->
            "${gap.id} is hidden behind something standing at the wall, not an opening" to "Not an opening"
        wall == null -> "no built wall under ${gap.id} — call build_room first" to "No wall under ${gap.id}"
        else -> null
    }

    private fun near(wall: RoomWall, p: FloorPoint) = distanceToWall(wall, p.x, p.z) <= ON_WALL_M

    private fun JsonObjectBuilder.wall(w: WallCandidate) {
        put("id", w.id)
        put("from", arr(w.from.x, w.from.z))
        put("to", arr(w.to.x, w.to.z))
        put("length_m", r2(w.length))
        put("height_m", r2(w.height))
        put("support", r2(w.support))
        put("points", w.points)
        put("plane_id", w.planeId?.let { JsonPrimitive(it) } ?: JsonNull)
        putJsonArray("gaps") {
            w.gaps.forEach { g ->
                addJsonObject {
                    put("id", g.id)
                    put("from_m", r2(w.from.distanceTo(g.from)))
                    put("to_m", r2(w.from.distanceTo(g.to)))
                    put("width_m", r2(g.width))
                    put("low_m", r2(g.low))
                    put("high_m", r2(g.high))
                    put("guess", g.guess)
                }
            }
        }
    }

    private fun JsonObjectBuilder.cluster(k: ObjectCandidate) {
        put("id", k.id)
        put("center", arr(k.box.center.x, k.box.center.z))
        put("size", arr(k.box.size.x, k.box.size.z))
        put("height_m", r2(k.box.size.y))
        put("yaw_deg", r2(k.box.yawDegrees))
        put("points", k.points)
        put("against_wall", k.againstWall?.let { JsonPrimitive(it) } ?: JsonNull)
    }

    /** Metres along [wall] from its start to the projection of [p], clipped to the wall. */
    private fun along(wall: RoomWall, p: FloorPoint): Float {
        val dx = wall.to.x - wall.from.x
        val dz = wall.to.z - wall.from.z
        val l = wall.length.coerceAtLeast(1e-4f)
        return (((p.x - wall.from.x) * dx + (p.z - wall.from.z) * dz) / l).coerceIn(0f, wall.length)
    }

    private fun segment(from: FloorPoint, to: FloorPoint, y: Float) =
        Look.Segment(Vec3(from.x, y, from.z), Vec3(to.x, y, to.z), true)

    private fun fail(model: RoomModel, message: String, caption: String) =
        ToolOutcome("error: $message", true, model, caption = caption)

    private fun arr(vararg v: Float): JsonArray = buildJsonArray { v.forEach { add(r2(it)) } }

    private fun JsonObject.strings(name: String): List<String> {
        val values = (this[name] as? JsonArray)?.map { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
        if (values == null || values.any { it == null }) throw ToolInputException("'$name' must be an array of strings")
        return values.filterNotNull()
    }

    private companion object {
        /** How far a gap's ends may sit from a built wall's line and still belong to it. */
        const val ON_WALL_M = 0.3f
        const val MIN_OPENING_M = 0.3f
    }
}
