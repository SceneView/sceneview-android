package io.github.sceneview.demo.ai.scene

import io.github.sceneview.ai.AiEvent
import io.github.sceneview.ai.AiProvider
import io.github.sceneview.ai.AiTurn
import io.github.sceneview.demo.demos.internal.Vec3
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Make the wall blue" on the rebuilt sample room: one set_material call paints the walls, never
 * a detour through free floor; undo brings the plaster back; asked before any rebuild, it says
 * plainly that there is nothing to paint yet.
 */
class PaintEditTest {

    /** The recorded provider, with every tool it calls written down. */
    private class Spy(private val inner: AiProvider = RecordedAiProvider(paceMillis = 0)) : AiProvider by inner {
        val calls = mutableListOf<String>()
        override fun run(turn: AiTurn): Flow<AiEvent> =
            inner.run(turn).onEach { if (it is AiEvent.ToolCall) calls += it.name }
    }

    private fun agent(dispatcher: CoroutineDispatcher) =
        SceneAgent(SceneToolExecutor(ShowcaseSession.session), computeContext = dispatcher, applyContext = dispatcher)

    @Test
    fun `make the wall blue calls set_material and paints the wall`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        agent.ask(RecordedAiProvider(paceMillis = 0), RecordedAiProvider.REBUILD_PROMPT)
        val walls = agent.state.value.model.walls.map { it.id }
        assertTrue(walls.isNotEmpty())

        val spy = Spy()
        agent.ask(spy, RecordedAiProvider.PAINT_WALL_PROMPT)
        val state = agent.state.value

        assertEquals(listOf(SceneToolExecutor.SET_MATERIAL), spy.calls)
        walls.forEach { assertEquals("blue", state.model.finishOf(it)) }
        assertFalse("a colour change is not a rebuild", state.structureChanged)
        assertTrue(state.lines.last { it.kind == ChatLine.Kind.Assistant }.text.contains("blue"))

        agent.undo()
        walls.forEach { assertNull(agent.state.value.model.finishOf(it)) }
    }

    @Test
    fun `the French request paints the walls too`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        agent.ask(RecordedAiProvider(paceMillis = 0), RecordedAiProvider.REBUILD_PROMPT)
        agent.ask(RecordedAiProvider(paceMillis = 0), "change le mur en bleu")
        val model = agent.state.value.model
        model.walls.forEach { assertEquals("blue", model.finishOf(it.id)) }
    }

    @Test
    fun `before any rebuild the request fails plainly and nothing else runs`() = runTest {
        val agent = agent(UnconfinedTestDispatcher(testScheduler))
        val spy = Spy()
        agent.ask(spy, RecordedAiProvider.PAINT_WALL_PROMPT)
        val state = agent.state.value

        assertEquals(listOf(SceneToolExecutor.SET_MATERIAL), spy.calls)
        assertTrue(state.model.finishes.isEmpty())
        assertEquals(1, state.lines.count { it.kind == ChatLine.Kind.ToolRefused })
        val reply = state.lines.last { it.kind == ChatLine.Kind.Assistant }.text
        assertTrue(reply, reply.contains("could not paint") && reply.contains("rebuild the room first"))
    }

    @Test
    fun `set_material paints the floor and refuses a product with the targets it can take`() {
        val executor = SceneToolExecutor(ShowcaseSession.session)
        val model = RoomModel(
            walls = listOf(RoomWall("W1", FloorPoint(0f, 0f), FloorPoint(2f, 0f), 2.4f, "test")),
            items = listOf(
                PlacedItem("I2", "armchair", BOX, "linen"),
                PlacedItem("I3", "rug", BOX, "sage"),
            ),
        )
        fun input(target: String, finish: String) = buildJsonObject {
            put("target_id", JsonPrimitive(target))
            put("finish", JsonPrimitive(finish))
        }

        val floor = executor.execute(model, SceneToolExecutor.SET_MATERIAL, input("floor", "walnut"))
        assertFalse(floor.isError)
        assertEquals("walnut", floor.model.finishOf(SceneToolExecutor.FLOOR))

        val rug = executor.execute(model, SceneToolExecutor.SET_MATERIAL, input("I3", "blue"))
        assertEquals("blue", rug.model.items.first { it.id == "I3" }.material)

        val chair = executor.execute(model, SceneToolExecutor.SET_MATERIAL, input("I2", "blue"))
        assertTrue(chair.isError)
        assertTrue(chair.content, chair.content.contains("W1") && chair.content.contains("I3"))
        assertEquals(model, chair.model)
    }

    private companion object {
        val BOX = OrientedBox(Vec3(0f, 0.5f, 1f), Vec3(0.5f, 1f, 0.5f), 0f)
    }
}
