package io.github.sceneview.ai.claude

import io.github.sceneview.ai.AiContent
import io.github.sceneview.ai.AiEvent
import io.github.sceneview.ai.AiMessage
import io.github.sceneview.ai.AiTurn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The Messages API wire format, both ways, pure: [encode] an [AiTurn] into the request body,
 * [decode] a response body into [AiEvent]s. No network here, so fixtures test it whole.
 *
 * The request lets the model choose: `tool_choice` auto, adaptive thinking (no token budget),
 * every tool `strict` so its input always matches the schema the app validates.
 */
object ClaudeCodec {
    const val MODEL = "claude-sonnet-5-5"
    const val API_VERSION = "2023-06-01"

    /** The Messages path, the same under the vendor's host and under a relay's `anthropic/` prefix. */
    const val VENDOR_PATH = "v1/messages"
    const val ENDPOINT = "https://api.anthropic.com/$VENDOR_PATH"
    const val RELAY_PATH = "anthropic/$VENDOR_PATH"
    const val MAX_TOKENS = 8192

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(turn: AiTurn, model: String = MODEL, maxTokens: Int = MAX_TOKENS): JsonObject = buildJsonObject {
        put("model", model)
        put("max_tokens", maxTokens)
        put("system", turn.system)
        putJsonObject("thinking") { put("type", "adaptive") }
        putJsonObject("tool_choice") { put("type", "auto") }
        putJsonArray("tools") {
            for (tool in turn.tools) addJsonObject {
                put("name", tool.name)
                put("description", tool.description)
                put("input_schema", tool.inputSchema)
                put("strict", true)
            }
        }
        putJsonArray("messages") {
            for (message in turn.messages) addJsonObject {
                put("role", if (message.role == AiMessage.Role.User) "user" else "assistant")
                putJsonArray("content") { message.content.forEach { add(block(it)) } }
            }
        }
    }

    private fun block(content: AiContent): JsonObject = when (content) {
        is AiContent.Text -> buildJsonObject {
            put("type", "text")
            put("text", content.text)
        }
        is AiContent.Image -> buildJsonObject {
            put("type", "image")
            putJsonObject("source") {
                put("type", "base64")
                put("media_type", content.mediaType)
                put("data", content.base64)
            }
        }
        is AiContent.ToolUse -> buildJsonObject {
            put("type", "tool_use")
            put("id", content.id)
            put("name", content.name)
            put("input", content.input)
        }
        is AiContent.ToolResult -> buildJsonObject {
            put("type", "tool_result")
            put("tool_use_id", content.toolUseId)
            put("content", content.content)
            if (content.isError) put("is_error", true)
        }
        is AiContent.Opaque -> content.block
    }

    /**
     * A response body as events: its text, its tool calls, the usage, then [AiEvent.Done] with
     * the whole assistant message (thinking blocks kept, signatures untouched) — or one
     * [AiEvent.Error] for an API error or a body this does not understand.
     */
    fun decode(body: String): List<AiEvent> {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return listOf(AiEvent.Error("Unreadable response from the API"))
        if (root["type"]?.jsonPrimitive?.contentOrNull == "error") return listOf(AiEvent.Error(errorMessage(root)))
        val blocks = root["content"]?.let { runCatching { it.jsonArray }.getOrNull() }
            ?: return listOf(AiEvent.Error("Response without content"))
        val events = ArrayList<AiEvent>()
        val transcript = ArrayList<AiContent>()
        for (block in blocks.filterIsInstance<JsonObject>()) {
            when (block["type"]?.jsonPrimitive?.contentOrNull) {
                "text" -> {
                    val text = block["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    events += AiEvent.TextDelta(text)
                    transcript += AiContent.Text(text)
                }
                "tool_use" -> {
                    val id = block["id"]?.jsonPrimitive?.contentOrNull
                    val name = block["name"]?.jsonPrimitive?.contentOrNull
                    val input = block["input"] as? JsonObject ?: JsonObject(emptyMap())
                    if (id != null && name != null) {
                        events += AiEvent.ToolCall(id, name, input)
                        transcript += AiContent.ToolUse(id, name, input)
                    }
                }
                // thinking, redacted_thinking and anything newer: sent back exactly as received.
                else -> transcript += AiContent.Opaque(block)
            }
        }
        root["usage"]?.let { it as? JsonObject }?.let { usage ->
            events += AiEvent.Usage(
                usage["input_tokens"]?.jsonPrimitive?.intOrNull ?: 0,
                usage["output_tokens"]?.jsonPrimitive?.intOrNull ?: 0,
            )
        }
        events += AiEvent.Done(root["stop_reason"]?.jsonPrimitive?.contentOrNull ?: "end_turn", transcript)
        return events
    }

    /** The API's own message for an error body, never anything of the request (the key is in its headers). */
    fun errorMessage(root: JsonObject): String {
        val error = root["error"] as? JsonObject
        val type = error?.get("type")?.jsonPrimitive?.contentOrNull ?: "error"
        val message = error?.get("message")?.jsonPrimitive?.contentOrNull ?: "Unknown API error"
        return when (type) {
            "authentication_error" -> "The key was refused. Check it in the settings."
            "rate_limit_error", "overloaded_error" -> "The API is busy ($type). Try again in a moment."
            else -> "$type: $message"
        }
    }

    /** The HTTP status's error, when the body says nothing readable. */
    fun errorForStatus(code: Int, body: String): AiEvent.Error {
        val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (parsed != null && parsed["type"]?.jsonPrimitive?.contentOrNull == "error") {
            return AiEvent.Error(errorMessage(parsed))
        }
        return AiEvent.Error("The API answered HTTP $code")
    }
}
