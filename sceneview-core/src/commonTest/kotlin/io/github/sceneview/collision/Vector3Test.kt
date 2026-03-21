package io.github.sceneview.collision

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Vector3Test {

    @Test
    fun defaultConstructorCreatesZeroVector() {
        val v = Vector3()
        assertEquals(0f, v.x)
        assertEquals(0f, v.y)
        assertEquals(0f, v.z)
    }

    @Test
    fun constructorSetsValues() {
        val v = Vector3(1f, 2f, 3f)
        assertEquals(1f, v.x)
        assertEquals(2f, v.y)
        assertEquals(3f, v.z)
    }

    @Test
    fun addTwoVectors() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(4f, 5f, 6f)
        val result = Vector3.add(a, b)
        assertEquals(5f, result.x)
        assertEquals(7f, result.y)
        assertEquals(9f, result.z)
    }

    @Test
    fun subtractTwoVectors() {
        val a = Vector3(4f, 5f, 6f)
        val b = Vector3(1f, 2f, 3f)
        val result = Vector3.subtract(a, b)
        assertEquals(3f, result.x)
        assertEquals(3f, result.y)
        assertEquals(3f, result.z)
    }

    @Test
    fun dotProduct() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(0f, 1f, 0f)
        assertEquals(0f, Vector3.dot(a, b))

        val c = Vector3(1f, 2f, 3f)
        val d = Vector3(4f, 5f, 6f)
        assertEquals(32f, Vector3.dot(c, d))
    }

    @Test
    fun crossProduct() {
        val x = Vector3(1f, 0f, 0f)
        val y = Vector3(0f, 1f, 0f)
        val z = Vector3.cross(x, y)
        assertEquals(0f, z.x)
        assertEquals(0f, z.y)
        assertEquals(1f, z.z)
    }

    @Test
    fun lengthOfUnitVector() {
        val v = Vector3(1f, 0f, 0f)
        assertEquals(1f, v.length())
    }

    @Test
    fun lengthOf345Triangle() {
        val v = Vector3(3f, 4f, 0f)
        assertEquals(5f, v.length())
    }

    @Test
    fun scaleVector() {
        val v = Vector3(1f, 2f, 3f)
        v.set(Vector3(v.x * 2, v.y * 2, v.z * 2))
        assertEquals(2f, v.x)
        assertEquals(4f, v.y)
        assertEquals(6f, v.z)
    }

    @Test
    fun lerpHalfway() {
        val a = Vector3(0f, 0f, 0f)
        val b = Vector3(10f, 10f, 10f)
        val result = Vector3.lerp(a, b, 0.5f)
        assertEquals(5f, result.x)
        assertEquals(5f, result.y)
        assertEquals(5f, result.z)
    }
}
