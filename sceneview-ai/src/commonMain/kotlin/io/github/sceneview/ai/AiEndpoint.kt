package io.github.sceneview.ai

/**
 * Where a provider sends its requests, and what it authenticates with. Two shapes, one per way
 * of paying:
 *
 * - [Vendor] — the model vendor's own API with the user's own key (bring your own key). The key
 *   is read when a request is built, lives in memory for that request, is never logged.
 * - [Relay] — a SceneView relay in front of the vendor. The request body is the vendor's, byte
 *   for byte; the path is the vendor's under [Relay.baseUrl] with a vendor prefix
 *   (`anthropic/v1/messages`, `gemini/v1beta/models/<model>:generateContent`); the credential is
 *   a SceneView token sent as `Authorization: Bearer`, never a vendor key. The relay holds the
 *   vendor key, meters credits and answers with the vendor's response, so the same codec reads
 *   both. This is the contract the metered relay implements.
 *
 * ```kotlin
 * ClaudeAiProvider(AiEndpoint.Vendor { keyStore.load() })                          // BYOK
 * ClaudeAiProvider(AiEndpoint.Relay("https://ar.sceneview.dev/ai") { session.token }) // credits
 * ```
 */
sealed interface AiEndpoint {
    /** The secret of the moment — `null` or blank when the user has none yet ([AiStatus.NeedsKey]). */
    val credential: () -> String?

    /** Where a request goes: [vendorUrl] as is, or [relayPath] under the relay. */
    fun requestUrl(vendorUrl: String, relayPath: String): String = when (this) {
        is Vendor -> vendorUrl
        is Relay -> url(relayPath)
    }

    /** The credential header: the vendor's own ([vendorHeader]) for a key, `Authorization: Bearer` for a relay token. */
    fun authHeader(vendorHeader: String, credential: String): Pair<String, String> = when (this) {
        is Vendor -> vendorHeader to credential
        is Relay -> "Authorization" to "Bearer $credential"
    }

    data class Vendor(override val credential: () -> String?) : AiEndpoint

    data class Relay(val baseUrl: String, override val credential: () -> String?) : AiEndpoint {
        /** The vendor path under the relay: `https://relay.example/ai/anthropic/v1/messages`. */
        fun url(vendorPath: String): String = baseUrl.trimEnd('/') + "/" + vendorPath.trimStart('/')
    }
}
