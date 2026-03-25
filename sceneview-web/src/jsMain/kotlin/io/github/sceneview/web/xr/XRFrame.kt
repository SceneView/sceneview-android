@file:Suppress("INTERFACE_WITH_SUPERCLASS")

package io.github.sceneview.web.xr

/**
 * External declaration for the WebXR [XRFrame](https://developer.mozilla.org/en-US/docs/Web/API/XRFrame) interface.
 *
 * Provides per-frame tracking data: viewer pose, device pose, and hit test results.
 * Received as the second argument to [XRSession.requestAnimationFrame] callbacks.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/XRFrame">MDN XRFrame</a>
 */
external interface XRFrame {
    /** The session this frame belongs to. */
    val session: XRSession

    /** The predicted display time for this frame (DOMHighResTimeStamp). */
    val predictedDisplayTime: Double

    /**
     * Get the viewer's pose (head position/orientation) relative to a reference space.
     *
     * @param referenceSpace The space to track relative to
     * @return The viewer pose, or null if tracking is lost
     */
    fun getViewerPose(referenceSpace: XRReferenceSpace): XRViewerPose?

    /**
     * Get the pose of one space relative to another.
     *
     * Useful for tracking controllers, hands, or anchors.
     *
     * @param space The space to get the pose of (e.g., controller grip space)
     * @param baseSpace The reference space to express the pose in
     * @return The pose, or null if the relationship cannot be determined
     */
    fun getPose(space: XRSpace, baseSpace: XRReferenceSpace): XRPose?

    /**
     * Get hit test results for a given hit test source.
     *
     * @param source The hit test source created via session.requestHitTestSource
     * @return Array of hit test results, closest first
     */
    fun getHitTestResults(source: XRHitTestSource): Array<XRHitTestResult>

    /**
     * Create an anchor at the specified pose.
     *
     * Requires the "anchors" feature to be enabled.
     *
     * @param pose The pose at which to create the anchor
     * @param space The reference space for the pose
     * @return Promise resolving to an [XRAnchor]
     */
    fun createAnchor(pose: XRRigidTransform, space: XRReferenceSpace): dynamic /* Promise<XRAnchor>? */
}

/**
 * External declaration for [XRViewerPose](https://developer.mozilla.org/en-US/docs/Web/API/XRViewerPose).
 *
 * Extends [XRPose] with an array of [XRView]s — one per eye for stereo VR,
 * or a single view for AR and inline sessions.
 */
external interface XRViewerPose : XRPose {
    /** The views for this frame. One for mono, two for stereo VR. */
    val views: Array<XRView>
}

/**
 * External declaration for [XRPose](https://developer.mozilla.org/en-US/docs/Web/API/XRPose).
 *
 * Represents a position and orientation in 3D space.
 */
external interface XRPose {
    /** The transform (position + orientation) as a rigid body transform. */
    val transform: XRRigidTransform

    /** Linear velocity (m/s), if available. */
    val linearVelocity: DOMPointReadOnly?

    /** Angular velocity (rad/s), if available. */
    val angularVelocity: DOMPointReadOnly?

    /** Whether the pose is being emulated (e.g., 3DoF extrapolation). */
    val emulatedPosition: Boolean
}

/**
 * External declaration for [XRView](https://developer.mozilla.org/en-US/docs/Web/API/XRView).
 *
 * Represents a single view (eye) in the XR display. VR headsets have two views
 * (left/right), AR and inline sessions typically have one ("none").
 */
external interface XRView {
    /** Which eye this view represents: "left", "right", or "none". */
    val eye: String

    /** The 4x4 projection matrix as a Float32Array. */
    val projectionMatrix: dynamic /* Float32Array */

    /** The view transform (inverse of the eye's model matrix). */
    val transform: XRRigidTransform

    /** Recommended viewport width, if available. */
    val recommendedViewportScale: Double?

    /**
     * Request a specific viewport scale for this view.
     * Values between 0 and 1 control resolution scaling for performance.
     */
    fun requestViewportScale(scale: Double)
}

/**
 * External declaration for [XRRigidTransform](https://developer.mozilla.org/en-US/docs/Web/API/XRRigidTransform).
 *
 * Represents a position and orientation as a 4x4 matrix.
 */
external interface XRRigidTransform {
    /** Position as a DOMPointReadOnly (x, y, z, w=1). */
    val position: DOMPointReadOnly

    /** Orientation as a quaternion DOMPointReadOnly (x, y, z, w). */
    val orientation: DOMPointReadOnly

    /** The 4x4 transform matrix as a Float32Array (column-major). */
    val matrix: dynamic /* Float32Array */

    /** The inverse of this transform. */
    val inverse: XRRigidTransform
}

/**
 * External declaration for [DOMPointReadOnly](https://developer.mozilla.org/en-US/docs/Web/API/DOMPointReadOnly).
 *
 * Used for positions (w=1) and quaternion orientations in WebXR.
 */
external interface DOMPointReadOnly {
    val x: Double
    val y: Double
    val z: Double
    val w: Double
}

/**
 * External declaration for [XRAnchor](https://developer.mozilla.org/en-US/docs/Web/API/XRAnchor).
 *
 * Represents a tracked point in real-world space that persists across frames.
 * Requires the "anchors" session feature.
 */
external interface XRAnchor {
    /** The space associated with this anchor — use with XRFrame.getPose. */
    val anchorSpace: XRSpace

    /** Remove this anchor from tracking. */
    fun delete()
}
