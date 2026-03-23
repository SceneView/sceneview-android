package io.github.sceneview.rendering

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.translation
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class NodeTransformTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 0.001f) {
        assertTrue(abs(expected - actual) < epsilon, "Expected $expected but got $actual")
    }

    @Test
    fun localToWorldWithIdentityParent() {
        val parent = Mat4() // Identity
        val localPos = Position(1f, 2f, 3f)
        val worldPos = NodeTransform.getWorldPosition(parent, localPos)
        assertClose(1f, worldPos.x)
        assertClose(2f, worldPos.y)
        assertClose(3f, worldPos.z)
    }

    @Test
    fun localToWorldWithTranslatedParent() {
        val parent = translation(Float3(10f, 0f, 0f))
        val localPos = Position(1f, 0f, 0f)
        val worldPos = NodeTransform.getWorldPosition(parent, localPos)
        assertClose(11f, worldPos.x)
        assertClose(0f, worldPos.y)
        assertClose(0f, worldPos.z)
    }

    @Test
    fun worldToLocalRoundTrip() {
        val parent = translation(Float3(5f, 10f, 15f))
        val localPos = Position(1f, 2f, 3f)

        val worldPos = NodeTransform.getWorldPosition(parent, localPos)
        val backToLocal = NodeTransform.getLocalPosition(parent, worldPos)

        assertClose(localPos.x, backToLocal.x)
        assertClose(localPos.y, backToLocal.y)
        assertClose(localPos.z, backToLocal.z)
    }

    @Test
    fun transformRoundTrip() {
        val parent = translation(Float3(5f, 0f, 0f))
        val localTransform = translation(Float3(1f, 2f, 3f))

        val world = NodeTransform.getWorldTransform(parent, localTransform)
        val backToLocal = NodeTransform.getLocalTransform(parent, world)

        // Check translation column
        assertClose(localTransform.w.x, backToLocal.w.x)
        assertClose(localTransform.w.y, backToLocal.w.y)
        assertClose(localTransform.w.z, backToLocal.w.z)
    }

    @Test
    fun quaternionWorldLocalRoundTrip() {
        val parentQuat = Quaternion(0f, 0.707f, 0f, 0.707f) // ~90° Y
        val localQuat = Quaternion()

        val worldQuat = NodeTransform.getWorldQuaternion(parentQuat, localQuat)
        val backToLocal = NodeTransform.getLocalQuaternion(parentQuat, worldQuat)

        assertClose(localQuat.x, backToLocal.x, 0.01f)
        assertClose(localQuat.y, backToLocal.y, 0.01f)
        assertClose(localQuat.z, backToLocal.z, 0.01f)
        assertClose(localQuat.w, backToLocal.w, 0.01f)
    }
}
