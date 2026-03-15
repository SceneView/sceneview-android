package com.google.ar.sceneform.rendering

/**
 * Represents shared data used by [ViewRenderable]s for rendering. The data will be released
 * when all [ViewRenderable]s using this data are finalized.
 */
class ViewRenderableInternalData(private val renderView: RenderViewToExternalTexture) {
    fun getRenderView(): RenderViewToExternalTexture = renderView
}
