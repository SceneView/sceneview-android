package io.github.sceneview.ar.arcore

import com.google.ar.core.Earth
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.toQuaternion

/**
 * Creates a Geospatial anchor at the given WGS84 coordinates with a [Quaternion] orientation.
 *
 * @param latitude  WGS84 latitude in degrees.
 * @param longitude WGS84 longitude in degrees.
 * @param altitude  Altitude in meters above the WGS84 ellipsoid.
 * @param quaternion East-up-south rotation quaternion.
 */
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

/**
 * Creates a Geospatial anchor at the given WGS84 coordinates with Euler [Rotation].
 *
 * The rotation is converted to a quaternion before being passed to ARCore.
 *
 * @param latitude  WGS84 latitude in degrees.
 * @param longitude WGS84 longitude in degrees.
 * @param altitude  Altitude in meters above the WGS84 ellipsoid.
 * @param rotation  Euler rotation (pitch, yaw, roll) in degrees.
 */
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
