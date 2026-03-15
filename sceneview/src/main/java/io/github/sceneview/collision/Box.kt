package io.github.sceneview.collision

import android.util.Log

/**
 * Mathematical representation of a box. Used to perform intersection and collision tests against
 * oriented boxes.
 */
class Box : CollisionShape {
    private val center = Vector3.zero()
    private val size = Vector3.one()
    private val rotationMatrix = Matrix()

    companion object {
        private val TAG = Box::class.java.simpleName
    }

    /** Create a box with a center of (0,0,0) and a size of (1,1,1). */
    constructor()

    /**
     * Create a box with a center of (0,0,0) and a specified size.
     *
     * @param size the size of the box.
     */
    constructor(size: Vector3) : this(size, Vector3.zero())

    /**
     * Create a box with a specified center and size.
     *
     * @param size the size of the box
     * @param center the center of the box
     */
    constructor(size: Vector3, center: Vector3) {
        Preconditions.checkNotNull(center, "Parameter \"center\" was null.")
        Preconditions.checkNotNull(size, "Parameter \"size\" was null.")

        setCenter(center)
        setSize(size)
    }

    /**
     * Set the center of this box.
     *
     * @see getCenter
     * @param center the new center of the box
     */
    fun setCenter(center: Vector3) {
        Preconditions.checkNotNull(center, "Parameter \"center\" was null.")
        this.center.set(center)
        onChanged()
    }

    /**
     * Get a copy of the box's center.
     *
     * @see setCenter
     * @return a new vector that represents the box's center
     */
    fun getCenter(): Vector3 = Vector3(center)

    /**
     * Set the size of this box.
     *
     * @see getSize
     * @param size the new size of the box
     */
    fun setSize(size: Vector3) {
        Preconditions.checkNotNull(size, "Parameter \"size\" was null.")
        this.size.set(size)
        onChanged()
    }

    /**
     * Get a copy of the box's size.
     *
     * @see setSize
     * @return a new vector that represents the box's size
     */
    fun getSize(): Vector3 = Vector3(size)

    /**
     * Calculate the extents (half the size) of the box.
     *
     * @return a new vector that represents the box's extents
     */
    fun getExtents(): Vector3 = getSize().scaled(0.5f)

    /**
     * Set the rotation of this box.
     *
     * @see getRotation
     * @param rotation the new rotation of the box
     */
    fun setRotation(rotation: Quaternion) {
        Preconditions.checkNotNull(rotation, "Parameter \"rotation\" was null.")
        rotationMatrix.makeRotation(rotation)
        onChanged()
    }

    /**
     * Get a copy of the box's rotation.
     *
     * @see setRotation
     * @return a new quaternion that represents the box's rotation
     */
    fun getRotation(): Quaternion {
        val result = Quaternion()
        rotationMatrix.extractQuaternion(result)
        return result
    }

    override fun makeCopy(): Box = Box(getSize(), getCenter())

    /**
     * Get the raw rotation matrix representing the box's orientation. Do not modify directly.
     * Instead, use setRotation.
     *
     * @return a reference to the box's raw rotation matrix
     */
    internal fun getRawRotationMatrix(): Matrix = rotationMatrix

