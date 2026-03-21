package io.github.sceneview.components

import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position

/**
 * Cross-platform light component interface.
 *
 * Defines light properties using pure kotlin-math types.
 * Platform modules implement this with their rendering backend.
 */
interface LightComponent : Component {

    /** Type of light source. */
    enum class LightType {
        /** Directional light (sun-like, no position). */
        DIRECTIONAL,
        /** Point light (omnidirectional from a position). */
        POINT,
        /** Focused cone light from a position in a direction. */
        FOCUSED_SPOT,
        /** Spot light with a softer cone. */
        SPOT,
        /** Sun light (directional with angular radius for soft shadows). */
        SUN
    }

    /** The type of this light. */
    val type: LightType

    /** Light position in local space. */
    var lightPosition: Position

    /** Light orientation as quaternion. */
    var lightQuaternion: Quaternion

    /** Light direction vector. */
    var lightDirection: Direction

    /** Light color (linear RGB). */
    var color: Color

    /**
     * Luminous intensity in candela (for point/spot) or lux (for directional).
     */
    var intensity: Float

    /**
     * Distance at which the light has no more influence (point/spot lights).
     */
    var falloff: Float

    /** Whether this light casts shadows. */
    var isShadowCaster: Boolean

    /**
     * Sets spot light cone angles.
     *
     * @param inner inner cone angle in radians
     * @param outer outer cone angle in radians
     */
    fun setSpotLightCone(inner: Float, outer: Float)

    /**
     * Sets intensity from physical units.
     *
     * @param watts power consumption
     * @param efficiency luminous efficiency (0-1)
     */
    fun setIntensity(watts: Float, efficiency: Float)

    /**
     * Sets intensity directly in candela.
     */
    fun setIntensityCandela(intensity: Float)

    /** Sun angular radius (SUN type only). */
    var sunAngularRadius: Float

    /** Sun halo size (SUN type only). */
    var sunHaloSize: Float

    /** Sun halo falloff (SUN type only). */
    var sunHaloFalloff: Float

    /**
     * Enables/disables a light channel.
     */
    fun setLightChannel(channel: Int, enable: Boolean)

    /**
     * Returns whether a light channel is enabled.
     */
    fun getLightChannel(channel: Int): Boolean
}
