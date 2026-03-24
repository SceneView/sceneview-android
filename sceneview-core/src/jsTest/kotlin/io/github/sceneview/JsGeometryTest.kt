package io.github.sceneview

import io.github.sceneview.collision.Vector3
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.collision.Ray
import io.github.sceneview.collision.Box
import io.github.sceneview.collision.Sphere
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * JS-specific tests for core geometry and math modules.
 * Validates floating-point behavior in the browser/WASM environment.
 */
class JsGeometryTest {

    @Test
    fun vector3AddSubtract() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(4f, 5f, 6f)
        val sum = Vector3.add(a, b)
        assertEquals(5f, sum.x)
        assertEquals(7f, sum.y)
        assertEquals(9f, sum.z)

        val diff = Vector3.subtract(b, a)
        assertEquals(3f, diff.x)
        assertEquals(3f, diff.y)
        assertEquals(3f, diff.z)
    }

    @Test
    fun vector3DotProduct() {
        val x = Vector3(1f, 0f, 0f)
        val y = Vector3(0f, 1f, 0f)
        assertEquals(0f, Vector3.dot(x, y), "Perpendicular vectors: dot = 0")

        val parallel = Vector3.dot(x, x)
        assertEquals(1f, parallel, "Same direction: dot = 1")
    }

    @Test
    fun vector3CrossProduct() {
        val x = Vector3(1f, 0f, 0f)
        val y = Vector3(0f, 1f, 0f)
        val z = Vector3.cross(x, y)
        assertEquals(0f, z.x)
        assertEquals(0f, z.y)
        assertEquals(1f, z.z, "X cross Y = Z")
    }

    @Test
    fun vector3Length() {
        val v = Vector3(3f, 4f, 0f)
        assertEquals(5f, v.length(), "3-4-5 triangle hypotenuse")
    }

    @Test
    fun quaternionIdentity() {
        val q = Quaternion()
        assertEquals(0f, q.x)
        assertEquals(0f, q.y)
        assertEquals(0f, q.z)
        assertEquals(1f, q.w)
    }

    @Test
    fun rayConstruction() {
        val origin = Vector3(0f, 0f, 0f)
        val direction = Vector3(0f, 0f, -1f)
        val ray = Ray(origin, direction)
        assertEquals(0f, ray.getOrigin().x)
        assertEquals(-1f, ray.getDirection().z)
    }

    @Test
    fun boxCenter() {
        val box = Box(Vector3(2f, 2f, 2f), Vector3(1f, 2f, 3f))
        assertEquals(1f, box.getCenter().x)
        assertEquals(2f, box.getCenter().y)
        assertEquals(3f, box.getCenter().z)
    }

    @Test
    fun sphereRadius() {
        val sphere = Sphere(1f, Vector3(0f, 0f, 0f))
        assertEquals(1f, sphere.getRadius())
    }
}
