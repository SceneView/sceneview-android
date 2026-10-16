package io.github.sceneview.ai.claude

import io.github.sceneview.ai.AiContent
import io.github.sceneview.ai.AiEndpoint
import io.github.sceneview.ai.AiEvent
import io.github.sceneview.ai.AiFixtures
import io.github.sceneview.ai.AiMessage
import io.github.sceneview.ai.AiStatus
import io.github.sceneview.ai.AiTurn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Messages API codec against fixtures, and the provider's HTTP exchange against a local
 * server — with a key, and through a relay. The real API is not called here.
 */
class ClaudeCodecTest {

    private val turn = AiFixtures.TURN

    @Test
    fun `the request lets the model choose, thinks adaptively and keeps every tool strict`() {
        val body = ClaudeCodec.encode(turn)
        assertEquals("claude-sonnet-5-5", body.getValue("model").jsonPrimitive.content)
        assertEquals("adaptive", body.getValue("thinking").jsonObject.getValue("type").jsonPrimitive.content)
        assertNull(body.getValue("thinking").jsonObject["budget_tokens"])
        assertEquals("auto", body.getValue("tool_choice").jsonObject.getValue("type").jsonPrimitive.content)
        val tools = body.getValue("tools").jsonArray
        assertEquals(AiFixtures.TOOLS.size, tools.size)
        tools.forEach { assertTrue(it.jsonObject.getValue("strict").jsonPrimitive.boolean) }
        assertEquals(AiFixtures.TOOLS[1].inputSchema, tools[1].jsonObject.getValue("input_schema"))

        val messages = body.getValue("messages").jsonArray.map { it.jsonObject }
        assertEquals(listOf("user", "assistant", "user"), messages.map { it.getValue("role").jsonPrimitive.content })
        val image = messages[0].getValue("content").jsonArray[1].jsonObject
        assertEquals("image", image.getValue("type").jsonPrimitive.content)
        assertEquals("base64", image.getValue("source").jsonObject.getValue("type").jsonPrimitive.content)
        val results = messages[2].getValue("content").jsonArray.map { it.jsonObject }
        assertNull(results[0]["is_error"])
        assertTrue(results[1].getValue("is_error").jsonPrimitive.boolean)
        assertEquals("toolu_2", results[1].getValue("tool_use_id").jsonPrimitive.content)
    }

    @Test
    fun `a reply decodes to text, tool calls and usage, thinking kept untouched for the next turn`() {
        val events = ClaudeCodec.decode(REPLY)
        val text = events.filterIsInstance<AiEvent.TextDelta>().single()
        assertEquals("Reading the planes first.", text.text)
        val call = events.filterIsInstance<AiEvent.ToolCall>().single()
        assertEquals("toolu_01", call.id)
        assertEquals("raycast", call.name)
        assertEquals("160", call.input.getValue("frame").jsonPrimitive.content)
        assertEquals(AiEvent.Usage(1200, 85), events.filterIsInstance<AiEvent.Usage>().single())
        val done = events.last() as AiEvent.Done
        assertEquals("tool_use", done.stopReason)
        val thinking = done.transcript.first() as AiContent.Opaque
        assertEquals("sig==", thinking.block.getValue("signature").jsonPrimitive.content)
        // Sent back next turn exactly as received.
        val assistant = AiMessage(AiMessage.Role.Assistant, done.transcript)
        val again = ClaudeCodec.encode(AiTurn("s", listOf(assistant), emptyList()))
        val first = again.getValue("messages").jsonArray[0].jsonObject.getValue("content").jsonArray[0]
        assertEquals(Json.parseToJsonElement(REPLY).jsonObject.getValue("content").jsonArray[0], first)
    }

