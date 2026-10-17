package io.github.sceneview.demo.ai.scene

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recorded demo end to end on the real bundled session: the recorded choices, executed by
 * [SceneToolExecutor] against the cloud. What the screen shows is what these assert.
 */
class RecordedSceneTest {

    private suspend fun rebuilt(agent: SceneAgent) {
        agent.ask(RecordedAiProvider(paceMillis = 0), RecordedAiProvider.REBUILD_PROMPT)
    }

    private fun agent(dispatcher: kotlinx.coroutines.CoroutineDispatcher) =
        SceneAgent(SceneToolExecutor(ShowcaseSession.session), computeContext = dispatcher, applyContext = dispatcher)

    @Test
    fun `rebuild keeps the seen wall, refuses the unseen one and fits five objects`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        rebuilt(agent)
        val state = agent.state.value
        val model = state.model

        assertEquals(1, model.walls.size)
        assertEquals("W1", model.walls[0].id)
        assertEquals(1.96f, model.walls[0].length, 0.03f)
        assertEquals(1, model.rejections.size)
        assertEquals(1, state.lines.count { it.kind == ChatLine.Kind.ToolRefused })
        assertEquals(1, model.openings.size)
        assertEquals(listOf("bed", "desk", "office_chair", "plant", "radiator"), model.objects.map { it.category })
        // The desk sits on ARCore's horizontal plane; the radiator stops under the window sill.
        assertEquals(0.74f, model.objects.first { it.category == "desk" }.box.size.y, 0.02f)
        val radiator = model.objects.first { it.category == "radiator" }.box.size.y
        assertTrue("radiator $radiator m", radiator in 0.7f..0.88f)
        assertFalse(state.busy)
        assertTrue(state.canUndo)
        assertEquals(3, state.lines.count { it.kind == ChatLine.Kind.Assistant })
    }

    @Test
    fun `reading corner places catalogue items on free floor and computes the takeoff`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        rebuilt(agent)
        agent.ask(RecordedAiProvider(paceMillis = 0), RecordedAiProvider.READING_CORNER_PROMPT)
        val state = agent.state.value
        val model = state.model

        assertEquals(listOf("armchair", "floor_lamp"), model.items.map { it.category })
        assertEquals(listOf("I10", "I11"), model.items.map { it.id })
        // Catalogue sizes, never the model's.
        val chair = model.items[0].box
        assertEquals(0.68f, chair.size.x, 1e-3f)
        assertEquals(0.82f, chair.size.z, 1e-3f)
        // Inside its free region, clear of every fitted object.
        val region = model.regions.first { it.id == "F8" }.box
        assertTrue(SceneToolExecutor.fits(region, chair))
        model.objects.forEach { assertFalse(it.id, SceneToolExecutor.overlaps(chair, it.box)) }
        assertEquals(2, model.dimensions.size)
        val clearance = model.dimensions.first { it.kind == DimensionKind.Clearance }.value
        assertEquals(0.17f, clearance, 0.01f)
        assertTrue(state.takeoffShown)
        // No step of the corner was refused.
        assertEquals(1, state.lines.count { it.kind == ChatLine.Kind.ToolRefused })
    }

    @Test
    fun `the recorded replies quote the numbers the code computed`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        rebuilt(agent)
        agent.ask(RecordedAiProvider(paceMillis = 0), RecordedAiProvider.READING_CORNER_PROMPT)
        val takeoff = Takeoff.of(agent.state.value.model)
        val replies = agent.state.value.lines
            .filter { it.kind == ChatLine.Kind.Assistant }
            .joinToString(" ") { it.text }

        assertTrue(replies.contains(takeoff.walls.single().detail.removeSuffix(" m")))
        assertTrue(replies.contains(takeoff.openings.single().detail.removeSuffix(" m")))
        assertTrue(replies.contains(SceneToolExecutor.fmt(takeoff.netWallArea)))
        assertEquals(4.56f, takeoff.wallArea, 0.02f)
        assertEquals(2.53f, takeoff.netWallArea, 0.02f)
    }

    @Test
    fun `undo returns to the room before the last request, reset empties it`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        rebuilt(agent)
        val afterRebuild = agent.state.value.model
        agent.ask(RecordedAiProvider(paceMillis = 0), RecordedAiProvider.READING_CORNER_PROMPT)
        agent.undo()
        assertEquals(afterRebuild, agent.state.value.model)
        agent.undo()
        assertTrue(agent.state.value.model.isEmpty)
        assertFalse(agent.state.value.canUndo)
        agent.reset()
        assertEquals(AgentState(), agent.state.value)
    }

    @Test
    fun `an unknown prompt gets the recorded demo's explanation and no tool call`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        agent.ask(RecordedAiProvider(paceMillis = 0), "Turn the room into a kitchen")
        val state = agent.state.value
        assertTrue(state.model.isEmpty)
        assertTrue(state.lines.last().text.contains(RecordedAiProvider.REBUILD_PROMPT))
    }
}
