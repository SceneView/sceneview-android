package io.github.sceneview.demo.ai.scene

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The reconstruction against the synthetic room, whose dimensions are known exactly: the code
 * must find the four walls, close them into the 14.4 m² plan, read the 2.6 m ceiling and box the
 * furniture — every number within 3 % — and refuse what it cannot see.
 */
class RoomReconstructionTest {

    private val executor = SceneToolExecutor(SyntheticScanSession.session)
    private val proposal get() = executor.proposal

    private fun input(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun run(name: String, json: String = "{}", model: RoomModel = RoomModel()) =
        executor.execute(model, name, input(json))

    private fun json(outcome: ToolOutcome): JsonObject = Json.parseToJsonElement(outcome.content).jsonObject

    private fun assertWithin(expected: Float, actual: Float, tolerance: Float, what: String) =
        assertTrue("$what: expected $expected ± $tolerance, got $actual", abs(expected - actual) <= tolerance)

    private fun assertWithinPercent(expected: Float, actual: Float, percent: Float, what: String) =
        assertWithin(expected, actual, expected * percent / 100f, what)

    /** `x` for a wall line along z (constant x), `z` for one along x, with the constant coordinate. */
    private fun WallCandidate.axisAndCoord(): Pair<Char, Float> =
        if (abs(from.x - to.x) < abs(from.z - to.z)) 'x' to (from.x + to.x) / 2 else 'z' to (from.z + to.z) / 2

    private fun wallAt(axis: Char, coord: Float): WallCandidate =
        proposal.walls.first { val (a, c) = it.axisAndCoord(); a == axis && abs(c - coord) < COORD_TOLERANCE_M }

    private fun clusterNear(x: Float, z: Float): ObjectCandidate =
        proposal.objects.minByOrNull { abs(it.box.center.x - x) + abs(it.box.center.z - z) }
            ?: error("no cluster proposed")

    @Test
    fun `the fused synthetic scan is a dense cloud with normals`() {
        val session = SyntheticScanSession.session
        assertTrue("points: ${session.pointCount}", session.pointCount > 50_000)
        assertNotNull(session.normals)
        assertEquals(session.points.size, session.normals?.size)
    }

    @Test
    fun `the ceiling is read at 2,6 m and the frame is square to the room`() {
        val ceiling = proposal.ceilingHeight
        assertNotNull("ceiling not seen: ${proposal.notes}", ceiling)
        assertWithinPercent(CEILING_M, checkNotNull(ceiling), TOLERANCE_PERCENT, "ceiling height")
        assertTrue("yaw ${proposal.yawDegrees}", abs(proposal.yawDegrees) < YAW_TOLERANCE_DEG)
    }

    @Test
    fun `four wall lines are proposed, one per side, at the room's bounds`() {
        val found = proposal.walls.map { it.axisAndCoord() }
        assertEquals("walls: $found", 4, proposal.walls.size)
        wallAt('x', -HALF_WIDTH_M)
        wallAt('x', HALF_WIDTH_M)
        wallAt('z', -HALF_DEPTH_M)
        wallAt('z', HALF_DEPTH_M)
        proposal.walls.forEach {
            assertTrue("${it.id} height ${it.height}", it.height >= RoomReconstruction.WALL_MIN_HEIGHT_M)
            assertTrue("${it.id} height ${it.height}", it.height <= CEILING_M * (1 + TOLERANCE_PERCENT / 100f))
            assertTrue("${it.id} support ${it.support}", it.support > MIN_SUPPORT)
            val expected = if (it.axisAndCoord().first == 'x') DEPTH_M else WIDTH_M
            // The scanned extent stops short of the corners; build_room closes the lines, not the extents.
            assertWithin(expected, it.length, EXTENT_TOLERANCE_M, "${it.id} scanned extent")
        }
    }

    @Test
    fun `build_room closes the four lines into the 14,4 m² plan`() {
        val ids = proposal.walls.map { "\"${it.id}\"" }
        val outcome = run(SceneToolExecutor.BUILD_ROOM, "{\"wall_ids\": [${ids.joinToString()}]}")
        assertFalse(outcome.content, outcome.isError)
        val model = outcome.model
        val plan = checkNotNull(model.plan)
        assertTrue(plan.closed)
        assertEquals(4, model.walls.size)
        assertEquals(4, plan.corners.size)
        assertWithinPercent(AREA_M2, checkNotNull(plan.area), TOLERANCE_PERCENT, "area")
        assertWithinPercent(PERIMETER_M, plan.perimeter, TOLERANCE_PERCENT, "perimeter")
        assertWithinPercent(CEILING_M, checkNotNull(plan.ceilingHeight), TOLERANCE_PERCENT, "ceiling in plan")
        val lengths = model.walls.map { it.length }.sorted()
        assertWithinPercent(DEPTH_M, lengths[0], TOLERANCE_PERCENT, "short wall")
        assertWithinPercent(DEPTH_M, lengths[1], TOLERANCE_PERCENT, "short wall")
        assertWithinPercent(WIDTH_M, lengths[2], TOLERANCE_PERCENT, "long wall")
        assertWithinPercent(WIDTH_M, lengths[3], TOLERANCE_PERCENT, "long wall")
        for (corner in plan.corners) {
            assertWithin(HALF_WIDTH_M, abs(corner.x), CORNER_TOLERANCE_M, "corner x")
            assertWithin(HALF_DEPTH_M, abs(corner.z), CORNER_TOLERANCE_M, "corner z")
        }
        assertEquals(listOf("W1", "W2", "W3", "W4"), model.walls.map { it.id })
        assertTrue(model.walls.all { it.evidence.startsWith("C") && "points" in it.evidence })
        val content = json(outcome)
        assertTrue(content.getValue("closed").jsonPrimitive.boolean)
        assertWithinPercent(AREA_M2, content.getValue("area_m2").jsonPrimitive.float, TOLERANCE_PERCENT, "area_m2")
        assertEquals(4, content.getValue("corners").jsonArray.size)
        assertTrue(outcome.caption, outcome.caption.startsWith("Room built: 4 walls, 14"))
    }

    @Test
    fun `three lines leave the outline open and the area unknown`() {
        val ids = proposal.walls.drop(1).map { "\"${it.id}\"" }
        val outcome = run(SceneToolExecutor.BUILD_ROOM, "{\"wall_ids\": [${ids.joinToString()}]}")
        assertFalse(outcome.content, outcome.isError)
        val plan = checkNotNull(outcome.model.plan)
        assertFalse(plan.closed)
        assertNull(plan.area)
        assertTrue(plan.corners.isEmpty())
        assertEquals(3, outcome.model.walls.size)
        assertNull(json(outcome)["area_m2"])
        assertTrue(json(outcome).getValue("note").jsonPrimitive.content.contains("do not close"))
    }

    @Test
    fun `the room has no door and no window - the only gaps are the walls hidden behind furniture`() {
        val gaps = proposal.walls.flatMap { w -> w.gaps.map { w to it } }
        for ((wall, gap) in gaps) {
            val hiddenOrUnknown = gap.guess == "occluded" || gap.guess == "unknown"
            assertTrue("${gap.id} on ${wall.id} guessed ${gap.guess}", hiddenOrUnknown)
        }
        // The cabinet stands against the right wall: the wall behind it is never seen, up to the cabinet's top.
        val right = wallAt('x', HALF_WIDTH_M)
        val behindCabinet = right.gaps.firstOrNull { g ->
            val lo = min(g.from.z, g.to.z)
            val hi = max(g.from.z, g.to.z)
            g.guess == "occluded" && lo < CABINET_Z1 && hi > CABINET_Z0
        }
        assertNotNull("no occluded gap behind the cabinet: ${right.gaps}", behindCabinet)
        assertTrue("${behindCabinet?.high}", checkNotNull(behindCabinet).high <= CABINET_HEIGHT_M + GAP_TOP_SLACK_M)
        // Behind the sofa, the back wall is hidden up to the sofa's back.
        val back = wallAt('z', -HALF_DEPTH_M)
        val behindSofa = back.gaps.firstOrNull { g ->
            val lo = min(g.from.x, g.to.x)
            val hi = max(g.from.x, g.to.x)
            g.guess == "occluded" && lo < SOFA_X1 && hi > SOFA_X0
        }
        assertNotNull("no occluded gap behind the sofa: ${back.gaps}", behindSofa)
    }

    @Test
    fun `the furniture is boxed where the synthetic pieces stand`() {
        assertTrue("clusters: ${proposal.objects.map { it.id }}", proposal.objects.size >= 4)
        val sofa = clusterNear(SOFA_X, SOFA_Z)
        assertBox(sofa, SOFA_X, SOFA_Z, SOFA_W, SOFA_D, SOFA_H, "sofa")
        val table = clusterNear(TABLE_X, TABLE_Z)
        assertBox(table, TABLE_X, TABLE_Z, TABLE_W, TABLE_D, TABLE_H, "table")
        val cabinet = clusterNear(CABINET_X, CABINET_Z)
        assertBox(cabinet, CABINET_X, CABINET_Z, CABINET_W, CABINET_D, CABINET_HEIGHT_M, "cabinet")
        assertEquals("cabinet against the right wall", wallAt('x', HALF_WIDTH_M).id, cabinet.againstWall)
        val ball = clusterNear(BALL_X, BALL_Z)
        assertBox(ball, BALL_X, BALL_Z, BALL_D, BALL_D, BALL_H, "ball")
        assertEquals(4, setOf(sofa.id, table.id, cabinet.id, ball.id).size)
    }

    private fun assertBox(k: ObjectCandidate, x: Float, z: Float, w: Float, d: Float, h: Float, what: String) {
        assertWithin(x, k.box.center.x, CENTER_TOLERANCE_M, "$what (${k.id}) centre x")
        assertWithin(z, k.box.center.z, CENTER_TOLERANCE_M, "$what (${k.id}) centre z")
        // A box is reported at a yaw the code picked, so the footprint may come back turned by a quarter.
        assertWithin(max(w, d), max(k.box.size.x, k.box.size.z), SIZE_TOLERANCE_M, "$what (${k.id}) long side")
        assertWithin(min(w, d), min(k.box.size.x, k.box.size.z), SIZE_TOLERANCE_M, "$what (${k.id}) short side")
        assertWithin(h, k.box.size.y, HEIGHT_TOLERANCE_M, "$what (${k.id}) height")
        assertWithin(h / 2, k.box.center.y, HEIGHT_TOLERANCE_M, "$what (${k.id}) centre height")
    }

    @Test
    fun `propose_room reports the proposal and draws it`() {
        val outcome = run(SceneToolExecutor.PROPOSE_ROOM)
        assertFalse(outcome.isError)
        val content = json(outcome)
        assertEquals(4, content.getValue("walls").jsonArray.size)
        assertEquals(proposal.objects.size, content.getValue("objects").jsonArray.size)
        val ceiling = content.getValue("ceiling_height_m").jsonPrimitive.float
        assertWithinPercent(CEILING_M, ceiling, TOLERANCE_PERCENT, "ceiling")
        val wall = content.getValue("walls").jsonArray[0].jsonObject
        assertEquals(proposal.walls[0].id, wall.getValue("id").jsonPrimitive.content)
        assertTrue(wall.containsKey("gaps"))
        assertEquals(4 + proposal.objects.size, outcome.looks.size)
        assertTrue(outcome.caption, outcome.caption.startsWith("Proposed 4 walls"))
        assertTrue(outcome.model.isEmpty)
    }

    @Test
    fun `accept_object labels a cluster from its points and relabels it in place`() {
        val sofa = clusterNear(SOFA_X, SOFA_Z)
        val first = run(SceneToolExecutor.ACCEPT_OBJECT, "{\"cluster_id\": \"${sofa.id}\", \"category\": \"sofa\"}")
        assertFalse(first.content, first.isError)
        assertEquals(1, first.model.objects.size)
        val o = first.model.objects[0]
        assertEquals("O1", o.id)
        assertEquals("sofa", o.category)
        assertEquals(sofa.box, o.box)
        assertEquals("cluster ${sofa.id}, ${sofa.points} points", o.evidence)
        assertTrue(first.caption, first.caption.startsWith("sofa O1 — "))
        val again = run(
            SceneToolExecutor.ACCEPT_OBJECT, "{\"cluster_id\": \"${sofa.id}\", \"category\": \"bed\"}", first.model,
        )
        assertFalse(again.isError)
        assertEquals(1, again.model.objects.size)
        assertEquals("O1", again.model.objects[0].id)
        assertEquals("bed", again.model.objects[0].category)
        assertEquals("O1", json(again).getValue("id").jsonPrimitive.content)
    }

    @Test
    fun `what the proposal does not list is refused`() {
        assertTrue(run(SceneToolExecutor.BUILD_ROOM, "{\"wall_ids\": []}").isError)
        assertTrue(run(SceneToolExecutor.BUILD_ROOM, "{\"wall_ids\": [\"C99\"]}").content.contains("unknown wall id"))
        assertTrue(run(SceneToolExecutor.BUILD_ROOM, "{\"wall_ids\": \"C1\"}").content.contains("array of strings"))
        assertTrue(run(SceneToolExecutor.ACCEPT_OBJECT, "{\"cluster_id\": \"K99\", \"category\": \"sofa\"}").isError)
        val cluster = proposal.objects[0].id
        val badCategory = run(SceneToolExecutor.ACCEPT_OBJECT, "{\"cluster_id\": \"$cluster\", \"category\": \"boat\"}")
        assertTrue(badCategory.content, badCategory.content.contains("category must be one of"))
        assertTrue(run(SceneToolExecutor.ACCEPT_OPENING, "{\"gap_id\": \"G99\", \"kind\": \"door\"}").isError)
    }

    @Test
    fun `a gap hidden behind furniture is never an opening, and a gap needs its wall built first`() {
        val occluded = proposal.walls.flatMap { it.gaps }.first { it.guess == "occluded" }
        val refused = run(SceneToolExecutor.ACCEPT_OPENING, "{\"gap_id\": \"${occluded.id}\", \"kind\": \"door\"}")
        assertTrue(refused.isError)
        assertTrue(refused.content, refused.content.contains("hidden"))
        assertTrue(refused.model.openings.isEmpty())
        val unknown = proposal.walls.flatMap { it.gaps }.firstOrNull { it.guess == "unknown" } ?: return
        val noWall = run(SceneToolExecutor.ACCEPT_OPENING, "{\"gap_id\": \"${unknown.id}\", \"kind\": \"window\"}")
        assertTrue(noWall.content, noWall.content.contains("call build_room first"))
    }

    @Test
    fun `closing lines that overshoot or stop short meets them at the corners`() {
        val a = FloorPoint(-1.9f, -1f) to FloorPoint(2.1f, -1f) // along x, overshooting on the right
        val b = FloorPoint(2f, -0.9f) to FloorPoint(2f, 0.95f) // along z, stopping short
        val c = FloorPoint(1.9f, 1f) to FloorPoint(-2f, 1f)
        val d = FloorPoint(-2f, 0.9f) to FloorPoint(-2f, -1.1f)
        val outline = RoomReconstruction.close(listOf(c, a, d, b))
        assertTrue(outline.closed)
        assertEquals(4, outline.corners.size)
        assertWithinPercent(8f, checkNotNull(outline.area), 0.01f, "area")
        assertWithinPercent(12f, outline.perimeter, 0.01f, "perimeter")
        assertEquals(setOf(0, 1, 2, 3), outline.walls.map { it.index }.toSet())
        for (w in outline.walls) {
            val (from, to) = listOf(c, a, d, b)[w.index]
            // Each closed wall lies on its input line, with ends moved to the corners.
            val alongX = abs(from.z - to.z) < 1e-3f
            if (alongX) assertEquals(from.z, w.from.z, 1e-3f) else assertEquals(from.x, w.from.x, 1e-3f)
            assertWithin(if (alongX) 4f else 2f, w.length, 1e-3f, "wall ${w.index} length")
        }
        val open = RoomReconstruction.close(listOf(a, b))
        assertFalse(open.closed)
        assertNull(open.area)
        assertEquals(2, open.walls.size)
        assertEquals(a.first, open.walls[0].from)
    }

    @Test
    fun `the sparse showcase scan still gets a proposal, without normals`() {
        val sparse = RoomReconstruction.propose(ShowcaseSession.session)
        assertNull(ShowcaseSession.session.normals)
        assertTrue("yaw ${sparse.yawDegrees}", abs(sparse.yawDegrees) <= 45f)
        assertTrue(sparse.walls.all { it.length >= RoomReconstruction.MIN_WALL_LENGTH_M })
        assertTrue(sparse.objects.size <= RoomReconstruction.MAX_CLUSTERS)
    }

    @Test
    fun `the four reconstruction tools are in the strict spec list`() {
        val names = SceneToolExecutor.SPECS.map { it.name }
        assertTrue(
            names.containsAll(
                listOf(
                    SceneToolExecutor.PROPOSE_ROOM, SceneToolExecutor.BUILD_ROOM,
                    SceneToolExecutor.ACCEPT_OPENING, SceneToolExecutor.ACCEPT_OBJECT,
                ),
            ),
        )
        val build = SceneToolExecutor.SPECS.first { it.name == SceneToolExecutor.BUILD_ROOM }
        val wallIds = build.inputSchema.getValue("properties").jsonObject.getValue("wall_ids").jsonObject
        assertEquals("array", wallIds.getValue("type").jsonPrimitive.content)
        assertEquals("string", wallIds.getValue("items").jsonObject.getValue("type").jsonPrimitive.content)
    }

    private companion object {
        // RerunSyntheticRoom: interior x in -2..2, z in -1.8..1.8, 2.6 m high.
        const val WIDTH_M = 4f
        const val DEPTH_M = 3.6f
        const val HALF_WIDTH_M = WIDTH_M / 2
        const val HALF_DEPTH_M = DEPTH_M / 2
        const val CEILING_M = 2.6f
        const val AREA_M2 = WIDTH_M * DEPTH_M
        const val PERIMETER_M = 2 * (WIDTH_M + DEPTH_M)
        const val TOLERANCE_PERCENT = 3f
        const val COORD_TOLERANCE_M = 0.05f
        const val CORNER_TOLERANCE_M = 0.06f
        const val EXTENT_TOLERANCE_M = 0.3f
        const val YAW_TOLERANCE_DEG = 2f
        const val MIN_SUPPORT = 0.8f

        // Sofa: seat -1.7..-0.3 × 0..0.45 × -1.8..-0.95, back to 0.9 m.
        const val SOFA_X0 = -1.7f
        const val SOFA_X1 = -0.3f
        const val SOFA_X = -1f
        const val SOFA_Z = -1.375f
        const val SOFA_W = 1.4f
        const val SOFA_D = 0.85f
        const val SOFA_H = 0.9f
        // Table top 0.3..1.3 × 0.7..0.76 × -0.3..0.4.
        const val TABLE_X = 0.8f
        const val TABLE_Z = 0.05f
        const val TABLE_W = 1f
        const val TABLE_D = 0.7f
        const val TABLE_H = 0.76f
        // Cabinet 1.55..2 × 0..1.6 × 0.6..1.6, against the right wall.
        const val CABINET_X = 1.775f
        const val CABINET_Z = 1.1f
        const val CABINET_Z0 = 0.6f
        const val CABINET_Z1 = 1.6f
        const val CABINET_W = 0.45f
        const val CABINET_D = 1f
        const val CABINET_HEIGHT_M = 1.6f
        // Ball centre (-1.1, 0.3, 1.0), radius 0.3.
        const val BALL_X = -1.1f
        const val BALL_Z = 1f
        const val BALL_D = 0.6f
        const val BALL_H = 0.6f
        const val CENTER_TOLERANCE_M = 0.15f
        const val SIZE_TOLERANCE_M = 0.2f
        const val HEIGHT_TOLERANCE_M = 0.1f
        const val GAP_TOP_SLACK_M = 0.2f
    }
}
