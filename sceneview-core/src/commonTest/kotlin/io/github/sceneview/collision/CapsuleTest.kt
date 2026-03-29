package io.github.sceneview.collision

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.math.abs

class CapsuleTest {

    @Test
    fun capsuleDefaults() {
        val c = Capsule()
        assertTrue(abs(c.getRadius() - 0.5f) < 1e-5f)
        assertTrue(abs(c.getHeight() - 2f) < 1e-5f)
    }

    @Test
    fun capsuleSettersWork() {
        val c = Capsule()
        c.setRadius(1f)
        c.setHeight(4f)
        c.setCenter(Vector3(1f, 2f, 3f))
        assertTrue(abs(c.getRadius() - 1f) < 1e-5f)
        assertTrue(abs(c.getHeight() - 4f) < 1e-5f)
        assertTrue(abs(c.getCenter().x - 1f) < 1e-5f)
    }

    @Test
    fun capsuleSegmentEndpoints() {
        val c = Capsule(0.5f, 4f, Vector3.zero())
        val (bottom, top) = c.getSegmentEndpoints()
        // Half segment = 4/2 - 0.5 = 1.5
        assertTrue(abs(bottom.y - (-1.5f)) < 1e-4f)
        assertTrue(abs(top.y - 1.5f) < 1e-4f)
    }

    @Test
    fun capsuleRayIntersectionHit() {
        val c = Capsule(1f, 4f, Vector3.zero())
        val ray = Ray(Vector3(5f, 0f, 0f), Vector3(-1f, 0f, 0f))
        val hit = RayHit()
        assertTrue(c.rayIntersection(ray, hit))
        assertTrue(hit.getDistance() > 0f)
    }

    @Test
    fun capsuleRayIntersectionMiss() {
        val c = Capsule(1f, 4f, Vector3.zero())
        val ray = Ray(Vector3(5f, 0f, 0f), Vector3(0f, 1f, 0f))
        val hit = RayHit()
        assertFalse(c.rayIntersection(ray, hit))
    }

    @Test
    fun capsuleSphereIntersection() {
        val c = Capsule(0.5f, 2f, Vector3.zero())
        val s = Sphere(0.5f, Vector3(0.8f, 0f, 0f))
        assertTrue(c.sphereIntersection(s))
    }

    @Test
    fun capsuleSphereNoIntersection() {
        val c = Capsule(0.5f, 2f, Vector3.zero())
        val s = Sphere(0.1f, Vector3(5f, 0f, 0f))
        assertFalse(c.sphereIntersection(s))
    }

    @Test
    fun capsuleCapsuleIntersection() {
        val c1 = Capsule(0.5f, 2f, Vector3.zero())
        val c2 = Capsule(0.5f, 2f, Vector3(0.8f, 0f, 0f))
        assertTrue(c1.capsuleIntersection(c2))
    }

    @Test
    fun capsuleCapsuleNoIntersection() {
        val c1 = Capsule(0.5f, 2f, Vector3.zero())
        val c2 = Capsule(0.5f, 2f, Vector3(5f, 0f, 0f))
        assertFalse(c1.capsuleIntersection(c2))
    }

    @Test
    fun capsuleMakeCopy() {
        val original = Capsule(1f, 3f, Vector3(1f, 2f, 3f))
        val copy = original.makeCopy()
        assertTrue(abs(copy.getRadius() - 1f) < 1e-5f)
        assertTrue(abs(copy.getHeight() - 3f) < 1e-5f)
        assertTrue(abs(copy.getCenter().x - 1f) < 1e-5f)
    }

    @Test
    fun closestPointOnSegmentAtStart() {
        val a = Vector3(0f, 0f, 0f)
        val b = Vector3(0f, 2f, 0f)
        val p = Vector3(0f, -1f, 0f)
        val (t, closest) = closestPointOnSegment(a, b, p)
        assertTrue(abs(t) < 1e-5f)
        assertTrue(abs(closest.y) < 1e-5f)
    }

    @Test
    fun closestPointOnSegmentAtEnd() {
        val a = Vector3(0f, 0f, 0f)
        val b = Vector3(0f, 2f, 0f)
        val p = Vector3(0f, 5f, 0f)
        val (t, closest) = closestPointOnSegment(a, b, p)
        assertTrue(abs(t - 1f) < 1e-5f)
        assertTrue(abs(closest.y - 2f) < 1e-5f)
    }
}
