package io.github.sceneview.collision

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A Vector with 3 floats.
 */
class Vector3 {
    var x: Float
    var y: Float
    var z: Float

    /** Construct a Vector3 and assign zero to all values */
    constructor() {
        x = 0f
        y = 0f
        z = 0f
    }

    /** Construct a Vector3 and assign each value */
    constructor(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    /** Construct a Vector3 and copy the values */
    constructor(v: Vector3) {
        x = v.x
        y = v.y
        z = v.z
    }

    /** Copy the values from another Vector3 to this Vector3 */
    fun set(v: Vector3) {
        x = v.x
        y = v.y
        z = v.z
    }

    /** Set each value */
    fun set(vx: Float, vy: Float, vz: Float) {
        x = vx
        y = vy
        z = vz
    }

    internal fun setZero() {
        set(0f, 0f, 0f)
    }

    internal fun setOne() {
        set(1f, 1f, 1f)
    }

    internal fun setForward() {
        set(0f, 0f, -1f)
    }

    internal fun setBack() {
        set(0f, 0f, 1f)
    }

    internal fun setUp() {
        set(0f, 1f, 0f)
    }

    internal fun setDown() {
        set(0f, -1f, 0f)
    }

    internal fun setRight() {
        set(1f, 0f, 0f)
    }

    internal fun setLeft() {
        set(-1f, 0f, 0f)
    }

    fun lengthSquared(): Float = x * x + y * y + z * z

    fun length(): Float = sqrt(lengthSquared())

    override fun toString(): String = "[x=$x, y=$y, z=$z]"

    /** Scales the Vector3 to the unit length */
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

    /** Uniformly scales a Vector3 */
    fun scaled(a: Float): Vector3 = Vector3(x * a, y * a, z * a)

    /** Negates a Vector3 */
    fun negated(): Vector3 = Vector3(-x, -y, -z)

    override fun equals(other: Any?): Boolean {
        if (other !is Vector3) {
            return false
        }
        if (this === other) {
            return true
        }
        return equals(this, other)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + x.toBits()
        result = prime * result + y.toBits()
        result = prime * result + z.toBits()
        return result
    }

    companion object {
        fun add(lhs: Vector3, rhs: Vector3): Vector3 =
            Vector3(lhs.x + rhs.x, lhs.y + rhs.y, lhs.z + rhs.z)

        fun subtract(lhs: Vector3, rhs: Vector3): Vector3 =
            Vector3(lhs.x - rhs.x, lhs.y - rhs.y, lhs.z - rhs.z)

        fun multiply(lhs: Vector3, rhs: Vector3): Vector3 =
            Vector3(lhs.x * rhs.x, lhs.y * rhs.y, lhs.z * rhs.z)

        fun dot(lhs: Vector3, rhs: Vector3): Float =
            lhs.x * rhs.x + lhs.y * rhs.y + lhs.z * rhs.z

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

        fun min(lhs: Vector3, rhs: Vector3): Vector3 =
            Vector3(min(lhs.x, rhs.x), min(lhs.y, rhs.y), min(lhs.z, rhs.z))

        fun max(lhs: Vector3, rhs: Vector3): Vector3 =
            Vector3(max(lhs.x, rhs.x), max(lhs.y, rhs.y), max(lhs.z, rhs.z))

        internal fun componentMax(a: Vector3): Float = max(max(a.x, a.y), a.z)

        internal fun componentMin(a: Vector3): Float = min(min(a.x, a.y), a.z)

        fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 = Vector3(
            MathHelper.lerp(a.x, b.x, t), MathHelper.lerp(a.y, b.y, t), MathHelper.lerp(a.z, b.z, t)
        )

        /**
         * Returns the shortest angle in degrees between two vectors. The result is never greater than 180
         * degrees.
         */
        fun angleBetweenVectors(a: Vector3, b: Vector3): Float {
            val lengthA = a.length()
            val lengthB = b.length()
            val combinedLength = lengthA * lengthB

            if (MathHelper.almostEqualRelativeAndAbs(combinedLength, 0.0f)) {
                return 0.0f
            }

            var cos = dot(a, b) / combinedLength
            cos = MathHelper.clamp(cos, -1.0f, 1.0f)
            val angleRadians = acos(cos)
            return (angleRadians * (180.0 / PI)).toFloat()
        }

        fun equals(lhs: Vector3, rhs: Vector3): Boolean {
            var result = true
            result = result and MathHelper.almostEqualRelativeAndAbs(lhs.x, rhs.x)
            result = result and MathHelper.almostEqualRelativeAndAbs(lhs.y, rhs.y)
            result = result and MathHelper.almostEqualRelativeAndAbs(lhs.z, rhs.z)
            return result
        }

        fun zero(): Vector3 = Vector3()
        fun one(): Vector3 = Vector3(1f, 1f, 1f)
        fun forward(): Vector3 = Vector3(0f, 0f, -1f)
        fun back(): Vector3 = Vector3(0f, 0f, 1f)
        fun up(): Vector3 = Vector3(0f, 1f, 0f)
        fun down(): Vector3 = Vector3(0f, -1f, 0f)
        fun right(): Vector3 = Vector3(1f, 0f, 0f)
        fun left(): Vector3 = Vector3(-1f, 0f, 0f)
    }
}
