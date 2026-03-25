package io.github.sceneview.collision

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class QuaternionTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.001f) {
        assertTrue(abs(expected - actual) < epsilon, "Expected $expected but got $actual")
    }

    @Test
    fun identityQuaternion() {
        val q = Quaternion.identity()
        assertClose(0f, q.x)
        assertClose(0f, q.y)
        assertClose(0f, q.z)
        assertClose(1f, q.w)
    }

    @Test
    fun multiplyByIdentity() {
        val q = Quaternion(0.5f, 0.5f, 0.5f, 0.5f)
        val identity = Quaternion.identity()
        val result = Quaternion.multiply(q, identity)
        assertClose(q.x, result.x)
        assertClose(q.y, result.y)
        assertClose(q.z, result.z)
        assertClose(q.w, result.w)
    }

    @Test
    fun normalizeQuaternion() {
        val q = Quaternion(1f, 1f, 1f, 1f)
        q.normalize()
        val length = kotlin.math.sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w)
        assertClose(1f, length)
    }

    @Test
    fun slerpIdentities() {
        val a = Quaternion.identity()
        val b = Quaternion.identity()
        val result = Quaternion.slerp(a, b, 0.5f)
        assertClose(0f, result.x)
        assertClose(0f, result.y)
        assertClose(0f, result.z)
        assertClose(1f, result.w)
    }

    // --- Additional tests ---

    @Test
    fun axisAngle90DegreesAroundY() {
        val q = Quaternion.axisAngle(Vector3.up(), 90f)
        // Rotating (1,0,0) by 90 degrees around Y should give (0,0,-1)
        val rotated = Quaternion.rotateVector(q, Vector3(1f, 0f, 0f))
        assertClose(0f, rotated.x, 0.01f)
        assertClose(0f, rotated.y, 0.01f)
        assertClose(-1f, rotated.z, 0.01f)
    }

    @Test
    fun axisAngle180DegreesAroundZ() {
        val q = Quaternion.axisAngle(Vector3(0f, 0f, 1f), 180f)
        // Rotating (1,0,0) by 180 degrees around Z should give (-1,0,0)
        val rotated = Quaternion.rotateVector(q, Vector3(1f, 0f, 0f))
        assertClose(-1f, rotated.x, 0.01f)
        assertClose(0f, rotated.y, 0.01f)
        assertClose(0f, rotated.z, 0.01f)
    }

    @Test
    fun invertedQuaternionReversesRotation() {
        val q = Quaternion.axisAngle(Vector3.up(), 45f)
        val inv = q.inverted()
        val product = Quaternion.multiply(q, inv)
        // Should be approximately identity
        assertClose(0f, product.x, 0.01f)
        assertClose(0f, product.y, 0.01f)
        assertClose(0f, product.z, 0.01f)
        assertClose(1f, product.w, 0.01f)
    }

    @Test
    fun eulerAnglesToQuaternionAndBack() {
        val euler = Vector3(30f, 45f, 60f)
        val q = Quaternion.eulerAngles(euler)
        val backEuler = q.getEulerAngles()
        assertClose(euler.x, backEuler.x, 1f)
        assertClose(euler.y, backEuler.y, 1f)
        assertClose(euler.z, backEuler.z, 1f)
    }

    @Test
    fun rotateVectorPreservesLength() {
        val q = Quaternion.axisAngle(Vector3(1f, 1f, 1f).normalized(), 60f)
        val v = Vector3(3f, 4f, 0f)
        val rotated = Quaternion.rotateVector(q, v)
        assertClose(v.length(), rotated.length(), 0.01f)
    }

    @Test
    fun rotationBetweenParallelVectors() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(2f, 0f, 0f)
        val q = Quaternion.rotationBetweenVectors(a, b)
        // Should be near identity
        assertClose(0f, q.x, 0.01f)
        assertClose(0f, q.y, 0.01f)
        assertClose(0f, q.z, 0.01f)
        assertClose(1f, q.w, 0.01f)
    }

    @Test
    fun rotationBetweenPerpendicularVectors() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(0f, 1f, 0f)
        val q = Quaternion.rotationBetweenVectors(a, b)
        val rotated = Quaternion.rotateVector(q, a.normalized())
        assertClose(0f, rotated.x, 0.01f)
        assertClose(1f, rotated.y, 0.01f)
        assertClose(0f, rotated.z, 0.01f)
    }

    @Test
    fun slerpAtBoundaries() {
        val a = Quaternion.identity()
        val b = Quaternion.axisAngle(Vector3.up(), 90f)
        val atZero = Quaternion.slerp(a, b, 0f)
        assertClose(a.x, atZero.x)
        assertClose(a.y, atZero.y)
        assertClose(a.z, atZero.z)
        assertClose(a.w, atZero.w)

        val atOne = Quaternion.slerp(a, b, 1f)
        assertClose(b.x, atOne.x, 0.01f)
        assertClose(b.y, atOne.y, 0.01f)
        assertClose(b.z, atOne.z, 0.01f)
        assertClose(b.w, atOne.w, 0.01f)
    }

    @Test
    fun quaternionEquality() {
        val a = Quaternion.axisAngle(Vector3.up(), 45f)
        val b = Quaternion.axisAngle(Vector3.up(), 45f)
        assertTrue(a == b)
    }
}
