package io.github.sceneview.collision

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollisionResponseTest {

    @Test
    fun sphereSphereResponseDetectsCollision() {
        val a = Sphere(1f, Vector3.zero())
        val b = Sphere(1f, Vector3(1.5f, 0f, 0f))
        val result = sphereSphereResponse(a, b)
        assertTrue(result.collided)
        assertTrue(result.penetrationDepth > 0f)
    }

    @Test
    fun sphereSphereResponseNoCollision() {
        val a = Sphere(1f, Vector3.zero())
        val b = Sphere(1f, Vector3(5f, 0f, 0f))
        val result = sphereSphereResponse(a, b)
        assertFalse(result.collided)
    }

    @Test
    fun sphereSphereResponseNormalDirection() {
        val a = Sphere(1f, Vector3.zero())
        val b = Sphere(1f, Vector3(1f, 0f, 0f))
        val result = sphereSphereResponse(a, b)
        assertTrue(result.collided)
        // Normal should point from A to B (positive X)
        assertTrue(result.normal.x > 0f)
    }

    @Test
    fun sphereSphereResponsePenetrationDepth() {
        val a = Sphere(1f, Vector3.zero())
        val b = Sphere(1f, Vector3(1f, 0f, 0f))
        val result = sphereSphereResponse(a, b)
        // Combined radius = 2, distance = 1, penetration = 1
        assertTrue(abs(result.penetrationDepth - 1f) < 0.01f)
    }

    @Test
    fun spherePlaneResponseDetectsCollision() {
        val sphere = Sphere(1f, Vector3(0f, 0.5f, 0f))
        val result = spherePlaneResponse(sphere, Vector3(0f, 1f, 0f), 0f)
        assertTrue(result.collided)
        assertTrue(result.penetrationDepth > 0f)
    }

    @Test
    fun spherePlaneResponseNoCollision() {
        val sphere = Sphere(1f, Vector3(0f, 5f, 0f))
        val result = spherePlaneResponse(sphere, Vector3(0f, 1f, 0f), 0f)
        assertFalse(result.collided)
    }

    @Test
    fun reflectVector() {
        // Incoming direction: (1, -1, 0) hitting a floor (normal = 0,1,0)
        val dir = Vector3(1f, -1f, 0f)
        val normal = Vector3(0f, 1f, 0f)
        val reflected = reflect(dir, normal)
        // Should bounce to (1, 1, 0)
        assertTrue(abs(reflected.x - 1f) < 0.01f)
        assertTrue(abs(reflected.y - 1f) < 0.01f)
    }

    @Test
    fun reflectPerpendicular() {
        // Straight down onto a floor
        val dir = Vector3(0f, -1f, 0f)
        val normal = Vector3(0f, 1f, 0f)
        val reflected = reflect(dir, normal)
        assertTrue(abs(reflected.y - 1f) < 0.01f)
        assertTrue(abs(reflected.x) < 0.01f)
    }

    @Test
    fun separationVectorOverlapping() {
        val a = Sphere(1f, Vector3.zero())
        val b = Sphere(1f, Vector3(1f, 0f, 0f))
        val sep = separationVector(a, b)
        // Should push A away from B (negative X direction, since A is at origin)
        assertTrue(sep.x < 0f)
        assertTrue(sep.length() > 0.5f)
    }

    @Test
    fun separationVectorNotOverlapping() {
        val a = Sphere(1f, Vector3.zero())
        val b = Sphere(1f, Vector3(5f, 0f, 0f))
        val sep = separationVector(a, b)
        assertTrue(abs(sep.x) < 0.01f)
    }

    @Test
    fun sphereSphereResponseBounceDirection() {
        val a = Sphere(1f, Vector3.zero())
        val b = Sphere(1f, Vector3(1f, 0f, 0f))
        val velocity = Vector3(1f, 0f, 0f) // Moving toward B
        val result = sphereSphereResponse(a, b, velocity)
        assertTrue(result.collided)
        // Bounce should be in negative X direction
        assertTrue(result.bounceDirection.x < 0f)
    }
}
