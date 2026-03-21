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
}
