package io.github.sceneview.ai

import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import java.io.IOException
import java.util.concurrent.TimeUnit

/** An HTTP reply as the codecs read it: the status and the whole body. */
class AiHttpReply(val code: Int, val body: String) {
    val isSuccessful: Boolean get() = code in SUCCESS

    private companion object {
        val SUCCESS = 200..299
    }
}

/**
 * The one HTTP exchange every provider makes: a JSON body posted whole, the reply read whole.
 * Not streamed, so the codecs stay pure and testable against fixtures.
 */
object AiHttp {
    private val JSON = "application/json".toMediaType()

    /** 15 s to connect, two minutes to read: thinking over a room's worth of tools can take a while. */
    val defaultClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
            .build()
    }

    /**
     * POSTs [body] with [headers]. A network failure is the [IOException] — its message is
     * OkHttp's (host, timeout) and never carries a header, so callers may show its class name.
     */
    suspend fun post(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String>,
        body: JsonObject,
    ): Result<AiHttpReply> {
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON))
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        return try {
            client.newCall(request).executeAsync().use { response ->
                Result.success(AiHttpReply(response.code, response.body.string()))
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    /** The events for a reply: decoded when it succeeded, one error otherwise. */
    fun events(
        reply: Result<AiHttpReply>,
        decode: (String) -> List<AiEvent>,
        errorForStatus: (Int, String) -> AiEvent.Error,
    ): List<AiEvent> = reply.fold(
        onSuccess = { if (it.isSuccessful) decode(it.body) else listOf(errorForStatus(it.code, it.body)) },
        onFailure = { listOf(AiEvent.Error("No answer from the API: ${it::class.simpleName}")) },
    )

    private const val CONNECT_TIMEOUT_S = 15L
    private const val READ_TIMEOUT_S = 120L
}
