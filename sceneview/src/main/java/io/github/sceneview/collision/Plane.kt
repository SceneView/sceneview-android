package io.github.sceneview.collision

/**
 * Mathematical representation of a plane with an infinite size. Used for intersection tests.
 *
 * @hide
 */
class Plane(center: Vector3, normal: Vector3) {
    private val center = Vector3()
    private val normal = Vector3()

    companion object {
        private const val NEAR_ZERO_THRESHOLD = 1e-6
    }

    init {
        setCenter(center)
        setNormal(normal)
    }

    fun setCenter(center: Vector3) {
        Preconditions.checkNotNull(center, "Parameter \"center\" was null.")
        this.center.set(center)
    }

    fun getCenter(): Vector3 = Vector3(center)

    fun setNormal(normal: Vector3) {
        Preconditions.checkNotNull(normal, "Parameter \"normal\" was null.")
        this.normal.set(normal.normalized())
    }

    fun getNormal(): Vector3 = Vector3(normal)

    fun rayIntersection(ray: Ray, result: RayHit): Boolean {
        Preconditions.checkNotNull(ray, "Parameter \"ray\" was null.")
        Preconditions.checkNotNull(result, "Parameter \"result\" was null.")

        val rayDirection = ray.getDirection()
        val rayOrigin = ray.getOrigin()

        val denominator = Vector3.dot(normal, rayDirection)
        if (Math.abs(denominator) > NEAR_ZERO_THRESHOLD) {
            val delta = Vector3.subtract(center, rayOrigin)
            val distance = Vector3.dot(delta, normal) / denominator
            if (distance >= 0) {
                result.setDistance(distance)
                result.setPoint(ray.getPoint(result.getDistance()))
                return true
            }
        }

        return false
    }
}
