package io.github.sceneview.demo.ai.scene

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** One line of the conversation as the screen shows it. */
data class ChatLine(val id: Int, val kind: Kind, val text: String) {
    enum class Kind { User, Assistant, ToolOk, ToolRefused, Note, Error }
}

/** Everything the AI Scene screen draws, one immutable snapshot. */
data class AgentState(
    val model: RoomModel = RoomModel(),
    val lines: List<ChatLine> = emptyList(),
    /** What the model looked at during the current request: rays, probes, tried walls. */
    val looks: List<Look> = emptyList(),
    val busy: Boolean = false,
    val canUndo: Boolean = false,
    /** Set once the model asked for the takeoff: the card shows [Takeoff.of] the current model. */
    val takeoffShown: Boolean = false,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
)

/**
 * The loop between a provider and the room: send the turn, run the tool calls it returns against
 * the recorded session, send the results back — at most [MAX_ROUNDS] round trips per request.
 *
 * The provider only chooses. [SceneToolExecutor] computes every number off [computeContext]; the
 * new room is swapped in on [applyContext] (the main thread in the app), one tool call at a time,
 * [toolPaceMillis] apart so each step is seen. Each request is one undo step.
 */
class SceneAgent(
    private val executor: SceneToolExecutor,
    private val keyframes: suspend () -> List<AiContent> = { emptyList() },
    private val toolPaceMillis: Long = 0L,
    private val computeContext: CoroutineContext = Dispatchers.Default,
    private val applyContext: CoroutineContext = Dispatchers.Main.immediate,
) {
    private val mutableState = MutableStateFlow(AgentState())
    val state: StateFlow<AgentState> = mutableState.asStateFlow()

    private val messages = ArrayList<AiMessage>()
    private val undoStack = ArrayDeque<RoomModel>()
    private var pendingNote: String? = null
    private var lineIds = 0

    /** Runs [prompt] through [provider] to the end of its tool calls. Ignored while busy. */
    suspend fun ask(provider: AiProvider, prompt: String) {
        if (state.value.busy || prompt.isBlank()) return
        val before = state.value.model
        val opening = buildList {
            if (messages.isEmpty()) {
                add(AiContent.Text("Session digest: ${executor.digest()}"))
                if (provider.capabilities.images) addAll(keyframes())
            }
            pendingNote?.let { add(AiContent.Text(it)) }
            add(AiContent.Text(RecordedAiProvider.PROMPT_MARK + prompt.trim()))
        }
        pendingNote = null
        appendUser(opening)
        apply { it.copy(busy = true, looks = emptyList(), lines = it.lines + line(ChatLine.Kind.User, prompt.trim())) }
        try {
            converse(provider)
        } finally {
            if (state.value.model != before) undoStack.addLast(before)
            apply { it.copy(busy = false, canUndo = undoStack.isNotEmpty()) }
        }
    }

    private suspend fun converse(provider: AiProvider) {
        for (round in 1..MAX_ROUNDS) {
            val turn = AiTurn(SYSTEM_PROMPT, messages.toList(), SceneToolExecutor.SPECS)
            val calls = ArrayList<AiEvent.ToolCall>()
            var transcript: List<AiContent> = emptyList()
            var failed = false
            val text = StringBuilder()
            val replyId = ++lineIds
            provider.run(turn)
                .catch { emit(AiEvent.Error(it.message ?: it.javaClass.simpleName)) }
                .collect { event ->
                    when (event) {
                        is AiEvent.TextDelta -> {
                            text.append(event.text)
                            val reply = ChatLine(replyId, ChatLine.Kind.Assistant, text.toString().trim())
                            apply { s -> s.copy(lines = s.lines.filterNot { it.id == replyId } + reply) }
                        }
                        is AiEvent.ToolCall -> calls += event
                        is AiEvent.Usage -> apply {
                            it.copy(
                                inputTokens = it.inputTokens + event.inputTokens,
                                outputTokens = it.outputTokens + event.outputTokens,
                            )
                        }
                        is AiEvent.Done -> transcript = event.transcript
                        is AiEvent.Error -> {
                            failed = true
                            apply { it.copy(lines = it.lines + line(ChatLine.Kind.Error, event.message)) }
                        }
                    }
                }
            if (failed) return
            messages += AiMessage(
                AiMessage.Role.Assistant,
                transcript.ifEmpty {
                    buildList {
                        if (text.isNotBlank()) add(AiContent.Text(text.toString().trim()))
                        calls.forEach { add(AiContent.ToolUse(it.id, it.name, it.input)) }
                    }
                },
            )
            if (calls.isEmpty()) return
            val results = calls.map { call ->
                val outcome = runTool(call)
                AiContent.ToolResult(call.id, outcome.content, outcome.isError)
            }
            appendUser(results)
            if (round == MAX_ROUNDS) {
                apply { it.copy(lines = it.lines + line(ChatLine.Kind.Note, "Stopped after $MAX_ROUNDS round trips.")) }
            }
        }
    }

    private suspend fun runTool(call: AiEvent.ToolCall): ToolOutcome {
        val outcome = withContext(computeContext) { executor.execute(state.value.model, call.name, call.input) }
        apply {
            it.copy(
                model = outcome.model,
                looks = it.looks + outcome.looks,
                takeoffShown = it.takeoffShown || (call.name == SceneToolExecutor.COMPUTE_TAKEOFF && !outcome.isError),
                lines = it.lines + line(
                    if (outcome.isError) ChatLine.Kind.ToolRefused else ChatLine.Kind.ToolOk,
                    outcome.caption,
                ),
            )
        }
        if (toolPaceMillis > 0) delay(toolPaceMillis)
        return outcome
    }

    /** Back to the room before the last request. The model is told on its next turn. */
    fun undo() {
        if (state.value.busy) return
        val previous = undoStack.removeLastOrNull() ?: return
        pendingNote = "The user pressed Undo: the changes of your last request were reverted."
        mutableState.update {
            it.copy(
                model = previous,
                looks = emptyList(),
                canUndo = undoStack.isNotEmpty(),
                takeoffShown = it.takeoffShown && !previous.isEmpty,
                lines = it.lines + line(ChatLine.Kind.Note, "Undone."),
            )
        }
    }

    /** An empty room and a new conversation. */
    fun reset() {
        if (state.value.busy) return
        messages.clear()
        undoStack.clear()
        pendingNote = null
        mutableState.value = AgentState()
    }

    private fun appendUser(content: List<AiContent>) {
        val last = messages.lastOrNull()
        // Consecutive user content stays one message, as the API wants roles to alternate.
        if (last?.role == AiMessage.Role.User) {
            messages[messages.lastIndex] = last.copy(content = last.content + content)
        } else {
            messages += AiMessage(AiMessage.Role.User, content)
        }
    }

    private suspend fun apply(change: (AgentState) -> AgentState) {
        withContext(applyContext) { mutableState.update(change) }
    }

    private fun line(kind: ChatLine.Kind, text: String) = ChatLine(++lineIds, kind, text)

    companion object {
        const val MAX_ROUNDS = 4

        val SYSTEM_PROMPT = """
            You rebuild and furnish a real room from an AR scan recorded on a phone. You never see a
            measurement you did not get from a tool: the tools read the recorded session (ARCore
            planes, the feature-point cloud, the camera's path and its keyframe photos) and compute
            every length, height, area and clearance. You choose; the code measures.

            Order of work:
            1. Read the scan: list_planes, raycast through keyframes, points_in_box, measure.
            2. Reconstruct: add_wall along a wall's floor line, add_opening for windows and doors,
               add_object for each piece of furniture with a rough footprint. Each call is checked
               against the points: a wall or an object the scan never saw is refused — leave it out
               rather than insist.
            3. Only then place catalogue items: free_floor_area, place_item, add_dimension,
               set_material, and compute_takeoff when quantities are asked for.

            Positions are [x, z] on the floor or [x, height, z] in metres, heights above the floor.
            Use the ids tools return (W1, O3, F8, I10). Keep replies to two or three short
            sentences, plain words, no lists. You have at most $MAX_ROUNDS tool rounds per request.
        """.trimIndent()
    }
}
