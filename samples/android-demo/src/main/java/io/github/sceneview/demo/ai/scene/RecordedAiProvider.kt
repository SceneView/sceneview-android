package io.github.sceneview.demo.ai.scene

import io.github.sceneview.ai.AiCapabilities
import io.github.sceneview.ai.AiContent
import io.github.sceneview.ai.AiEvent
import io.github.sceneview.ai.AiMessage
import io.github.sceneview.ai.AiProvider
import io.github.sceneview.ai.AiStatus
import io.github.sceneview.ai.AiTurn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The recorded demo: model turns written once against the bundled Rerun session, replayed
 * without a network or a key. Only the *choices* are recorded — which wall to propose, which
 * object, which slot. Every tool call still runs on the real session through
 * [SceneToolExecutor], so the rejected wall is rejected by the points, the radiator's height comes
 * from the cloud, and the takeoff is computed, exactly as with a live model.
 *
 * The recording knows the sample room only; the screen keeps it off a scan of the user's own.
 * Even so, nothing it says is written in advance: every figure in a reply is read from a tool
 * result of the conversation, a refused or failed tool is said to be, and the frames it casts
 * rays through are spread over the keyframes the session actually has.
 *
 * [paceMillis] is the delay between streamed words, so the reply reads as it arrives; tests pass zero.
 */
class RecordedAiProvider(private val paceMillis: Long = DEFAULT_PACE_MS) : AiProvider {

    override val id: String = ID

    override val capabilities = AiCapabilities(tools = true, images = false, live = false)

    override suspend fun status(): AiStatus = AiStatus.Ready

    override fun run(turn: AiTurn): Flow<AiEvent> = flow {
        val step = stepFor(turn.messages)
        for (word in step.text.split(' ')) {
            emit(AiEvent.TextDelta("$word "))
            if (paceMillis > 0) delay(paceMillis)
        }
        step.calls.forEachIndexed { i, (name, input) ->
            emit(AiEvent.ToolCall("rec_${turn.messages.size}_$i", name, Json.parseToJsonElement(input).jsonObject))
        }
        emit(AiEvent.Done(if (step.calls.isEmpty()) "end_turn" else "tool_use"))
    }

    /** One recorded assistant turn: what it says, then the calls it makes (name, JSON input). */
    data class Step(val text: String, val calls: List<Pair<String, String>> = emptyList())

    /** A tool call of the conversation and what the executor answered: [result] parsed, or [error]. */
    data class Ran(val name: String, val input: JsonObject, val result: JsonObject?, val error: String?) {
        val ok: Boolean get() = error == null
        fun num(key: String): Float? = result?.get(key)?.jsonPrimitive?.floatOrNull
        fun str(key: String): String? = result?.get(key)?.jsonPrimitive?.contentOrNull
        fun asked(key: String): String? = input[key]?.jsonPrimitive?.contentOrNull
    }

    /** What a recorded turn reads: the session's keyframe count, and the calls run so far. */
    class Context(val keyframes: Int, val all: List<Ran>, val request: List<Ran>) {
        /** The id the executor gave the last object of [category] it kept, `null` if none. */
        fun objectId(category: String): String? =
            all.lastOrNull { it.name == SceneToolExecutor.ADD_OBJECT && it.ok && it.str("category") == category }
                ?.str("id")
    }

    /** A request the recording answers, matched on its prompt. */
    enum class Request(val prompt: String, val steps: List<(Context) -> Step>) {
        Rebuild(REBUILD_PROMPT, listOf(::readScan, ::proposeRoom, ::rebuildSummary)),
        ReadingCorner(
            READING_CORNER_PROMPT,
            listOf({ _ -> FIND_FREE_FLOOR }, ::placeCorner, ::dimensionCorner, ::cornerSummary),
        ),
    }

