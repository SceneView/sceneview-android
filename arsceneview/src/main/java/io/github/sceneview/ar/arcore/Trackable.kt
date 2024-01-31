package io.github.sceneview.ar.arcore

import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState

val Trackable.isTracking get() = trackingState == TrackingState.TRACKING

/**
 * Creates an anchor that is attached to this trackable, using the given initial pose in the world
 * coordinate space. The type of trackable will determine the semantics of attachment and how the
 * anchor's pose will be updated to maintain this relationship. Note that the relative offset
 * between the pose of multiple anchors attached to a trackable may adjust slightly over time as
 * ARCore updates its model of the world.
 *
 * @return `null` if an exception was thrown during anchor creation.
 */
fun Trackable.createAnchorOrNull(pose: Pose): Anchor? =
    runCatching { createAnchor(pose) }.getOrNull()