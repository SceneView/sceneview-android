package io.github.sceneview

/**
 * Selects the backing surface type for [Scene] and [io.github.sceneview.ar.ARScene].
 */
enum class SurfaceType {
    /**
     * Backed by a SurfaceView via [androidx.compose.ui.viewinterop.AndroidExternalSurface].
     *
     * Renders *behind* the Compose hierarchy (Z-ordered below all Compose content).
     * Prefer this for best performance when no Compose UI needs to appear beneath the 3D scene.
     */
    Surface,

    /**
     * Backed by a TextureView via [androidx.compose.ui.viewinterop.AndroidEmbeddedExternalSurface].
     *
     * Renders *inline* with the Compose hierarchy — supports alpha blending and allows Compose
     * content to be drawn both above and below the scene.
     */
    TextureSurface
}
