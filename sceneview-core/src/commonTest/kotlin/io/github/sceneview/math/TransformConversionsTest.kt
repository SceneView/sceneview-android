package io.github.sceneview.math

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.inverse
import dev.romainguy.kotlin.math.translation
import dev.romainguy.kotlin.math.scale
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class TransformConversionsTest {

    private val epsilon = 0.01f

    private fun assertFloat3Near(expected: Float3, actual: Float3, msg: String = "") {
        assertTrue(abs(expected.x - actual.x) < epsilon, "$msg x: ${expected.x} vs ${actual.x}")
        assertTrue(abs(expected.y - actual.y) < epsilon, "$msg y: ${expected.y} vs ${actual.y}")
        assertTrue(abs(expected.z - actual.z) < epsilon, "$msg z: ${expected.z} vs ${actual.z}")
    }

    @Test
    fun identityTransformNoChange() {
        val worldTransform = Mat4.identity()
        val worldToLocal = inverse(worldTransform)
        val pos = Position(3f, 4f, 5f)

        val localPos = worldToLocalPosition(pos, worldToLocal)
        assertFloat3Near(pos, localPos, "Identity should not change position")
    }

    @Test
    fun translationConvertsCorrectly() {
        // Node is at (10, 0, 0) in world space
        val worldTransform = translation(Float3(10f, 0f, 0f))
        val worldToLocal = inverse(worldTransform)

        // World point (15, 0, 0) should be (5, 0, 0) in local space
        val localPos = worldToLocalPosition(Position(15f, 0f, 0f), worldToLocal)
        assertFloat3Near(Position(5f, 0f, 0f), localPos)
    }

    @Test
    fun roundTripPositionPreserved() {
        val worldTransform = translation(Float3(5f, -3f, 7f))
        val worldToLocal = inverse(worldTransform)
        val originalPos = Position(1f, 2f, 3f)

        val localPos = worldToLocalPosition(originalPos, worldToLocal)
        val roundTrip = localToWorldPosition(localPos, worldTransform)
        assertFloat3Near(originalPos, roundTrip, "Round-trip should preserve position")
    }

    @Test
    fun scaleConvertsCorrectly() {
        // Node scaled 2x
        val worldTransform = scale(Float3(2f, 2f, 2f))
        val worldToLocal = inverse(worldTransform)

        // World (4, 6, 8) → local (2, 3, 4)
        val localPos = worldToLocalPosition(Position(4f, 6f, 8f), worldToLocal)
        assertFloat3Near(Position(2f, 3f, 4f), localPos)
    }

    @Test
    fun localToWorldPositionWithTranslation() {
        val worldTransform = translation(Float3(10f, 20f, 30f))
        val localPos = Position(1f, 2f, 3f)

        val worldPos = localToWorldPosition(localPos, worldTransform)
        assertFloat3Near(Position(11f, 22f, 33f), worldPos)
    }

    @Test
    fun transformRoundTripPreserved() {
        val worldTransform = translation(Float3(5f, 0f, 0f))
        val worldToLocal = inverse(worldTransform)
        val childTransform = translation(Float3(1f, 1f, 1f))

        val localT = worldToLocalTransform(childTransform, worldToLocal)
        val roundTrip = localToWorldTransform(localT, worldTransform)

        // Check the translation component (column 3)
        assertFloat3Near(
            Float3(childTransform[3].x, childTransform[3].y, childTransform[3].z),
            Float3(roundTrip[3].x, roundTrip[3].y, roundTrip[3].z),
            "Transform round-trip"
        )
    }

    @Test
    fun computeWorldToLocalMatchesInverse() {
        val worldTransform = translation(Float3(7f, -2f, 4f))
        val computed = computeWorldToLocal(worldTransform)
        val expected = inverse(worldTransform)

        // Check translation columns match
        assertFloat3Near(
            Float3(expected[3].x, expected[3].y, expected[3].z),
            Float3(computed[3].x, computed[3].y, computed[3].z)
        )
    }
}
