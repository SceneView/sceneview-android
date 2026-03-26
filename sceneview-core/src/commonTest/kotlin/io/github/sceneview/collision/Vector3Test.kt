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

    // --- Additional tests ---

    @Test
    fun normalizeUnitVector() {
        val v = Vector3(1f, 0f, 0f)
        val n = v.normalized()
        assertEquals(1f, n.x)
        assertEquals(0f, n.y)
        assertEquals(0f, n.z)
    }

    @Test
    fun normalizeArbitraryVector() {
        val v = Vector3(3f, 4f, 0f)
        val n = v.normalized()
        assertTrue(kotlin.math.abs(n.length() - 1f) < 1e-5f, "Normalized length should be 1")
        assertTrue(kotlin.math.abs(n.x - 0.6f) < 1e-5f)
        assertTrue(kotlin.math.abs(n.y - 0.8f) < 1e-5f)
    }

    @Test
    fun normalizeZeroVectorReturnsZero() {
        val v = Vector3(0f, 0f, 0f)
        val n = v.normalized()
        assertEquals(0f, n.x)
        assertEquals(0f, n.y)
        assertEquals(0f, n.z)
    }

    @Test
    fun negatedVector() {
        val v = Vector3(1f, -2f, 3f)
        val n = v.negated()
        assertEquals(-1f, n.x)
        assertEquals(2f, n.y)
        assertEquals(-3f, n.z)
    }

    @Test
    fun scaledVector() {
        val v = Vector3(1f, 2f, 3f)
        val s = v.scaled(3f)
        assertEquals(3f, s.x)
        assertEquals(6f, s.y)
        assertEquals(9f, s.z)
    }

    @Test
    fun lengthSquared() {
        val v = Vector3(1f, 2f, 3f)
        assertEquals(14f, v.lengthSquared())
    }

    @Test
    fun copyConstructor() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(a)
        assertEquals(1f, b.x)
        assertEquals(2f, b.y)
        assertEquals(3f, b.z)
        // Verify it's a copy, not a reference
        a.x = 99f
        assertEquals(1f, b.x)
    }

    @Test
    fun multiplyComponentWise() {
        val a = Vector3(2f, 3f, 4f)
        val b = Vector3(5f, 6f, 7f)
        val result = Vector3.multiply(a, b)
        assertEquals(10f, result.x)
        assertEquals(18f, result.y)
        assertEquals(28f, result.z)
    }

    @Test
    fun minVector() {
        val a = Vector3(1f, 5f, 3f)
        val b = Vector3(4f, 2f, 6f)
        val result = Vector3.min(a, b)
        assertEquals(1f, result.x)
        assertEquals(2f, result.y)
        assertEquals(3f, result.z)
    }

    @Test
    fun maxVector() {
        val a = Vector3(1f, 5f, 3f)
        val b = Vector3(4f, 2f, 6f)
        val result = Vector3.max(a, b)
        assertEquals(4f, result.x)
        assertEquals(5f, result.y)
        assertEquals(6f, result.z)
    }

    @Test
    fun angleBetweenPerpendicularVectors() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(0f, 1f, 0f)
        val angle = Vector3.angleBetweenVectors(a, b)
        assertTrue(kotlin.math.abs(angle - 90f) < 0.1f, "Expected ~90 degrees, got $angle")
    }

    @Test
    fun angleBetweenParallelVectors() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(2f, 0f, 0f)
        val angle = Vector3.angleBetweenVectors(a, b)
        assertTrue(kotlin.math.abs(angle) < 0.1f, "Expected ~0 degrees, got $angle")
    }

    @Test
    fun angleBetweenOppositeVectors() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(-1f, 0f, 0f)
        val angle = Vector3.angleBetweenVectors(a, b)
        assertTrue(kotlin.math.abs(angle - 180f) < 0.1f, "Expected ~180 degrees, got $angle")
    }

    @Test
    fun vectorEquality() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(1f, 2f, 3f)
        assertTrue(a == b)
    }

    @Test
    fun vectorInequality() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(1f, 2f, 4f)
        assertTrue(a != b)
    }

    @Test
    fun lerpAtBoundaries() {
        val a = Vector3(0f, 0f, 0f)
        val b = Vector3(10f, 20f, 30f)
        val atZero = Vector3.lerp(a, b, 0f)
        assertEquals(0f, atZero.x)
        val atOne = Vector3.lerp(a, b, 1f)
        assertEquals(10f, atOne.x)
        assertEquals(20f, atOne.y)
        assertEquals(30f, atOne.z)
    }

    @Test
    fun factoryMethods() {
        val zero = Vector3.zero()
        assertEquals(0f, zero.x)
        assertEquals(0f, zero.y)
        assertEquals(0f, zero.z)

        val one = Vector3.one()
        assertEquals(1f, one.x)
        assertEquals(1f, one.y)
        assertEquals(1f, one.z)

        val up = Vector3.up()
        assertEquals(0f, up.x)
        assertEquals(1f, up.y)
        assertEquals(0f, up.z)

        val forward = Vector3.forward()
        assertEquals(0f, forward.x)
        assertEquals(0f, forward.y)
        assertEquals(-1f, forward.z)
    }

    @Test
    fun crossProductAnticommutative() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(4f, 5f, 6f)
        val ab = Vector3.cross(a, b)
        val ba = Vector3.cross(b, a)
        assertEquals(ab.x, -ba.x)
        assertEquals(ab.y, -ba.y)
        assertEquals(ab.z, -ba.z)
    }

    // --- Zero vector edge cases ---

    @Test
    fun zeroVectorLength() {
        val v = Vector3(0f, 0f, 0f)
        assertEquals(0f, v.length())
    }

    @Test
    fun zeroVectorLengthSquared() {
        val v = Vector3(0f, 0f, 0f)
        assertEquals(0f, v.lengthSquared())
    }

    @Test
    fun zeroVectorDotProduct() {
        val zero = Vector3(0f, 0f, 0f)
        val v = Vector3(1f, 2f, 3f)
        assertEquals(0f, Vector3.dot(zero, v))
    }

    @Test
    fun zeroVectorCrossProduct() {
        val zero = Vector3(0f, 0f, 0f)
        val v = Vector3(1f, 0f, 0f)
        val result = Vector3.cross(zero, v)
        assertEquals(0f, result.x)
        assertEquals(0f, result.y)
        assertEquals(0f, result.z)
    }

    @Test
    fun normalizeZeroVectorStaysZero() {
        val v = Vector3(0f, 0f, 0f)
        val n = v.normalized()
        assertEquals(0f, n.x)
        assertEquals(0f, n.y)
        assertEquals(0f, n.z)
    }

    @Test
    fun angleBetweenZeroVectors() {
        val a = Vector3(0f, 0f, 0f)
        val b = Vector3(1f, 0f, 0f)
        val angle = Vector3.angleBetweenVectors(a, b)
        assertEquals(0f, angle)
    }

    // --- Negative vector operations ---

    @Test
    fun negatedZeroVector() {
        val v = Vector3(0f, 0f, 0f)
        val n = v.negated()
        // -0f == 0f in IEEE 754, but assertEquals uses toBits() which distinguishes them
        assertTrue(kotlin.math.abs(n.x) == 0f)
        assertTrue(kotlin.math.abs(n.y) == 0f)
        assertTrue(kotlin.math.abs(n.z) == 0f)
    }

    @Test
    fun scaledByZero() {
        val v = Vector3(3f, 4f, 5f)
        val s = v.scaled(0f)
        assertEquals(0f, s.x)
        assertEquals(0f, s.y)
        assertEquals(0f, s.z)
    }

    @Test
    fun scaledByNegative() {
        val v = Vector3(1f, 2f, 3f)
        val s = v.scaled(-1f)
        assertEquals(-1f, s.x)
        assertEquals(-2f, s.y)
        assertEquals(-3f, s.z)
    }

    // --- Very large and very small vectors ---

    @Test
    fun normalizeLargeVector() {
        val v = Vector3(1e6f, 0f, 0f)
        val n = v.normalized()
        assertTrue(kotlin.math.abs(n.length() - 1f) < 1e-4f)
        assertTrue(kotlin.math.abs(n.x - 1f) < 1e-4f)
    }

    @Test
    fun normalizeSmallVectorBelowThresholdReturnsZero() {
        // Vector with lengthSquared < MAX_DELTA (1e-10) is treated as zero
        val v = Vector3(1e-6f, 0f, 0f)
        val n = v.normalized()
        // lengthSquared = 1e-12 < 1e-10, so normalized returns zero
        assertTrue(n.length() == 0f, "Very small vector should normalize to zero")
    }

    @Test
    fun normalizeSmallButAboveThreshold() {
        // Vector with lengthSquared > MAX_DELTA normalizes correctly
        val v = Vector3(1e-4f, 0f, 0f)
        val n = v.normalized()
        assertTrue(kotlin.math.abs(n.length() - 1f) < 1e-3f)
    }

    // --- lerp extrapolation ---

    @Test
    fun lerpBeyondOne() {
        val a = Vector3(0f, 0f, 0f)
        val b = Vector3(10f, 0f, 0f)
        val result = Vector3.lerp(a, b, 2f)
        assertEquals(20f, result.x)
    }

    @Test
    fun lerpNegativeT() {
        val a = Vector3(0f, 0f, 0f)
        val b = Vector3(10f, 0f, 0f)
        val result = Vector3.lerp(a, b, -1f)
        assertEquals(-10f, result.x)
    }

    // --- Dot product sign ---

    @Test
    fun dotProductOppositeVectors() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(-1f, 0f, 0f)
        assertEquals(-1f, Vector3.dot(a, b))
    }

    @Test
    fun dotProductSameVector() {
        val v = Vector3(3f, 4f, 0f)
        assertEquals(25f, Vector3.dot(v, v))
    }

    // --- Cross product with self ---

    @Test
    fun crossProductWithSelfIsZero() {
        val v = Vector3(1f, 2f, 3f)
        val result = Vector3.cross(v, v)
        assertEquals(0f, result.x)
        assertEquals(0f, result.y)
        assertEquals(0f, result.z)
    }

    // --- componentMin / componentMax ---

    @Test
    fun componentMaxReturnsLargest() {
        val v = Vector3(-5f, 3f, 10f)
        assertEquals(10f, Vector3.componentMax(v))
    }

    @Test
    fun componentMinReturnsSmallest() {
        val v = Vector3(-5f, 3f, 10f)
        assertEquals(-5f, Vector3.componentMin(v))
    }
}
