package io.github.sceneview.sample

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Client for the Sketchfab public API.
 *
 * The **search** endpoint is public and requires no API key.
 * The **download** endpoint requires an API token for most models — pass one via
 * [apiToken] if you need download URLs.
 *
 * Usage:
 * ```kotlin
 * val api = SketchfabApi()
 * val page = api.search("car", pageSize = 12)
 * page.results.forEach { model ->
 *     println("${model.name} by ${model.authorName}")
 * }
 * // Next page
 * val nextPage = page.nextCursor?.let { api.searchByUrl(it) }
 * ```
 */
class SketchfabApi(
    private val apiToken: String? = null,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 15_000,
) {
    companion object {
        private const val BASE_URL = "https://api.sketchfab.com/v3"
        private const val DEFAULT_PAGE_SIZE = 24
    }

    // ---- Search ----

    /**
     * Search for downloadable 3D models on Sketchfab.
     *
     * @param query Search query string.
     * @param pageSize Number of results per page (max 24).
     * @param cursor Pagination cursor returned from a previous search (use [SketchfabSearchResult.nextCursor]).
     * @param animated Filter for animated models only when `true`.
     * @param staffPicked Filter for staff-picked models only when `true`.
     * @param sortBy Sort order. Common values: `"-likeCount"`, `"-viewCount"`, `"-createdAt"`, `"relevance"`.
     * @return A [SketchfabSearchResult] containing the models and a cursor for the next page.
     * @throws IOException on network errors.
     * @throws SketchfabApiException on non-2xx HTTP responses.
     */
    suspend fun search(
        query: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        cursor: String? = null,
        animated: Boolean? = null,
        staffPicked: Boolean? = null,
        sortBy: String? = null,
    ): SketchfabSearchResult = withContext(Dispatchers.IO) {
        val params = buildString {
            append("type=models")
            append("&downloadable=true")
            append("&q=")
            append(URLEncoder.encode(query, "UTF-8"))
            append("&count=")
            append(pageSize.coerceIn(1, 24))
            cursor?.let {
                append("&cursor=")
                append(URLEncoder.encode(it, "UTF-8"))
            }
            animated?.let { append("&animated=").append(it) }
            staffPicked?.let { append("&staffpicked=").append(it) }
            sortBy?.let {
                append("&sort_by=")
                append(URLEncoder.encode(it, "UTF-8"))
            }
        }
        val url = "$BASE_URL/search?$params"
        val json = httpGet(url)
        parseSearchResponse(json)
    }

    /**
     * Fetch a search results page by its full URL (typically [SketchfabSearchResult.nextCursor]).
     *
     * Use this for pagination — the next-page URL is returned directly by the API.
     */
    suspend fun searchByUrl(url: String): SketchfabSearchResult = withContext(Dispatchers.IO) {
        val json = httpGet(url)
        parseSearchResponse(json)
    }

    // ---- Download ----

    /**
     * Retrieve download URLs for a specific model.
     *
     * Requires a valid [apiToken] for most models. Returns a map of format name
     * (e.g. `"glb"`, `"gltf"`, `"source"`) to download URL, or an empty map if
     * the download is unavailable or authentication fails.
     *
     * @param modelUid The unique ID of the model.
     * @return Map of format name to download URL.
     */
    suspend fun getDownloadUrl(modelUid: String): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/models/$modelUid/download"
            val json = httpGet(url)
            parseDownloadResponse(json)
        } catch (e: SketchfabApiException) {
            // 401/403 means authentication required — return empty rather than crash
            if (e.httpCode in listOf(401, 403)) {
                emptyMap()
            } else {
                throw e
            }
        }
    }

    // ---- Model details ----

    /**
     * Fetch details for a single model by UID.
     */
    suspend fun getModel(modelUid: String): SketchfabModel = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/models/$modelUid"
        val json = httpGet(url)
        val obj = JSONObject(json)
        parseModel(obj)
    }

    // ---- HTTP ----

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("Accept", "application/json")
            apiToken?.let {
                connection.setRequestProperty("Authorization", "Token $it")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = try {
                    connection.errorStream?.let { stream ->
                        BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
                    } ?: ""
                } catch (_: Exception) { "" }
                throw SketchfabApiException(
                    httpCode = responseCode,
                    message = "Sketchfab API error $responseCode: $errorBody"
                )
            }

            return BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                .use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    // ---- JSON parsing ----

    private fun parseSearchResponse(json: String): SketchfabSearchResult {
        val root = JSONObject(json)
        val nextUrl = root.optString("next", "").ifBlank { null }
        val totalCount = root.optInt("totalCount", 0)
        val resultsArray = root.optJSONArray("results") ?: return SketchfabSearchResult(
            results = emptyList(),
            nextCursor = nextUrl,
            totalCount = totalCount,
        )

        val models = (0 until resultsArray.length()).mapNotNull { i ->
            try {
                parseModel(resultsArray.getJSONObject(i))
            } catch (_: Exception) {
                null // skip malformed entries
            }
        }

        return SketchfabSearchResult(
            results = models,
            nextCursor = nextUrl,
            totalCount = totalCount,
        )
    }

    private fun parseModel(obj: JSONObject): SketchfabModel {
        val uid = obj.getString("uid")
        val name = obj.optString("name", "Untitled")

        // Thumbnail: pick the largest available image
        val thumbnailUrl = obj.optJSONObject("thumbnails")?.let { thumbs ->
            val images = thumbs.optJSONArray("images")
            if (images != null && images.length() > 0) {
                // Pick the image with the largest width
                var bestUrl: String? = null
                var bestWidth = 0
                for (j in 0 until images.length()) {
                    val img = images.optJSONObject(j) ?: continue
                    val width = img.optInt("width", 0)
                    val imgUrl = img.optString("url", "")
                    if (imgUrl.isNotBlank() && width > bestWidth) {
                        bestWidth = width
                        bestUrl = imgUrl
                    }
                }
                bestUrl
            } else null
        }

        // Author info
        val user = obj.optJSONObject("user")
        val authorName = user?.optString("displayName")
            ?: user?.optString("username")
            ?: "Unknown"
        val authorProfileUrl = user?.optString("profileUrl")

        val viewerUrl = obj.optString("viewerUrl", "").ifBlank { null }
        val vertexCount = obj.optLong("vertexCount", 0)
        val faceCount = obj.optLong("faceCount", 0)
        val isDownloadable = obj.optBoolean("isDownloadable", false)

        val license = obj.optJSONObject("license")?.optString("label")

        return SketchfabModel(
            uid = uid,
            name = name,
            thumbnailUrl = thumbnailUrl,
            authorName = authorName,
            authorProfileUrl = authorProfileUrl,
            viewerUrl = viewerUrl,
            vertexCount = vertexCount,
            faceCount = faceCount,
            isDownloadable = isDownloadable,
            license = license,
        )
    }

    private fun parseDownloadResponse(json: String): Map<String, String> {
        val root = JSONObject(json)
        val result = mutableMapOf<String, String>()

        // The download response has format keys like "glb", "gltf", "usdz", "source"
        // Each contains a "url" field
        for (key in root.keys()) {
            val formatObj = root.optJSONObject(key)
            val downloadUrl = formatObj?.optString("url", "")
            if (!downloadUrl.isNullOrBlank()) {
                result[key] = downloadUrl
            }
        }

        return result
    }
}

/**
 * Exception thrown when the Sketchfab API returns a non-2xx response.
 */
class SketchfabApiException(
    val httpCode: Int,
    override val message: String,
) : IOException(message)
