package io.github.sceneview.demo.ai.scene

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Claude through the Messages API with the user's own key ([key] reads it from [ApiKeyStore]
 * when a turn starts; it lives in memory for that request only and is never logged).
 *
 * One request per turn, not streamed: the reply arrives whole and is replayed as events, which
 * keeps the codec pure and tested. The live path itself is not covered by tests — it needs a key.
 */
class ClaudeAiProvider(
    private val key: () -> String?,
    private val client: OkHttpClient = defaultClient,
    private val endpoint: String = ClaudeCodec.ENDPOINT,
) : AiProvider {

    override val id: String = ID

    override val capabilities = AiCapabilities(tools = true, images = true, live = true)

    override suspend fun status(): AiStatus = if (key().isNullOrBlank()) AiStatus.NeedsKey else AiStatus.Ready

    override fun run(turn: AiTurn): Flow<AiEvent> = flow {
        val apiKey = key()
        if (apiKey.isNullOrBlank()) {
            emit(AiEvent.Error("Add your Claude key in the settings first."))
            return@flow
        }
        val body = ClaudeCodec.encode(turn).toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url(endpoint)
            .header("x-api-key", apiKey)
            .header("anthropic-version", ClaudeCodec.API_VERSION)
            .post(body)
            .build()
        val events = try {
            client.newCall(request).executeAsync().use { response ->
                val text = response.body.string()
                if (response.isSuccessful) ClaudeCodec.decode(text) else listOf(ClaudeCodec.errorForStatus(
                    response.code,
                    text,
                ))
            }
        } catch (e: IOException) {
            // The exception's message is OkHttp's (host, timeout) — it never carries a header.
            listOf(AiEvent.Error("No answer from the API: ${e.javaClass.simpleName}"))
        }
        events.forEach { emit(it) }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val ID = "claude"
        private val JSON = "application/json".toMediaType()

        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                // Adaptive thinking over a room's worth of tools can take a while.
                .readTimeout(120, TimeUnit.SECONDS)
                .build()
        }
    }
}
