package io.github.sceneview.collision

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaneCollisionTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.001f) {
        assertTrue(abs(expected - actual) < epsilon, "Expected $expected but got $actual")
    }

    // ── Construction and accessors ──────────────────────────────────────────

    @Test
    fun planeStoresCenter() {
        val plane = Plane(Vector3(1f, 2f, 3f), Vector3(0f, 1f, 0f))
        val center = plane.getCenter()
        assertClose(1f, center.x)
        assertClose(2f, center.y)
        assertClose(3f, center.z)
    }

    @Test
    fun planeNormalizesNormal() {
        val plane = Plane(Vector3(0f, 0f, 0f), Vector3(0f, 3f, 0f))
        val normal = plane.getNormal()
        assertClose(0f, normal.x)
        assertClose(1f, normal.y)
        assertClose(0f, normal.z)
    }

    @Test
    fun setNormalNormalizesInput() {
        val plane = Plane(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        plane.setNormal(Vector3(0f, 0f, 5f))
        val normal = plane.getNormal()
        assertClose(0f, normal.x)
        assertClose(0f, normal.y)
        assertClose(1f, normal.z)
    }

    @Test
    fun setCenterUpdatesCenter() {
        val plane = Plane(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        plane.setCenter(Vector3(10f, 20f, 30f))
        val center = plane.getCenter()
        assertClose(10f, center.x)
        assertClose(20f, center.y)
        assertClose(30f, center.z)
    }

    // ── Ray-plane intersection ──────────────────────────────────────────────

    @Test
    fun rayPerpendicularToPlaneHits() {
        val plane = Plane(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        val ray = Ray(Vector3(0f, 10f, 0f), Vector3(0f, -1f, 0f))
        val hit = RayHit()
        assertTrue(plane.rayIntersection(ray, hit))
        assertClose(10f, hit.getDistance())
    }

    @Test
    fun rayFromBelowPlaneHits() {
        val plane = Plane(Vector3(0f, 5f, 0f), Vector3(0f, 1f, 0f))
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        val hit = RayHit()
        assertTrue(plane.rayIntersection(ray, hit))
        assertClose(5f, hit.getDistance())
    }

    @Test
    fun rayHitsXZPlane() {
        // Plane facing along X axis
        val plane = Plane(Vector3(5f, 0f, 0f), Vector3(1f, 0f, 0f))
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val hit = RayHit()
        assertTrue(plane.rayIntersection(ray, hit))
        assertClose(5f, hit.getDistance())
    }

    @Test
    fun rayHitsDiagonalPlane() {
        // Plane with diagonal normal
        val normal = Vector3(1f, 1f, 0f).normalized()
        val plane = Plane(Vector3(0f, 0f, 0f), normal)
        val ray = Ray(Vector3(-5f, -5f, 0f), Vector3(1f, 1f, 0f).normalized())
        val hit = RayHit()
        assertTrue(plane.rayIntersection(ray, hit))
        // Hit point should be at origin
        val hitPoint = hit.getPoint()
        assertClose(0f, hitPoint.x, 0.01f)
        assertClose(0f, hitPoint.y, 0.01f)
    }

    @Test
    fun rayOnPlaneDoesNotIntersect() {
        // Ray lies in the plane (parallel, denominator ~ 0)
        val plane = Plane(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f))
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val hit = RayHit()
        assertFalse(plane.rayIntersection(ray, hit))
    }

    @Test
    fun rayPointingAwayFromOffsetPlaneMisses() {
        val plane = Plane(Vector3(0f, 10f, 0f), Vector3(0f, 1f, 0f))
        // Ray below plane, pointing down
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(0f, -1f, 0f))
        val hit = RayHit()
        assertFalse(plane.rayIntersection(ray, hit))
    }

    @Test
    fun hitPointLiesOnPlane() {
        val plane = Plane(Vector3(0f, 3f, 0f), Vector3(0f, 1f, 0f))
        val ray = Ray(Vector3(5f, 10f, 5f), Vector3(0f, -1f, 0f))
        val hit = RayHit()
        assertTrue(plane.rayIntersection(ray, hit))
        // Hit point y should be 3 (the plane's y)
        assertClose(3f, hit.getPoint().y)
        // X and Z should be preserved from ray
        assertClose(5f, hit.getPoint().x)
        assertClose(5f, hit.getPoint().z)
    }
}
