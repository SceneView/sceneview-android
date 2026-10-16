package io.github.sceneview.demo.ai.scene

import io.github.sceneview.ai.AiSchemas
import io.github.sceneview.ai.AiToolSpec
import io.github.sceneview.demo.demos.internal.DebugPlane
import io.github.sceneview.demo.demos.internal.DebugPlaneKind
import io.github.sceneview.demo.demos.internal.DebugPose
import io.github.sceneview.demo.demos.internal.Vec3
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** What the model was shown doing: the rays it cast, the boxes it probed, the walls it tried. */
sealed interface Look {
    data class Ray(val from: Vec3, val to: Vec3, val hit: Boolean) : Look

    data class Probe(val box: OrientedBox, val ok: Boolean) : Look

    data class Segment(val from: Vec3, val to: Vec3, val ok: Boolean) : Look
}

/**
 * One tool call, executed: [content] goes back to the model, [model] is the room after it,
 * [looks] is what the screen draws for it, [caption] the one line the chat shows.
 */
data class ToolOutcome(
    val content: String,
    val isError: Boolean,
    val model: RoomModel,
    val looks: List<Look> = emptyList(),
    val caption: String,
)

/** A catalogue item: its real size, metres — width (local X), height, depth (local Z). */
data class CatalogueItem(val category: String, val label: String, val width: Float, val height: Float, val depth: Float)

/**
 * The tools the model reads the room and acts with, and their execution on the recorded
 * [session]. Pure — no Android, no Filament — so the whole recorded run is a JVM test.
 *
 * The rule: the model *chooses* (which wall, which object, which slot); this code *computes*
 * every length, height, area and clearance from the cloud and the catalogue.
 */
// One private handler per tool plus its geometry helpers: splitting them would scatter the evidence rules.
@Suppress("TooManyFunctions")
class SceneToolExecutor(private val session: SpatialSession) {

    private val floorY = session.floorY

    /** The reconstruction tools (propose_room, build_room, accept_opening, accept_object) and their cached proposal. */
    private val reconstruction = RoomReconstructionTools(session)

    /** The code's reading of the scan — the candidates `propose_room` lists, computed once. */
    val proposal: RoomProposal get() = reconstruction.proposal

    /** The last free-floor grid, one row per line: '.' free, '#' points, '?' unseen or kept clear. */
    internal var lastFreeMap: String = ""

    fun execute(model: RoomModel, name: String, input: JsonObject): ToolOutcome = try {
        when (name) {
            LIST_PLANES -> listPlanes(model)
            RAYCAST -> raycast(model, input)
            POINTS_IN_BOX -> pointsInBox(model, input)
            MEASURE -> measure(model, input)
            CAMERA_POSE_AT -> cameraPoseAt(model, input)
            FREE_FLOOR_AREA -> freeFloorArea(model, input)
            ADD_WALL -> addWall(model, input)
            ADD_OPENING -> addOpening(model, input)
            ADD_OBJECT -> addObject(model, input)
            PLACE_ITEM -> placeItem(model, input)
            ADD_DIMENSION -> addDimension(model, input)
            SET_MATERIAL -> setMaterial(model, input)
            REMOVE_NODE -> removeNode(model, input)
            COMPUTE_TAKEOFF -> computeTakeoff(model)
            PROPOSE_ROOM -> reconstruction.propose(model)
            BUILD_ROOM -> reconstruction.build(model, input)
            ACCEPT_OPENING -> reconstruction.acceptOpening(model, input)
            ACCEPT_OBJECT -> reconstruction.acceptObject(model, input)
            else -> fail(model, "unknown tool '$name'", "Unknown tool $name")
        }
    } catch (e: ToolInputException) {
        fail(model, "invalid input: ${e.message}", "Invalid $name call")
    }

    /** The compact view of the session the model gets up front, before any tool call. */
    fun digest(): JsonObject = buildJsonObject {
        put("units", "metres; heights are above the floor; positions are [x, height, z] in the session's world frame")
        put("floor_height_world_y", r2(floorY))
        put("points", session.pointCount)
        val xs = (0 until session.pointCount).map { session.points[it * 3] }
        val zs = (0 until session.pointCount).map { session.points[it * 3 + 2] }
        if (xs.isNotEmpty()) {
            putJsonObject("cloud_bounds_xz") {
                put("x", arr(xs.min(), xs.max()))
                put("z", arr(zs.min(), zs.max()))
            }
        }
        put("planes", session.planes.size)
        put("keyframes", session.keyframes.size)
        put("duration_s", r2(session.duration))
        put("image_coordinates", "x, y in 0..1 from the image's top-left corner")
        putJsonArray("keyframe_poses") {
            val step = max(1, session.keyframes.size / DIGEST_KEYFRAMES)
            for (i in session.keyframes.indices step step) {
                val k = session.keyframes[i]
                add(poseJson(k.index, k.time, k.pose))
            }
        }
    }

    // ─── Queries ────────────────────────────────────────────────────────────────────────────

    private fun listPlanes(model: RoomModel): ToolOutcome {
        val planes = buildJsonArray {
            for (plane in session.planes) add(planeJson(plane))
        }
        val content = buildJsonObject { put("planes", planes) }.toString()
        return ToolOutcome(content, false, model, caption = "Listed ${session.planes.size} planes")
    }

    private fun planeJson(plane: DebugPlane): JsonObject = buildJsonObject {
        put("id", plane.id)
        put("orientation", SpatialSession.kindName(plane.kind))
        val n = session.planeNormal(plane)
        if (plane.kind == DebugPlaneKind.Wall || abs(n.y) < VERTICAL_NORMAL_Y) {
            val (a, b) = verticalBase(plane)
            putJsonArray("base") {
                add(arr(a.x, a.z))
                add(arr(b.x, b.z))
            }
            put("length_m", r2(a.distanceTo(b)))
            val ys = (0 until plane.vertexCount).map { plane.polygon[it * 3 + 1] - floorY }
            put("bottom_m", r2(ys.min()))
            put("top_m", r2(ys.max()))
        } else {
            put("height_m", r2(session.planeHeight(plane) - floorY))
            putJsonArray("outline") {
                for (i in 0 until plane.vertexCount) add(arr(plane.polygon[i * 3], plane.polygon[i * 3 + 2]))
            }
        }
    }

    /** A vertical plane's footprint: its polygon's extent along its horizontal direction. */
    private fun verticalBase(plane: DebugPlane): Pair<FloorPoint, FloorPoint> {
        val n = session.planeNormal(plane)
        val dir = Vec3(-n.z, 0f, n.x).let { if (it.length() < 1e-4f) Vec3(1f, 0f, 0f) else it.normalized() }
        val c = session.planeCentroid(plane)
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (i in 0 until plane.vertexCount) {
            val t = (plane.polygon[i * 3] - c.x) * dir.x + (plane.polygon[i * 3 + 2] - c.z) * dir.z
            lo = min(lo, t)
            hi = max(hi, t)
        }
        return FloorPoint(c.x + dir.x * lo, c.z + dir.z * lo) to FloorPoint(c.x + dir.x * hi, c.z + dir.z * hi)
    }

