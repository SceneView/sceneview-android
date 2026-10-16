package io.github.sceneview.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiSchemasTest {

    @Test
    fun `an object schema is strict - every property required, nothing else allowed`() {
        val schema = AiFixtures.TOOLS[1].inputSchema
        assertEquals("object", schema.getValue("type").jsonPrimitive.content)
        assertEquals(listOf("frame", "x", "y"), schema.getValue("properties").jsonObject.keys.toList())
        assertEquals(listOf("frame", "x", "y"), schema.getValue("required").jsonArray.map { it.jsonPrimitive.content })
        assertFalse(schema.getValue("additionalProperties").jsonPrimitive.boolean)
        val frame = schema.getValue("properties").jsonObject.getValue("frame").jsonObject
        assertEquals("integer", frame.getValue("type").jsonPrimitive.content)
        assertEquals("Keyframe index", frame.getValue("description").jsonPrimitive.content)
    }

    @Test
    fun `a sized number array and an enum say exactly what they take`() {
        val wall = AiFixtures.TOOLS[2].inputSchema.getValue("properties").jsonObject
        val from = wall.getValue("from").jsonObject
        assertEquals("array", from.getValue("type").jsonPrimitive.content)
        assertEquals("number", from.getValue("items").jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals(2, from.getValue("minItems").jsonPrimitive.int)
        assertEquals(2, from.getValue("maxItems").jsonPrimitive.int)
        val kind = wall.getValue("kind").jsonObject
        assertEquals(listOf("solid", "glass"), kind.getValue("enum").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun `the Gemini form drops what its API refuses and keeps the meaning`() {
        val gemini = AiSchemas.forGemini(AiFixtures.TOOLS[2].inputSchema)
        assertNull(gemini["additionalProperties"])
        val from = gemini.getValue("properties").jsonObject.getValue("from").jsonObject
        assertNull(from["minItems"])
        assertNull(from["maxItems"])
        assertEquals("number", from.getValue("items").jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals("[x, z] of one end", from.getValue("description").jsonPrimitive.content)
        val required = gemini.getValue("required").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("from", "to", "kind"), required)
    }

    @Test
    fun `the tools export is the MCP listing - name, description, input schema`() {
        val listing = AiSchemas.toolsJson(AiFixtures.TOOLS)
        assertEquals(3, listing.size)
        val raycast = listing[1].jsonObject
        assertEquals("raycast", raycast.getValue("name").jsonPrimitive.content)
        assertEquals("A ray through a keyframe pixel.", raycast.getValue("description").jsonPrimitive.content)
        assertEquals(AiFixtures.TOOLS[1].inputSchema, raycast.getValue("input_schema"))
        assertTrue(listing[0].jsonObject.getValue("input_schema").jsonObject.getValue("properties") is JsonObject)
    }
}
