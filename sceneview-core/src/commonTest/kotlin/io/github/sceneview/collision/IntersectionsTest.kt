package io.github.sceneview.collision

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntersectionsTest {

    @Test
    fun rayHitsSphere() {
        val sphere = Sphere(Vector3(0f, 0f, 0f), 1f)
        val ray = Ray(Vector3(0f, 0f, -5f), Vector3(0f, 0f, 1f))
        val hit = RayHit()
        assertTrue(sphere.rayIntersection(ray, hit))
        assertTrue(hit.getDistance() > 0f)
    }

    @Test
    fun rayMissesSphere() {
        val sphere = Sphere(Vector3(0f, 0f, 0f), 1f)
        val ray = Ray(Vector3(5f, 5f, -5f), Vector3(0f, 0f, 1f))
        val hit = RayHit()
        assertFalse(sphere.rayIntersection(ray, hit))
    }

    @Test
    fun rayHitsBox() {
        val box = Box(Vector3(2f, 2f, 2f), Vector3(0f, 0f, 0f))
        val ray = Ray(Vector3(0f, 0f, -5f), Vector3(0f, 0f, 1f))
        val hit = RayHit()
        assertTrue(box.rayIntersection(ray, hit))
    }

    @Test
    fun rayMissesBox() {
        val box = Box(Vector3(2f, 2f, 2f), Vector3(0f, 0f, 0f))
        val ray = Ray(Vector3(5f, 5f, -5f), Vector3(0f, 0f, 1f))
        val hit = RayHit()
        assertFalse(box.rayIntersection(ray, hit))
    }

    @Test
    fun sphereIntersectsSphere() {
        val a = Sphere(Vector3(0f, 0f, 0f), 1f)
        val b = Sphere(Vector3(1.5f, 0f, 0f), 1f)
        assertTrue(a.shapeIntersection(b))
    }

    @Test
    fun sphereDoesNotIntersectDistantSphere() {
        val a = Sphere(Vector3(0f, 0f, 0f), 1f)
        val b = Sphere(Vector3(5f, 0f, 0f), 1f)
        assertFalse(a.shapeIntersection(b))
    }
}
