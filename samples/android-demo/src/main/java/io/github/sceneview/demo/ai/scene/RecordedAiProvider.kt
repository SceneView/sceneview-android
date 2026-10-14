package io.github.sceneview.demo.ai.scene

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * The recorded demo: model turns written once against the bundled Rerun session, replayed
 * without a network or a key. Only the *choices* are recorded — which wall to propose, which
 * object, which slot. Every tool call still runs on the real session through
 * [SceneToolExecutor], so the rejected wall is rejected by the points, the radiator's height comes
 * from the cloud, and the takeoff is computed, exactly as with a live model.
 *
 * [pace] is the delay between streamed words, so the reply reads as it arrives; tests pass zero.
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

    /** A request the recording answers, matched on its prompt. */
    enum class Request(val prompt: String, val steps: List<Step>) {
        Rebuild(REBUILD_PROMPT, REBUILD),
        ReadingCorner(READING_CORNER_PROMPT, READING_CORNER),
    }

    companion object {
        const val ID = "recorded"
        const val DEFAULT_PACE_MS = 28L
        const val REBUILD_PROMPT = "Rebuild this room"
        const val READING_CORNER_PROMPT = "Add a reading corner"

        /**
         * The step to replay: the last user prompt picks the request, the assistant turns since
         * it say how far into it the conversation is.
         */
        private fun isPrompt(content: AiContent) =
            content is AiContent.Text && content.text.startsWith(PROMPT_MARK)

        fun stepFor(messages: List<AiMessage>): Step {
            val asked = messages.indexOfLast { m ->
                m.role == AiMessage.Role.User && m.content.any(::isPrompt)
            }
            if (asked < 0) return UNKNOWN
            val prompt = (messages[asked].content.first(::isPrompt) as AiContent.Text)
                .text.removePrefix(PROMPT_MARK).trim()
            val request = Request.entries.firstOrNull { prompt.equals(it.prompt, ignoreCase = true) } ?: return UNKNOWN
            val done = messages.drop(asked + 1).count { it.role == AiMessage.Role.Assistant }
            return request.steps.getOrElse(done) { Step("") }
        }

        /** How the agent marks the user's own words inside a turn that also carries the digest. */
        const val PROMPT_MARK = "Request: "

        private val UNKNOWN = Step(
            "This recorded demo replays two requests on the bundled scan: \"$REBUILD_PROMPT\" and " +
                "\"$READING_CORNER_PROMPT\". Add your own Claude key in the settings to ask anything else.",
        )

        private val REBUILD = listOf(
            Step(
                "First I read what the scan holds: the planes ARCore found, then a ray through four keyframes " +
                    "to see what each one was looking at.",
                listOf(
                    SceneToolExecutor.LIST_PLANES to "{}",
                    SceneToolExecutor.RAYCAST to """{"frame":0,"x":0.5,"y":0.6}""",
                    SceneToolExecutor.RAYCAST to """{"frame":40,"x":0.5,"y":0.5}""",
                    SceneToolExecutor.RAYCAST to """{"frame":120,"x":0.5,"y":0.5}""",
                    SceneToolExecutor.RAYCAST to """{"frame":160,"x":0.5,"y":0.5}""",
                ),
            ),
            Step(
                "Plane 3 stands vertical and the ray from frame 160 lands on it, behind the radiator: that is a " +
                    "wall, " +
                    "with the window above. The bed's long side looks like it runs along a second wall, so I propose " +
                    "it too and let the points decide. Then the five things the rays and photos show: the bed, the " +
                    "desk at plane 2, the office chair, the plant and the radiator.",
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
            ),
            Step(
                "Rebuilt from the scan: one wall, 1.96 × 2.33 m, with a 1.60 × 1.27 m window, and five objects " +
                    "fitted to their own points. The wall I guessed behind the bed was refused: the scan never saw " +
                    "it, so that side stays open instead of invented.",
            ),
        )

        private val READING_CORNER = listOf(
            Step(
                "Looking for floor the camera saw empty, with 5 cm to spare around the bed, the desk and the wall.",
                listOf(SceneToolExecutor.FREE_FLOOR_AREA to """{"clearance_m":0.05}"""),
            ),
            Step(
                "F8 is the roomiest spot, between the bed and the desk. The armchair goes there facing the window " +
                    "for the daylight, and the floor lamp at its side in F9.",
                listOf(
                    SceneToolExecutor.PLACE_ITEM to
                        """{"category":"armchair","region_id":"F8","slot":"center","yaw_toward_id":"O7"}""",
                    SceneToolExecutor.PLACE_ITEM to
                        """{"category":"floor_lamp","region_id":"F9","slot":"front_left","yaw_toward_id":""}""",
                ),
            ),
            Step(
                "Dimensioning the armchair and its clearance to the bed, then the takeoff.",
                listOf(
                    SceneToolExecutor.ADD_DIMENSION to """{"item_id":"I10","kind":"width","to_id":""}""",
                    SceneToolExecutor.ADD_DIMENSION to """{"item_id":"I10","kind":"clearance","to_id":"O3"}""",
                    SceneToolExecutor.COMPUTE_TAKEOFF to "{}",
                ),
            ),
            Step(
                "Reading corner in place: a 0.68 × 0.82 m armchair facing the window with 0.17 m to the bed, and a " +
                    "floor lamp beside it. The wall has 2.53 m² to paint once the window is taken off.",
            ),
        )
    }
}
