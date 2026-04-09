package io.github.sceneview.collision

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SphereTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.001f) {
        assertTrue(abs(expected - actual) < epsilon, "Expected $expected but got $actual")
    }

    // ── Constructors ────────────────────────────────────────────────────────

    @Test
    fun defaultSphereHasRadius1() {
        val sphere = Sphere()
        assertClose(1f, sphere.getRadius())
    }

    @Test
    fun defaultSphereHasZeroCenter() {
        val sphere = Sphere()
        val center = sphere.getCenter()
        assertClose(0f, center.x)
        assertClose(0f, center.y)
        assertClose(0f, center.z)
    }

    @Test
    fun radiusOnlyConstructor() {
        val sphere = Sphere(5f)
        assertClose(5f, sphere.getRadius())
        val center = sphere.getCenter()
        assertClose(0f, center.x)
        assertClose(0f, center.y)
        assertClose(0f, center.z)
    }

    @Test
    fun radiusAndCenterConstructor() {
        val sphere = Sphere(3f, Vector3(1f, 2f, 3f))
        assertClose(3f, sphere.getRadius())
        val center = sphere.getCenter()
        assertClose(1f, center.x)
        assertClose(2f, center.y)
        assertClose(3f, center.z)
    }

    // ── Setters ─────────────────────────────────────────────────────────────

    @Test
    fun setCenterUpdatesCenter() {
        val sphere = Sphere()
        sphere.setCenter(Vector3(10f, 20f, 30f))
        val center = sphere.getCenter()
        assertClose(10f, center.x)
        assertClose(20f, center.y)
        assertClose(30f, center.z)
    }

    @Test
    fun setRadiusUpdatesRadius() {
        val sphere = Sphere()
        sphere.setRadius(42f)
        assertClose(42f, sphere.getRadius())
    }

    // ── makeCopy ────────────────────────────────────────────────────────────

    @Test
    fun makeCopyCreatesEqualSphere() {
        val original = Sphere(5f, Vector3(1f, 2f, 3f))
        val copy = original.makeCopy()
        assertClose(5f, copy.getRadius())
        assertClose(1f, copy.getCenter().x)
        assertClose(2f, copy.getCenter().y)
        assertClose(3f, copy.getCenter().z)
    }

    @Test
    fun makeCopyIsIndependent() {
        val original = Sphere(1f, Vector3(0f, 0f, 0f))
        val copy = original.makeCopy()
        original.setRadius(99f)
        original.setCenter(Vector3(99f, 99f, 99f))
        assertClose(1f, copy.getRadius())
        assertClose(0f, copy.getCenter().x)
    }

    // ── ChangeId tracking ───────────────────────────────────────────────────

    @Test
    fun changingCenterUpdatesChangeId() {
        val sphere = Sphere()
        val idBefore = sphere.getId().get()
        sphere.setCenter(Vector3(1f, 0f, 0f))
        assertTrue(sphere.getId().get() != idBefore)
    }

    @Test
    fun changingRadiusUpdatesChangeId() {
        val sphere = Sphere()
        val idBefore = sphere.getId().get()
        sphere.setRadius(5f)
        assertTrue(sphere.getId().get() != idBefore)
    }

    // ── Transform ───────────────────────────────────────────────────────────

    @Test
    fun transformWithIdentityPreservesSphere() {
        val sphere = Sphere(2f, Vector3(1f, 2f, 3f))
        val identity = TransformProvider { Matrix() }
        val result = sphere.transform(identity) as Sphere
        assertClose(2f, result.getRadius())
        assertClose(1f, result.getCenter().x)
        assertClose(2f, result.getCenter().y)
        assertClose(3f, result.getCenter().z)
    }

    // ── Ray intersection edge cases ─────────────────────────────────────────

    @Test
    fun rayHitsLargeSphere() {
        val sphere = Sphere(100f, Vector3(0f, 0f, 0f))
        val ray = Ray(Vector3(0f, 0f, -200f), Vector3(0f, 0f, 1f))
        val hit = RayHit()
        assertTrue(sphere.rayIntersection(ray, hit))
        assertClose(100f, hit.getDistance(), 0.1f)
    }

    @Test
    fun rayMissesDistantSphere() {
        val sphere = Sphere(1f, Vector3(1000f, 1000f, 1000f))
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val hit = RayHit()
        assertFalse(sphere.rayIntersection(ray, hit))
    }

    @Test
    fun rayBehindSphereMisses() {
        val sphere = Sphere(1f, Vector3(0f, 0f, 5f))
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(0f, 0f, -1f))
        val hit = RayHit()
        assertFalse(sphere.rayIntersection(ray, hit))
    }

    @Test
    fun rayFromSphereEdgeHits() {
        // Ray starting at the surface of the sphere, pointing inward
        val sphere = Sphere(1f, Vector3(0f, 0f, 0f))
        val ray = Ray(Vector3(1f, 0f, 0f), Vector3(-1f, 0f, 0f))
        val hit = RayHit()
        assertTrue(sphere.rayIntersection(ray, hit))
    }

    // ── Symmetry of intersection ────────────────────────────────────────────

    @Test
    fun sphereIntersectionIsSymmetric() {
        val a = Sphere(1f, Vector3(0f, 0f, 0f))
        val b = Sphere(1f, Vector3(1.5f, 0f, 0f))
        // a-vs-b and b-vs-a should agree
        assertTrue(a.shapeIntersection(b) == b.shapeIntersection(a))
    }

    @Test
    fun sphereBoxIntersectionIsSymmetric() {
        val sphere = Sphere(1f, Vector3(0f, 0f, 0f))
        val box = Box(Vector3(2f, 2f, 2f), Vector3(1f, 0f, 0f))
        // sphere.shapeIntersection(box) and box.shapeIntersection(sphere) should agree
        assertTrue(sphere.shapeIntersection(box) == box.shapeIntersection(sphere))
    }
}