    companion object {
        const val ID = "recorded"
        const val DEFAULT_PACE_MS = 28L
        const val REBUILD_PROMPT = "Rebuild this room"
        const val READING_CORNER_PROMPT = "Add a reading corner"

        /** How the agent marks the user's own words inside a turn that also carries the digest. */
        const val PROMPT_MARK = "Request: "

        /** How the agent opens a conversation: the executor's digest of the session. */
        const val DIGEST_MARK = "Session digest: "

        /** The frames the recording casts its rays through on the sample, which has more keyframes. */
        private val SAMPLE_RAY_FRAMES = listOf(0, 40, 120, 160)

        private fun isPrompt(content: AiContent) =
            content is AiContent.Text && content.text.startsWith(PROMPT_MARK)

        /**
         * The step to replay: the last user prompt picks the request, the assistant turns since
         * it say how far into it the conversation is, the tool results so far say what to write.
         */
        fun stepFor(messages: List<AiMessage>): Step {
            val asked = messages.indexOfLast { m ->
                m.role == AiMessage.Role.User && m.content.any(::isPrompt)
            }
            if (asked < 0) return UNKNOWN
            val prompt = (messages[asked].content.first(::isPrompt) as AiContent.Text)
                .text.removePrefix(PROMPT_MARK).trim()
            val request = Request.entries.firstOrNull { prompt.equals(it.prompt, ignoreCase = true) } ?: return UNKNOWN
            val done = messages.drop(asked + 1).count { it.role == AiMessage.Role.Assistant }
            val step = request.steps.getOrNull(done) ?: return Step("")
            return step(Context(keyframesOf(messages), ran(messages), ran(messages.drop(asked + 1))))
        }

        /**
         * The rays' frames, inside the session: the sample's own where it has them, else the same
         * spread over its 0..[keyframes]−1.
         */
        fun rayFrames(keyframes: Int): List<Int> {
            if (keyframes <= 0) return emptyList()
            val last = SAMPLE_RAY_FRAMES.last()
            if (keyframes > last) return SAMPLE_RAY_FRAMES
            return SAMPLE_RAY_FRAMES.map { it * (keyframes - 1) / last }.distinct()
        }

        private fun keyframesOf(messages: List<AiMessage>): Int {
            val digest = messages.firstOrNull { it.role == AiMessage.Role.User }?.content
                ?.filterIsInstance<AiContent.Text>()
                ?.firstOrNull { it.text.startsWith(DIGEST_MARK) }
                ?: return 0
            return runCatching {
                Json.parseToJsonElement(digest.text.removePrefix(DIGEST_MARK)).jsonObject["keyframes"]
                    ?.jsonPrimitive?.intOrNull
            }.getOrNull() ?: 0
        }

        /** Every tool call in [messages], paired with the result that answered it. */
        internal fun ran(messages: List<AiMessage>): List<Ran> {
            val results = messages.flatMap { it.content }.filterIsInstance<AiContent.ToolResult>()
                .associateBy { it.toolUseId }
            return messages.flatMap { it.content }.filterIsInstance<AiContent.ToolUse>().mapNotNull { use ->
                val result = results[use.id] ?: return@mapNotNull null
                val parsed = if (result.isError) null else runCatching {
                    Json.parseToJsonElement(result.content).jsonObject
                }.getOrNull()
                val error = if (result.isError) result.content.removePrefix("error: ") else null
                Ran(use.name, use.input, parsed, error)
            }
        }

        private val UNKNOWN = Step(
            "This recorded demo replays two requests on the bundled scan: \"$REBUILD_PROMPT\" and " +
                "\"$READING_CORNER_PROMPT\". Add your own Claude or Gemini key in the settings to ask anything else.",
        )

        // ─── Rebuild ────────────────────────────────────────────────────────────────────────

        private fun readScan(context: Context): Step {
            val frames = rayFrames(context.keyframes)
            val text = if (frames.isEmpty()) {
                "First I read what the scan holds: the planes ARCore found. This scan has no keyframe to cast " +
                    "a ray through."
            } else {
                "First I read what the scan holds: the planes ARCore found, then a ray through keyframes spread " +
                    "over the walk, to see what the camera was looking at."
            }
            return Step(
                text,
                listOf(SceneToolExecutor.LIST_PLANES to "{}") + frames.map { frame ->
                    SceneToolExecutor.RAYCAST to """{"frame":$frame,"x":0.5,"y":${if (frame == 0) 0.6 else 0.5}}"""
                },
            )
        }

        private fun proposeRoom(context: Context): Step {
            val hits = context.request.filter { it.name == SceneToolExecutor.RAYCAST && it.ok }
                .mapNotNull { ray -> ray.str("target")?.let { "frame ${ray.asked("frame")}, $it" } }
            val seen = if (hits.isEmpty()) "No ray came back. " else "What the rays met: ${hits.joinToString("; ")}. "
            return Step(
                seen + "Now I propose the room I expect: a wall behind the radiator with its window, a second " +
                    "one along the bed, then the bed, the desk, the office chair, the plant and the radiator. " +
                    "The points decide which stay.",
                listOf(
                    SceneToolExecutor.ADD_WALL to """{"from":[1.98,-2.52],"to":[2.84,-0.76],"height_m":2.4}""",
                    SceneToolExecutor.ADD_WALL to """{"from":[1.69,-3.08],"to":[-1.27,-1.62],"height_m":2.4}""",
                    SceneToolExecutor.ADD_OPENING to
                        """{"wall_id":"W1","kind":"window","from_m":0.25,"to_m":1.85,"sill_m":0.88,"head_m":2.15}""",
                    SceneToolExecutor.ADD_OBJECT to
                        """{"category":"bed","center":[-0.02,-1.59],"size":[1.0,2.2],"yaw_deg":-63.7}""",
                    SceneToolExecutor.ADD_OBJECT to
                        """{"category":"desk","center":[1.72,-0.55],"size":[0.75,2.1],"yaw_deg":-63.7}""",
                    SceneToolExecutor.ADD_OBJECT to
                        """{"category":"office_chair","center":[1.76,-1.05],"size":[0.6,0.6],"yaw_deg":-63.7}""",
                    SceneToolExecutor.ADD_OBJECT to
                        """{"category":"plant","center":[1.46,-2.09],"size":[0.45,0.45],"yaw_deg":-63.7}""",
                    SceneToolExecutor.ADD_OBJECT to
                        """{"category":"radiator","center":[2.22,-1.75],"size":[1.0,0.2],"yaw_deg":-63.7}""",
                ),
            )
        }

        private fun rebuildSummary(context: Context): Step {
            val walls = context.request.filter { it.name == SceneToolExecutor.ADD_WALL }
            val openings = context.request.filter { it.name == SceneToolExecutor.ADD_OPENING }
            val objects = context.request.filter { it.name == SceneToolExecutor.ADD_OBJECT }
            val keptWalls = walls.filter { it.ok }
            val keptObjects = objects.filter { it.ok }
            if (keptWalls.isEmpty() && keptObjects.isEmpty()) {
                return Step(
                    "Nothing I proposed matches this scan: the points refused every wall and every object, so " +
                        "the room stays empty instead of invented.",
                )
            }
            val built = buildList {
                keptWalls.forEach { wall ->
                    val window = openings.firstOrNull { it.ok && it.str("wall_id") == wall.str("id") }
                    add(
                        "wall ${wall.str("id")}, ${size(wall.num("length_m"), wall.num("height_m"))} m" +
                            (window?.let { ", with a ${size(it.num("width_m"), it.num("height_m"))} m window" } ?: ""),
                    )
                }
                if (keptObjects.isNotEmpty()) {
                    add(spoken(keptObjects.map { "the ${label(it.str("category"))}" }) +
                        if (keptObjects.size == 1) ", fitted to its own points" else ", each fitted to its own points")
                }
            }
            val refused = buildList {
                val rejectedWalls = walls.count { !it.ok }
                if (rejectedWalls > 0) {
                    add(if (keptWalls.isEmpty()) "every wall I proposed" else "the other wall I proposed")
                }
                objects.filter { !it.ok }.forEach { add("the ${label(it.asked("category"))}") }
            }
            val failed = openings.filter { !it.ok }.map { "The window was not added: ${it.error?.trimEnd('.')}." }
            val text = buildString {
                append("Rebuilt from the scan: ${built.joinToString(", and ")}.")
                if (refused.isNotEmpty()) {
                    append(" Refused, because the scan never saw them: ${spoken(refused)}. ")
                    append(
                        if (keptWalls.isEmpty()) "No wall stands, so the sides stay open." else "That side stays open.",
                    )
                }
                failed.forEach { append(" $it") }
            }
            return Step(text)
        }

        // ─── Reading corner ─────────────────────────────────────────────────────────────────

        private val FIND_FREE_FLOOR = Step(
            "Looking for floor the camera saw empty, with a little room to spare around what already stands.",
            listOf(SceneToolExecutor.FREE_FLOOR_AREA to """{"clearance_m":0.05}"""),
        )

        private fun placeCorner(context: Context): Step {
            val floor = context.request.lastOrNull { it.name == SceneToolExecutor.FREE_FLOOR_AREA }
            val regions = (floor?.result?.get("regions") as? JsonArray)
                ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
                .orEmpty()
            if (floor == null || !floor.ok || regions.isEmpty()) {
                val why = floor?.error ?: "no region came back"
                return Step("There is no free floor to put a reading corner on: $why.")
            }
            val roomiest = regions.first()
            val side = regions.getOrElse(1) { roomiest }
            val facing = context.objectId("radiator")
            val window = context.all.any { it.name == SceneToolExecutor.ADD_OPENING && it.ok }
            val toward = when {
                facing == null -> ""
                window -> " facing the window for the daylight"
                else -> " facing the radiator"
            }
            return Step(
                "$roomiest is the roomiest free spot. The armchair goes there$toward, and the floor lamp at its " +
                    "side in $side.",
                listOf(
                    SceneToolExecutor.PLACE_ITEM to
                        """{"category":"armchair","region_id":"$roomiest","slot":"center",""" +
                            """"yaw_toward_id":"${facing ?: ""}"}""",
                    SceneToolExecutor.PLACE_ITEM to
                        """{"category":"floor_lamp","region_id":"$side","slot":"front_left","yaw_toward_id":""}""",
                ),
            )
        }

        private fun dimensionCorner(context: Context): Step {
            val chair = placed(context, "armchair")
            val bed = context.objectId("bed")
            if (chair == null || !chair.ok) {
                return Step(
                    "The armchair did not go in: ${chair?.error?.trimEnd('.') ?: "it was never placed"}. " +
                        "The takeoff of what stands, then.",
                    listOf(SceneToolExecutor.COMPUTE_TAKEOFF to "{}"),
                )
            }
            val id = chair.str("id")
            return Step(
                if (bed != null) "Dimensioning the armchair and its clearance to the bed, then the takeoff."
                else "Dimensioning the armchair, then the takeoff.",
                buildList {
                    add(SceneToolExecutor.ADD_DIMENSION to """{"item_id":"$id","kind":"width","to_id":""}""")
                    if (bed != null) {
                        val clearance = """{"item_id":"$id","kind":"clearance","to_id":"$bed"}"""
                        add(SceneToolExecutor.ADD_DIMENSION to clearance)
                    }
                    add(SceneToolExecutor.COMPUTE_TAKEOFF to "{}")
                },
            )
        }

        private fun cornerSummary(context: Context): Step {
            val chair = placed(context, "armchair")
            val lamp = placed(context, "floor_lamp")
            val clearance = context.request.lastOrNull {
                it.name == SceneToolExecutor.ADD_DIMENSION && it.asked("kind") == "clearance"
            }
            val takeoff = context.request.lastOrNull { it.name == SceneToolExecutor.COMPUTE_TAKEOFF && it.ok }
            val text = buildString {
                if (chair != null && chair.ok) {
                    append("Reading corner in place: a ${size(chair.num("width_m"), chair.num("depth_m"))} m armchair")
                    val gap = clearance?.takeIf { it.ok }?.num("value_m")
                    if (gap != null) append(" with ${fmt(gap)} m to the bed")
                    append(".")
                    if (clearance != null && !clearance.ok) {
                        append(" Its clearance was not measured: ${clearance.error}.")
                    }
                } else {
                    append("No reading corner: the armchair did not go in (${chair?.error ?: "never placed"}).")
                }
                when {
                    lamp == null -> Unit
                    lamp.ok -> append(" The floor lamp stands beside it.")
                    else -> append(" The floor lamp did not go in: ${lamp.error}.")
                }
                val net = takeoff?.num("net_wall_area_m2")
                when {
                    takeoff == null -> append(" The takeoff could not be computed.")
                    net == null || (takeoff.num("wall_area_m2") ?: 0f) <= 0f ->
                        append(" No wall stands, so there is no wall to paint.")
                    else -> append(" The walls have ${fmt(net)} m² to paint once the openings are taken off.")
                }
            }
            return Step(text)
        }

        private fun placed(context: Context, category: String): Ran? =
            context.request.lastOrNull { it.name == SceneToolExecutor.PLACE_ITEM && it.asked("category") == category }

        /** "a", "a and b", "a, b and c". */
        private fun spoken(items: List<String>): String =
            if (items.size < 2) items.joinToString() else items.dropLast(1).joinToString(", ") + " and " + items.last()

        private fun label(category: String?): String = (category ?: "object").replace('_', ' ')

        private fun fmt(value: Float): String = SceneToolExecutor.fmt(value)

        private fun size(a: Float?, b: Float?): String = "${fmt(a ?: 0f)} × ${fmt(b ?: 0f)}"
    }
}
