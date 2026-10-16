package io.github.sceneview.ai.claude

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
 * Claude through the Messages API — with the user's own key ([AiEndpoint.Vendor]) or through a
 * SceneView relay that holds the key and meters credits ([AiEndpoint.Relay]). The credential is
 * read when a turn starts, lives in memory for that request only and is never logged.
 *
 * One request per turn, not streamed: the reply arrives whole and is replayed as events, which
 * keeps [ClaudeCodec] pure and tested against fixtures.
 */
class ClaudeAiProvider(
    private val endpoint: AiEndpoint,
    override val model: String = ClaudeCodec.MODEL,
    private val client: OkHttpClient = AiHttp.defaultClient,
    private val vendorUrl: String = ClaudeCodec.ENDPOINT,
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
                    else "Add your Claude key in the settings first.",
                ),
            )
            return@flow
        }
        val headers = mapOf(
            endpoint.authHeader("x-api-key", credential),
            "anthropic-version" to ClaudeCodec.API_VERSION,
        )
        val url = endpoint.requestUrl(vendorUrl, ClaudeCodec.RELAY_PATH)
        val reply = AiHttp.post(client, url, headers, ClaudeCodec.encode(turn, model))
        AiHttp.events(reply, ClaudeCodec::decode, ClaudeCodec::errorForStatus).forEach { emit(it) }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val ID = "claude"
    }
}
