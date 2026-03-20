package io.github.sceneview.collision

import io.github.sceneview.logging.logWarning
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Mathematical representation of a sphere. Used to perform intersection and collision tests against
 * spheres.
 */
class Sphere : CollisionShape {
    private val center = Vector3()
    internal var radius = 1.0f

    /** Create a sphere with a center of (0,0,0) and a radius of 1. */
    constructor()

    constructor(radius: Float) : this(radius, Vector3.zero())

    constructor(radius: Float, center: Vector3) {
        Preconditions.checkNotNull(center, "Parameter \"center\" was null.")
        setCenter(center)
        setRadius(radius)
    }

    fun setCenter(center: Vector3) {
        Preconditions.checkNotNull(center, "Parameter \"center\" was null.")
        this.center.set(center)
        onChanged()
    }

    fun getCenter(): Vector3 = Vector3(center)

    fun setRadius(radius: Float) {
        this.radius = radius
        onChanged()
    }

    fun getRadius(): Float = radius

    override fun makeCopy(): Sphere = Sphere(getRadius(), getCenter())

    override fun rayIntersection(ray: Ray, result: RayHit): Boolean {
        Preconditions.checkNotNull(ray, "Parameter \"ray\" was null.")
        Preconditions.checkNotNull(result, "Parameter \"result\" was null.")

        val rayDirection = ray.getDirection()
        val rayOrigin = ray.getOrigin()

        val difference = Vector3.subtract(rayOrigin, center)
        val b = 2.0f * Vector3.dot(difference, rayDirection)
        val c = Vector3.dot(difference, difference) - radius * radius
        val discriminant = b * b - 4.0f * c

        if (discriminant < 0.0f) {
            return false
        }

        val discriminantSqrt = sqrt(discriminant.toDouble()).toFloat()
        val tMinus = (-b - discriminantSqrt) / 2.0f
        val tPlus = (-b + discriminantSqrt) / 2.0f

        if (tMinus < 0.0f && tPlus < 0.0f) {
            return false
        }

        if (tMinus < 0 && tPlus > 0) {
            result.setDistance(tPlus)
        } else {
            result.setDistance(tMinus)
        }

        result.setPoint(ray.getPoint(result.getDistance()))
        return true
    }

    override fun shapeIntersection(shape: CollisionShape): Boolean {
        Preconditions.checkNotNull(shape, "Parameter \"shape\" was null.")
        return shape.sphereIntersection(this)
    }

    override fun sphereIntersection(sphere: Sphere): Boolean =
        Intersections.sphereSphereIntersection(this, sphere)

    override fun boxIntersection(box: Box): Boolean =
        Intersections.sphereBoxIntersection(this, box)

    override fun transform(transformProvider: TransformProvider): CollisionShape {
        Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.")
        val result = Sphere()
        transform(transformProvider, result)
        return result
    }

    override fun transform(transformProvider: TransformProvider, result: CollisionShape) {
        Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.")
        Preconditions.checkNotNull(result, "Parameter \"result\" was null.")

        if (result !is Sphere) {
            logWarning(TAG, "Cannot pass CollisionShape of a type other than Sphere into Sphere.transform.")
            return
        }

        val modelMatrix = transformProvider.getTransformationMatrix()

        result.setCenter(modelMatrix.transformPoint(center))

        val worldScale = Vector3()
        modelMatrix.decomposeScale(worldScale)
        val maxScale = max(
            abs(min(min(worldScale.x, worldScale.y), worldScale.z)),
            max(max(worldScale.x, worldScale.y), worldScale.z)
        )
        result.radius = radius * maxScale
    }

    companion object {
        private const val TAG = "Sphere"
    }
}
