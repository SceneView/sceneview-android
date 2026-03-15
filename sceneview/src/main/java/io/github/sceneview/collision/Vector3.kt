package io.github.sceneview.collision

import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A Vector with 3 floats.
 */
// TODO: Evaluate consolidating internal math. Additional bugs: b/69935335
class Vector3 {
    @JvmField var x: Float
    @JvmField var y: Float
    @JvmField var z: Float

    /**
     * Construct a Vector3 and assign zero to all values
     */
    constructor() {
        x = 0f
        y = 0f
        z = 0f
    }

    /**
     * Construct a Vector3 and assign each value
     */
    constructor(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    /**
     * Construct a Vector3 and copy the values
     */
    constructor(v: Vector3) {
        x = v.x
        y = v.y
        z = v.z
    }

    /**
     * Copy the values from another Vector3 to this Vector3
     */
    fun set(v: Vector3) {
        x = v.x
        y = v.y
        z = v.z
    }

    /**
     * Set each value
     */
    fun set(vx: Float, vy: Float, vz: Float) {
        x = vx
        y = vy
        z = vz
    }

    /**
     * Set each value to zero
     */
    internal fun setZero() {
        set(0f, 0f, 0f)
    }

    /**
     * Set each value to one
     */
    internal fun setOne() {
        set(1f, 1f, 1f)
    }

    /**
     * Forward into the screen is the negative Z direction
     */
    internal fun setForward() {
        set(0f, 0f, -1f)
    }

    /**
     * Back out of the screen is the positive Z direction
     */
    internal fun setBack() {
        set(0f, 0f, 1f)
    }

    /**
     * Up is the positive Y direction
     */
    internal fun setUp() {
        set(0f, 1f, 0f)
    }

    /**
     * Down is the negative Y direction
     */
    internal fun setDown() {
        set(0f, -1f, 0f)
    }

    /**
     * Right is the positive X direction
     */
    internal fun setRight() {
        set(1f, 0f, 0f)
    }

    /**
     * Left is the negative X direction
     */
    internal fun setLeft() {
        set(-1f, 0f, 0f)
    }

    fun lengthSquared(): Float = x * x + y * y + z * z

    fun length(): Float = sqrt(lengthSquared())

    override fun toString(): String = "[x=$x, y=$y, z=$z]"

    /**
     * Scales the Vector3 to the unit length
     */
    fun normalized(): Vector3 {
        val result = Vector3(this)
        val normSquared = dot(this, this)

        if (MathHelper.almostEqualRelativeAndAbs(normSquared, 0.0f)) {
            result.setZero()
        } else if (normSquared != 1f) {
            val norm = (1.0 / sqrt(normSquared)).toFloat()
            result.set(this.scaled(norm))
        }
        return result
    }

    /**
     * Uniformly scales a Vector3
     *
     * @return a Vector3 multiplied by a scalar amount
     */
    fun scaled(a: Float): Vector3 = Vector3(x * a, y * a, z * a)

    /**
     * Negates a Vector3
     *
     * @return A Vector3 with opposite direction
     */
    fun negated(): Vector3 = Vector3(-x, -y, -z)

    /**
     * Returns true if the other object is a Vector3 and each component is equal within a tolerance.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Vector3) {
            return false
        }
        if (this === other) {
            return true
        }
        return equals(this, other)
    }

    /**
     * @hide
     */
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + x.toBits()
        result = prime * result + y.toBits()
        result = prime * result + z.toBits()
        return result
    }

