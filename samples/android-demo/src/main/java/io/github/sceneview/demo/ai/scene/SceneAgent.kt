package io.github.sceneview.demo.ai.scene

import io.github.sceneview.ai.AiContent
import io.github.sceneview.ai.AiConversation
import io.github.sceneview.ai.AiEvent
import io.github.sceneview.ai.AiProvider
import io.github.sceneview.ai.AiUsageLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** Every request made to a provider since the last [reset]: tokens, latency, tools, errors. */
    val usage = AiUsageLog()

    private val conversation = AiConversation(SYSTEM_PROMPT, SceneToolExecutor.SPECS)
    private val undoStack = ArrayDeque<RoomModel>()
    private var pendingNote: String? = null
    private var lineIds = 0

    /** Runs [prompt] through [provider] to the end of its tool calls. Ignored while busy. */
    suspend fun ask(provider: AiProvider, prompt: String) {
        if (state.value.busy || prompt.isBlank()) return
        val before = state.value.model
        val opening = buildList {
            if (conversation.isEmpty) {
                add(AiContent.Text("Session digest: ${executor.digest()}"))
                if (provider.capabilities.images) addAll(keyframes())
            }
            pendingNote?.let { add(AiContent.Text(it)) }
            add(AiContent.Text(RecordedAiProvider.PROMPT_MARK + prompt.trim()))
        }
        pendingNote = null
        conversation.user(opening)
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
            val replyId = ++lineIds
            val result = conversation.round(provider) { text ->
                val reply = ChatLine(replyId, ChatLine.Kind.Assistant, text.trim())
                apply { s -> s.copy(lines = s.lines.filterNot { it.id == replyId } + reply) }
            }
            usage.add(provider.id, provider.model, round, result)
            result.usage?.let { tokens ->
                apply {
                    it.copy(
                        inputTokens = it.inputTokens + tokens.inputTokens,
                        outputTokens = it.outputTokens + tokens.outputTokens,
                    )
                }
            }
            result.error?.let { message ->
                apply { it.copy(lines = it.lines + line(ChatLine.Kind.Error, message)) }
                return
            }
            if (result.calls.isEmpty()) return
            val results = result.calls.map { call ->
                val outcome = runTool(call)
                AiContent.ToolResult(call.id, outcome.content, outcome.isError)
            }
            conversation.toolResults(results)
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
        conversation.clear()
        usage.clear()
        undoStack.clear()
        pendingNote = null
        mutableState.value = AgentState()
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
            1. propose_room first: the code reads the whole scan and lists the wall lines it found
               (C1, C2…) with the gaps in them (G1, G2…, guessed door, window, occluded or unknown),
               the furniture clusters (K1, K2…) and the ceiling height.
            2. Reconstruct from that list: build_room with the C ids of the lines that bound the
               room (the code closes them into a polygon and measures), accept_opening for the gaps
               that are doors or windows, accept_object to label each cluster that is a piece of
               furniture. Use add_wall, add_opening and add_object only for what the proposal missed,
               after checking with list_planes, raycast, points_in_box or measure. Each call is
               checked against the points: what the scan never saw is refused — leave it out rather
               than insist.
            3. Only then place catalogue items: free_floor_area, place_item, add_dimension,
               set_material, and compute_takeoff when quantities are asked for.

            Positions are [x, z] on the floor or [x, height, z] in metres, heights above the floor.
            Use the ids tools return (C2, G1, K3, W1, D2, O3, F8, I10). Keep replies to two or three
            short sentences, plain words, no lists. You have at most $MAX_ROUNDS tool rounds per request.
        """.trimIndent()
    }
}