    private fun raycast(model: RoomModel, input: JsonObject): ToolOutcome {
        val index = input.int("frame")
        val frame = session.keyframe(index)
            ?: throw ToolInputException("frame must be 0..${session.keyframes.size - 1}")
        val x = input.num("x").coerceIn(0f, 1f)
        val y = input.num("y").coerceIn(0f, 1f)
        val hit = session.raycast(frame, x, y)
        val content = buildJsonObject {
            put("frame", index)
            put("camera", vec(hit.origin))
            if (hit.point != null) {
                put("hit", vec(hit.point))
                put("target", hit.target)
                put("distance_m", r2(hit.distance))
            } else {
                put("hit", JsonPrimitive(null as String?))
                put("target", "nothing within ${SpatialSession.MAX_RAY_M.toInt()} m")
            }
        }.toString()
        val end = hit.point ?: session.rayOf(frame, x, y).let { (o, d) -> o + d * MISS_RAY_DRAWN_M }
        return ToolOutcome(
            content, false, model,
            looks = listOf(Look.Ray(hit.origin, end, hit.point != null)),
            caption = if (hit.point != null) "Ray from frame $index hit ${hit.target}"
                else "Ray from frame $index hit nothing",
        )
    }

    private fun pointsInBox(model: RoomModel, input: JsonObject): ToolOutcome {
        val center = input.vec3("center")
        val size = input.vec3("size")
        if (size.x <= 0f || size.y <= 0f || size.z <= 0f) throw ToolInputException("size must be positive")
        val box = OrientedBox(Vec3(center.x, floorY + center.y, center.z), size, input.num("yaw_deg"))
        val inside = session.pointsIn(box)
        val content = buildJsonObject {
            put("count", inside.count)
            if (inside.count > 0) {
                val ys = (0 until inside.count).map { inside.local[it * 3 + 1] + box.center.y - floorY }.sorted()
                put("height_range_m", arr(ys.first(), ys.last()))
                val xs = (0 until inside.count).map { inside.local[it * 3] }.sorted()
                val zs = (0 until inside.count).map { inside.local[it * 3 + 2] }.sorted()
                put("local_x_extent_m", arr(xs.percentile(LO), xs.percentile(HI)))
                put("local_z_extent_m", arr(zs.percentile(LO), zs.percentile(HI)))
            }
        }.toString()
        return ToolOutcome(
            content, false, model,
            looks = listOf(Look.Probe(box, inside.count >= MIN_OBJECT_POINTS)),
            caption = "${inside.count} points in the probe",
        )
    }

    private fun measure(model: RoomModel, input: JsonObject): ToolOutcome {
        val a = input.vec3("from").let { Vec3(it.x, floorY + it.y, it.z) }
        val b = input.vec3("to").let { Vec3(it.x, floorY + it.y, it.z) }
        val d = b - a
        val content = buildJsonObject {
            put("distance_m", r2(d.length()))
            put("horizontal_m", r2(sqrt(d.x * d.x + d.z * d.z)))
            put("vertical_m", r2(abs(d.y)))
        }.toString()
        return ToolOutcome(
            content,
            false,
            model,
            looks = listOf(Look.Segment(a, b, true)),
            caption = "Measured ${fmt(d.length())} m",
        )
    }

    private fun cameraPoseAt(model: RoomModel, input: JsonObject): ToolOutcome {
        val t = input.num("t")
        val pose = session.cameraPoseAt(t)
        val nearest = session.keyframes.minByOrNull { abs(it.time - t) }
        val content = poseJson(nearest?.index ?: -1, t, pose).toString()
        return ToolOutcome(content, false, model, caption = "Camera pose at ${fmt(t)} s")
    }

    private fun poseJson(index: Int, time: Float, pose: DebugPose): JsonObject = buildJsonObject {
        put("frame", index)
        put("t", r2(time))
        put("position", vec(pose.position))
        val f = pose.forward
        put("forward", arr(f.x, f.y, f.z))
    }

