package io.github.sceneview.ar.arcore

import com.google.ar.core.Earth
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.toQuaternion

fun Earth.createAnchor(
    latitude: Double,
    longitude: Double,
    altitude: Double,
    quaternion: Quaternion
) = createAnchor(
    latitude,
    longitude,
    altitude,
    quaternion.x,
    quaternion.y,
    quaternion.z,
    quaternion.w
)

fun Earth.createAnchor(
    latitude: Double,
    longitude: Double,
    altitude: Double,
    rotation: Rotation
) = createAnchor(
    latitude,
    longitude,
    altitude,
    rotation.toQuaternion()
)
