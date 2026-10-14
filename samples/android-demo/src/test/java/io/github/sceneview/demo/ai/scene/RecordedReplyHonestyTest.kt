package io.github.sceneview.demo.ai.scene

import io.github.sceneview.demo.demos.internal.DebugPlane
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The recorded demo never states a figure it did not get from a tool. Its choices were written
 * for the sample room, but on any other scan its replies still read the tool results: no frame
 * outside the session, no wall size, window or area written in advance, and a refusal said.
 */
class RecordedReplyHonestyTest {

    /** Every tool call, every result and the digest the provider was shown, as one text. */
    private class Witness(private val inner: AiProvider) : AiProvider by inner {
        var seen: List<AiMessage> = emptyList()

        override fun run(turn: AiTurn): Flow<AiEvent> {
            seen = turn.messages
            return inner.run(turn)
        }

        fun toolText(): String = seen.flatMap { it.content }.joinToString("\n") { content ->
            when (content) {
                is AiContent.ToolUse -> content.input.toString()
                is AiContent.ToolResult -> content.content
                is AiContent.Text -> if (content.text.startsWith(RecordedAiProvider.DIGEST_MARK)) content.text else ""
                else -> ""
            }
        }
    }

    private class Run(val state: AgentState, val toolText: String) {
        val replies: List<String> = state.lines.filter { it.kind == ChatLine.Kind.Assistant }.map { it.text }
    }

    private suspend fun runBoth(session: SpatialSession, dispatcher: kotlinx.coroutines.CoroutineDispatcher): Run {
        val agent = SceneAgent(SceneToolExecutor(session), computeContext = dispatcher, applyContext = dispatcher)
        val witness = Witness(RecordedAiProvider(paceMillis = 0))
        agent.ask(witness, RecordedAiProvider.REBUILD_PROMPT)
        agent.ask(witness, RecordedAiProvider.READING_CORNER_PROMPT)
        return Run(agent.state.value, witness.toolText())
    }

    /** Each number of [reply] — ids aside — that no tool call, result or digest holds. */
    private fun unsourced(reply: String, toolText: String): List<String> {
        val sourced = NUMBER.findAll(toolText).map { it.value.toDouble() }.toSet()
        val ids = ID.findAll(reply).map { it.value }.toList()
        ids.forEach { id -> assertTrue("$id is an id a tool gave", toolText.contains("\"$id\"")) }
        return NUMBER.findAll(ID.replace(reply, " ")).map { it.value }
            .filter { n -> sourced.none { abs(it - n.toDouble()) < TOLERANCE } }
            .toList()
    }

    @Test
    fun `on a scan other than the sample, every figure in a reply comes from a tool`() = runTest {
        val run = runBoth(otherRoom, UnconfinedTestDispatcher(testScheduler))

        // The rays stay inside the session's keyframes.
        val refused = run.state.lines.filter { it.kind == ChatLine.Kind.ToolRefused }.map { it.text }
        assertTrue(refused.toString(), refused.none { it.startsWith("Invalid") })
        // The sample's figures are not quoted for a room that does not have them.
        val all = run.replies.joinToString(" ")
        listOf("1.96 × 2.33", "1.60 × 1.27", "2.53 m²", "0.17 m").forEach {
            assertTrue("\"$it\" quoted on another room: $all", !all.contains(it))
        }
        run.replies.forEach { reply -> assertEquals(reply, emptyList<String>(), unsourced(reply, run.toolText)) }
        // A refusal is said, not hidden.
        if (run.state.model.rejections.isNotEmpty() || refused.isNotEmpty()) {
            assertTrue(all, all.contains("Refused") || all.contains("did not go in") || all.contains("refused"))
        }
    }

    @Test
    fun `on the sample, the replies quote only what the tools computed`() = runTest {
        val run = runBoth(ShowcaseSession.session, UnconfinedTestDispatcher(testScheduler))
        run.replies.forEach { reply -> assertEquals(reply, emptyList<String>(), unsourced(reply, run.toolText)) }
        assertTrue(run.replies.joinToString(" ").contains("Refused"))
    }

    @Test
    fun `the rays spread over the keyframes the session has`() {
        assertEquals(listOf(0, 40, 120, 160), RecordedAiProvider.rayFrames(ShowcaseSession.session.keyframes.size))
        assertEquals(listOf(0, 7, 23, 31), RecordedAiProvider.rayFrames(OTHER_KEYFRAMES))
        assertEquals(listOf(0), RecordedAiProvider.rayFrames(1))
        assertEquals(emptyList<Int>(), RecordedAiProvider.rayFrames(0))
    }

    private companion object {
        val NUMBER = Regex("""\d+(?:\.\d+)?""")
        val ID = Regex("""\b[A-Z]\d+\b""")
        const val TOLERANCE = 0.006
        const val OTHER_KEYFRAMES = 32
        const val SHIFT_X = 0.9f
        const val SHIFT_Z = 1.3f

        /**
         * Another room, as a fresh scan of the user's own: the sample's cloud and planes moved
         * aside, its walk cut to the 32 keyframes of a short scan. The recording's choices miss.
         */
        val otherRoom: SpatialSession by lazy {
            val s = ShowcaseSession.session
            fun shifted(xyz: FloatArray) = xyz.copyOf().also {
                for (i in 0 until it.size / 3) {
                    it[i * 3] += SHIFT_X
                    it[i * 3 + 2] += SHIFT_Z
                }
            }
            val keyframes = s.keyframes.take(OTHER_KEYFRAMES).map { k ->
                SpatialKeyframe(k.index, k.time, k.pose.copy(x = k.pose.x + SHIFT_X, z = k.pose.z + SHIFT_Z), k.path)
            }
            SpatialSession(
                points = shifted(s.points),
                colors = s.colors,
                planes = s.planes.map { DebugPlane(it.id, it.kind, shifted(it.polygon)) },
                keyframes = keyframes,
                lens = s.lens,
                floorY = s.floorY,
                poseTimes = FloatArray(keyframes.size) { keyframes[it].time },
                poses = keyframes.map { it.pose },
            )
        }
    }
}