    /**
     * The floor nobody uses: inside ARCore's floor polygon, seen empty from [SEEN_MIN] keyframes,
     * no point between [CLEAR_LOW_M] and [CLEAR_HIGH_M], away from walls and fitted objects by
     * the clearance. Up to three disjoint rectangles, aligned with the first wall, become regions.
     */
    // Geometry scan kept in one pass so the evidence rules read top to bottom; covered by SceneToolsTest.
    @Suppress(
        "CyclomaticComplexMethod",
        "LongMethod",
        "LoopWithTooManyJumpStatements",
        "DestructuringDeclarationWithTooManyEntries",
    )
    private fun freeFloorArea(model: RoomModel, input: JsonObject): ToolOutcome {
        val clearance = input.num("clearance_m").coerceIn(0f, MAX_CLEARANCE_M)
        val floor = session.floorPlane ?: return fail(model, "no floor plane in this session", "No floor found")
        val yaw = model.walls.firstOrNull()?.yawDegrees ?: 0f
        val frame = OrientedBox(Vec3(floor.polygon[0], floorY, floor.polygon[2]), Vec3(1f, 1f, 1f), yaw)
        // The floor polygon in the frame, for the grid's bounds.
        var s0 = Float.MAX_VALUE
        var s1 = -Float.MAX_VALUE
        var t0 = Float.MAX_VALUE
        var t1 = -Float.MAX_VALUE
        for (i in 0 until floor.vertexCount) {
            val l = frame.toLocal(floor.polygon[i * 3], floorY, floor.polygon[i * 3 + 2])
            s0 = min(s0, l.x)
            s1 = max(s1, l.x)
            t0 = min(t0, l.z)
            t1 = max(t1, l.z)
        }
        val cols = ((s1 - s0) / CELL_M).toInt() + 1
        val rows = ((t1 - t0) / CELL_M).toInt() + 1
        val occupied = IntArray(cols * rows)
        for (i in 0 until session.pointCount) {
            val h = session.points[i * 3 + 1] - floorY
            if (h < CLEAR_LOW_M || h > CLEAR_HIGH_M) continue
            val l = frame.toLocal(session.points[i * 3], 0f, session.points[i * 3 + 2])
            val c = floor((l.x - s0) / CELL_M).toInt()
            val r = floor((l.z - t0) / CELL_M).toInt()
            if (c in 0 until cols && r in 0 until rows) occupied[r * cols + c]++
        }
        val keyframes = session.keyframes.filterIndexed { i, _ -> i % SEEN_STRIDE == 0 }
        val free = BooleanArray(cols * rows)
        var unseen = 0
        for (r in 0 until rows) for (c in 0 until cols) {
            val w = frame.toWorld(s0 + (c + 0.5f) * CELL_M, 0f, t0 + (r + 0.5f) * CELL_M)
            if (!session.onFloor(w.x, w.z)) continue
            if (occupied[r * cols + c] >= MIN_CELL_POINTS) continue
            if (model.objects.any { it.box.contains(w.x, it.box.center.y, w.z, clearance + CELL_M / 2f) }) continue
            if (model.walls.any { distanceToWall(
                it,
                w.x,
                w.z,
            ) < clearance + WALL_HALF_THICKNESS_M + CELL_M / 2f }) continue
            val spot = Vec3(w.x, floorY + SEEN_LIFT_M, w.z)
            val seen = keyframes.count { k -> sees(k.pose, spot) && model.objects.none { it.box.blocks(
                k.pose.position,
                spot,
            ) } }
            if (seen < SEEN_MIN) {
                unseen++
                continue
            }
            free[r * cols + c] = true
        }
        lastFreeMap = buildString {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val w = frame.toWorld(s0 + (c + 0.5f) * CELL_M, 0f, t0 + (r + 0.5f) * CELL_M)
                    append(
                        when {
                            free[r * cols + c] -> '.'
                            !session.onFloor(w.x, w.z) -> ' '
                            occupied[r * cols + c] >= MIN_CELL_POINTS -> '#'
                            else -> '?'
                        },
                    )
                }
                append('\n')
            }
        }
        // Up to MAX_REGIONS disjoint rectangles, largest first, so the model can pick the shape it needs.
        val regions = ArrayList<FreeRegion>()
        var next = model
        while (regions.size < MAX_REGIONS) {
            // The roomiest first (an armchair needs width), then the longest run, then the roomiest left.
            val (c0, r0, c1, r1) = bestRectangle(free, cols, rows, roomiest = regions.size != 1) ?: break
            val width = (c1 - c0 + 1) * CELL_M
            val depth = (r1 - r0 + 1) * CELL_M
            if (min(width, depth) < MIN_REGION_M) break
            for (r in r0..r1) for (c in c0..c1) free[r * cols + c] = false
            val centre = frame.toWorld(s0 + (c0 + c1 + 1) * CELL_M / 2f, 0f, t0 + (r0 + r1 + 1) * CELL_M / 2f)
            val (id, after) = next.nextId("F")
            next = after
            regions += FreeRegion(id, OrientedBox(Vec3(centre.x, floorY, centre.z), Vec3(width, 0.02f, depth), yaw))
        }
        if (regions.isEmpty()) return fail(model, "no free floor with ${fmt(clearance)} m clearance", "No free floor")
        val content = buildJsonObject {
            putJsonArray("regions") {
                for (region in regions) addJsonObject {
                    put("id", region.id)
                    put("center", arr(region.box.center.x, region.box.center.z))
                    put("width_m", r2(region.box.size.x))
                    put("depth_m", r2(region.box.size.z))
                    put("yaw_deg", r1(yaw))
                    put("area_m2", r2(region.box.size.x * region.box.size.z))
                }
            }
            put("back", "each region's side nearest the first wall")
            put("unseen_cells", unseen)
            put("cell_m", r2(CELL_M))
        }.toString()
        val first = regions.first().box.size
        return ToolOutcome(
            content, false, next.copy(regions = regions),
            looks = regions.map { Look.Probe(it.box, true) },
            caption = "Free floor ${regions.size} regions, largest ${fmt(first.x)} × ${fmt(first.z)} m",
        )
    }

    /** Whether floor point [p] falls inside keyframe [pose]'s picture, in front, within range. */
    private fun sees(pose: DebugPose, p: Vec3): Boolean {
        val inverse = DebugPose(0f, 0f, 0f, -pose.qx, -pose.qy, -pose.qz, pose.qw)
        val d = p - pose.position
        val l = inverse.rotate(d.x, d.y, d.z)
        val depth = -l.z
        if (depth < MIN_SEEN_DEPTH_M || depth > MAX_SEEN_DEPTH_M) return false
        return abs(l.x / depth) <= session.lens.halfWidthPerDepth && abs(l.y / depth) <= session.lens.halfHeightPerDepth
    }

    // ─── Reconstruction ─────────────────────────────────────────────────────────────────────

    /**
     * A wall is kept only when the cloud backs it: an ARCore vertical plane on its line, or
     * [MIN_WALL_POINTS] points within [WALL_BAND_M] spread over [MIN_WALL_STRETCHES] of its ten
     * stretches. Kept, it is snapped onto that evidence and its height measured from it.
     */
    // Geometry scan kept in one pass so the evidence rules read top to bottom; covered by SceneToolsTest.
    @Suppress("LongMethod", "LoopWithTooManyJumpStatements", "DestructuringDeclarationWithTooManyEntries")
    private fun addWall(model: RoomModel, input: JsonObject): ToolOutcome {
        val from = input.xz("from")
        val to = input.xz("to")
        val proposed = from.distanceTo(to)
        if (proposed < MIN_WALL_M) throw ToolInputException("a wall is at least ${MIN_WALL_M} m long")
        val ux = (to.x - from.x) / proposed
        val uz = (to.z - from.z) / proposed
        // Support: the points near the line.
        val along = ArrayList<Float>()
        val heights = ArrayList<Float>()
        val stretches = BooleanArray(WALL_STRETCHES)
        for (i in 0 until session.pointCount) {
            val h = session.points[i * 3 + 1] - floorY
            if (h < WALL_LOW_M || h > WALL_HIGH_M) continue
            val dx = session.points[i * 3] - from.x
            val dz = session.points[i * 3 + 2] - from.z
            val t = dx * ux + dz * uz
            val off = abs(dx * -uz + dz * ux)
            if (t < 0f || t > proposed || off > WALL_BAND_M) continue
            along += t
            heights += h
            stretches[min(WALL_STRETCHES - 1, (t / proposed * WALL_STRETCHES).toInt())] = true
        }
        val covered = stretches.count { it }
        val plane = session.planes.firstOrNull { coincides(it, from, ux, uz, proposed) }
        val supported = plane != null || (along.size >= MIN_WALL_POINTS && covered >= MIN_WALL_STRETCHES)
        val proposedLook = Look.Segment(Vec3(from.x, floorY, from.z), Vec3(to.x, floorY, to.z), supported)
        if (!supported) {
            val reason = "only ${along.size} points within ${cm(WALL_BAND_M)} cm of it over ${fmt(proposed)} m, " +
                "$covered of $WALL_STRETCHES stretches seen, and no ARCore plane on it: the scan never saw this wall"
            val next = model.copy(rejections = model.rejections + Rejection("wall", from, to, reason))
            return ToolOutcome(
                "rejected: $reason. Leave this side open rather than inventing it.", true, next,
                looks = listOf(proposedLook), caption = "Wall rejected — no points",
            )
        }
        // Snap: onto the plane's line, or the points' own; length from the evidence's extent.
        val (a, b, height, evidence) = if (plane != null) {
            val (pa, pb) = verticalBase(plane)
            val du = pb.x - pa.x
            val dv = pb.z - pa.z
            val len = sqrt(du * du + dv * dv).coerceAtLeast(1e-4f)
            // Keep the model's direction.
            val (sa, sb) = if (du * ux + dv * uz >= 0f) pa to pb else pb to pa
            val sorted = along.sorted()
            val dirx = (sb.x - sa.x) / len
            val dirz = (sb.z - sa.z) / len
            // Extend along the points beyond the plane's patch.
            val project = { t: Float ->
                val px = from.x + ux * t
                val pz = from.z + uz * t
                (px - sa.x) * dirx + (pz - sa.z) * dirz
            }
            val lo = min(0f, if (sorted.isEmpty()) 0f else project(sorted.percentile(EDGE_LO)))
            val hi = max(len, if (sorted.isEmpty()) len else project(sorted.percentile(EDGE_HI)))
            val top = (0 until plane.vertexCount).maxOf { plane.polygon[it * 3 + 1] } - floorY
            val pointsTop = if (heights.isEmpty()) 0f else heights.sorted().percentile(HEIGHT_HI)
            WallFit(
                FloorPoint(sa.x + dirx * lo, sa.z + dirz * lo),
                FloorPoint(sa.x + dirx * hi, sa.z + dirz * hi),
                max(top, pointsTop),
                "ARCore plane ${plane.id} and ${along.size} points",
            )
        } else {
            val sorted = along.sorted()
            val lo = sorted.percentile(EDGE_LO)
            val hi = sorted.percentile(EDGE_HI)
            WallFit(
                FloorPoint(from.x + ux * lo, from.z + uz * lo),
                FloorPoint(from.x + ux * hi, from.z + uz * hi),
                heights.sorted().percentile(HEIGHT_HI),
                "${along.size} points",
            )
        }
        val (id, next) = model.nextId("W")
        val wall = RoomWall(id, a, b, height, evidence)
        val content = buildJsonObject {
            put("id", id)
            put("from", arr(a.x, a.z))
            put("to", arr(b.x, b.z))
            put("length_m", r2(wall.length))
            put("height_m", r2(height))
            put("your_height_estimate_m", r2(input.num("height_m")))
            put("evidence", evidence)
        }.toString()
        return ToolOutcome(
            content, false, next.copy(walls = next.walls + wall, plan = null),
            looks = listOf(proposedLook),
            caption = "Wall $id kept — $evidence",
        )
    }

    private data class WallFit(val a: FloorPoint, val b: FloorPoint, val height: Float, val evidence: String)

    /** Whether vertical [plane] lies on the proposed line: parallel within 10°, within 15 cm, overlapping. */
    private fun coincides(plane: DebugPlane, from: FloorPoint, ux: Float, uz: Float, length: Float): Boolean {
        val n = session.planeNormal(plane)
        if (abs(n.y) > VERTICAL_NORMAL_Y) return false
        val horizontal = sqrt(n.x * n.x + n.z * n.z)
        val parallel = abs(n.x * ux + n.z * uz) / horizontal
        if (parallel > sin(Math.toRadians(MAX_WALL_ANGLE_DEG)).toFloat()) return false
        val c = session.planeCentroid(plane)
        val off = abs((c.x - from.x) * -uz + (c.z - from.z) * ux)
        val t = (c.x - from.x) * ux + (c.z - from.z) * uz
        return off <= WALL_PLANE_OFFSET_M && t > -WALL_PLANE_OFFSET_M && t < length + WALL_PLANE_OFFSET_M
    }

    private fun addOpening(model: RoomModel, input: JsonObject): ToolOutcome {
        val wallId = input.str("wall_id")
        val wall = model.wall(wallId) ?: return fail(
            model,
            "no wall '$wallId'; add_wall first",
            "Opening on unknown wall",
        )
        val kind = OpeningKind.entries.firstOrNull { it.wire == input.str("kind") }
            ?: throw ToolInputException("kind must be window or door")
        val a = input.num("from_m").coerceIn(0f, wall.length)
        val b = input.num("to_m").coerceIn(0f, wall.length)
        val from = min(a, b)
        val to = max(a, b)
        val sill = if (kind == OpeningKind.Door) 0f else input.num("sill_m").coerceIn(0f, wall.height)
        val head = input.num("head_m").coerceIn(sill, wall.height)
        if (to - from < MIN_OPENING_M || head - sill < MIN_OPENING_M) {
            return fail(
                model,
                "opening smaller than ${cm(MIN_OPENING_M)} cm once fitted on ${wall.id}",
                "Opening too small",
            )
        }
        val (id, next) = model.nextId(if (kind == OpeningKind.Window) "N" else "D")
        val opening = RoomOpening(id, wall.id, kind, from, to, sill, head)
        val content = buildJsonObject {
            put("id", id)
            put("wall_id", wall.id)
            put("width_m", r2(opening.width))
            put("height_m", r2(opening.height))
            put("sill_m", r2(sill))
        }.toString()
        return ToolOutcome(
            content,
            false,
            next.copy(openings = next.openings + opening),
            caption = "${kind.wire.replaceFirstChar { it.uppercase() }} $id on ${wall.id}",
        )
    }

    /**
     * An object is fitted, never taken as given: a table-height ARCore plane inside the footprint
     * gives its top and outline; otherwise the points inside the proposal (grown by
     * [FIT_MARGIN_M]) give its extent and height. Too few points, and it is refused.
     */
    // Geometry scan kept in one pass so the evidence rules read top to bottom; covered by SceneToolsTest.
    @Suppress("LongMethod", "DestructuringDeclarationWithTooManyEntries")
    private fun addObject(model: RoomModel, input: JsonObject): ToolOutcome {
        val category = input.str("category")
        if (category !in OBJECT_CATEGORIES) throw ToolInputException("category must be one of $OBJECT_CATEGORIES")
        val c = input.xz("center")
        val size = input.xz("size")
        val yaw = input.num("yaw_deg")
        if (size.x <= 0f || size.z <= 0f) throw ToolInputException("size must be positive")
        val probe = OrientedBox(
            Vec3(c.x, floorY + PROBE_HEIGHT_M / 2 + OBJECT_FLOOR_GAP_M, c.z),
            Vec3(size.x, PROBE_HEIGHT_M, size.z),
            yaw,
        )
        val surface = session.planes.firstOrNull { plane ->
            plane.kind == DebugPlaneKind.Floor &&
                session.planeHeight(plane) - floorY > MIN_SURFACE_M &&
                session.planeCentroid(plane).let { probe.contains(it.x, probe.center.y, it.z, FIT_MARGIN_M) }
        }
        val fitted: Pair<OrientedBox, String>? = if (surface != null) {
            val top = session.planeHeight(surface) - floorY
            fitExtent(
                probe,
                (0 until surface.vertexCount).map { surface.polygon[it * 3] to surface.polygon[it * 3 + 2] },
                0f,
                1f,
            )
                ?.let { (x0, x1, z0, z1) -> boxOf(
                    probe,
                    x0,
                    x1,
                    z0,
                    z1,
                    top,
                ) to "ARCore plane ${surface.id} at ${fmt(top)} m" }
        } else {
            val inside = withoutWalls(model, session.pointsIn(probe, FIT_MARGIN_M))
            if (inside.count < MIN_OBJECT_POINTS) {
                null
            } else {
                val xs = (0 until inside.count).map { inside.local[it * 3] }.sorted()
                val zs = (0 until inside.count).map { inside.local[it * 3 + 2] }.sorted()
                val sill = sillOver(model, probe)
                val ys = (0 until inside.count).map { inside.local[it * 3 + 1] + probe.center.y - floorY }
                    .filter { it < sill }.sorted().ifEmpty { listOf(MIN_OBJECT_M) }
                val box = boxOf(
                    probe,
                    xs.percentile(LO),
                    xs.percentile(HI),
                    zs.percentile(LO),
                    zs.percentile(HI),
                    standingTop(ys),
                )
                val under = if (sill < Float.MAX_VALUE) ", under a sill at ${fmt(sill)} m" else ""
                box to "${inside.count} points$under"
            }
        }
        if (fitted == null) {
            val n = session.pointsIn(probe, FIT_MARGIN_M).count
            return ToolOutcome(
                "rejected: only $n points inside that footprint (needs $MIN_OBJECT_POINTS); nothing the scan saw " +
                    "is there",
                true, model, looks = listOf(Look.Probe(probe, false)), caption = "No $category there — $n points",
            )
        }
        val (box, evidence) = fitted
        val (id, next) = model.nextId("O")
        val content = buildJsonObject {
            put("id", id)
            put("category", category)
            put("center", arr(box.center.x, box.center.z))
            put("width_m", r2(box.size.x))
            put("depth_m", r2(box.size.z))
            put("height_m", r2(box.size.y))
            put("yaw_deg", r1(yaw))
            put("evidence", evidence)
        }.toString()
        return ToolOutcome(
            content, false, next.copy(objects = next.objects + RoomObject(id, category, box, evidence)),
            // The box the points confirmed, not the full-height volume searched: it lights up what was found.
            looks = listOf(Look.Probe(box, true)),
            caption = "${category.replace('_', ' ').replaceFirstChar { it.uppercase() }} $id — $evidence",
        )
    }

    /**
     * [inside] minus the points that belong to a kept wall — on it, or behind it, seen through a
     * window — so an object against a wall is not fitted to the wall.
     */
    private fun withoutWalls(model: RoomModel, inside: BoxPoints): BoxPoints {
        if (model.walls.isEmpty()) return inside
        val eye = session.keyframes.firstOrNull()?.pose?.position ?: return inside
        val kept = ArrayList<Float>(inside.local.size)
        for (i in 0 until inside.count) {
            val w = inside.box.toWorld(inside.local[i * 3], inside.local[i * 3 + 1], inside.local[i * 3 + 2])
            val onWall = model.walls.any { wall -> roomSideDistance(wall, eye, w.x, w.z) < WALL_SKIN_M }
            if (!onWall) {
                kept += inside.local[i * 3]
                kept += inside.local[i * 3 + 1]
                kept += inside.local[i * 3 + 2]
            }
        }
        return BoxPoints(inside.box, kept.toFloatArray())
    }

    /**
     * The lowest sill of a window cut in a kept wall right behind [probe], or [Float.MAX_VALUE]:
     * what stands under a window stops at its sill — the points above are the window's.
     */
    private fun sillOver(model: RoomModel, probe: OrientedBox): Float {
        val eye = session.keyframes.firstOrNull()?.pose?.position ?: return Float.MAX_VALUE
        var sill = Float.MAX_VALUE
        for (opening in model.openings.filter { it.kind == OpeningKind.Window }) {
            val wall = model.wall(opening.wallId) ?: continue
            val along = ((probe.center.x - wall.from.x) * (wall.to.x - wall.from.x) +
                (probe.center.z - wall.from.z) * (wall.to.z - wall.from.z)) / wall.length.coerceAtLeast(1e-4f)
            val off = roomSideDistance(wall, eye, probe.center.x, probe.center.z)
            if (along in opening.from..opening.to && off < SILL_REACH_M) sill = minOf(sill, opening.sill)
        }
        return sill
    }

    /** Signed distance of floor point ([x], [z]) from [wall]'s line, positive on [eye]'s side. */
    private fun roomSideDistance(wall: RoomWall, eye: Vec3, x: Float, z: Float): Float {
        val ux = (wall.to.x - wall.from.x) / wall.length
        val uz = (wall.to.z - wall.from.z) / wall.length
        val side = { px: Float, pz: Float -> (px - wall.from.x) * -uz + (pz - wall.from.z) * ux }
        val sign = if (side(eye.x, eye.z) >= 0f) 1f else -1f
        return side(x, z) * sign
    }

    /**
     * The top of what stands on the floor: the sorted heights climbed from the lowest until a
     * gap of [STACK_GAP_M] — a radiator stops under the window sill, not at the curtain rail.
     */
    private fun standingTop(sorted: List<Float>): Float {
        var top = sorted.first()
        for (h in sorted) {
            if (h - top > STACK_GAP_M) break
            top = h
        }
        return top
    }

    /** Extent (p3–p97 for points, full for a polygon) of world xz points in [probe]'s local frame. */
    private fun fitExtent(probe: OrientedBox, xz: List<Pair<Float, Float>>, lo: Float, hi: Float): List<Float>? {
        if (xz.size < 3) return null
        val local = xz.map { (x, z) -> probe.toLocal(x, probe.center.y, z) }
        val xs = local.map { it.x }.sorted()
        val zs = local.map { it.z }.sorted()
        return listOf(xs.percentile(lo), xs.percentile(hi), zs.percentile(lo), zs.percentile(hi))
    }

    private fun boxOf(probe: OrientedBox, x0: Float, x1: Float, z0: Float, z1: Float, top: Float): OrientedBox {
        val w = (x1 - x0).coerceAtLeast(MIN_OBJECT_M)
        val d = (z1 - z0).coerceAtLeast(MIN_OBJECT_M)
        val h = top.coerceAtLeast(MIN_OBJECT_M)
        val mid = probe.toWorld((x0 + x1) / 2f, 0f, (z0 + z1) / 2f)
        return OrientedBox(Vec3(mid.x, floorY + h / 2f, mid.z), Vec3(w, h, d), probe.yawDegrees)
    }

    // ─── Furnishing ─────────────────────────────────────────────────────────────────────────

    // Geometry scan kept in one pass so the evidence rules read top to bottom; covered by SceneToolsTest.
    @Suppress("LongMethod")
    private fun placeItem(model: RoomModel, input: JsonObject): ToolOutcome {
        val item = CATALOGUE.firstOrNull { it.category == input.str("category") }
            ?: throw ToolInputException("category must be one of ${CATALOGUE.map { it.category }}")
        val regionId = input.str("region_id")
        val region = model.regions.firstOrNull { it.id == regionId }
            ?: return fail(model, "no region '$regionId'; call free_floor_area first", "Unknown region")
        val slot = input.str("slot")
        if (slot !in SLOTS) throw ToolInputException("slot must be one of $SLOTS")
        val rb = region.box
        // Facing: local +Z towards the target's centre, or along the region's own front.
        val targetId = input.str("yaw_toward_id")
        val target = if (targetId.isEmpty()) null else centreOf(model, targetId)
            ?: return fail(model, "nothing called '$targetId' to face", "Unknown target")
        // Where the slot puts it: the item's footprint, turned, pushed into the slot's corner.
        fun boxAt(yaw: Float, cx: Float, cz: Float) =
            OrientedBox(Vec3(cx, floorY + item.height / 2f, cz), Vec3(item.width, item.height, item.depth), yaw)
        val (sx, sz) = when (slot) {
            "back_left" -> -1f to -1f
            "back_right" -> 1f to -1f
            "front_left" -> -1f to 1f
            "front_right" -> 1f to 1f
            else -> 0f to 0f
        }
        // Two passes: the facing depends on the position, the position on the turned footprint.
        var yaw = rb.yawDegrees
        var centre = rb.center
        repeat(2) {
            // Square to the room: the target picks which of the region's four sides the front faces.
            if (target != null) {
                val turn = yawToward(centre.x, centre.z, target.x, target.z) - rb.yawDegrees
                yaw = rb.yawDegrees + QUARTER_TURN * Math.round(normalise(turn) / QUARTER_TURN)
            }
            val probe = boxAt(yaw, 0f, 0f)
            val corners = probe.footprint()
            var hx = 0f
            var hz = 0f
            for (i in 0 until 4) {
                val l = rb.toLocal(corners[i * 2] + rb.center.x, 0f, corners[i * 2 + 1] + rb.center.z)
                hx = max(hx, abs(l.x))
                hz = max(hz, abs(l.z))
            }
            val lx = sx * (rb.size.x / 2f - hx).coerceAtLeast(0f)
            val lz = sz * (rb.size.z / 2f - hz).coerceAtLeast(0f)
            centre = rb.toWorld(lx, 0f, lz)
        }
        val box = boxAt(yaw, centre.x, centre.z)
        if (!fits(rb, box)) {
            return fail(
                model,
                "a ${item.label} (${fmt(item.width)} × ${fmt(item.depth)} m) does not fit in ${region.id} " +
                    "(${fmt(rb.size.x)} × ${fmt(rb.size.z)} m) at that angle",
                "${item.label} does not fit",
            )
        }
        if (item.category != "rug") {
            val standing = model.items.filter { it.category != "rug" }.map { it.id to it.box }
            val clash = (model.objects.map { it.id to it.box } + standing)
                .firstOrNull { (_, other) -> overlaps(box, other) }
            if (clash != null) return fail(
                model,
                "it would overlap ${clash.first}",
                "${item.label} overlaps ${clash.first}",
            )
        }
        val (id, next) = model.nextId("I")
        val placed = PlacedItem(id, item.category, box, DEFAULT_MATERIAL[item.category] ?: MATERIALS.first())
        val content = buildJsonObject {
            put("id", id)
            put("category", item.category)
            put("center", arr(centre.x, centre.z))
            put("width_m", r2(item.width))
            put("depth_m", r2(item.depth))
            put("height_m", r2(item.height))
            put("yaw_deg", r1(yaw))
            put("material", placed.material)
        }.toString()
        return ToolOutcome(content, false, next.copy(items = next.items + placed), caption = "Placed ${item.label} $id")
    }

    private fun centreOf(model: RoomModel, id: String): Vec3? =
        model.boxOf(id)?.center ?: model.wall(id)?.let { w ->
            val m = w.at(w.length / 2f)
            Vec3(m.x, floorY, m.z)
        }

    private fun addDimension(model: RoomModel, input: JsonObject): ToolOutcome {
        val itemId = input.str("item_id")
        val box = model.boxOf(itemId) ?: return fail(model, "no item '$itemId'", "Unknown item")
        val kind = DimensionKind.entries.firstOrNull { it.wire == input.str("kind") }
            ?: throw ToolInputException("kind must be width, depth, height or clearance")
        val lift = floorY + DIMENSION_LIFT_M
        val (from, to) = when (kind) {
            DimensionKind.Width -> box.toWorld(-box.size.x / 2, 0f, box.size.z / 2 + DIMENSION_OFFSET_M)
                .let { Vec3(it.x, lift, it.z) } to box.toWorld(
                    box.size.x / 2,
                    0f,
                    box.size.z / 2 + DIMENSION_OFFSET_M,
                ).let { Vec3(it.x, lift, it.z) }
            DimensionKind.Depth -> box.toWorld(box.size.x / 2 + DIMENSION_OFFSET_M, 0f, -box.size.z / 2)
                .let { Vec3(it.x, lift, it.z) } to box.toWorld(
                    box.size.x / 2 + DIMENSION_OFFSET_M,
                    0f,
                    box.size.z / 2,
                ).let { Vec3(it.x, lift, it.z) }
            DimensionKind.Height -> box.toWorld(box.size.x / 2 + DIMENSION_OFFSET_M, 0f, box.size.z / 2)
                .let { Vec3(it.x, floorY, it.z) to Vec3(it.x, floorY + box.size.y, it.z) }
            DimensionKind.Clearance -> {
                val toId = input.str("to_id")
                val pair = clearance(model, box, toId) ?: return fail(
                    model,
                    "nothing called '$toId' to measure to",
                    "Unknown target",
                )
                pair.first.let { Vec3(it.x, lift, it.z) } to pair.second.let { Vec3(it.x, lift, it.z) }
            }
        }
        val (id, next) = model.nextId("M")
        val dimension = RoomDimension(id, itemId, kind, from, to)
        val content = buildJsonObject {
            put("id", id)
            put("kind", kind.wire)
            put("value_m", r2(dimension.value))
        }.toString()
        return ToolOutcome(
            content,
            false,
            next.copy(dimensions = next.dimensions + dimension),
            caption = "${kind.wire.replaceFirstChar { it.uppercase() }} ${fmt(dimension.value)} m",
        )
    }

    /** The nearest pair of floor points between [box]'s footprint and [toId]'s. */
    private fun clearance(model: RoomModel, box: OrientedBox, toId: String): Pair<FloorPoint, FloorPoint>? {
        val mine = box.footprint().toPoints()
        val theirs = model.boxOf(toId)?.footprint()?.toPoints()
            ?: model.wall(toId)?.let { listOf(it.from, it.to) }
            ?: return null
        var best: Pair<FloorPoint, FloorPoint>? = null
        var bestD = Float.MAX_VALUE
        fun consider(a: FloorPoint, b: FloorPoint) {
            val d = a.distanceTo(b)
            if (d < bestD) {
                bestD = d
                best = a to b
            }
        }
        for (p in mine) theirs.edges().forEach { (a, b) -> consider(p, closestOnSegment(p, a, b)) }
        for (p in theirs) mine.edges().forEach { (a, b) -> consider(closestOnSegment(p, a, b), p) }
        return best
    }

    private fun setMaterial(model: RoomModel, input: JsonObject): ToolOutcome {
        val itemId = input.str("item_id")
        val material = input.str("material")
        if (material !in MATERIALS) throw ToolInputException("material must be one of $MATERIALS")
        val item = model.items.firstOrNull { it.id == itemId } ?: return fail(
            model,
            "no placed item '$itemId'",
            "Unknown item",
        )
        val next = model.copy(items = model.items.map { if (it.id == itemId) it.copy(material = material) else it })
        return ToolOutcome(
            """{"id":"${item.id}","material":"$material"}""",
            false,
            next,
            caption = "${item.id} in $material",
        )
    }

    private fun removeNode(model: RoomModel, input: JsonObject): ToolOutcome {
        val id = input.str("id")
        val next = model.remove(id) ?: return fail(model, "nothing called '$id'", "Unknown id")
        return ToolOutcome("""{"removed":"$id"}""", false, next, caption = "Removed $id")
    }

    private fun computeTakeoff(model: RoomModel): ToolOutcome {
        val takeoff = Takeoff.of(model)
        return ToolOutcome(takeoff.json().toString(), false, model, caption = "Takeoff computed")
    }

    private fun fail(model: RoomModel, message: String, caption: String) =
        ToolOutcome("error: $message", true, model, caption = caption)

    companion object {
        const val LIST_PLANES = "list_planes"
        const val RAYCAST = "raycast"
        const val POINTS_IN_BOX = "points_in_box"
        const val MEASURE = "measure"
        const val CAMERA_POSE_AT = "camera_pose_at"
        const val FREE_FLOOR_AREA = "free_floor_area"
        const val ADD_WALL = "add_wall"
        const val ADD_OPENING = "add_opening"
        const val ADD_OBJECT = "add_object"
        const val PLACE_ITEM = "place_item"
        const val ADD_DIMENSION = "add_dimension"
        const val SET_MATERIAL = "set_material"
        const val REMOVE_NODE = "remove_node"
        const val COMPUTE_TAKEOFF = "compute_takeoff"
        const val PROPOSE_ROOM = "propose_room"
        const val BUILD_ROOM = "build_room"
        const val ACCEPT_OPENING = "accept_opening"
        const val ACCEPT_OBJECT = "accept_object"

        val OBJECT_CATEGORIES = listOf(
            "bed", "sofa", "desk", "table", "chair", "office_chair", "plant", "radiator", "shelf", "cabinet", "other",
        )
        val CATALOGUE = listOf(
            CatalogueItem("armchair", "Armchair", 0.68f, 1.00f, 0.82f),
            CatalogueItem("floor_lamp", "Floor lamp", 0.30f, 1.60f, 0.30f),
            CatalogueItem("rug", "Rug", 1.60f, 0.01f, 2.30f),
            CatalogueItem("bookshelf", "Bookshelf", 0.80f, 1.80f, 0.30f),
            CatalogueItem("side_table", "Side table", 0.45f, 0.50f, 0.45f),
        )
        val SLOTS = listOf("center", "back_left", "back_right", "front_left", "front_right")
        val MATERIALS = listOf("oak", "walnut", "linen", "charcoal", "terracotta", "sage")
        private val DEFAULT_MATERIAL = mapOf(
            "armchair" to "linen",
            "floor_lamp" to "charcoal",
            "rug" to "sage",
            "bookshelf" to "oak",
            "side_table" to "walnut",
        )

        // Evidence thresholds. Tuned on the bundled session; the tests pin them.
        const val MIN_WALL_POINTS = 40
        const val MIN_WALL_STRETCHES = 5
        const val WALL_STRETCHES = 10
        const val WALL_BAND_M = 0.08f
        const val MIN_OBJECT_POINTS = 15
        private const val WALL_LOW_M = 0.15f
        private const val WALL_HIGH_M = 2.6f
        private const val WALL_PLANE_OFFSET_M = 0.15f
        private const val MAX_WALL_ANGLE_DEG = 10.0
        private const val MIN_WALL_M = 0.3f
        private const val VERTICAL_NORMAL_Y = 0.3f
        private const val MIN_OPENING_M = 0.3f
        private const val PROBE_HEIGHT_M = 2.2f
        private const val OBJECT_FLOOR_GAP_M = 0.03f
        private const val FIT_MARGIN_M = 0.1f
        private const val MIN_SURFACE_M = 0.3f
        private const val MIN_OBJECT_M = 0.05f
        private const val LO = 0.03f
        private const val HI = 0.97f
        private const val EDGE_LO = 0.02f
        private const val EDGE_HI = 0.98f
        private const val HEIGHT_HI = 0.95f
        const val CELL_M = 0.1f
        private const val CLEAR_LOW_M = 0.12f
        private const val MIN_CELL_POINTS = 2
        private const val STACK_GAP_M = 0.25f
        private const val SILL_REACH_M = 0.6f
        private const val QUARTER_TURN = 90f
        private const val MAX_REGIONS = 3
        private const val MIN_REGION_M = 0.5f
        private const val CLEAR_HIGH_M = 1.8f
        private const val MAX_CLEARANCE_M = 1f
        private const val SEEN_MIN = 2
        private const val SEEN_STRIDE = 2
        private const val SEEN_LIFT_M = 0.02f
        private const val WALL_SKIN_M = 0.05f
        private const val MIN_SEEN_DEPTH_M = 0.2f
        private const val MAX_SEEN_DEPTH_M = 5f
        const val WALL_HALF_THICKNESS_M = 0.05f
        private const val MISS_RAY_DRAWN_M = 3f
        private const val DIMENSION_LIFT_M = 0.02f
        private const val DIMENSION_OFFSET_M = 0.12f
        private const val DIGEST_KEYFRAMES = 8

        /** The tool specs, strict JSON Schema: every property required, nothing else allowed. */
        val SPECS: List<AiToolSpec> = listOf(
            spec(
                LIST_PLANES,
                "List ARCore's detected planes: vertical ones by their base segment and top, " +
                    "horizontal ones by height and outline.",
            ),
            spec(
                RAYCAST,
                "Cast a ray through image point (x, y) of keyframe `frame` against the planes and the point cloud; " +
                    "returns the 3D hit.",
                "frame" to integer(), "x" to number(), "y" to number(),
            ),
            spec(
                POINTS_IN_BOX,
                "Count the cloud's points inside a box: center [x, height, z], size [width, height, depth], turned " +
                    "yaw_deg about up.",
                "center" to numbers(), "size" to numbers(), "yaw_deg" to number(),
            ),
            spec(MEASURE, "Distance between two points [x, height, z].", "from" to numbers(), "to" to numbers()),
            spec(
                CAMERA_POSE_AT,
                "The recording camera's pose at time t seconds, and the nearest keyframe.",
                "t" to number(),
            ),
            spec(
                FREE_FLOOR_AREA,
                "Find the largest empty floor rectangle the scan saw, keeping clearance_m from walls and objects. " +
                    "Returns a region id to place items in.",
                "clearance_m" to number(),
            ),
            spec(
                ADD_WALL,
                "Propose a wall from floor point [x, z] to [x, z]. Code checks it against the points and planes, " +
                    "snaps it and measures its height; a wall the scan never saw is rejected.",
                "from" to numbers(), "to" to numbers(), "height_m" to number(),
            ),
            spec(
                ADD_OPENING,
                "Cut a window or door in a kept wall, from_m..to_m along it from its start, sill_m..head_m above " +
                    "the floor.",
                "wall_id" to string(), "kind" to enumOf(OpeningKind.entries.map { it.wire }),
                "from_m" to number(), "to_m" to number(), "sill_m" to number(), "head_m" to number(),
            ),
            spec(
                ADD_OBJECT,
                "Propose an existing object by its rough footprint: center [x, z], size [width, depth], yaw_deg. " +
                    "Code fits it to the points or a table-height plane and measures its height.",
                "category" to enumOf(OBJECT_CATEGORIES),
                "center" to numbers(),
                "size" to numbers(),
                "yaw_deg" to number(),
            ),
            spec(
                PLACE_ITEM,
                "Place a catalogue item at its real size in a free region's slot ('back' is the side nearest the " +
                    "first wall). yaw_toward_id turns its front to the region side nearest an object, item or wall " +
                    "(furniture stays square to the room); empty string faces the region's front.",
                "category" to enumOf(CATALOGUE.map { it.category }), "region_id" to string(), "slot" to enumOf(SLOTS),
                "yaw_toward_id" to string(),
            ),
            spec(
                ADD_DIMENSION,
                "Show a dimension of a placed item or object: its width, depth, height, or its clearance to to_id " +
                    "(empty string otherwise).",
                "item_id" to string(), "kind" to enumOf(DimensionKind.entries.map { it.wire }), "to_id" to string(),
            ),
            spec(
                SET_MATERIAL,
                "Change a placed item's finish.",
                "item_id" to string(),
                "material" to enumOf(MATERIALS),
            ),
            spec(REMOVE_NODE, "Remove a wall, opening, object, item or dimension by id.", "id" to string()),
            spec(
                COMPUTE_TAKEOFF,
                "Compute the quantities: wall areas net of openings, openings, objects, placed items.",
            ),
            spec(
                PROPOSE_ROOM,
                "Read the whole scan and list what the code found: wall lines (C1, C2…) with their floor segment, " +
                    "height, point support and the gaps in them (G1, G2… guessed door, window, occluded or " +
                    "unknown), furniture clusters (K1, K2…) as boxes, and the ceiling height. Call it first; " +
                    "nothing is built yet.",
            ),
            spec(
                BUILD_ROOM,
                "Build the room from the proposed wall lines that bound it (their C ids). Code closes them into " +
                    "a polygon when they meet, measures every wall, the area, the perimeter; replaces earlier walls.",
                "wall_ids" to AiSchemas.strings(""),
            ),
            spec(
                ACCEPT_OPENING,
                "Turn a proposed gap (its G id) into a door or window in the built wall under it. A gap hidden " +
                    "behind furniture is refused.",
                "gap_id" to string(), "kind" to enumOf(OpeningKind.entries.map { it.wire }),
            ),
            spec(
                ACCEPT_OBJECT,
                "Label a proposed furniture cluster (its K id) with a category; its box, size and height come from " +
                    "the points. Calling it again on the same cluster relabels it.",
                "cluster_id" to string(), "category" to enumOf(OBJECT_CATEGORIES),
            ),
        )

        // The strict schema every provider accepts, built by the shared layer (sceneview-ai).
        private fun spec(name: String, description: String, vararg properties: Pair<String, JsonObject>) =
            AiToolSpec(name, description, AiSchemas.objectSchema(*properties))

        private fun number() = AiSchemas.number("")
        private fun integer() = AiSchemas.integer("")
        private fun string() = AiSchemas.string("")
        private fun numbers() = AiSchemas.numbers("")
        private fun enumOf(values: List<String>) = AiSchemas.enumOf(values, "")

        /** [deg] folded into -180..180. */
        fun normalise(deg: Float): Float = ((deg % 360f) + 540f) % 360f - 180f

        fun yawToward(x: Float, z: Float, tx: Float, tz: Float): Float =
            Math.toDegrees(atan2((tx - x).toDouble(), (tz - z).toDouble())).toFloat()

        fun distanceToWall(wall: RoomWall, x: Float, z: Float): Float =
            FloorPoint(x, z).distanceTo(closestOnSegment(FloorPoint(x, z), wall.from, wall.to))

        fun closestOnSegment(p: FloorPoint, a: FloorPoint, b: FloorPoint): FloorPoint {
            val dx = b.x - a.x
            val dz = b.z - a.z
            val l2 = dx * dx + dz * dz
            if (l2 < 1e-8f) return a
            val t = (((p.x - a.x) * dx + (p.z - a.z) * dz) / l2).coerceIn(0f, 1f)
            return FloorPoint(a.x + dx * t, a.z + dz * t)
        }

        /** Whether [inner]'s footprint lies inside [outer]'s, a centimetre of slack. */
        fun fits(outer: OrientedBox, inner: OrientedBox): Boolean {
            val c = inner.footprint()
            for (i in 0 until 4) {
                val l = outer.toLocal(c[i * 2], outer.center.y, c[i * 2 + 1])
                if (abs(l.x) > outer.size.x / 2 + FIT_SLACK_M || abs(l.z) > outer.size.z / 2 + FIT_SLACK_M) return false
            }
            return true
        }

        /** Separating-axis test on two footprints. */
        fun overlaps(a: OrientedBox, b: OrientedBox): Boolean {
            val pa = a.footprint().toPoints()
            val pb = b.footprint().toPoints()
            for (poly in listOf(pa, pb)) for ((p, q) in poly.edges()) {
                val nx = -(q.z - p.z)
                val nz = q.x - p.x
                val ra = pa.map { it.x * nx + it.z * nz }
                val rb = pb.map { it.x * nx + it.z * nz }
                if (ra.max() <= rb.min() + FIT_SLACK_M || rb.max() <= ra.min() + FIT_SLACK_M) return false
            }
            return true
        }

        private const val FIT_SLACK_M = 0.01f

        /**
         * The best all-true axis rectangle, (c0, r0, c1, r1), or `null`: the roomiest (largest
         * shorter side, then area) when [roomiest], else the largest area. Every row band is
         * scanned for runs of fully free columns — rows² × cols, a few thousand cells at most.
         */
        @Suppress("NestedBlockDepth") // the classic row-band × run scan; flattening it hides the bounds.
        fun bestRectangle(grid: BooleanArray, cols: Int, rows: Int, roomiest: Boolean): List<Int>? {
            var result: List<Int>? = null
            var bestKey = -1L
            val columnFree = BooleanArray(cols)
            for (r0 in 0 until rows) {
                columnFree.fill(true)
                for (r1 in r0 until rows) {
                    val height = r1 - r0 + 1
                    var run = 0
                    for (c in 0..cols) {
                        if (c < cols) columnFree[c] = columnFree[c] && grid[r1 * cols + c]
                        if (c < cols && columnFree[c]) {
                            run++
                            continue
                        }
                        if (run > 0) {
                            val area = run.toLong() * height
                            val key = if (roomiest) min(run, height) * AREA_RANK + area else area
                            if (key > bestKey) {
                                bestKey = key
                                result = listOf(c - run, r0, c - 1, r1)
                            }
                        }
                        run = 0
                    }
                }
            }
            return result
        }

        private const val AREA_RANK = 1_000_000L

        fun r2(v: Float): Double = (v * 100f).roundToInt() / 100.0
        fun r1(v: Float): Double = (v * 10f).roundToInt() / 10.0
        fun fmt(v: Float): String = "%.2f".format(java.util.Locale.ROOT, v)
        private fun cm(v: Float): Int = (v * 100f).roundToInt()
        private fun arr(vararg v: Float): JsonArray = buildJsonArray { v.forEach { add(r2(it)) } }
    }

    /** [x, height above floor, z]. */
    private fun vec(v: Vec3): JsonArray = arr(v.x, v.y - floorY, v.z)
}

