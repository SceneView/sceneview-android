package io.github.sceneview.ai.gemini

import io.github.sceneview.ai.AiContent
import io.github.sceneview.ai.AiEvent
import io.github.sceneview.ai.AiMessage
import io.github.sceneview.ai.AiSchemas
import io.github.sceneview.ai.AiTurn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.random.Random

/**
 * The Gemini `generateContent` wire format, both ways, pure — the same [AiTurn] in, the same
 * [AiEvent]s out as [io.github.sceneview.ai.claude.ClaudeCodec], so an app switches provider
 * without touching its tools.
 *
 * Gemini's differences are absorbed here: tool schemas go through [AiSchemas.forGemini]; a
 * function call has no id, so the codec gives it one and finds the call's name back when its
 * result is sent; the `thoughtSignature` on a call travels in [AiContent.ToolUse.meta] and is
 * echoed next turn; thought summaries are neither shown nor sent back; a whole number the model
 * writes as `160.0` is sent to the tool as `160`.
 */
object GeminiCodec {
    /** The newest flash model the API lists as stable on 2026-10-16; cheaper and faster than pro. */
    const val MODEL = "gemini-3.8-flash"
    const val VENDOR_BASE_URL = "https://generativelanguage.googleapis.com"
    const val MAX_OUTPUT_TOKENS = 16_384

    private val json = Json { ignoreUnknownKeys = true }

    /** The `generateContent` path for [model], the same under the vendor's host and a relay's `gemini/` prefix. */
    fun vendorPath(model: String): String = "v1beta/models/$model:generateContent"

    fun vendorUrl(model: String, baseUrl: String = VENDOR_BASE_URL): String =
        "${baseUrl.trimEnd('/')}/${vendorPath(model)}"

    fun relayPath(model: String): String = "gemini/${vendorPath(model)}"

    fun encode(turn: AiTurn, maxOutputTokens: Int = MAX_OUTPUT_TOKENS): JsonObject = buildJsonObject {
        putJsonObject("systemInstruction") { putJsonArray("parts") { addJsonObject { put("text", turn.system) } } }
        val calls = turn.messages.flatMap { it.content }.filterIsInstance<AiContent.ToolUse>().associateBy { it.id }
        putJsonArray("contents") {
            for (message in turn.messages) addJsonObject {
                put("role", if (message.role == AiMessage.Role.User) "user" else "model")
                putJsonArray("parts") { message.content.forEach { add(part(it, calls)) } }
            }
        }
        if (turn.tools.isNotEmpty()) {
            putJsonArray("tools") {
                addJsonObject {
                    putJsonArray("functionDeclarations") {
                        for (tool in turn.tools) addJsonObject {
                            put("name", tool.name)
                            put("description", tool.description)
                            // Gemini refuses an object schema without properties: a tool with no input has none.
                            val properties = tool.inputSchema["properties"] as? JsonObject
                            if (!properties.isNullOrEmpty()) put("parameters", AiSchemas.forGemini(tool.inputSchema))
                        }
                    }
                }
            }
            putJsonObject("toolConfig") { putJsonObject("functionCallingConfig") { put("mode", "AUTO") } }
        }
        putJsonObject("generationConfig") { put("maxOutputTokens", maxOutputTokens) }
    }

    private fun part(content: AiContent, calls: Map<String, AiContent.ToolUse>): JsonObject = when (content) {
        is AiContent.Text -> buildJsonObject { put("text", content.text) }
        is AiContent.Image -> buildJsonObject {
            putJsonObject("inlineData") {
                put("mimeType", content.mediaType)
                put("data", content.base64)
            }
        }
        is AiContent.ToolUse -> buildJsonObject {
            putJsonObject("functionCall") {
                put("name", content.name)
                put("args", content.input)
                content.meta?.get("id")?.let { put("id", it) }
            }
            content.meta?.get("thoughtSignature")?.let { put("thoughtSignature", it) }
        }
        is AiContent.ToolResult -> buildJsonObject {
            val call = calls[content.toolUseId]
            putJsonObject("functionResponse") {
                put("name", call?.name ?: content.toolUseId)
                call?.meta?.get("id")?.let { put("id", it) }
                put("response", responseObject(content))
            }
        }
        is AiContent.Opaque -> content.block
    }

    /** Gemini wants an object: the tool's JSON as is, or the text under `output` / `error`. */
    private fun responseObject(result: AiContent.ToolResult): JsonObject {
        val parsed = runCatching { json.parseToJsonElement(result.content) }.getOrNull() as? JsonObject
        return when {
            parsed != null && !result.isError -> parsed
            parsed != null -> JsonObject(parsed + ("is_error" to JsonPrimitive(true)))
            result.isError -> buildJsonObject { put("error", result.content) }
            else -> buildJsonObject { put("output", result.content) }
        }
    }