    /** @hide protected method */
    override fun rayIntersection(ray: Ray, result: RayHit): Boolean {
        Preconditions.checkNotNull(ray, "Parameter \"ray\" was null.")
        Preconditions.checkNotNull(result, "Parameter \"result\" was null.")

        val rayDirection = ray.getDirection()
        val rayOrigin = ray.getOrigin()
        val max = getExtents()
        val min = max.negated()

        // tMin is the farthest "near" intersection (amongst the X,Y and Z planes pairs)
        var tMin = Float.MIN_VALUE

        // tMax is the nearest "far" intersection (amongst the X,Y and Z planes pairs)
        var tMax = Float.MAX_VALUE

        val delta = Vector3.subtract(center, rayOrigin)

        // Test intersection with the 2 planes perpendicular to the OBB's x axis.
        val axes = rotationMatrix.data
        var axis = Vector3(axes[0], axes[1], axes[2])
        var e = Vector3.dot(axis, delta)
        var f = Vector3.dot(rayDirection, axis)

        if (!MathHelper.almostEqualRelativeAndAbs(f, 0.0f)) {
            var t1 = (e + min.x) / f
            var t2 = (e + max.x) / f

            if (t1 > t2) {
                val temp = t1; t1 = t2; t2 = temp
            }

            tMax = Math.min(t2, tMax)
            tMin = Math.max(t1, tMin)

            if (tMax < tMin) {
                return false
            }
        } else if (-e + min.x > 0.0f || -e + max.x < 0.0f) {
            // Ray is almost parallel to one of the planes.
            return false
        }

        // Test intersection with the 2 planes perpendicular to the OBB's y axis.
        axis = Vector3(axes[4], axes[5], axes[6])
        e = Vector3.dot(axis, delta)
        f = Vector3.dot(rayDirection, axis)

        if (!MathHelper.almostEqualRelativeAndAbs(f, 0.0f)) {
            var t1 = (e + min.y) / f
            var t2 = (e + max.y) / f

            if (t1 > t2) {
                val temp = t1; t1 = t2; t2 = temp
            }

            tMax = Math.min(t2, tMax)
            tMin = Math.max(t1, tMin)

            if (tMax < tMin) {
                return false
            }
        } else if (-e + min.y > 0.0f || -e + max.y < 0.0f) {
            // Ray is almost parallel to one of the planes.
            return false
        }

        // Test intersection with the 2 planes perpendicular to the OBB's z axis.
        axis = Vector3(axes[8], axes[9], axes[10])
        e = Vector3.dot(axis, delta)
        f = Vector3.dot(rayDirection, axis)

        if (!MathHelper.almostEqualRelativeAndAbs(f, 0.0f)) {
            var t1 = (e + min.z) / f
            var t2 = (e + max.z) / f

            if (t1 > t2) {
                val temp = t1; t1 = t2; t2 = temp
            }

            tMax = Math.min(t2, tMax)
            tMin = Math.max(t1, tMin)

            if (tMax < tMin) {
                return false
            }
        } else if (-e + min.z > 0.0f || -e + max.z < 0.0f) {
            // Ray is almost parallel to one of the planes.
            return false
        }

        result.setDistance(tMin)
        result.setPoint(ray.getPoint(result.getDistance()))
        return true
    }

    /** @hide protected method */
    override fun shapeIntersection(shape: CollisionShape): Boolean {
        Preconditions.checkNotNull(shape, "Parameter \"shape\" was null.")
        return shape.boxIntersection(this)
    }

    /** @hide internal method */
    override fun sphereIntersection(sphere: Sphere): Boolean =
        Intersections.sphereBoxIntersection(sphere, this)

    /** @hide internal method */
    override fun boxIntersection(box: Box): Boolean =
        Intersections.boxBoxIntersection(this, box)

    override fun transform(transformProvider: TransformProvider): CollisionShape {
        Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.")

        val result = Box()
        transform(transformProvider, result)
        return result
    }

    override fun transform(transformProvider: TransformProvider, result: CollisionShape) {
        Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.")
        Preconditions.checkNotNull(result, "Parameter \"result\" was null.")

        if (result !is Box) {
            Log.w(TAG, "Cannot pass CollisionShape of a type other than Box into Box.transform.")
            return
        }

        if (result === this) {
            throw IllegalArgumentException("Box cannot transform itself.")
        }

        val modelMatrix = transformProvider.getTransformationMatrix()

        // Transform the center of the box.
        result.center.set(modelMatrix.transformPoint(center))

        // Transform the size of the box.
        val worldScale = Vector3()
        modelMatrix.decomposeScale(worldScale)
        result.size.x = size.x * worldScale.x
        result.size.y = size.y * worldScale.y
        result.size.z = size.z * worldScale.z

        // Transform the rotation of the box.
        modelMatrix.decomposeRotation(worldScale, result.rotationMatrix)
        Matrix.multiply(rotationMatrix, result.rotationMatrix, result.rotationMatrix)
    }
}