/** The quantities of a [RoomModel], computed: what a quote or an order is made from. */
data class Takeoff(
    val walls: List<Line>,
    val openings: List<Line>,
    val objects: List<Line>,
    val items: List<Line>,
    val wallArea: Float,
    val openingArea: Float,
) {
    data class Line(val id: String, val label: String, val detail: String)

    val netWallArea: Float get() = wallArea - openingArea

    fun json(): JsonObject = buildJsonObject {
        put("wall_area_m2", SceneToolExecutor.r2(wallArea))
        put("opening_area_m2", SceneToolExecutor.r2(openingArea))
        put("net_wall_area_m2", SceneToolExecutor.r2(netWallArea))
        for ((key, lines) in listOf("walls" to walls, "openings" to openings, "objects" to objects, "items" to items)) {
            putJsonArray(key) {
                lines.forEach { line -> addJsonObject { put("id", line.id); put("label", line.label); put(
                    "detail",
                    line.detail,
                ) } }
            }
        }
    }

    companion object {
        fun of(model: RoomModel): Takeoff {
            val f = SceneToolExecutor::fmt
            return Takeoff(
                walls = model.walls.map { Line(it.id, "Wall", "${f(it.length)} × ${f(it.height)} m") },
                openings = model.openings.map {
                    Line(
                        it.id,
                        it.kind.wire.replaceFirstChar { c -> c.uppercase() },
                        "${f(it.width)} × ${f(it.height)} m",
                    )
                },
                objects = model.objects.map {
                    Line(
                        it.id,
                        it.category.replace('_', ' ').replaceFirstChar { c -> c.uppercase() },
                        "${f(it.box.size.x)} × ${f(it.box.size.z)} × ${f(it.box.size.y)} m",
                    )
                },
                items = model.items.map { placed ->
                    val label = SceneToolExecutor.CATALOGUE
                        .firstOrNull { it.category == placed.category }?.label ?: placed.category
                    val size = placed.box.size
                    Line(placed.id, label, "${f(size.x)} × ${f(size.z)} × ${f(size.y)} m · ${placed.material}")
                },
                wallArea = model.walls.sumOf { (it.length * it.height).toDouble() }.toFloat(),
                openingArea = model.openings.sumOf { (it.width * it.height).toDouble() }.toFloat(),
            )
        }
    }
}

