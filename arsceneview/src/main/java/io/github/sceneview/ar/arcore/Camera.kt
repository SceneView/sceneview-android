package io.github.sceneview.ar.arcore

import com.google.ar.core.Camera
import com.google.ar.core.TrackingState
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toTransform

/**
 * Whether the AR camera is currently in [TrackingState.TRACKING].
 *
 * When `true`, the camera pose is reliable and AR content can be rendered.
 * When `false`, the screen may be kept unlocked but content should be hidden or frozen.
 */
val Camera.isTracking get() = trackingState == TrackingState.TRACKING

/**
 * Returns the camera's projection matrix as a [Transform] for the given near/far clip planes.
 *
 * @param near Near clip plane distance in meters.
 * @param far  Far clip plane distance in meters.
 */
fun Camera.getProjectionTransform(near: Float, far: Float) = FloatArray(16).apply {
    getProjectionMatrix(this, 0, near, far)
}.toTransform()

/**
 * The camera's view matrix as a [Transform].
 *
 * Converts the ARCore camera's 4x4 view matrix into a SceneView [Transform].
 */
val Camera.viewTransform: Transform
    get() = FloatArray(16).apply { getViewMatrix(this, 0) }.toTransform()