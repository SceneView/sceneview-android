package io.github.sceneview.collision

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class RayTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.001f) {
        assertTrue(abs(expected - actual) < epsilon, "Expected $expected but got $actual")
    }

    @Test
    fun defaultRayOriginIsZero() {
        val ray = Ray()
        val origin = ray.getOrigin()
        assertClose(0f, origin.x)
        assertClose(0f, origin.y)
        assertClose(0f, origin.z)
    }

    @Test
    fun defaultRayDirectionIsForward() {
        val ray = Ray()
        val dir = ray.getDirection()
        assertClose(0f, dir.x)
        assertClose(0f, dir.y)
        assertClose(-1f, dir.z)
    }

    @Test
    fun directionIsNormalized() {
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(3f, 0f, 0f))
        val dir = ray.getDirection()
        assertClose(1f, dir.x)
        assertClose(0f, dir.y)
        assertClose(0f, dir.z)
    }

    @Test
    fun getPointAlongRay() {
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val point = ray.getPoint(5f)
        assertClose(5f, point.x)
        assertClose(0f, point.y)
        assertClose(0f, point.z)
    }

    @Test
    fun getPointWithOffset() {
        val ray = Ray(Vector3(1f, 2f, 3f), Vector3(0f, 1f, 0f))
        val point = ray.getPoint(10f)
        assertClose(1f, point.x)
        assertClose(12f, point.y)
        assertClose(3f, point.z)
    }
}
