package io.github.sceneview.ai

import kotlinx.coroutines.flow.catch
import kotlin.time.TimeSource

/** What one request to the provider came back with. */
data class AiRound(
    /** The reply's text, whole. */
    val text: String,
    val calls: List<AiEvent.ToolCall>,
    val usage: AiEvent.Usage?,
    /** The vendor's stop reason (`end_turn`, `tool_use`, `STOP`…), `null` on an error. */
    val stopReason: String?,
    val error: String?,
    val latencyMillis: Long,
) {
    val failed: Boolean get() = error != null
}

/**
 * The messages between a provider and an app's tools, kept the way the next request needs them:
 * consecutive user content merged into one message (the APIs want roles to alternate), the
 * assistant's transcript sent back exactly as received. One of these per conversation; the app
 * runs the tool calls and feeds the results back with [toolResults].
 *
 * ```kotlin
 * val conversation = AiConversation(system, tools)
 * conversation.user(listOf(AiContent.Text("Request: Rebuild this room")))
 * repeat(maxRounds) {
 *     val round = conversation.round(provider) { partial -> show(partial) }
 *     if (round.failed || round.calls.isEmpty()) return
 *     conversation.toolResults(round.calls.map { call -> execute(call) })
 * }
 * ```
 */
class AiConversation(val system: String, val tools: List<AiToolSpec>) {
    private val messages = ArrayList<AiMessage>()

    val isEmpty: Boolean get() = messages.isEmpty()

    /** The messages so far, as the next turn would send them. */
    fun snapshot(): List<AiMessage> = messages.toList()

    /** User content for the next turn; merged into the last message when that was the user's. */
    fun user(content: List<AiContent>) {
        if (content.isEmpty()) return
        val last = messages.lastOrNull()
        if (last?.role == AiMessage.Role.User) {
            messages[messages.lastIndex] = last.copy(content = last.content + content)
        } else {
            messages += AiMessage(AiMessage.Role.User, content)
        }
    }

    fun toolResults(results: List<AiContent.ToolResult>) = user(results)

    /**
     * One request: the conversation so far to [provider], its reply appended as the assistant's
     * message — the transcript when the provider gives one, else the text and calls it streamed.
     * [onText] sees the reply's text as it grows. An error appends nothing.
     */
    suspend fun round(provider: AiProvider, onText: suspend (String) -> Unit = {}): AiRound {
        val started = TimeSource.Monotonic.markNow()
        val text = StringBuilder()
        val calls = ArrayList<AiEvent.ToolCall>()
        var usage: AiEvent.Usage? = null
        var stopReason: String? = null
        var error: String? = null
        var transcript: List<AiContent> = emptyList()
        provider.run(AiTurn(system, snapshot(), tools))
            .catch { emit(AiEvent.Error(it.message ?: it::class.simpleName ?: "error")) }
            .collect { event ->
                when (event) {
                    is AiEvent.TextDelta -> {
                        text.append(event.text)
                        onText(text.toString())
                    }
                    is AiEvent.ToolCall -> calls += event
                    is AiEvent.Usage -> usage = event
                    is AiEvent.Done -> {
                        stopReason = event.stopReason
                        transcript = event.transcript
                    }
                    is AiEvent.Error -> error = event.message
                }
            }
        val latency = started.elapsedNow().inWholeMilliseconds
        if (error == null) {
            messages += AiMessage(
                AiMessage.Role.Assistant,
                transcript.ifEmpty {
                    buildList {
                        if (text.isNotBlank()) add(AiContent.Text(text.toString().trim()))
                        calls.forEach { add(AiContent.ToolUse(it.id, it.name, it.input)) }
                    }
                },
            )
        }
        return AiRound(text.toString().trim(), calls, usage, stopReason, error, latency)
    }

    fun clear() = messages.clear()
}
