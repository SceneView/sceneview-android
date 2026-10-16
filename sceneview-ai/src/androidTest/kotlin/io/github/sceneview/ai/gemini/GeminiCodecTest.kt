package io.github.sceneview.ai.gemini

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
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The generateContent codec against fixtures, and the provider's HTTP exchange against a local
 * server — with a key, and through a relay. The real API is not called here.
 */
class GeminiCodecTest {

    private val turn = AiFixtures.TURN

    @Test
    fun `the request carries the system instruction, alternating turns, and the tools in Gemini's form`() {
        val body = GeminiCodec.encode(turn)
        val system = body.getValue("systemInstruction").jsonObject.getValue("parts").jsonArray[0].jsonObject
        assertEquals("system prompt", system.getValue("text").jsonPrimitive.content)
        val contents = body.getValue("contents").jsonArray.map { it.jsonObject }
        assertEquals(listOf("user", "model", "user"), contents.map { it.getValue("role").jsonPrimitive.content })
        val image = contents[0].getValue("parts").jsonArray[1].jsonObject.getValue("inlineData").jsonObject
        assertEquals("image/jpeg", image.getValue("mimeType").jsonPrimitive.content)
        assertEquals("AAAA", image.getValue("data").jsonPrimitive.content)

        val call = contents[1].getValue("parts").jsonArray[0].jsonObject.getValue("functionCall").jsonObject
        assertEquals("list_planes", call.getValue("name").jsonPrimitive.content)
        // The result finds its call's name back through the id; JSON content is sent as the object it is.
        val results = contents[2].getValue("parts").jsonArray
            .map { it.jsonObject.getValue("functionResponse").jsonObject }
        assertEquals("list_planes", results[0].getValue("name").jsonPrimitive.content)
        assertEquals(0, results[0].getValue("response").jsonObject.getValue("planes").jsonArray.size)
        assertEquals("refused", results[1].getValue("response").jsonObject.getValue("error").jsonPrimitive.content)

        val declarations = body.getValue("tools").jsonArray[0].jsonObject
            .getValue("functionDeclarations").jsonArray.map { it.jsonObject }
        assertEquals(AiFixtures.TOOLS.map { it.name }, declarations.map { it.getValue("name").jsonPrimitive.content })
        // No parameters for a tool without input: Gemini refuses an empty object schema.
        assertNull(declarations[0]["parameters"])
        val raycast = declarations[1].getValue("parameters").jsonObject
        assertNull(raycast["additionalProperties"])
        assertEquals(listOf("frame", "x", "y"), raycast.getValue("required").jsonArray.map { it.jsonPrimitive.content })
        val config = body.getValue("toolConfig").jsonObject.getValue("functionCallingConfig").jsonObject
        assertEquals("AUTO", config.getValue("mode").jsonPrimitive.content)
    }

    @Test
    fun `a reply decodes to text, a tool call with an id of its own, and usage counting the thinking`() {
        val events = GeminiCodec.decode(REPLY)
        // The thought summary is skipped; the answer's text is shown once.
        assertEquals("Reading the planes first.", events.filterIsInstance<AiEvent.TextDelta>().single().text)
        val call = events.filterIsInstance<AiEvent.ToolCall>().single()
        assertEquals("call_resp42_1", call.id)
        assertEquals("raycast", call.name)
        // 160.0 reaches the tool as the integer it is.
        assertEquals("160", call.input.getValue("frame").jsonPrimitive.content)
        assertEquals(0.5, call.input.getValue("x").jsonPrimitive.content.toDouble())
        assertEquals(AiEvent.Usage(1200, 85), events.filterIsInstance<AiEvent.Usage>().single())
        val done = events.last() as AiEvent.Done
        assertEquals("STOP", done.stopReason)
        val use = done.transcript.filterIsInstance<AiContent.ToolUse>().single()
        assertEquals("sig==", use.meta?.getValue("thoughtSignature")?.jsonPrimitive?.content)

        // Next turn: the call goes back with its signature, the result under the call's name.
        val again = GeminiCodec.encode(
            AiTurn(
                "s",
                listOf(
                    AiMessage(AiMessage.Role.Assistant, done.transcript),
                    AiMessage(
                        AiMessage.Role.User,
                        listOf(AiContent.ToolResult(call.id, "{\"hit\":true}", isError = false)),
                    ),
                ),
                emptyList(),
            ),
        )
        val contents = again.getValue("contents").jsonArray.map { it.jsonObject }
        val callPart = contents[0].getValue("parts").jsonArray[1].jsonObject
        assertEquals("sig==", callPart.getValue("thoughtSignature").jsonPrimitive.content)
        val args = callPart.getValue("functionCall").jsonObject.getValue("args").jsonObject
        assertEquals(160, args.getValue("frame").jsonPrimitive.int)
        val response = contents[1].getValue("parts").jsonArray[0].jsonObject.getValue("functionResponse").jsonObject
        assertEquals("raycast", response.getValue("name").jsonPrimitive.content)
        assertTrue(response.getValue("response").jsonObject.getValue("hit").jsonPrimitive.boolean)
        assertNull(again["tools"])
    }

