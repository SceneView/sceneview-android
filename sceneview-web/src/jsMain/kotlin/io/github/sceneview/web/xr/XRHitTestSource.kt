@file:Suppress("INTERFACE_WITH_SUPERCLASS")

package io.github.sceneview.web.xr

/**
 * External declaration for [XRHitTestSource](https://developer.mozilla.org/en-US/docs/Web/API/XRHitTestSource).
 *
 * Represents an ongoing hit test subscription. Created via `session.requestHitTestSource()`.
 * Use with [XRFrame.getHitTestResults] to get per-frame ray-surface intersections.
 *
 * Typical usage for tap-to-place AR:
 * ```kotlin
 * // Create a hit test source from the viewer's gaze direction
 * session.requestReferenceSpace("viewer").then { viewerSpace ->
 *     val options = js("{}")
 *     options.space = viewerSpace
 *     session.asDynamic().requestHitTestSource(options).then { source: XRHitTestSource ->
 *         hitTestSource = source
 *     }
 * }
 *
 * // In the frame loop:
 * val results = frame.getHitTestResults(hitTestSource)
 * if (results.isNotEmpty()) {
 *     val pose = results[0].getPose(referenceSpace)
 *     // Place content at pose.transform.position
 * }
 * ```
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/XRHitTestSource">MDN XRHitTestSource</a>
 */
external interface XRHitTestSource {
    /** Cancel this hit test source — it will no longer produce results. */
    fun cancel()
}

/**
 * External declaration for [XRHitTestResult](https://developer.mozilla.org/en-US/docs/Web/API/XRHitTestResult).
 *
 * Represents a single intersection of a ray with a real-world surface.
 */
external interface XRHitTestResult {
    /**
     * Get the pose of this hit test result relative to a reference space.
     *
     * @param baseSpace The reference space to express the pose in
     * @return The hit pose (position on surface + surface normal orientation), or null
     */
    fun getPose(baseSpace: XRReferenceSpace): XRPose?

    /**
     * Create an anchor at this hit test result location.
     *
     * Requires the "anchors" feature. Returns a Promise resolving to [XRAnchor].
     */
    fun createAnchor(): dynamic /* Promise<XRAnchor>? */
}

/**
 * External declaration for [XRTransientInputHitTestSource](https://developer.mozilla.org/en-US/docs/Web/API/XRTransientInputHitTestSource).
 *
 * Like [XRHitTestSource] but automatically tracks transient input sources
 * (e.g., screen taps on mobile AR). Created via `session.requestHitTestSourceForTransientInput()`.
 */
external interface XRTransientInputHitTestSource {
    /** Cancel this transient hit test source. */
    fun cancel()
}

/**
 * External declaration for [XRTransientInputHitTestResult](https://developer.mozilla.org/en-US/docs/Web/API/XRTransientInputHitTestResult).
 */
external interface XRTransientInputHitTestResult {
    /** The input source that generated this result. */
    val inputSource: XRInputSource

    /** The hit test results for this input source. */
    val results: Array<XRHitTestResult>
}
