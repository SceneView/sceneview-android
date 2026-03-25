@file:Suppress("INTERFACE_WITH_SUPERCLASS")

package io.github.sceneview.web.xr

/**
 * External declaration for [XRSpace](https://developer.mozilla.org/en-US/docs/Web/API/XRSpace).
 *
 * Base interface for all spatial tracking in WebXR. Opaque — you use it
 * with [XRFrame.getPose] to get its position relative to another space.
 */
external interface XRSpace

/**
 * External declaration for [XRReferenceSpace](https://developer.mozilla.org/en-US/docs/Web/API/XRReferenceSpace).
 *
 * A coordinate system used for tracking. Created via [XRSession.requestReferenceSpace].
 *
 * ```kotlin
 * session.requestReferenceSpace(XRReferenceSpaceType.LOCAL_FLOOR).then { space: XRReferenceSpace ->
 *     // Use space for pose tracking
 * }
 * ```
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/XRReferenceSpace">MDN XRReferenceSpace</a>
 */
external interface XRReferenceSpace : XRSpace {
    /**
     * Create a new reference space offset from this one by the given transform.
     *
     * @param originOffset The transform to apply
     * @return A new [XRReferenceSpace] offset from this one
     */
    fun getOffsetReferenceSpace(originOffset: XRRigidTransform): XRReferenceSpace
}

/**
 * External declaration for [XRBoundedReferenceSpace](https://developer.mozilla.org/en-US/docs/Web/API/XRBoundedReferenceSpace).
 *
 * A reference space with defined physical boundaries (the user's play area).
 * Only available when requesting "bounded-floor" reference space type.
 */
external interface XRBoundedReferenceSpace : XRReferenceSpace {
    /** The boundary polygon as an array of DOMPointReadOnly vertices on the floor plane. */
    val boundsGeometry: Array<DOMPointReadOnly>
}

/**
 * WebXR reference space type constants.
 *
 * Use with [XRSession.requestReferenceSpace].
 */
object XRReferenceSpaceType {
    /**
     * Viewer-relative — origin at the device, moves with the user.
     * Always available. Useful for head-locked content and hit test sources.
     */
    const val VIEWER = "viewer"

    /**
     * Local — a stable origin near the user at session start.
     * Does not account for floor level. Good for seated VR or phone AR.
     */
    const val LOCAL = "local"

    /**
     * Local floor — like "local" but with the Y origin at floor level.
     * Best for standing AR experiences and room-scale VR.
     */
    const val LOCAL_FLOOR = "local-floor"

    /**
     * Bounded floor — floor-level origin with defined play area boundaries.
     * Returns [XRBoundedReferenceSpace] with boundsGeometry.
     */
    const val BOUNDED_FLOOR = "bounded-floor"

    /**
     * Unbounded — large-scale tracking without predefined boundaries.
     * For outdoor AR, warehouse-scale VR, etc.
     */
    const val UNBOUNDED = "unbounded"
}
