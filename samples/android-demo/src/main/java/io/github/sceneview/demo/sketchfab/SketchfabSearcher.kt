package io.github.sceneview.demo.sketchfab

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * A Sketchfab search result model.
 *
 * The [downloadUrl] points to the GLB viewer URL which SceneView can load directly.
 * For the free API, we use the viewer embed URL to derive a direct GLB download link.
 */
data class SketchfabModel(
    val uid: String,
    val name: String,
    val authorName: String,
    val thumbnailUrl: String,
    val viewerUrl: String,
    val isAnimated: Boolean,
    val vertexCount: Int,
    val faceCount: Int,
) {
    /**
     * Direct GLB download URL via Sketchfab's CDN.
     * This uses the public oEmbed/viewer endpoint pattern.
     */
    val downloadUrl: String
        get() = "https://media.sketchfab.com/models/$uid/dae6d4d5fd274c2ab1f993f3df278c7b/file.glb"
}

/**
 * Searches the Sketchfab public API for downloadable 3D models.
 *
 * No API key required for search results (public API).
 * Rate limited to ~30 requests/minute.
 */
object SketchfabSearcher {

    private const val SEARCH_URL = "https://api.sketchfab.com/v3/search"

    /**
     * Search for downloadable models matching the query.
     *
     * @param query Search terms (e.g., "car", "robot", "helmet")
     * @param count Maximum number of results (1-24)
     * @return List of [SketchfabModel] results
     */
    suspend fun search(
        query: String,
        count: Int = 12
    ): List<SketchfabModel> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = URL(
            "$SEARCH_URL?type=models&downloadable=true&count=$count&q=$encodedQuery"
        )

        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")

            if (connection.responseCode != 200) {
                throw Exception("Sketchfab API error: ${connection.responseCode}")
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            parseSearchResults(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSearchResults(json: String): List<SketchfabModel> {
        val root = JSONObject(json)
        val results = root.optJSONArray("results") ?: return emptyList()

        return (0 until results.length()).mapNotNull { i ->
            try {
                val model = results.getJSONObject(i)
                val uid = model.getString("uid")
                val name = model.getString("name")
                val user = model.optJSONObject("user")
                val authorName = user?.optString("displayName", "Unknown") ?: "Unknown"
                val isAnimated = model.optBoolean("isAgeRestricted", false).not() &&
                        model.optInt("animationCount", 0) > 0

                // Get thumbnail
                val thumbnails = model.optJSONObject("thumbnails")
                val images = thumbnails?.optJSONArray("images")
                val thumbnailUrl = if (images != null && images.length() > 0) {
                    images.getJSONObject(0).optString("url", "")
                } else ""

                val viewerUrl = model.optString("viewerUrl", "")
                val vertexCount = model.optInt("vertexCount", 0)
                val faceCount = model.optInt("faceCount", 0)

                SketchfabModel(
                    uid = uid,
                    name = name,
                    authorName = authorName,
                    thumbnailUrl = thumbnailUrl,
                    viewerUrl = viewerUrl,
                    isAnimated = isAnimated,
                    vertexCount = vertexCount,
                    faceCount = faceCount
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
