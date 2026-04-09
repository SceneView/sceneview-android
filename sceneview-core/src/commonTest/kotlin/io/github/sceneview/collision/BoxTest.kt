package io.github.sceneview.collision

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoxTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.001f) {
        assertTrue(abs(expected - actual) < epsilon, "Expected $expected but got $actual")
    }

    // ── Constructors ────────────────────────────────────────────────────────

    @Test
    fun defaultBoxHasUnitSize() {
        val box = Box()
        val size = box.getSize()
        assertClose(1f, size.x)
        assertClose(1f, size.y)
        assertClose(1f, size.z)
    }

    @Test
    fun defaultBoxHasZeroCenter() {
        val box = Box()
        val center = box.getCenter()
        assertClose(0f, center.x)
        assertClose(0f, center.y)
        assertClose(0f, center.z)
    }

    @Test
    fun sizeOnlyConstructor() {
        val box = Box(Vector3(2f, 4f, 6f))
        val size = box.getSize()
        assertClose(2f, size.x)
        assertClose(4f, size.y)
        assertClose(6f, size.z)
        val center = box.getCenter()
        assertClose(0f, center.x)
        assertClose(0f, center.y)
        assertClose(0f, center.z)
    }

    @Test
    fun sizeAndCenterConstructor() {
        val box = Box(Vector3(2f, 4f, 6f), Vector3(1f, 2f, 3f))
        val size = box.getSize()
        assertClose(2f, size.x)
        assertClose(4f, size.y)
        assertClose(6f, size.z)
        val center = box.getCenter()
        assertClose(1f, center.x)
        assertClose(2f, center.y)
        assertClose(3f, center.z)
    }

    // ── getExtents ──────────────────────────────────────────────────────────

    @Test
    fun getExtentsIsHalfSize() {
        val box = Box(Vector3(4f, 6f, 8f))
        val extents = box.getExtents()
        assertClose(2f, extents.x)
        assertClose(3f, extents.y)
        assertClose(4f, extents.z)
    }

    // ── setCenter / setSize ─────────────────────────────────────────────────

    @Test
    fun setCenterUpdatesCenter() {
        val box = Box()
        box.setCenter(Vector3(5f, 10f, 15f))
        val center = box.getCenter()
        assertClose(5f, center.x)
        assertClose(10f, center.y)
        assertClose(15f, center.z)
    }

    @Test
    fun setSizeUpdatesSize() {
        val box = Box()
        box.setSize(Vector3(3f, 5f, 7f))
        val size = box.getSize()
        assertClose(3f, size.x)
        assertClose(5f, size.y)
        assertClose(7f, size.z)
    }

    // ── Rotation ────────────────────────────────────────────────────────────

    @Test
    fun setAndGetRotation() {
        val box = Box()
        val rotation = Quaternion.axisAngle(Vector3.up(), 90f)
        box.setRotation(rotation)
        val result = box.getRotation()
        // Quaternion components should be close to the original
        assertClose(rotation.x, result.x, 0.01f)
        assertClose(rotation.y, result.y, 0.01f)
        assertClose(rotation.z, result.z, 0.01f)
        assertClose(rotation.w, result.w, 0.01f)
    }

    // ── makeCopy ────────────────────────────────────────────────────────────

    @Test
    fun makeCopyCreatesEqualBox() {
        val original = Box(Vector3(3f, 5f, 7f), Vector3(1f, 2f, 3f))
        val copy = original.makeCopy()
        val copySize = copy.getSize()
        val copyCenter = copy.getCenter()
        assertClose(3f, copySize.x)
        assertClose(5f, copySize.y)
        assertClose(7f, copySize.z)
        assertClose(1f, copyCenter.x)
        assertClose(2f, copyCenter.y)
        assertClose(3f, copyCenter.z)
    }

    @Test
    fun makeCopyIsIndependent() {
        val original = Box(Vector3(2f, 2f, 2f), Vector3(0f, 0f, 0f))
        val copy = original.makeCopy()
        original.setCenter(Vector3(99f, 99f, 99f))
        val copyCenter = copy.getCenter()
        assertClose(0f, copyCenter.x)
        assertClose(0f, copyCenter.y)
        assertClose(0f, copyCenter.z)
    }

    // ── ChangeId tracking ───────────────────────────────────────────────────

    @Test
    fun changingCenterUpdatesChangeId() {
        val box = Box()
        val idBefore = box.getId().get()
        box.setCenter(Vector3(1f, 0f, 0f))
        assertTrue(box.getId().get() != idBefore, "ChangeId should update after setCenter")
    }

    @Test
    fun changingSizeUpdatesChangeId() {
        val box = Box()
        val idBefore = box.getId().get()
        box.setSize(Vector3(5f, 5f, 5f))
        assertTrue(box.getId().get() != idBefore, "ChangeId should update after setSize")
    }

    @Test
    fun changingRotationUpdatesChangeId() {
        val box = Box()
        val idBefore = box.getId().get()
        box.setRotation(Quaternion.axisAngle(Vector3.up(), 45f))
        assertTrue(box.getId().get() != idBefore, "ChangeId should update after setRotation")
    }

    // ── Transform ───────────────────────────────────────────────────────────

    @Test
    fun transformWithIdentityPreservesBox() {
        val box = Box(Vector3(2f, 4f, 6f), Vector3(1f, 2f, 3f))
        val identity = TransformProvider { Matrix() }
        val result = box.transform(identity) as Box
        assertClose(1f, result.getCenter().x)
        assertClose(2f, result.getCenter().y)
        assertClose(3f, result.getCenter().z)
        assertClose(2f, result.getSize().x)
        assertClose(4f, result.getSize().y)
        assertClose(6f, result.getSize().z)
    }

    @Test
    fun transformCannotTransformSelf() {
        val box = Box()
        val identity = TransformProvider { Matrix() }
        assertFailsWith<IllegalArgumentException> {
            box.transform(identity, box)
        }
    }

    // ── Ray intersection with offset center ─────────────────────────────────

    @Test
    fun rayHitsOffsetBoxAtCorrectDistance() {
        val box = Box(Vector3(2f, 2f, 2f), Vector3(10f, 0f, 0f))
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val hit = RayHit()
        assertTrue(box.rayIntersection(ray, hit))
        // Box from 9 to 11 on X axis, so distance should be ~9
        assertClose(9f, hit.getDistance(), 0.01f)
    }

    @Test
    fun rayMissesSmallBox() {
        val box = Box(Vector3(0.1f, 0.1f, 0.1f), Vector3(100f, 100f, 100f))
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val hit = RayHit()
        assertFalse(box.rayIntersection(ray, hit))
    }
}