    /**
     * A response body as events: the reply's text, its function calls (ids made here, unique per
     * response), the usage with the thinking tokens counted as output, then [AiEvent.Done] with
     * the transcript to send back — or one [AiEvent.Error] for an API error, a blocked prompt or
     * a body this does not understand.
     */
    fun decode(body: String): List<AiEvent> {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return listOf(AiEvent.Error("Unreadable response from the API"))
        root["error"]?.let { return listOf(AiEvent.Error(errorMessage(it))) }
        val candidate = (root["candidates"] as? JsonArray)?.firstOrNull() as? JsonObject
        if (candidate == null) {
            val reason = (root["promptFeedback"] as? JsonObject)?.get("blockReason")?.jsonPrimitive?.contentOrNull
            val message = if (reason != null) "The API blocked the request ($reason)." else "Response without content"
            return listOf(AiEvent.Error(message))
        }
        val parts = ((candidate["content"] as? JsonObject)?.get("parts") as? JsonArray)
            .orEmpty().filterIsInstance<JsonObject>()
        val finishReason = candidate["finishReason"]?.jsonPrimitive?.contentOrNull ?: "STOP"
        if (parts.isEmpty() && finishReason !in setOf("STOP", "MAX_TOKENS")) {
            return listOf(AiEvent.Error("The model's answer was dropped ($finishReason)."))
        }
        val responseId = root["responseId"]?.jsonPrimitive?.contentOrNull
            ?: Random.nextInt(RANDOM_ID_SPAN).toString(RADIX)
        val events = ArrayList<AiEvent>()
        val transcript = ArrayList<AiContent>()
        var callCount = 0
        for (part in parts) {
            // A thought summary is for reading along, never for the next request.
            if (part["thought"]?.jsonPrimitive?.booleanOrNull == true) continue
            val signature = part["thoughtSignature"]
            val text = part["text"]?.jsonPrimitive?.contentOrNull
            val call = part["functionCall"] as? JsonObject
            val name = call?.get("name")?.jsonPrimitive?.contentOrNull
            when {
                text != null -> {
                    events += AiEvent.TextDelta(text)
                    // A signature on a text part goes back with it, as the part was received.
                    transcript += if (signature != null) AiContent.Opaque(part) else AiContent.Text(text)
                }
                name != null -> {
                    val args = wholeNumbers(call["args"] as? JsonObject ?: JsonObject(emptyMap())) as JsonObject
                    val id = call["id"]?.jsonPrimitive?.contentOrNull ?: "call_${responseId}_${++callCount}"
                    val meta = buildJsonObject {
                        signature?.let { put("thoughtSignature", it) }
                        call["id"]?.let { put("id", it) }
                    }.takeIf { it.isNotEmpty() }
                    events += AiEvent.ToolCall(id, name, args)
                    transcript += AiContent.ToolUse(id, name, args, meta)
                }
                else -> transcript += AiContent.Opaque(part)
            }
        }
        (root["usageMetadata"] as? JsonObject)?.let { usage ->
            events += AiEvent.Usage(
                usage["promptTokenCount"]?.jsonPrimitive?.intOrNull ?: 0,
                (usage["candidatesTokenCount"]?.jsonPrimitive?.intOrNull ?: 0) +
                    (usage["thoughtsTokenCount"]?.jsonPrimitive?.intOrNull ?: 0),
            )
        }
        events += AiEvent.Done(finishReason, transcript)
        return events
    }

    /** `160.0` → `160`, so an integer parameter the model wrote as a float still reads as an integer. */
    private fun wholeNumbers(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { (_, v) -> wholeNumbers(v) })
        is JsonArray -> JsonArray(element.map(::wholeNumbers))
        is JsonPrimitive -> if (!element.isString && WHOLE.matches(element.content)) {
            JsonPrimitive(element.content.substringBefore('.').toLong())
        } else {
            element
        }
    }

    /** The API's own message for an error body, never anything of the request (the key is in its headers). */
    fun errorMessage(error: JsonElement): String {
        val obj = error as? JsonObject
        val code = obj?.get("code")?.jsonPrimitive?.intOrNull
        val status = obj?.get("status")?.jsonPrimitive?.contentOrNull ?: "error"
        val message = obj?.get("message")?.jsonPrimitive?.contentOrNull ?: "Unknown API error"
        return when {
            code in KEY_REFUSED_CODES || status in KEY_REFUSED_STATUS ||
                message.contains("API key", ignoreCase = true) ->
                "The key was refused. Check it in the settings."
            code in BUSY_CODES || status in BUSY_STATUS -> "The API is busy ($status). Try again in a moment."
            else -> "$status: $message"
        }
    }

    /** The HTTP status's error, when the body says nothing readable. */
    fun errorForStatus(code: Int, body: String): AiEvent.Error {
        val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        parsed?.get("error")?.let { return AiEvent.Error(errorMessage(it)) }
        return AiEvent.Error("The API answered HTTP $code")
    }

    private val WHOLE = Regex("-?\\d+\\.0+")
    private val KEY_REFUSED_CODES = setOf(401, 403)
    private val KEY_REFUSED_STATUS = setOf("UNAUTHENTICATED", "PERMISSION_DENIED")
    private val BUSY_CODES = setOf(429, 503)
    private val BUSY_STATUS = setOf("RESOURCE_EXHAUSTED", "UNAVAILABLE")
    private const val RANDOM_ID_SPAN = 0x1000000
    private const val RADIX = 16
}
