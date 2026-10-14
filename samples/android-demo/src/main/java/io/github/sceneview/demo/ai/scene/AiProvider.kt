package io.github.sceneview.demo.ai.scene

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

/**
 * A model that reads the room through tools and answers with tool calls: the recorded demo
 * ([RecordedAiProvider]) or Claude with the user's own key ([ClaudeAiProvider]).
 *
 * The provider only *chooses*. Every number the screen shows — a wall's length, a box's size, a
 * clearance — is computed by [SceneToolExecutor] from the recorded AR session.
 */
interface AiProvider {
    /** Stable id, the one the provider switch persists. */
    val id: String

    val capabilities: AiCapabilities

    /** Whether [run] can be called now; cheap, never touches the network. */
    suspend fun status(): AiStatus

    /** One model turn: streamed text, then the tool calls, then [AiEvent.Done] or [AiEvent.Error]. */
    fun run(turn: AiTurn): Flow<AiEvent>
}

/** What a provider can do: [images] means keyframes go along with the spatial digest. */
data class AiCapabilities(val tools: Boolean, val images: Boolean, val live: Boolean)

sealed interface AiStatus {
    data object Ready : AiStatus

    /** No key saved yet: the key field in the settings sheet is the way forward. */
    data object NeedsKey : AiStatus
}

sealed interface AiEvent {
    data class TextDelta(val text: String) : AiEvent

    data class ToolCall(val id: String, val name: String, val input: JsonObject) : AiEvent

    data class Usage(val inputTokens: Int, val outputTokens: Int) : AiEvent

    /**
     * The turn ended. [transcript] is the assistant message exactly as the provider needs it sent
     * back next turn — Claude's thinking blocks keep their signatures — or empty, in which case
     * the loop rebuilds it from the text and tool calls it saw.
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

    /** A keyframe photo, base64, with the pose it was taken from said in the text beside it. */
    data class Image(val mediaType: String, val base64: String) : AiContent

    data class ToolUse(val id: String, val name: String, val input: JsonObject) : AiContent

    data class ToolResult(val toolUseId: String, val content: String, val isError: Boolean) : AiContent

    /** A block only the provider understands (Claude's `thinking`), sent back untouched. */
    data class Opaque(val block: JsonObject) : AiContent
}

/** A tool as the model sees it: [inputSchema] is a JSON Schema object, strict-compatible. */
data class AiToolSpec(val name: String, val description: String, val inputSchema: JsonObject)