    @Test
    fun `errors become one readable message that never echoes the request`() {
        val refused = ClaudeCodec.decode(AUTH_ERROR)
        assertEquals(listOf(AiEvent.Error("The key was refused. Check it in the settings.")), refused)
        val busy = ClaudeCodec.errorForStatus(
            529,
            """{"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}""",
        )
        assertTrue(busy.message.startsWith("The API is busy"))
        assertEquals(AiEvent.Error("The API answered HTTP 502"), ClaudeCodec.errorForStatus(502, "<html>"))
        assertTrue(ClaudeCodec.decode("not json").single() is AiEvent.Error)
    }

    @Test
    fun `with a key, the provider posts the turn to the vendor with the key header and decodes the reply`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(200).body(REPLY).build())
            val provider = ClaudeAiProvider(
                AiEndpoint.Vendor { "test-key-not-real" },
                vendorUrl = server.url("/v1/messages").toString(),
            )
            assertEquals(AiStatus.Ready, provider.status())
            assertEquals(ClaudeCodec.MODEL, provider.model)
            val events = provider.run(turn).toList()
            assertTrue(events.last() is AiEvent.Done)
            val request = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("POST", request.method)
            assertEquals("/v1/messages", request.target)
            assertEquals("test-key-not-real", request.headers["x-api-key"])
            assertNull(request.headers["Authorization"])
            assertEquals(ClaudeCodec.API_VERSION, request.headers["anthropic-version"])
            val sent = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
            assertEquals(ClaudeCodec.encode(turn), sent)
        }
    }

    @Test
    fun `through a relay, the same body goes under the anthropic prefix with a bearer token and no key`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(200).body(REPLY).build())
            val provider = ClaudeAiProvider(
                AiEndpoint.Relay(server.url("/ai/").toString()) { "sceneview-token" },
                model = "claude-opus-5-5",
                vendorUrl = "https://vendor.invalid/never",
            )
            val events = provider.run(turn).toList()
            assertTrue(events.last() is AiEvent.Done)
            val request = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("/ai/anthropic/v1/messages", request.target)
            assertEquals("Bearer sceneview-token", request.headers["Authorization"])
            assertNull(request.headers["x-api-key"])
            assertEquals(ClaudeCodec.API_VERSION, request.headers["anthropic-version"])
            val sent = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
            assertEquals("claude-opus-5-5", sent.getValue("model").jsonPrimitive.content)
        }
    }

    @Test
    fun `the provider maps an HTTP error and asks for a key when there is none`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(401).body(AUTH_ERROR).build())
            val provider = ClaudeAiProvider(
                AiEndpoint.Vendor { "test-key-not-real" },
                vendorUrl = server.url("/v1/messages").toString(),
            )
            val events = provider.run(turn).toList()
            assertEquals(listOf(AiEvent.Error("The key was refused. Check it in the settings.")), events)
        }
        val keyless = ClaudeAiProvider(AiEndpoint.Vendor { null })
        assertEquals(AiStatus.NeedsKey, keyless.status())
        val events = keyless.run(turn).toList()
        assertTrue((events.single() as AiEvent.Error).message.contains("key"))
        assertFalse(events.any { it is AiEvent.Done })
        val signedOut = ClaudeAiProvider(AiEndpoint.Relay("https://relay.invalid") { "" })
        assertEquals(AiStatus.NeedsKey, signedOut.status())
        assertTrue((signedOut.run(turn).toList().single() as AiEvent.Error).message.contains("Sign in"))
    }

    private companion object {
        val REPLY =
            """
            {"id":"msg_01","type":"message","role":"assistant","model":"claude-sonnet-5-5",
             "content":[
               {"type":"thinking","thinking":"The ray from 160 should hit the wall.","signature":"sig=="},
               {"type":"text","text":"Reading the planes first."},
               {"type":"tool_use","id":"toolu_01","name":"raycast","input":{"frame":160,"x":0.5,"y":0.5}}
             ],
             "stop_reason":"tool_use","usage":{"input_tokens":1200,"output_tokens":85}}
            """.trimIndent()

        const val AUTH_ERROR =
            """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""
    }
}
