package io.github.sceneview.rendering

/**
 * Platform-independent asset path representation.
 *
 * Wraps a relative path to a bundled asset (model, texture, environment).
 * Platform implementations resolve this to the correct location:
 * - Android: assets directory or raw resources
 * - iOS: app bundle
 * - Desktop: classpath resources
 * - Web: URL relative to the page
 *
 * @param path Relative path to the asset (e.g., "models/helmet.glb", "environments/studio.hdr").
 */
@kotlin.jvm.JvmInline
value class AssetPath(val path: String) {

    /** The file extension without the dot (e.g., "glb", "hdr"). */
    val extension: String
        get() = path.substringAfterLast('.', "")

    /** The filename without the directory (e.g., "helmet.glb"). */
    val filename: String
        get() = path.substringAfterLast('/')

    /** The filename without extension (e.g., "helmet"). */
    val stem: String
        get() = filename.substringBeforeLast('.')

    /** The directory path (e.g., "models"). Empty string if no directory. */
    val directory: String
        get() = if ('/' in path) path.substringBeforeLast('/') else ""

    /** True if this is a glTF/GLB model file. */
    val isModel: Boolean
        get() = extension in MODEL_EXTENSIONS

    /** True if this is an HDR environment file. */
    val isEnvironment: Boolean
        get() = extension in ENVIRONMENT_EXTENSIONS

    /** True if this is a texture file. */
    val isTexture: Boolean
        get() = extension in TEXTURE_EXTENSIONS

    override fun toString(): String = path

    companion object {
        val MODEL_EXTENSIONS = setOf("glb", "gltf", "usdz")
        val ENVIRONMENT_EXTENSIONS = setOf("hdr", "ktx")
        val TEXTURE_EXTENSIONS = setOf("png", "jpg", "jpeg", "ktx2", "basis")
    }
}
