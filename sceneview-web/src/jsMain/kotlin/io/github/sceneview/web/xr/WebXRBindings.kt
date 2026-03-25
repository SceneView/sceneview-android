package io.github.sceneview.web.xr

/**
 * WebGL Layer for rendering XR content.
 *
 * External declaration for [XRWebGLLayer](https://developer.mozilla.org/en-US/docs/Web/API/XRWebGLLayer).
 *
 * Connects a WebGL rendering context to an XR session, providing the framebuffer
 * and viewport information needed to render stereoscopic or AR content.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/XRWebGLLayer">MDN XRWebGLLayer</a>
 */
external class XRWebGLLayer(
    session: XRSession,
    context: dynamic,
    options: dynamic = definedExternally
) {
    /** The WebGL framebuffer to render into. */
    val framebuffer: dynamic

    /** The framebuffer width in pixels. */
    val framebufferWidth: Int

    /** The framebuffer height in pixels. */
    val framebufferHeight: Int

    /** Whether the layer uses anti-aliasing. */
    val antialias: Boolean

    /** Whether the layer ignores the depth buffer. */
    val ignoreDepthValues: Boolean

    /**
     * Get the viewport for a specific [XRView] (eye).
     *
     * @param view The view to get the viewport for
     * @return The viewport rectangle
     */
    fun getViewport(view: XRView): XRViewport
}

/**
 * External declaration for [XRViewport](https://developer.mozilla.org/en-US/docs/Web/API/XRViewport).
 *
 * Describes a rectangular region of the framebuffer to render into for a single view.
 */
external interface XRViewport {
    val x: Int
    val y: Int
    val width: Int
    val height: Int
}
