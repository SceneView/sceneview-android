@file:Suppress("INTERFACE_WITH_SUPERCLASS")

package io.github.sceneview.web.xr

/**
 * External declaration for [XRInputSource](https://developer.mozilla.org/en-US/docs/Web/API/XRInputSource) interface.
 *
 * Represents an input device in a WebXR session: motion controllers, hand tracking,
 * screen-based input (tap on phone AR), gaze, etc.
 *
 * Accessed via [XRSession.inputSources] or the `oninputsourceschange` event.
 *
 * ```kotlin
 * session.onselect = { event ->
 *     val inputSource = event.asDynamic().inputSource as XRInputSource
 *     val gripSpace = inputSource.gripSpace
 *     if (gripSpace != null) {
 *         val pose = frame.getPose(gripSpace, referenceSpace)
 *         // Use pose for controller position
 *     }
 * }
 * ```
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/XRInputSource">MDN XRInputSource</a>
 */
external interface XRInputSource {
    /**
     * The handedness of this input source.
     * One of: "none", "left", "right".
     */
    val handedness: String

    /**
     * The targeting ray mode.
     * One of: "gaze", "tracked-pointer", "screen", "transient-pointer".
     */
    val targetRayMode: String

    /**
     * The space representing the targeting ray origin and direction.
     * Use with [XRFrame.getPose] to get the ray in world space.
     */
    val targetRaySpace: XRSpace

    /**
     * The space representing the physical grip (controller body).
     * Null for screen-based or gaze input.
     */
    val gripSpace: XRSpace?

    /**
     * The Gamepad object for button/axis state on controllers.
     * Null if the input source has no gamepad (e.g., hand tracking).
     */
    val gamepad: dynamic /* Gamepad? */

    /**
     * Profiles describing this input source (e.g., "oculus-touch-v3", "generic-trigger").
     * Ordered from most specific to most generic.
     */
    val profiles: Array<String>

    /**
     * The hand object for articulated hand tracking.
     * Null if this input source is not a tracked hand.
     * Requires the "hand-tracking" feature.
     */
    val hand: XRHand?
}

/**
 * XRInputSource handedness constants.
 */
object XRHandedness {
    const val NONE = "none"
    const val LEFT = "left"
    const val RIGHT = "right"
}

/**
 * XRInputSource target ray mode constants.
 */
object XRTargetRayMode {
    /** Gaze-based input (e.g., head direction on Cardboard). */
    const val GAZE = "gaze"

    /** Tracked pointer (e.g., motion controller, tracked hand ray). */
    const val TRACKED_POINTER = "tracked-pointer"

    /** Screen-based input (e.g., tap on phone AR). */
    const val SCREEN = "screen"

    /** Transient pointer (e.g., brief screen touch). */
    const val TRANSIENT_POINTER = "transient-pointer"
}

/**
 * External declaration for [XRHand](https://developer.mozilla.org/en-US/docs/Web/API/XRHand).
 *
 * Provides access to the 25 joints of a tracked hand.
 * Requires the "hand-tracking" session feature.
 *
 * Each joint is an [XRJointSpace] that can be used with [XRFrame.getPose]
 * to get its position and orientation.
 */
external interface XRHand {
    /** The number of joints (always 25 per the spec). */
    val size: Int

    /**
     * Get the [XRJointSpace] for a joint by name.
     *
     * @param jointName One of [XRHandJoint] constants (e.g., "index-finger-tip")
     * @return The joint space, or undefined if not tracked
     */
    fun get(jointName: String): XRJointSpace?
}

/**
 * External declaration for [XRJointSpace](https://developer.mozilla.org/en-US/docs/Web/API/XRJointSpace).
 *
 * Represents a single joint of a tracked hand. Extends [XRSpace].
 */
external interface XRJointSpace : XRSpace {
    /** The name of this joint (e.g., "index-finger-tip"). */
    val jointName: String
}

/**
 * WebXR hand joint name constants.
 *
 * The 25 joints defined by the WebXR Hand Input spec.
 */
object XRHandJoint {
    const val WRIST = "wrist"

    const val THUMB_METACARPAL = "thumb-metacarpal"
    const val THUMB_PHALANX_PROXIMAL = "thumb-phalanx-proximal"
    const val THUMB_PHALANX_DISTAL = "thumb-phalanx-distal"
    const val THUMB_TIP = "thumb-tip"

    const val INDEX_FINGER_METACARPAL = "index-finger-metacarpal"
    const val INDEX_FINGER_PHALANX_PROXIMAL = "index-finger-phalanx-proximal"
    const val INDEX_FINGER_PHALANX_INTERMEDIATE = "index-finger-phalanx-intermediate"
    const val INDEX_FINGER_PHALANX_DISTAL = "index-finger-phalanx-distal"
    const val INDEX_FINGER_TIP = "index-finger-tip"

    const val MIDDLE_FINGER_METACARPAL = "middle-finger-metacarpal"
    const val MIDDLE_FINGER_PHALANX_PROXIMAL = "middle-finger-phalanx-proximal"
    const val MIDDLE_FINGER_PHALANX_INTERMEDIATE = "middle-finger-phalanx-intermediate"
    const val MIDDLE_FINGER_PHALANX_DISTAL = "middle-finger-phalanx-distal"
    const val MIDDLE_FINGER_TIP = "middle-finger-tip"

    const val RING_FINGER_METACARPAL = "ring-finger-metacarpal"
    const val RING_FINGER_PHALANX_PROXIMAL = "ring-finger-phalanx-proximal"
    const val RING_FINGER_PHALANX_INTERMEDIATE = "ring-finger-phalanx-intermediate"
    const val RING_FINGER_PHALANX_DISTAL = "ring-finger-phalanx-distal"
    const val RING_FINGER_TIP = "ring-finger-tip"

    const val PINKY_FINGER_METACARPAL = "pinky-finger-metacarpal"
    const val PINKY_FINGER_PHALANX_PROXIMAL = "pinky-finger-phalanx-proximal"
    const val PINKY_FINGER_PHALANX_INTERMEDIATE = "pinky-finger-phalanx-intermediate"
    const val PINKY_FINGER_PHALANX_DISTAL = "pinky-finger-phalanx-distal"
    const val PINKY_FINGER_TIP = "pinky-finger-tip"
}
