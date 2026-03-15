package io.github.sceneview.collision

import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.math.toFloat3

/**
 * Stores the results of ray intersection tests against various types of CollisionShape.
 *
 * @hide
 */
open class RayHit {
    private var distance = Float.MAX_VALUE
    private val point = Vector3()

    /**
     * @hide
     */
    fun setDistance(distance: Float) {
        this.distance = distance
    }

    /**
     * Get the distance along the ray to the impact point on the surface of the collision shape.
     *
     * @return distance along the ray that the hit occurred at
     */
    fun getDistance(): Float = distance

    /**
     * @hide
     */
    fun setPoint(point: Vector3) {
        Preconditions.checkNotNull(point, "Parameter \"point\" was null.")
        this.point.set(point)
    }

    /**
     * Get the position in world-space where the ray hit the collision shape.
     *
     * @return a new vector that represents the position in world-space that the hit occurred at
     */
    fun getPoint(): Vector3 = Vector3(point)

    /**
     * Get the position in world-space where the ray hit the collision shape.
     *
     * @return a new Float3 that represents the position in world-space that the hit occurred at
     */
    fun getWorldPosition(): Float3 = getPoint().toFloat3()

    /**
     * @hide
     */
    fun set(other: RayHit) {
        Preconditions.checkNotNull(other, "Parameter \"other\" was null.")

        setDistance(other.distance)
        setPoint(other.point)
    }

    /**
     * @hide
     */
    open fun reset() {
        distance = Float.MAX_VALUE
        point.set(0f, 0f, 0f)
    }
}