    @Test
    fun `errors become one readable message that never echoes the request`() {
        val refused = AiEvent.Error("The key was refused. Check it in the settings.")
        assertEquals(listOf(refused), GeminiCodec.decode(KEY_ERROR))
        val busy = GeminiCodec.errorForStatus(
            429,
            """{"error":{"code":429,"message":"Resource has been exhausted","status":"RESOURCE_EXHAUSTED"}}""",
        )
        assertTrue(busy.message.startsWith("The API is busy"), busy.message)
        assertEquals(AiEvent.Error("The API answered HTTP 502"), GeminiCodec.errorForStatus(502, "<html>"))
        assertTrue(GeminiCodec.decode("not json").single() is AiEvent.Error)
        val blocked = GeminiCodec.decode("""{"promptFeedback":{"blockReason":"SAFETY"}}""").single() as AiEvent.Error
        assertTrue(blocked.message.contains("SAFETY"))
        val malformedReply = """{"candidates":[{"finishReason":"MALFORMED_FUNCTION_CALL"}]}"""
        val malformed = GeminiCodec.decode(malformedReply).single() as AiEvent.Error
        assertTrue(malformed.message.contains("MALFORMED_FUNCTION_CALL"))
    }

    @Test
    fun `with a key, the provider posts to the model's generateContent path with the key header`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(200).body(REPLY).build())
            val provider = GeminiAiProvider(
                AiEndpoint.Vendor { "test-key-not-real" },
                vendorBaseUrl = server.url("/").toString(),
            )
            assertEquals(AiStatus.Ready, provider.status())
            assertEquals("gemini-3.8-flash", provider.model)
            val events = provider.run(turn).toList()
            assertTrue(events.last() is AiEvent.Done)
            val request = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("POST", request.method)
            assertEquals("/v1beta/models/gemini-3.8-flash:generateContent", request.target)
            assertEquals("test-key-not-real", request.headers["x-goog-api-key"])
            assertNull(request.headers["Authorization"])
            assertEquals(GeminiCodec.encode(turn), Json.parseToJsonElement(request.body!!.utf8()).jsonObject)
        }
    }

    @Test
    fun `through a relay, the same body goes under the gemini prefix with a bearer token and no key`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(200).body(REPLY).build())
            val provider = GeminiAiProvider(
                AiEndpoint.Relay(server.url("/ai").toString()) { "sceneview-token" },
                model = "gemini-3.1-pro-preview",
                vendorBaseUrl = "https://vendor.invalid",
            )
            assertTrue(provider.run(turn).toList().last() is AiEvent.Done)
            val request = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("/ai/gemini/v1beta/models/gemini-3.1-pro-preview:generateContent", request.target)
            assertEquals("Bearer sceneview-token", request.headers["Authorization"])
            assertNull(request.headers["x-goog-api-key"])
        }
    }

    @Test
    fun `the provider maps an HTTP error and asks for a key when there is none`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(400).body(KEY_ERROR).build())
            val provider = GeminiAiProvider(
                AiEndpoint.Vendor { "test-key-not-real" },
                vendorBaseUrl = server.url("/").toString(),
            )
            val refused = AiEvent.Error("The key was refused. Check it in the settings.")
            assertEquals(listOf(refused), provider.run(turn).toList())
        }
        val keyless = GeminiAiProvider(AiEndpoint.Vendor { "" })
        assertEquals(AiStatus.NeedsKey, keyless.status())
        assertTrue((keyless.run(turn).toList().single() as AiEvent.Error).message.contains("Gemini key"))
    }

    private companion object {
        val REPLY =
            """
            {"candidates":[{"content":{"parts":[
               {"text":"I should look at the planes.","thought":true},
               {"text":"Reading the planes first."},
               {"functionCall":{"name":"raycast","args":{"frame":160.0,"x":0.5,"y":0.5}},"thoughtSignature":"sig=="}
             ],"role":"model"},"finishReason":"STOP","index":0}],
             "usageMetadata":{"promptTokenCount":1200,"candidatesTokenCount":60,"thoughtsTokenCount":25,"totalTokenCount":1285},
             "modelVersion":"gemini-3.8-flash","responseId":"resp42"}
            """.trimIndent()

        const val KEY_ERROR =
            """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}"""
    }
}