    companion object {
        /**
         * Adds two Vector3's
         *
         * @return The combined Vector3
         */
        @JvmStatic
        fun add(lhs: Vector3, rhs: Vector3): Vector3 =
            Vector3(lhs.x + rhs.x, lhs.y + rhs.y, lhs.z + rhs.z)

        /**
         * Subtract two Vector3
         *
         * @return The combined Vector3
         */
        @JvmStatic
        fun subtract(lhs: Vector3, rhs: Vector3): Vector3 =
            Vector3(lhs.x - rhs.x, lhs.y - rhs.y, lhs.z - rhs.z)

        @JvmStatic
        fun multiply(lhs: Vector3, rhs: Vector3): Vector3 =
            Vector3(lhs.x * rhs.x, lhs.y * rhs.y, lhs.z * rhs.z)

        /**
         * Get dot product of two Vector3's
         *
         * @return The scalar product of the Vector3's
         */
        @JvmStatic
        fun dot(lhs: Vector3, rhs: Vector3): Float =
            lhs.x * rhs.x + lhs.y * rhs.y + lhs.z * rhs.z

        /**
         * Get cross product of two Vector3's
         *
         * @return A Vector3 perpendicular to Vector3's
         */
        @JvmStatic
        fun cross(lhs: Vector3, rhs: Vector3): Vector3 {
            val lhsX = lhs.x
            val lhsY = lhs.y
            val lhsZ = lhs.z
            val rhsX = rhs.x
            val rhsY = rhs.y
            val rhsZ = rhs.z
            return Vector3(
                lhsY * rhsZ - lhsZ * rhsY, lhsZ * rhsX - lhsX * rhsZ, lhsX * rhsY - lhsY * rhsX
            )
        }

        /**
         * Get a Vector3 with each value set to the element wise minimum of two Vector3's values
         */
        @JvmStatic
        fun min(lhs: Vector3, rhs: Vector3): Vector3 =
            Vector3(min(lhs.x, rhs.x), min(lhs.y, rhs.y), min(lhs.z, rhs.z))

        /**
         * Get a Vector3 with each value set to the element wise maximum of two Vector3's values
         */
        @JvmStatic
        fun max(lhs: Vector3, rhs: Vector3): Vector3 =
            Vector3(max(lhs.x, rhs.x), max(lhs.y, rhs.y), max(lhs.z, rhs.z))

        /**
         * Get the maximum value in a single Vector3
         */
        @JvmStatic
        internal fun componentMax(a: Vector3): Float = max(max(a.x, a.y), a.z)

        /**
         * Get the minimum value in a single Vector3
         */
        @JvmStatic
        internal fun componentMin(a: Vector3): Float = min(min(a.x, a.y), a.z)

        /**
         * Linearly interpolates between a and b.
         *
         * @param a the beginning value
         * @param b the ending value
         * @param t ratio between the two floats.
         * @return interpolated value between the two floats
         */
        @JvmStatic
        fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 = Vector3(
            MathHelper.lerp(a.x, b.x, t), MathHelper.lerp(a.y, b.y, t), MathHelper.lerp(a.z, b.z, t)
        )

        /**
         * Returns the shortest angle in degrees between two vectors. The result is never greater than 180
         * degrees.
         */
        @JvmStatic
        fun angleBetweenVectors(a: Vector3, b: Vector3): Float {
            val lengthA = a.length()
            val lengthB = b.length()
            val combinedLength = lengthA * lengthB

            if (MathHelper.almostEqualRelativeAndAbs(combinedLength, 0.0f)) {
                return 0.0f
            }

            var cos = dot(a, b) / combinedLength

            // Clamp due to floating point precision that could cause dot to be > combinedLength.
            // Which would cause acos to return NaN.
            cos = MathHelper.clamp(cos, -1.0f, 1.0f)
            val angleRadians = acos(cos)
            return Math.toDegrees(angleRadians.toDouble()).toFloat()
        }

        /**
         * Compares two Vector3's are equal if each component is equal within a tolerance.
         */
        @JvmStatic
        fun equals(lhs: Vector3, rhs: Vector3): Boolean {
            var result = true
            result = result and MathHelper.almostEqualRelativeAndAbs(lhs.x, rhs.x)
            result = result and MathHelper.almostEqualRelativeAndAbs(lhs.y, rhs.y)
            result = result and MathHelper.almostEqualRelativeAndAbs(lhs.z, rhs.z)
            return result
        }

        /**
         * Gets a Vector3 with all values set to zero
         */
        @JvmStatic
        fun zero(): Vector3 = Vector3()

        /**
         * Gets a Vector3 with all values set to one
         */
        @JvmStatic
        fun one(): Vector3 = Vector3(1f, 1f, 1f)

        /**
         * Gets a Vector3 set to (0, 0, -1)
         */
        @JvmStatic
        fun forward(): Vector3 = Vector3(0f, 0f, -1f)

        /**
         * Gets a Vector3 set to (0, 0, 1)
         */
        @JvmStatic
        fun back(): Vector3 = Vector3(0f, 0f, 1f)

        /**
         * Gets a Vector3 set to (0, 1, 0)
         */
        @JvmStatic
        fun up(): Vector3 = Vector3(0f, 1f, 0f)

        /**
         * Gets a Vector3 set to (0, -1, 0)
         */
        @JvmStatic
        fun down(): Vector3 = Vector3(0f, -1f, 0f)

        /**
         * Gets a Vector3 set to (1, 0, 0)
         */
        @JvmStatic
        fun right(): Vector3 = Vector3(1f, 0f, 0f)

        /**
         * Gets a Vector3 set to (-1, 0, 0)
         */
        @JvmStatic
        fun left(): Vector3 = Vector3(-1f, 0f, 0f)
    }
}
