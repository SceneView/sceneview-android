package io.github.sceneview.ai

import io.github.sceneview.ai.AiMessage.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiConversationTest {

    private val thinking = AiContent.Opaque(buildJsonObject { put("type", "thinking"); put("signature", "sig==") })
    private val call = AiEvent.ToolCall("toolu_1", "list_planes", JsonObject(emptyMap()))

    @Test
    fun `consecutive user content is one message, as the APIs want roles to alternate`() {
        val conversation = AiConversation("s", AiFixtures.TOOLS)
        conversation.user(listOf(AiContent.Text("digest")))
        conversation.user(listOf(AiContent.Text("Request: go")))
        val messages = conversation.snapshot()
        assertEquals(1, messages.size)
        assertEquals(AiMessage.Role.User, messages[0].role)
        assertEquals(2, messages[0].content.size)
        conversation.user(emptyList())
        assertEquals(1, conversation.snapshot().size)
    }

    @Test
    fun `a round keeps the provider's transcript, or rebuilds one from what it streamed`() = runTest {
        val provider = ScriptedProvider(
            listOf(
                listOf(
                    AiEvent.TextDelta("Reading "),
                    AiEvent.TextDelta("the planes."),
                    call,
                    AiEvent.Usage(1200, 85),
                    AiEvent.Done(
                        "tool_use",
                        listOf(
                            thinking,
                            AiContent.Text("Reading the planes."),
                            AiContent.ToolUse(call.id, call.name, call.input),
                        ),
                    ),
                ),
                listOf(AiEvent.TextDelta("Done."), AiEvent.Done("end_turn")),
            ),
        )
        val conversation = AiConversation("s", AiFixtures.TOOLS)
        conversation.user(listOf(AiContent.Text("Request: go")))
        val partials = ArrayList<String>()
        val first = conversation.round(provider) { partials += it }
        assertEquals(listOf("Reading ", "Reading the planes."), partials)
        assertEquals("Reading the planes.", first.text)
        assertEquals(listOf(call), first.calls)
        assertEquals(AiEvent.Usage(1200, 85), first.usage)
        assertEquals("tool_use", first.stopReason)
        assertFalse(first.failed)
        assertTrue(first.latencyMillis >= 0)
        val assistant = conversation.snapshot().last()
        assertEquals(AiMessage.Role.Assistant, assistant.role)
        assertEquals(thinking, assistant.content.first())

        conversation.toolResults(listOf(AiContent.ToolResult(call.id, "{\"planes\":[]}", isError = false)))
        val second = conversation.round(provider)
        assertEquals("Done.", second.text)
        assertTrue(second.calls.isEmpty())
        assertEquals(listOf(AiContent.Text("Done.")), conversation.snapshot().last().content)
        val roles = conversation.snapshot().map { it.role }
        assertEquals(listOf(Role.User, Role.Assistant, Role.User, Role.Assistant), roles)
        assertEquals(3, provider.turns.last().messages.size)
        assertEquals(AiFixtures.TOOLS, provider.turns.last().tools)
    }

    @Test
    fun `an error - reported or thrown - appends nothing, so the next request can retry`() = runTest {
        val conversation = AiConversation("s", AiFixtures.TOOLS)
        conversation.user(listOf(AiContent.Text("Request: go")))
        val reported = conversation.round(ScriptedProvider(listOf(listOf(AiEvent.Error("The key was refused.")))))
        assertTrue(reported.failed)
        assertEquals("The key was refused.", reported.error)
        assertNull(reported.stopReason)
        assertEquals(1, conversation.snapshot().size)

        val thrown = conversation.round(ThrowingProvider)
        assertEquals("kaput", thrown.error)
        assertEquals(1, conversation.snapshot().size)

        conversation.clear()
        assertTrue(conversation.isEmpty)
    }

    @Test
    fun `the usage log sums the rounds and prices them`() = runTest {
        val provider = ScriptedProvider(
            listOf(
                listOf(call, AiEvent.Usage(1000, 300), AiEvent.Done("tool_use")),
                listOf(AiEvent.TextDelta("Done."), AiEvent.Usage(1500, 200), AiEvent.Done("end_turn")),
            ),
        )
        val conversation = AiConversation("s", AiFixtures.TOOLS)
        conversation.user(listOf(AiContent.Text("Request: go")))
        val log = AiUsageLog()
        log.add(provider.id, provider.model, 1, conversation.round(provider))
        conversation.toolResults(listOf(AiContent.ToolResult(call.id, "{}", isError = false)))
        log.add(provider.id, provider.model, 2, conversation.round(provider))
        assertEquals(2500, log.inputTokens)
        assertEquals(500, log.outputTokens)
        assertEquals(1, log.toolCalls)
        assertEquals(listOf("list_planes"), log.records[0].toolNames)
        assertEquals("scripted", log.records[0].model)
        val summary = log.summary(AiPricing(inputPerMillionUsd = 2.0, outputPerMillionUsd = 10.0))
        assertTrue(summary.startsWith("2 rounds, 1 tool calls, 2500 in / 500 out tokens"), summary)
        assertTrue(summary.endsWith("~$0.01 USD"), summary)
        assertEquals(2, log.json().size)
    }

    private class ScriptedProvider(private val rounds: List<List<AiEvent>>) : AiProvider {
        override val id = "scripted"
        override val capabilities = AiCapabilities(tools = true, images = false, live = false)
        val turns = ArrayList<AiTurn>()

        override suspend fun status(): AiStatus = AiStatus.Ready

        override fun run(turn: AiTurn): Flow<AiEvent> {
            turns += turn
            return rounds[turns.size - 1].asFlow()
        }
    }

    private object ThrowingProvider : AiProvider {
        override val id = "throwing"
        override val capabilities = AiCapabilities(tools = true, images = false, live = false)

        override suspend fun status(): AiStatus = AiStatus.Ready

        override fun run(turn: AiTurn): Flow<AiEvent> = flow { error("kaput") }
    }
}
