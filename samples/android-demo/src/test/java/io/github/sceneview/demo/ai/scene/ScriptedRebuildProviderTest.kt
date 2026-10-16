package io.github.sceneview.demo.ai.scene

import io.github.sceneview.ai.AiContent
import io.github.sceneview.ai.AiEvent
import io.github.sceneview.ai.AiMessage
import io.github.sceneview.ai.AiTurn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptedRebuildProviderTest {
    private fun agent(dispatcher: CoroutineDispatcher) = SceneAgent(
        SceneToolExecutor(SyntheticScanSession.session), computeContext = dispatcher, applyContext = dispatcher,
    )

    @Test
    fun `synthetic scan closes four walls and labels the measured objects`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        agent.ask(ScriptedRebuildProvider(), RecordedAiProvider.REBUILD_PROMPT)
        val state = agent.state.value
        val plan = checkNotNull(state.model.plan)
        assertTrue(plan.closed)
        assertEquals(4, state.model.walls.size)
        assertEquals(4, plan.corners.size)
        assertEquals(14.4f, checkNotNull(plan.area), 14.4f * 0.03f)
        assertEquals(listOf("cabinet", "other", "sofa", "table"), state.model.objects.map { it.category }.sorted())
        // Furniture occludes the only gaps; neither may become a door or window.
        assertTrue(state.model.openings.isEmpty())
        assertFalse(state.lines.any { it.kind == ChatLine.Kind.ToolRefused || it.kind == ChatLine.Kind.Error })
        assertFalse(state.lines.any { it.text.startsWith("Stopped after") })
        assertFalse(state.busy)
        assertTrue(state.canUndo)
        assertTrue(state.lines.last().text.contains("${SceneToolExecutor.fmt(checkNotNull(plan.area))} m²"))
    }

    @Test
    fun `undo then rebuild reads fresh request results and keeps the cloud boxes`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        val provider = ScriptedRebuildProvider()
        agent.ask(provider, RecordedAiProvider.REBUILD_PROMPT)
        val first = agent.state.value.model
        agent.undo()
        assertTrue(agent.state.value.model.isEmpty)
        agent.ask(provider, RecordedAiProvider.REBUILD_PROMPT)
        assertEquals(first, agent.state.value.model)
        val boxes = RoomReconstruction.propose(SyntheticScanSession.session).objects.map { it.box }
        assertEquals(boxes, agent.state.value.model.objects.map { it.box })
    }

    @Test
    fun `unsupported requests do not mutate the room`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        agent.ask(ScriptedRebuildProvider(), RecordedAiProvider.READING_CORNER_PROMPT)
        assertTrue(agent.state.value.model.isEmpty)
        assertFalse(agent.state.value.canUndo)
        assertTrue(agent.state.value.lines.last().text.contains("only rebuilds"))
    }

    @Test
    fun `openings wait for built walls and only door or window guesses are selected`() = runTest {
        val provider = ScriptedRebuildProvider()
        val messages = proposedGaps().toMutableList()
        val first = provider.run(AiTurn("", messages, emptyList())).toList().filterIsInstance<AiEvent.ToolCall>()
        assertEquals(listOf(SceneToolExecutor.BUILD_ROOM), first.map { it.name })
        assertEquals(listOf("C1"), first.single().input.getValue("wall_ids").jsonArray.map { it.jsonPrimitive.content })
        messages += answered("build", SceneToolExecutor.BUILD_ROOM, """{"walls":[{"id":"W1"}]}""")
        val second = provider.run(AiTurn("", messages, emptyList())).toList().filterIsInstance<AiEvent.ToolCall>()
        assertEquals(listOf("door", "window"), second.map { it.input.getValue("kind").jsonPrimitive.content })
        assertTrue(second.all { it.name == SceneToolExecutor.ACCEPT_OPENING })
        assertEquals(listOf("G1", "G2"), second.map { it.input.getValue("gap_id").jsonPrimitive.content })
    }

    @Test
    fun `failed build is reported without inventing area or trying dependent openings`() = runTest {
        val messages = proposedGaps() + answered("build", SceneToolExecutor.BUILD_ROOM, "error: no walls", error = true)
        val reply = ScriptedRebuildProvider().run(AiTurn("", messages, emptyList())).toList()
        assertTrue(reply.none { it is AiEvent.ToolCall })
        val text = reply.filterIsInstance<AiEvent.TextDelta>().joinToString("") { it.text }
        assertTrue(text.contains("0 walls"))
        assertTrue(text.contains("1 tool calls were refused"))
        assertFalse(text.contains("m²"))
    }

    private fun proposedGaps(): List<AiMessage> = listOf(
        AiMessage(AiMessage.Role.User, listOf(AiContent.Text(
            RecordedAiProvider.PROMPT_MARK + RecordedAiProvider.REBUILD_PROMPT,
        ))),
    ) + answered("proposal", SceneToolExecutor.PROPOSE_ROOM, """
        {"walls":[{"id":"C1","gaps":[
          {"id":"G1","guess":"door"},{"id":"G2","guess":"window"},
          {"id":"G3","guess":"occluded"},{"id":"G4","guess":"unknown"}
        ]}],"objects":[]}
    """.trimIndent())

    private fun answered(id: String, name: String, result: String, error: Boolean = false) = listOf(
        AiMessage(AiMessage.Role.Assistant, listOf(AiContent.ToolUse(id, name, JsonObject(emptyMap())))),
        AiMessage(AiMessage.Role.User, listOf(AiContent.ToolResult(id, result, error))),
    )
}
