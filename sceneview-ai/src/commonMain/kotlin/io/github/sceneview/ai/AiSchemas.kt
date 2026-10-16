package io.github.sceneview.ai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * JSON Schema for tool inputs, in the strict subset every provider accepts: objects with every
 * property required and no additional ones, numbers, integers, strings, booleans, enums and
 * arrays of those. One schema per tool, written once, served to Claude, to Gemini
 * ([forGemini]) and to an MCP server ([toolsJson]) unchanged in meaning.
 *
 * ```kotlin
 * val addWall = AiToolSpec(
 *     name = "add_wall",
 *     description = "A wall along its floor line, checked against the scan.",
 *     inputSchema = AiSchemas.objectSchema(
 *         "from" to AiSchemas.numbers("[x, z] of one end, metres", count = 2),
 *         "to" to AiSchemas.numbers("[x, z] of the other end, metres", count = 2),
 *         "height_m" to AiSchemas.number("Height above the floor, metres"),
 *     ),
 * )
 * ```
 */
object AiSchemas {
    /** An object with exactly [properties], all required. */
    fun objectSchema(vararg properties: Pair<String, JsonObject>): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { properties.forEach { (name, schema) -> put(name, schema) } }
        putJsonArray("required") { properties.forEach { (name, _) -> add(name) } }
        put("additionalProperties", false)
    }

    fun number(description: String): JsonObject = primitive("number", description)

    fun integer(description: String): JsonObject = primitive("integer", description)

    fun string(description: String): JsonObject = primitive("string", description)

    fun boolean(description: String): JsonObject = primitive("boolean", description)

    /** An array of numbers; with [count], exactly that many (a point, a size). */
    fun numbers(description: String, count: Int? = null): JsonObject =
        arrayOf(primitive("number", ""), description, count)

    fun strings(description: String): JsonObject = arrayOf(primitive("string", ""), description)

    fun enumOf(values: List<String>, description: String): JsonObject = buildJsonObject {
        put("type", "string")
        put("description", description)
        putJsonArray("enum") { values.forEach { add(it) } }
    }

    fun arrayOf(items: JsonObject, description: String, count: Int? = null): JsonObject = buildJsonObject {
        put("type", "array")
        put("description", description)
        put("items", items)
        if (count != null) {
            put("minItems", count)
            put("maxItems", count)
        }
    }

    /**
     * The tools as one JSON document: `[{"name", "description", "input_schema"}]` — what an MCP
     * server lists, so a tool is defined once for the app and for the agents outside it.
     */
    fun toolsJson(specs: List<AiToolSpec>): JsonArray = buildJsonArray {
        for (spec in specs) addJsonObject {
            put("name", spec.name)
            put("description", spec.description)
            put("input_schema", spec.inputSchema)
        }
    }

    /**
     * [schema] in the subset Gemini's function declarations accept: the OpenAPI-style object
     * without `additionalProperties`, `$schema` or `minItems`/`maxItems` on arrays, which the API
     * rejects as unknown fields. The meaning is kept — Gemini fills every property it is asked for.
     */
    fun forGemini(schema: JsonObject): JsonObject = strip(schema) as JsonObject

    private fun strip(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.filterKeys { it !in GEMINI_DROPPED }.mapValues { (_, v) -> strip(v) },
        )
        is JsonArray -> JsonArray(element.map(::strip))
        else -> element
    }

    private fun primitive(type: String, description: String): JsonObject = buildJsonObject {
        put("type", type)
        if (description.isNotEmpty()) put("description", description)
    }

    private val GEMINI_DROPPED = setOf("additionalProperties", "\$schema", "minItems", "maxItems")
}
