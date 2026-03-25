@file:Suppress("INTERFACE_WITH_SUPERCLASS")

package io.github.sceneview.web.xr

/**
 * External declaration for the WebXR [XRSystem](https://developer.mozilla.org/en-US/docs/Web/API/XRSystem) interface.
 *
 * Accessed via `navigator.xr`. This is the entry point for all WebXR functionality:
 * checking session support and requesting immersive sessions.
 *
 * ```kotlin
 * val xr = Navigator.xr ?: error("WebXR not supported")
 * xr.isSessionSupported(XRSessionMode.IMMERSIVE_AR).then { supported: Boolean ->
 *     if (supported) {
 *         xr.requestSession(XRSessionMode.IMMERSIVE_AR).then { session: XRSession ->
 *             // AR session is active
 *         }
 *     }
 * }
 * ```
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/XRSystem">MDN XRSystem</a>
 */
external interface XRSystem {
    /**
     * Check if a particular session mode is supported on this device.
     *
     * @param mode One of [XRSessionMode] constants: "inline", "immersive-vr", "immersive-ar"
     * @return Promise resolving to a Boolean
     */
    fun isSessionSupported(mode: String): dynamic /* Promise<Boolean> */

    /**
     * Request an XR session.
     *
     * Must be called from a user gesture (click/tap) for immersive sessions.
     *
     * @param mode Session mode — see [XRSessionMode]
     * @param options Optional session init dict with requiredFeatures / optionalFeatures
     * @return Promise resolving to an [XRSession]
     */
    fun requestSession(mode: String, options: dynamic = definedExternally): dynamic /* Promise<XRSession> */
}

/**
 * WebXR session mode constants.
 *
 * Use these with [XRSystem.isSessionSupported] and [XRSystem.requestSession].
 */
object XRSessionMode {
    /** Non-immersive session rendered inline in the page. */
    const val INLINE = "inline"

    /** Fully immersive VR — headset takes over display. */
    const val IMMERSIVE_VR = "immersive-vr"

    /** Immersive AR — camera passthrough with virtual content overlay. */
    const val IMMERSIVE_AR = "immersive-ar"
}

/**
 * Utility to access navigator.xr from Kotlin/JS.
 */
object Navigator {
    /** The XRSystem instance, or null if WebXR is not supported. */
    val xr: XRSystem?
        get() = js("navigator.xr") as? XRSystem
}
