package io.github.sceneview.demo.ai.scene

import io.github.sceneview.ai.AiCapabilities
import io.github.sceneview.ai.AiContent
import io.github.sceneview.ai.AiEvent
import io.github.sceneview.ai.AiMessage
import io.github.sceneview.ai.AiProvider
import io.github.sceneview.ai.AiStatus
import io.github.sceneview.ai.AiTurn
import io.github.sceneview.demo.ai.scene.RecordedAiProvider.Ran
import io.github.sceneview.demo.ai.scene.RecordedAiProvider.Step
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Offline reconstruction for QA. Chooses only ids and size-based labels from propose_room;
 * the same tools as a live provider compute all geometry and every number in the reply.
 * Stateless: a new request (including one after Undo) reads only its own tool results.
 */
internal class ScriptedRebuildProvider : AiProvider {
    override val id = "scripted-rebuild"
    override val capabilities = AiCapabilities(tools = true, images = false, live = false)
    override suspend fun status(): AiStatus = AiStatus.Ready

    override fun run(turn: AiTurn): Flow<AiEvent> = flow {
        val step = stepFor(turn.messages)
        emit(AiEvent.TextDelta(step.text))
        step.calls.forEachIndexed { index, (name, input) ->
            val json = Json.parseToJsonElement(input).jsonObject
            emit(AiEvent.ToolCall("scripted_${turn.messages.size}_$index", name, json))
        }
        emit(AiEvent.Done(if (step.calls.isEmpty()) "end_turn" else "tool_use"))
    }

    private fun stepFor(messages: List<AiMessage>): Step {
        val asked = messages.indexOfLast { message ->
            message.role == AiMessage.Role.User && message.content.any {
                it is AiContent.Text && it.text.startsWith(RecordedAiProvider.PROMPT_MARK)
            }
        }
        val prompt = messages.getOrNull(asked)?.content?.filterIsInstance<AiContent.Text>()
            ?.firstOrNull { it.text.startsWith(RecordedAiProvider.PROMPT_MARK) }
            ?.text?.removePrefix(RecordedAiProvider.PROMPT_MARK)?.trim()
        if (!prompt.equals(RecordedAiProvider.REBUILD_PROMPT, ignoreCase = true)) {
            return Step("The scripted provider only rebuilds the scan. Choose Rebuild this room to run it.")
        }
        val ran = RecordedAiProvider.ran(messages.drop(asked + 1))
        val proposal = ran.lastOrNull { it.name == SceneToolExecutor.PROPOSE_ROOM }
            ?: return Step(
                "Reading the scan for walls, gaps and objects.", listOf(SceneToolExecutor.PROPOSE_ROOM to "{}"),
            )
        val result = proposal.result ?: return Step("The scan could not be read; no reconstruction was added.")
        val build = ran.lastOrNull { it.name == SceneToolExecutor.BUILD_ROOM }
        val calls = when {
            build == null && ran.none { it.name == SceneToolExecutor.ACCEPT_OBJECT } -> reconstructionCalls(result)
            build?.result != null -> openingCalls(result, ran)
            else -> emptyList()
        }
        return if (calls.isEmpty()) Step(summary(ran, build))
        else Step("Checking the chosen scan candidates; object labels are size guesses.", calls)
    }

    private fun reconstructionCalls(proposal: JsonObject): List<Pair<String, String>> = buildList {
        val walls = proposal.objects("walls")
        if (walls.isNotEmpty()) {
            add(SceneToolExecutor.BUILD_ROOM to buildJsonObject {
                put("wall_ids", JsonArray(walls.mapNotNull { it["id"] }))
            }.toString())
        }
        proposal.objects("objects").forEach { cluster ->
            add(SceneToolExecutor.ACCEPT_OBJECT to buildJsonObject {
                put("cluster_id", cluster.text("id"))
                put("category", category(cluster))
            }.toString())
        }
    }

    private fun openingCalls(proposal: JsonObject, ran: List<Ran>): List<Pair<String, String>> =
        proposal.objects("walls").flatMap { it.objects("gaps") }.filter { gap ->
            gap.text("guess") in listOf("door", "window") &&
                ran.none { it.name == SceneToolExecutor.ACCEPT_OPENING && it.asked("gap_id") == gap.text("id") }
        }.map { gap ->
            SceneToolExecutor.ACCEPT_OPENING to buildJsonObject {
                put("gap_id", gap.text("id"))
                put("kind", gap.text("guess"))
            }.toString()
        }

    private fun summary(ran: List<Ran>, build: Ran?): String = buildString {
        val walls = build?.result?.objects("walls").orEmpty()
        val objects = ran.count { it.name == SceneToolExecutor.ACCEPT_OBJECT && it.result != null }
        val openings = ran.count { it.name == SceneToolExecutor.ACCEPT_OPENING && it.result != null }
        append("Rebuilt ${walls.size} walls, $openings openings and $objects objects from the scan.")
        build?.num("area_m2")?.let { append(" Floor area ${SceneToolExecutor.fmt(it)} m².") }
        if (walls.isNotEmpty() && build?.num("area_m2") == null) append(" The outline is open; area is unknown.")
        val refused = ran.count { !it.ok }
        if (refused > 0) append(" $refused tool calls were refused.")
        if (objects > 0) append(" Object labels are size guesses.")
    }

    private fun category(cluster: JsonObject): String {
        val sides = (cluster["size"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.floatOrNull }.orEmpty()
        val height = (cluster["height_m"] as? JsonPrimitive)?.floatOrNull ?: return "other"
        if (sides.size != 2) return "other"
        val long = sides.max()
        val short = sides.min()
        return when {
            height >= CABINET_MIN_HEIGHT_M -> "cabinet"
            long >= SOFA_MIN_LENGTH_M && short >= SOFA_MIN_DEPTH_M && height <= SOFA_MAX_HEIGHT_M -> "sofa"
            height in TABLE_HEIGHT_M && long >= TABLE_MIN_LENGTH_M && short >= TABLE_MIN_DEPTH_M -> "table"
            else -> "other"
        }
    }

    private fun JsonObject.objects(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()

    private fun JsonObject.text(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

    private companion object {
        const val CABINET_MIN_HEIGHT_M = 1.2f
        const val SOFA_MIN_LENGTH_M = 1.2f
        const val SOFA_MIN_DEPTH_M = 0.65f
        const val SOFA_MAX_HEIGHT_M = 1.1f
        val TABLE_HEIGHT_M = 0.65f..1.05f
        const val TABLE_MIN_LENGTH_M = 0.7f
        const val TABLE_MIN_DEPTH_M = 0.4f
    }
}
