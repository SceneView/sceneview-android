package io.github.sceneview.collision

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntersectionsTest {

    @Test
    fun rayHitsSphere() {
        val sphere = Sphere(1f, Vector3(0f, 0f, 0f))
        val ray = Ray(Vector3(0f, 0f, -5f), Vector3(0f, 0f, 1f))
        val hit = RayHit()
        assertTrue(sphere.rayIntersection(ray, hit))
        assertTrue(hit.getDistance() > 0f)
    }

    @Test
    fun rayMissesSphere() {
        val sphere = Sphere(1f, Vector3(0f, 0f, 0f))
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
        val a = Sphere(1f, Vector3(0f, 0f, 0f))
        val b = Sphere(1f, Vector3(1.5f, 0f, 0f))
        assertTrue(a.shapeIntersection(b))
    }

    @Test
    fun sphereDoesNotIntersectDistantSphere() {
        val a = Sphere(1f, Vector3(0f, 0f, 0f))
        val b = Sphere(1f, Vector3(5f, 0f, 0f))
        assertFalse(a.shapeIntersection(b))
    }

    // --- Additional ray-sphere tests ---

    @Test
    fun rayHitsSphereReportsCorrectDistance() {
        val sphere = Sphere(1f, Vector3(0f, 0f, 0f))
        val ray = Ray(Vector3(0f, 0f, -5f), Vector3(0f, 0f, 1f))
        val hit = RayHit()
        assertTrue(sphere.rayIntersection(ray, hit))
        // Distance should be 4 (origin at -5, sphere surface at -1)
        assertTrue(kotlin.math.abs(hit.getDistance() - 4f) < 0.01f,
            "Expected distance ~4, got ${hit.getDistance()}")
    }

    @Test
    fun rayFromInsideSphereHits() {
        val sphere = Sphere(5f, Vector3(0f, 0f, 0f))
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val hit = RayHit()
        assertTrue(sphere.rayIntersection(ray, hit))
    }

    @Test
    fun rayTangentToSphereMisses() {
        val sphere = Sphere(1f, Vector3(0f, 0f, 0f))
        // Ray just barely missing the sphere
        val ray = Ray(Vector3(1.01f, 0f, -5f), Vector3(0f, 0f, 1f))
        val hit = RayHit()
        assertFalse(sphere.rayIntersection(ray, hit))
    }

    // --- Ray-box additional tests ---

    @Test
    fun rayHitsBoxFromDifferentAxis() {
        val box = Box(Vector3(2f, 2f, 2f), Vector3(0f, 0f, 0f))
        val ray = Ray(Vector3(-5f, 0f, 0f), Vector3(1f, 0f, 0f))
        val hit = RayHit()
        assertTrue(box.rayIntersection(ray, hit))
        assertTrue(hit.getDistance() > 0f)
    }

    @Test
    fun rayHitsOffCenterBox() {
        val box = Box(Vector3(2f, 2f, 2f), Vector3(5f, 0f, 0f))
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val hit = RayHit()
        assertTrue(box.rayIntersection(ray, hit))
    }

    @Test
    fun rayBehindBoxMisses() {
        val box = Box(Vector3(2f, 2f, 2f), Vector3(0f, 0f, 5f))
        // Ray pointing away from box
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(0f, 0f, -1f))
        val hit = RayHit()
        assertFalse(box.rayIntersection(ray, hit))
    }

    // --- Ray-plane tests ---

    @Test
    fun rayHitsPlane() {
        val plane = Plane(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        val ray = Ray(Vector3(0f, 5f, 0f), Vector3(0f, -1f, 0f))
        val hit = RayHit()
        assertTrue(plane.rayIntersection(ray, hit))
        assertTrue(kotlin.math.abs(hit.getDistance() - 5f) < 0.01f)
    }

    @Test
    fun rayParallelToPlaneMisses() {
        val plane = Plane(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        val ray = Ray(Vector3(0f, 5f, 0f), Vector3(1f, 0f, 0f))
        val hit = RayHit()
        assertFalse(plane.rayIntersection(ray, hit))
    }

    @Test
    fun rayAwayFromPlaneMisses() {
        val plane = Plane(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        // Ray pointing away from plane
        val ray = Ray(Vector3(0f, 5f, 0f), Vector3(0f, 1f, 0f))
        val hit = RayHit()
        assertFalse(plane.rayIntersection(ray, hit))
    }

    @Test
    fun rayHitsPlaneAtAngle() {
        val plane = Plane(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        val ray = Ray(Vector3(-5f, 5f, 0f), Vector3(1f, -1f, 0f).normalized())
        val hit = RayHit()
        assertTrue(plane.rayIntersection(ray, hit))
        val hitPoint = hit.getPoint()
        assertTrue(kotlin.math.abs(hitPoint.y) < 0.01f, "Hit point should be on the plane (y~0)")
    }

    // --- Box-box intersection ---

    @Test
    fun overlappingBoxesIntersect() {
        val a = Box(Vector3(2f, 2f, 2f), Vector3(0f, 0f, 0f))
        val b = Box(Vector3(2f, 2f, 2f), Vector3(1f, 0f, 0f))
        assertTrue(a.shapeIntersection(b))
    }

    @Test
    fun separatedBoxesDoNotIntersect() {
        val a = Box(Vector3(2f, 2f, 2f), Vector3(0f, 0f, 0f))
        val b = Box(Vector3(2f, 2f, 2f), Vector3(5f, 0f, 0f))
        assertFalse(a.shapeIntersection(b))
    }

    // --- Sphere-box intersection ---

    @Test
    fun sphereInsideBoxIntersects() {
        val sphere = Sphere(0.5f, Vector3(0f, 0f, 0f))
        val box = Box(Vector3(4f, 4f, 4f), Vector3(0f, 0f, 0f))
        assertTrue(sphere.shapeIntersection(box))
    }

    @Test
    fun sphereFarFromBoxDoesNotIntersect() {
        val sphere = Sphere(1f, Vector3(10f, 10f, 10f))
        val box = Box(Vector3(2f, 2f, 2f), Vector3(0f, 0f, 0f))
        assertFalse(sphere.shapeIntersection(box))
    }

    // --- RayHit ---

    @Test
    fun rayHitGetPoint() {
        val sphere = Sphere(1f, Vector3(0f, 0f, 0f))
        val ray = Ray(Vector3(0f, 0f, -5f), Vector3(0f, 0f, 1f))
        val hit = RayHit()
        assertTrue(sphere.rayIntersection(ray, hit))
        val point = hit.getPoint()
        // Point should be on the sphere surface, z ~ -1
        assertTrue(kotlin.math.abs(point.z - (-1f)) < 0.01f,
            "Hit point z should be ~-1, got ${point.z}")
    }

    @Test
    fun rayHitReset() {
        val hit = RayHit()
        hit.setDistance(5f)
        hit.reset()
        assertTrue(hit.getDistance() == Float.MAX_VALUE)
    }
}
