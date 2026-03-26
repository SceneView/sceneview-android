package io.github.sceneview.collision

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class QuaternionTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.001f) {
        assertTrue(abs(expected - actual) < epsilon, "Expected $expected but got $actual")
    }

    @Test
    fun identityQuaternion() {
        val q = Quaternion.identity()
        assertClose(0f, q.x)
        assertClose(0f, q.y)
        assertClose(0f, q.z)
        assertClose(1f, q.w)
    }

    @Test
    fun multiplyByIdentity() {
        val q = Quaternion(0.5f, 0.5f, 0.5f, 0.5f)
        val identity = Quaternion.identity()
        val result = Quaternion.multiply(q, identity)
        assertClose(q.x, result.x)
        assertClose(q.y, result.y)
        assertClose(q.z, result.z)
        assertClose(q.w, result.w)
    }

    @Test
    fun normalizeQuaternion() {
        val q = Quaternion(1f, 1f, 1f, 1f)
        q.normalize()
        val length = kotlin.math.sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w)
        assertClose(1f, length)
    }

    @Test
    fun slerpIdentities() {
        val a = Quaternion.identity()
        val b = Quaternion.identity()
        val result = Quaternion.slerp(a, b, 0.5f)
        assertClose(0f, result.x)
        assertClose(0f, result.y)
        assertClose(0f, result.z)
        assertClose(1f, result.w)
    }

    // --- Additional tests ---

    @Test
    fun axisAngle90DegreesAroundY() {
        val q = Quaternion.axisAngle(Vector3.up(), 90f)
        // Rotating (1,0,0) by 90 degrees around Y should give (0,0,-1)
        val rotated = Quaternion.rotateVector(q, Vector3(1f, 0f, 0f))
        assertClose(0f, rotated.x, 0.01f)
        assertClose(0f, rotated.y, 0.01f)
        assertClose(-1f, rotated.z, 0.01f)
    }

    @Test
    fun axisAngle180DegreesAroundZ() {
        val q = Quaternion.axisAngle(Vector3(0f, 0f, 1f), 180f)
        // Rotating (1,0,0) by 180 degrees around Z should give (-1,0,0)
        val rotated = Quaternion.rotateVector(q, Vector3(1f, 0f, 0f))
        assertClose(-1f, rotated.x, 0.01f)
        assertClose(0f, rotated.y, 0.01f)
        assertClose(0f, rotated.z, 0.01f)
    }

    @Test
    fun invertedQuaternionReversesRotation() {
        val q = Quaternion.axisAngle(Vector3.up(), 45f)
        val inv = q.inverted()
        val product = Quaternion.multiply(q, inv)
        // Should be approximately identity
        assertClose(0f, product.x, 0.01f)
        assertClose(0f, product.y, 0.01f)
        assertClose(0f, product.z, 0.01f)
        assertClose(1f, product.w, 0.01f)
    }

    @Test
    fun eulerAnglesToQuaternionAndBack() {
        val euler = Vector3(30f, 45f, 60f)
        val q = Quaternion.eulerAngles(euler)
        val backEuler = q.getEulerAngles()
        // Quaternion round-trip may produce equivalent but different euler angles
        // Verify by re-composing and checking the quaternions match
        val q2 = Quaternion.eulerAngles(backEuler)
        // Two quaternions represent the same rotation if they're equal or negated
        val dot = q.x * q2.x + q.y * q2.y + q.z * q2.z + q.w * q2.w
        assertTrue(abs(abs(dot) - 1f) < 0.01f,
            "Euler round-trip failed: input=$euler, output=$backEuler, q=$q, q2=$q2, dot=$dot")
    }

    @Test
    fun rotateVectorPreservesLength() {
        val q = Quaternion.axisAngle(Vector3(1f, 1f, 1f).normalized(), 60f)
        val v = Vector3(3f, 4f, 0f)
        val rotated = Quaternion.rotateVector(q, v)
        assertClose(v.length(), rotated.length(), 0.01f)
    }

    @Test
    fun rotationBetweenParallelVectors() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(2f, 0f, 0f)
        val q = Quaternion.rotationBetweenVectors(a, b)
        // Should be near identity
        assertClose(0f, q.x, 0.01f)
        assertClose(0f, q.y, 0.01f)
        assertClose(0f, q.z, 0.01f)
        assertClose(1f, q.w, 0.01f)
    }

    @Test
    fun rotationBetweenPerpendicularVectors() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(0f, 1f, 0f)
        val q = Quaternion.rotationBetweenVectors(a, b)
        val rotated = Quaternion.rotateVector(q, a.normalized())
        assertClose(0f, rotated.x, 0.01f)
        assertClose(1f, rotated.y, 0.01f)
        assertClose(0f, rotated.z, 0.01f)
    }

    @Test
    fun slerpAtBoundaries() {
        val a = Quaternion.identity()
        val b = Quaternion.axisAngle(Vector3.up(), 90f)
        val atZero = Quaternion.slerp(a, b, 0f)
        assertClose(a.x, atZero.x)
        assertClose(a.y, atZero.y)
        assertClose(a.z, atZero.z)
        assertClose(a.w, atZero.w)

        val atOne = Quaternion.slerp(a, b, 1f)
        assertClose(b.x, atOne.x, 0.01f)
        assertClose(b.y, atOne.y, 0.01f)
        assertClose(b.z, atOne.z, 0.01f)
        assertClose(b.w, atOne.w, 0.01f)
    }

    @Test
    fun quaternionEquality() {
        val a = Quaternion.axisAngle(Vector3.up(), 45f)
        val b = Quaternion.axisAngle(Vector3.up(), 45f)
        assertTrue(a == b)
    }

    // --- Euler angles round-trip for various rotations ---

    @Test
    fun eulerRoundTripPureX() {
        val euler = Vector3(45f, 0f, 0f)
        val q = Quaternion.eulerAngles(euler)
        val back = q.getEulerAngles()
        val q2 = Quaternion.eulerAngles(back)
        val dot = abs(Quaternion.dot(q, q2))
        assertTrue(dot > 0.999f, "Pure X euler round-trip failed: dot=$dot")
    }

    @Test
    fun eulerRoundTripPureY() {
        val euler = Vector3(0f, 60f, 0f)
        val q = Quaternion.eulerAngles(euler)
        val back = q.getEulerAngles()
        val q2 = Quaternion.eulerAngles(back)
        val dot = abs(Quaternion.dot(q, q2))
        assertTrue(dot > 0.999f, "Pure Y euler round-trip failed: dot=$dot")
    }

    @Test
    fun eulerRoundTripPureZ() {
        val euler = Vector3(0f, 0f, 75f)
        val q = Quaternion.eulerAngles(euler)
        val back = q.getEulerAngles()
        val q2 = Quaternion.eulerAngles(back)
        val dot = abs(Quaternion.dot(q, q2))
        assertTrue(dot > 0.999f, "Pure Z euler round-trip failed: dot=$dot")
    }

    @Test
    fun eulerRoundTripCombined() {
        val euler = Vector3(15f, 30f, 45f)
        val q = Quaternion.eulerAngles(euler)
        val back = q.getEulerAngles()
        val q2 = Quaternion.eulerAngles(back)
        val dot = abs(Quaternion.dot(q, q2))
        assertTrue(dot > 0.999f, "Combined euler round-trip failed: dot=$dot")
    }

    @Test
    fun eulerRoundTripNegativeAngles() {
        val euler = Vector3(-20f, -45f, -60f)
        val q = Quaternion.eulerAngles(euler)
        val back = q.getEulerAngles()
        val q2 = Quaternion.eulerAngles(back)
        val dot = abs(Quaternion.dot(q, q2))
        assertTrue(dot > 0.999f, "Negative euler round-trip failed: dot=$dot")
    }

    @Test
    fun eulerRoundTrip90Degrees() {
        val euler = Vector3(90f, 0f, 0f)
        val q = Quaternion.eulerAngles(euler)
        val back = q.getEulerAngles()
        val q2 = Quaternion.eulerAngles(back)
        val dot = abs(Quaternion.dot(q, q2))
        assertTrue(dot > 0.999f, "90-degree euler round-trip failed: dot=$dot")
    }

    @Test
    fun eulerRoundTripSmallAngles() {
        val euler = Vector3(1f, 2f, 3f)
        val q = Quaternion.eulerAngles(euler)
        val back = q.getEulerAngles()
        val q2 = Quaternion.eulerAngles(back)
        val dot = abs(Quaternion.dot(q, q2))
        assertTrue(dot > 0.999f, "Small angle euler round-trip failed: dot=$dot")
    }

    @Test
    fun eulerRoundTrip180Y() {
        val euler = Vector3(0f, 179f, 0f)
        val q = Quaternion.eulerAngles(euler)
        val back = q.getEulerAngles()
        val q2 = Quaternion.eulerAngles(back)
        val dot = abs(Quaternion.dot(q, q2))
        assertTrue(dot > 0.99f, "180 Y euler round-trip failed: dot=$dot")
    }

    // --- lookRotation ---

    @Test
    fun lookRotationForward() {
        val q = Quaternion.lookRotation(Vector3.forward(), Vector3.up())
        // Looking forward with up=up should be near identity
        val rotated = Quaternion.rotateVector(q, Vector3.forward())
        assertClose(0f, rotated.x, 0.01f)
        assertClose(0f, rotated.y, 0.01f)
        assertClose(-1f, rotated.z, 0.01f)
    }

    @Test
    fun lookRotationRight() {
        val q = Quaternion.lookRotation(Vector3.right(), Vector3.up())
        val rotated = Quaternion.rotateVector(q, Vector3.forward())
        assertClose(1f, rotated.x, 0.01f)
        assertClose(0f, rotated.y, 0.01f)
        assertClose(0f, rotated.z, 0.01f)
    }

    // --- inverseRotateVector ---

    @Test
    fun inverseRotateVectorUndoesRotation() {
        val q = Quaternion.axisAngle(Vector3.up(), 60f)
        val v = Vector3(1f, 0f, 0f)
        val rotated = Quaternion.rotateVector(q, v)
        val restored = Quaternion.inverseRotateVector(q, rotated)
        assertClose(v.x, restored.x, 0.01f)
        assertClose(v.y, restored.y, 0.01f)
        assertClose(v.z, restored.z, 0.01f)
    }

    // --- Normalize zero quaternion ---

    @Test
    fun normalizeZeroQuaternionBecomesIdentity() {
        val q = Quaternion()
        q.x = 0f; q.y = 0f; q.z = 0f; q.w = 0f
        val success = q.normalize()
        assertTrue(!success)
        assertClose(0f, q.x)
        assertClose(0f, q.y)
        assertClose(0f, q.z)
        assertClose(1f, q.w)
    }

    // --- Negated quaternion represents same rotation ---

    @Test
    fun negatedQuaternionRepresentsSameRotation() {
        val q = Quaternion.axisAngle(Vector3(1f, 1f, 0f).normalized(), 73f)
        val neg = q.negated()
        val v = Vector3(1f, 2f, 3f)
        val r1 = Quaternion.rotateVector(q, v)
        val r2 = Quaternion.rotateVector(neg, v)
        assertClose(r1.x, r2.x, 0.01f)
        assertClose(r1.y, r2.y, 0.01f)
        assertClose(r1.z, r2.z, 0.01f)
    }

    // --- Slerp midpoint ---

    @Test
    fun slerpMidpointRotation() {
        val a = Quaternion.identity()
        val b = Quaternion.axisAngle(Vector3.up(), 90f)
        val mid = Quaternion.slerp(a, b, 0.5f)
        // Should rotate (1,0,0) by ~45 degrees around Y
        val rotated = Quaternion.rotateVector(mid, Vector3(1f, 0f, 0f))
        // cos(45) ~ 0.707, sin(45) ~ 0.707
        assertClose(0.707f, rotated.x, 0.02f)
        assertClose(0f, rotated.y, 0.01f)
        assertClose(-0.707f, rotated.z, 0.02f)
    }

    // --- Rotation between opposite vectors ---

    @Test
    fun rotationBetweenOppositeVectors() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(-1f, 0f, 0f)
        val q = Quaternion.rotationBetweenVectors(a, b)
        val rotated = Quaternion.rotateVector(q, a.normalized())
        assertClose(-1f, rotated.x, 0.01f)
        assertClose(0f, rotated.y, 0.01f)
        assertClose(0f, rotated.z, 0.01f)
    }
}
