package io.github.sceneview.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

/**
 * A model that works through tools: it reads a turn ([AiTurn]) and answers with text and tool
 * calls ([AiEvent]). Claude ([io.github.sceneview.ai.claude.ClaudeAiProvider]), Gemini
 * ([io.github.sceneview.ai.gemini.GeminiAiProvider]) or a scripted stand-in for tests and demos.
 *
 * The provider only *chooses*. Every number an app shows comes from the code behind its tools,
 * never from the model's text — that is the rule the whole layer is built around.
 *
 * ```kotlin
 * val provider = ClaudeAiProvider(AiEndpoint.Vendor { keyStore.load() })
 * val conversation = AiConversation(system = "You measure rooms through tools.", tools = specs)
 * conversation.user(listOf(AiContent.Text("Request: Rebuild this room")))
 * val round = conversation.round(provider)
 * round.calls.forEach { call -> /* run it, then conversation.toolResults(...) */ }
 * ```
 */
interface AiProvider {
    /** Stable id, the one a provider switch persists: `claude`, `gemini`, `recorded`. */
    val id: String

    /** The model as the vendor names and bills it (`claude-sonnet-5-5`, `gemini-3.8-flash`); [id] for a stand-in. */
    val model: String get() = id

    val capabilities: AiCapabilities

    /** Whether [run] can be called now; cheap, never touches the network. */
    suspend fun status(): AiStatus

    /** One model turn: streamed text, then the tool calls, then [AiEvent.Done] or [AiEvent.Error]. */
    fun run(turn: AiTurn): Flow<AiEvent>
}

/** What a provider can do: [images] means photos go along with the text, [live] that it costs. */
data class AiCapabilities(val tools: Boolean, val images: Boolean, val live: Boolean)

sealed interface AiStatus {
    data object Ready : AiStatus

    /** No credential yet: the app's key field (or its sign-in) is the way forward. */
    data object NeedsKey : AiStatus
}

sealed interface AiEvent {
    data class TextDelta(val text: String) : AiEvent

    data class ToolCall(val id: String, val name: String, val input: JsonObject) : AiEvent

    /** Tokens of one request. [outputTokens] includes the model's thinking where the API bills it. */
    data class Usage(val inputTokens: Int, val outputTokens: Int) : AiEvent

    /**
     * The turn ended. [transcript] is the assistant message exactly as the provider needs it sent
     * back next turn — Claude's thinking blocks keep their signatures, Gemini's calls their
     * thought signatures — or empty, in which case the loop rebuilds it from the text and tool
     * calls it saw.
     */
    data class Done(val stopReason: String, val transcript: List<AiContent> = emptyList()) : AiEvent

    data class Error(val message: String) : AiEvent
}

/** Everything one model turn sees: the instructions, the conversation, the tools. */
data class AiTurn(val system: String, val messages: List<AiMessage>, val tools: List<AiToolSpec>)

data class AiMessage(val role: Role, val content: List<AiContent>) {
    enum class Role { User, Assistant }
}

sealed interface AiContent {
    data class Text(val text: String) : AiContent

    /** A photo, base64, with what it shows said in the text beside it. */
    data class Image(val mediaType: String, val base64: String) : AiContent

    /**
     * A tool call the model made. [meta] is what the vendor needs back with it and nothing else
     * understands (Gemini's `thoughtSignature`); a codec that has none leaves it `null`.
     */
    data class ToolUse(
        val id: String,
        val name: String,
        val input: JsonObject,
        val meta: JsonObject? = null,
    ) : AiContent

    data class ToolResult(val toolUseId: String, val content: String, val isError: Boolean) : AiContent

    /** A block only its provider understands (Claude's `thinking`), sent back untouched. */
    data class Opaque(val block: JsonObject) : AiContent
}

/**
 * A tool as the model sees it. [inputSchema] is a JSON Schema object — strict: every property
 * required, no additional properties ([AiSchemas.objectSchema]) — the same document an MCP
 * server publishes for the tool, so an agent in the app and an agent over MCP call it alike.
 */
data class AiToolSpec(val name: String, val description: String, val inputSchema: JsonObject)