class ToolInputException(message: String) : IllegalArgumentException(message)

private fun JsonObject.field(name: String): JsonElement = this[name] ?: throw ToolInputException("missing '$name'")

internal fun JsonObject.num(name: String): Float =
    (field(name) as? JsonPrimitive)?.doubleOrNull?.toFloat() ?: throw ToolInputException("'$name' must be a number")

internal fun JsonObject.int(name: String): Int =
    (field(name) as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.roundToInt() }
        ?: throw ToolInputException("'$name' must be an integer")

internal fun JsonObject.str(name: String): String =
    (field(name) as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: throw ToolInputException("'$name' must be a string")

private fun JsonObject.numbers(name: String, count: Int): FloatArray {
    val values = runCatching { field(name).jsonArray }.getOrNull()
        ?.map { (it as? JsonPrimitive)?.doubleOrNull?.toFloat() }
    if (values == null || values.size != count || values.any { it == null }) {
        throw ToolInputException("'$name' must be an array of $count numbers")
    }
    return values.filterNotNull().toFloatArray()
}

private fun JsonObject.vec3(name: String): Vec3 = numbers(name, 3).let { Vec3(it[0], it[1], it[2]) }

private fun JsonObject.xz(name: String): FloorPoint = numbers(name, 2).let { FloorPoint(it[0], it[1]) }

private fun List<Float>.percentile(p: Float): Float =
    if (isEmpty()) 0f else this[((size - 1) * p).roundToInt().coerceIn(0, size - 1)]

private fun FloatArray.toPoints(): List<FloorPoint> = List(size / 2) { FloorPoint(this[it * 2], this[it * 2 + 1]) }

private fun List<FloorPoint>.edges(): List<Pair<FloorPoint, FloorPoint>> =
    if (size == 2) listOf(this[0] to this[1]) else indices.map { this[it] to this[(it + 1) % size] }
