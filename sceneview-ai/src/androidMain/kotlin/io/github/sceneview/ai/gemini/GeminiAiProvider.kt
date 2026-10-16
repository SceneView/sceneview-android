package io.github.sceneview.ai.gemini

import io.github.sceneview.ai.AiCapabilities
import io.github.sceneview.ai.AiEndpoint
import io.github.sceneview.ai.AiEvent
import io.github.sceneview.ai.AiHttp
import io.github.sceneview.ai.AiProvider
import io.github.sceneview.ai.AiStatus
import io.github.sceneview.ai.AiTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient

/**
 * Gemini through `generateContent` — with the user's own key ([AiEndpoint.Vendor]) or through a
 * SceneView relay ([AiEndpoint.Relay]). Same tools, same events as [io.github.sceneview.ai.claude.ClaudeAiProvider]:
 * the app's loop does not know which one it talks to.
 */
class GeminiAiProvider(
    private val endpoint: AiEndpoint,
    override val model: String = GeminiCodec.MODEL,
    private val client: OkHttpClient = AiHttp.defaultClient,
    private val vendorBaseUrl: String = GeminiCodec.VENDOR_BASE_URL,
) : AiProvider {

    override val id: String = ID

    override val capabilities = AiCapabilities(tools = true, images = true, live = true)

    override suspend fun status(): AiStatus =
        if (endpoint.credential().isNullOrBlank()) AiStatus.NeedsKey else AiStatus.Ready

    override fun run(turn: AiTurn): Flow<AiEvent> = flow {
        val credential = endpoint.credential()
        if (credential.isNullOrBlank()) {
            emit(
                AiEvent.Error(
                    if (endpoint is AiEndpoint.Relay) "Sign in to SceneView first."
                    else "Add your Gemini key in the settings first.",
                ),
            )
            return@flow
        }
        val headers = mapOf(endpoint.authHeader("x-goog-api-key", credential))
        val url = endpoint.requestUrl(GeminiCodec.vendorUrl(model, vendorBaseUrl), GeminiCodec.relayPath(model))
        val reply = AiHttp.post(client, url, headers, GeminiCodec.encode(turn))
        AiHttp.events(reply, GeminiCodec::decode, GeminiCodec::errorForStatus).forEach { emit(it) }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val ID = "gemini"
    }
}
