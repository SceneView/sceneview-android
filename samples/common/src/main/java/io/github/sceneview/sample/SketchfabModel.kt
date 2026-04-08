package io.github.sceneview.sample

/**
 * Represents a 3D model from the Sketchfab API.
 *
 * @param uid Unique identifier on Sketchfab.
 * @param name Display name of the model.
 * @param thumbnailUrl URL of the preview thumbnail image.
 * @param authorName Name of the model author.
 * @param authorProfileUrl URL of the author's Sketchfab profile.
 * @param viewerUrl URL to view the model on Sketchfab.
 * @param vertexCount Number of vertices in the model (0 if unknown).
 * @param faceCount Number of faces in the model (0 if unknown).
 * @param isDownloadable Whether the model is available for download.
 * @param license License information (e.g. "CC BY 4.0"), or null if unspecified.
 */
data class SketchfabModel(
    val uid: String,
    val name: String,
    val thumbnailUrl: String?,
    val authorName: String,
    val authorProfileUrl: String?,
    val viewerUrl: String?,
    val vertexCount: Long = 0,
    val faceCount: Long = 0,
    val isDownloadable: Boolean = true,
    val license: String? = null,
)

/**
 * A page of Sketchfab search results with cursor-based pagination.
 *
 * @param results The models on this page.
 * @param nextCursor URL for the next page, or null if this is the last page.
 * @param totalCount Total number of results matching the query (from the API).
 */
data class SketchfabSearchResult(
    val results: List<SketchfabModel>,
    val nextCursor: String? = null,
    val totalCount: Int = 0,
)
