package io.github.sceneview.collision

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatrixTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.001f) {
        assertTrue(abs(expected - actual) < epsilon, "Expected $expected but got $actual")
    }

    // --- Identity ---

    @Test
    fun defaultConstructorCreatesIdentity() {
        val m = Matrix()
        for (i in 0..15) {
            val expected = if (i % 5 == 0) 1f else 0f
            assertClose(expected, m.data[i])
        }
    }

    @Test
    fun identityTransformPointPreservesPoint() {
        val m = Matrix()
        val v = Vector3(3f, 7f, -2f)
        val result = m.transformPoint(v)
        assertClose(3f, result.x)
        assertClose(7f, result.y)
        assertClose(-2f, result.z)
    }

    @Test
    fun identityTransformDirectionPreservesDirection() {
        val m = Matrix()
        val v = Vector3(1f, 0f, 0f)
        val result = m.transformDirection(v)
        assertClose(1f, result.x)
        assertClose(0f, result.y)
        assertClose(0f, result.z)
    }

    // --- Translation ---

    @Test
    fun makeTranslationSetsCorrectValues() {
        val m = Matrix()
        m.makeTranslation(Vector3(5f, 10f, 15f))
        assertClose(5f, m.data[12])
        assertClose(10f, m.data[13])
        assertClose(15f, m.data[14])
    }

    @Test
    fun translationTransformsPoint() {
        val m = Matrix()
        m.makeTranslation(Vector3(1f, 2f, 3f))
        val result = m.transformPoint(Vector3(0f, 0f, 0f))
        assertClose(1f, result.x)
        assertClose(2f, result.y)
        assertClose(3f, result.z)
    }

    @Test
    fun translationDoesNotAffectDirection() {
        val m = Matrix()
        m.makeTranslation(Vector3(100f, 200f, 300f))
        val dir = m.transformDirection(Vector3(1f, 0f, 0f))
        assertClose(1f, dir.x)
        assertClose(0f, dir.y)
        assertClose(0f, dir.z)
    }

    // --- Scale ---

    @Test
    fun makeUniformScale() {
        val m = Matrix()
        m.makeScale(3f)
        assertClose(3f, m.data[0])
        assertClose(3f, m.data[5])
        assertClose(3f, m.data[10])
    }

    @Test
    fun makeNonUniformScale() {
        val m = Matrix()
        m.makeScale(Vector3(2f, 3f, 4f))
        assertClose(2f, m.data[0])
        assertClose(3f, m.data[5])
        assertClose(4f, m.data[10])
    }

    @Test
    fun scaleTransformsPoint() {
        val m = Matrix()
        m.makeScale(Vector3(2f, 3f, 4f))
        val result = m.transformPoint(Vector3(1f, 1f, 1f))
        assertClose(2f, result.x)
        assertClose(3f, result.y)
        assertClose(4f, result.z)
    }

    // --- Rotation ---

    @Test
    fun makeRotation90AroundY() {
        val m = Matrix()
        val q = Quaternion(Vector3.up(), 90f)
        m.makeRotation(q)
        // Rotating (1,0,0) by 90 degrees around Y should give approximately (0,0,-1)
        val result = m.transformPoint(Vector3(1f, 0f, 0f))
        assertClose(0f, result.x, 0.01f)
        assertClose(0f, result.y, 0.01f)
        assertClose(-1f, result.z, 0.01f)
    }

    // --- Decomposition ---

    @Test
    fun decomposeTranslation() {
        val m = Matrix()
        m.makeTranslation(Vector3(4f, 5f, 6f))
        val t = Vector3()
        m.decomposeTranslation(t)
        assertClose(4f, t.x)
        assertClose(5f, t.y)
        assertClose(6f, t.z)
    }

    @Test
    fun decomposeScale() {
        val m = Matrix()
        m.makeScale(Vector3(2f, 3f, 4f))
        val s = Vector3()
        m.decomposeScale(s)
        assertClose(2f, s.x)
        assertClose(3f, s.y)
        assertClose(4f, s.z)
    }

    // --- Multiply ---

    @Test
    fun multiplyIdentityByIdentity() {
        val a = Matrix()
        val b = Matrix()
        val result = Matrix()
        Matrix.multiply(a, b, result)
        assertTrue(Matrix.equals(result, Matrix()))
    }

    @Test
    fun multiplyTranslations() {
        val a = Matrix()
        a.makeTranslation(Vector3(1f, 0f, 0f))
        val b = Matrix()
        b.makeTranslation(Vector3(0f, 2f, 0f))
        val result = Matrix()
        Matrix.multiply(a, b, result)
        val point = result.transformPoint(Vector3(0f, 0f, 0f))
        assertClose(1f, point.x)
        assertClose(2f, point.y)
        assertClose(0f, point.z)
    }

    // --- Invert ---

    @Test
    fun invertIdentity() {
        val m = Matrix()
        val inv = Matrix()
        val success = Matrix.invert(m, inv)
        assertTrue(success)
        assertTrue(Matrix.equals(m, inv))
    }

    @Test
    fun invertTranslation() {
        val m = Matrix()
        m.makeTranslation(Vector3(3f, 4f, 5f))
        val inv = Matrix()
        val success = Matrix.invert(m, inv)
        assertTrue(success)
        // M * M^-1 should be identity
        val product = Matrix()
        Matrix.multiply(m, inv, product)
        assertTrue(Matrix.equals(product, Matrix()))
    }

    @Test
    fun invertScale() {
        val m = Matrix()
        m.makeScale(Vector3(2f, 4f, 8f))
        val inv = Matrix()
        val success = Matrix.invert(m, inv)
        assertTrue(success)
        val product = Matrix()
        Matrix.multiply(m, inv, product)
        assertTrue(Matrix.equals(product, Matrix()))
    }

    // --- TRS ---

    @Test
    fun makeTrsIdentity() {
        val m = Matrix()
        m.makeTrs(Vector3(0f, 0f, 0f), Quaternion.identity(), Vector3(1f, 1f, 1f))
        assertTrue(Matrix.equals(m, Matrix()))
    }

    @Test
    fun makeTrsDecomposesCorrectly() {
        val m = Matrix()
        val translation = Vector3(5f, 10f, 15f)
        m.makeTrs(translation, Quaternion.identity(), Vector3(1f, 1f, 1f))
        val t = Vector3()
        m.decomposeTranslation(t)
        assertClose(5f, t.x)
        assertClose(10f, t.y)
        assertClose(15f, t.z)
    }

    // --- Equals ---

    @Test
    fun equalMatricesAreEqual() {
        val a = Matrix()
        val b = Matrix()
        assertTrue(Matrix.equals(a, b))
    }

    @Test
    fun differentMatricesAreNotEqual() {
        val a = Matrix()
        val b = Matrix()
        b.data[0] = 99f
        assertFalse(Matrix.equals(a, b))
    }
}
