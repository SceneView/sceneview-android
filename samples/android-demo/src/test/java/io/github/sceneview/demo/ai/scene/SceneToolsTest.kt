package io.github.sceneview.demo.ai.scene

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The tools against the real bundled scan: what they read, what they accept, what they refuse. */
class SceneToolsTest {

    private val executor = SceneToolExecutor(ShowcaseSession.session)

    private fun input(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun run(
        name: String,
        json: String,
        model: RoomModel = RoomModel(),
    ) = executor.execute(model, name, input(json))

    private fun JsonObject.f(key: String) = getValue(key).jsonPrimitive.float

    @Test
    fun `the digest describes the scan without any measurement of the room`() {
        val digest = executor.digest()
        assertEquals(ShowcaseSession.session.pointCount, digest.getValue("points").jsonPrimitive.int)
        assertEquals(3, digest.getValue("planes").jsonPrimitive.int)
        assertTrue(digest.getValue("keyframe_poses").jsonArray.isNotEmpty())
    }

    @Test
    fun `a ray through frame 160 lands on the vertical plane behind the radiator`() {
        val outcome = run(SceneToolExecutor.RAYCAST, """{"frame":160,"x":0.5,"y":0.5}""")
        assertFalse(outcome.isError)
        val result = input(outcome.content)
        assertTrue(
            result.getValue("target").jsonPrimitive.content,
            result.getValue("target").jsonPrimitive.content.startsWith("plane 3"),
        )
        val ray = outcome.looks.single() as Look.Ray
        assertTrue(ray.hit)
    }

    @Test
    fun `a wall along the points is kept with a computed height, a wall the scan never saw is refused`() {
        val kept = run(SceneToolExecutor.ADD_WALL, """{"from":[1.98,-2.52],"to":[2.84,-0.76],"height_m":2.4}""")
        assertFalse(kept.content, kept.isError)
        val wall = kept.model.walls.single()
        assertEquals(1.96f, wall.length, 0.03f)
        // The height is the scan's, not the 2.4 m asked for.
        assertEquals(2.33f, wall.height, 0.03f)

        val refused = run(
            SceneToolExecutor.ADD_WALL,
            """{"from":[1.69,-3.08],"to":[-1.27,-1.62],"height_m":2.4}""",
            kept.model,
        )
        assertTrue(refused.isError)
        assertEquals(1, refused.model.walls.size)
        assertEquals(1, refused.model.rejections.size)
        // A refused wall does not consume an id: the next object is still numbered after W1.
        val obj = run(
            SceneToolExecutor.ADD_OBJECT,
            """{"category":"bed","center":[-0.02,-1.59],"size":[1.0,2.2],"yaw_deg":-63.7}""",
            refused.model,
        )
        assertEquals("O2", obj.model.objects.single().id)
    }

    @Test
    fun `an object over empty floor is refused`() {
        val outcome = run(
            SceneToolExecutor.ADD_OBJECT,
            """{"category":"bed","center":[8.0,8.0],"size":[1.0,2.0],"yaw_deg":0}""",
        )
        assertTrue(outcome.isError)
        assertTrue(outcome.model.objects.isEmpty())
    }

    @Test
    fun `points_in_box counts the cloud and measure computes the distance`() {
        val probe = run(
            SceneToolExecutor.POINTS_IN_BOX,
            """{"center":[2.22,0.5,-1.75],"size":[1.0,1.0,0.6],"yaw_deg":-63.7}""",
        )
        assertFalse(probe.isError)
        assertTrue(input(probe.content).getValue("count").jsonPrimitive.int > 0)

        val measured = run(SceneToolExecutor.MEASURE, """{"from":[0,0,0],"to":[3,0,4]}""")
        assertEquals(5f, input(measured.content).f("distance_m"), 1e-3f)
        assertEquals(0f, input(measured.content).f("vertical_m"), 1e-3f)
    }

    @Test
    fun `invalid input and unknown tools are refused without touching the room`() {
        val model = RoomModel()
        val badFrame = executor.execute(model, SceneToolExecutor.RAYCAST, input("""{"frame":99999,"x":0.5,"y":0.5}"""))
        assertTrue(badFrame.isError)
        assertSame(model, badFrame.model)
        val missing = executor.execute(model, SceneToolExecutor.ADD_WALL, input("""{"from":[0,0]}"""))
        assertTrue(missing.isError)
        assertSame(model, missing.model)
        val unknown = executor.execute(model, "teleport", input("{}"))
        assertTrue(unknown.isError)
    }

    @Test
    fun `placing needs a free region, the takeoff is computed from the model`() {
        val noRegion = run(
            SceneToolExecutor.PLACE_ITEM,
            """{"category":"armchair","region_id":"F1","slot":"center","yaw_toward_id":""}""",
        )
        assertTrue(noRegion.isError)
        val takeoff = run(SceneToolExecutor.COMPUTE_TAKEOFF, "{}")
        assertFalse(takeoff.isError)
        assertEquals(0f, input(takeoff.content).f("wall_area_m2"), 1e-3f)
    }

    @Test
    fun `every tool spec is strict, every property required and nothing else allowed`() {
        assertEquals(14, SceneToolExecutor.SPECS.size)
        for (spec in SceneToolExecutor.SPECS) {
            val schema = spec.inputSchema
            assertEquals(spec.name, "object", schema.getValue("type").jsonPrimitive.content)
            assertFalse(spec.name, schema.getValue("additionalProperties").jsonPrimitive.boolean)
            val properties = schema.getValue("properties").jsonObject.keys
            val required = schema.getValue("required").jsonArray.map { it.jsonPrimitive.content }.toSet()
            assertEquals(spec.name, properties, required)
        }
    }

    @Test
    fun `yaw normalises into -180 to 180`() {
        assertEquals(116.3f, SceneToolExecutor.normalise(-243.7f), 1e-3f)
        assertEquals(-90f, SceneToolExecutor.normalise(270f), 1e-3f)
        assertEquals(0f, SceneToolExecutor.normalise(360f), 1e-3f)
    }
}
