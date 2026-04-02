package io.github.sceneview.node

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [NodeState] data class.
 *
 * NodeState is a pure data class with no Filament dependencies, so it is fully testable
 * as a JVM unit test.
 *
 * NOTE: [Node.toState] and [Node.applyState] cannot be tested here because [Node] requires
 * a Filament [com.google.android.filament.Engine] instance (native JNI). Those extension
 * functions are trivial pass-through assignments and should be covered by instrumented tests.
 */
class NodeStateTest {

    @Test
    fun `default constructor produces expected defaults`() {
        val state = NodeState()

        assertEquals(Float3(0f, 0f, 0f), state.position)
        assertEquals(Quaternion(), state.quaternion)
        assertEquals(Float3(1f, 1f, 1f), state.scale)
        assertTrue(state.isVisible)
        assertFalse(state.isEditable)
        assertTrue(state.isTouchable)
    }

    @Test
    fun `custom values are preserved`() {
        val pos = Float3(1f, 2f, 3f)
        val quat = Quaternion(0f, 0.707f, 0f, 0.707f)
        val scale = Float3(2f, 2f, 2f)

        val state = NodeState(
            position = pos,
            quaternion = quat,
            scale = scale,
            isVisible = false,
            isEditable = true,
            isTouchable = false
        )

        assertEquals(pos, state.position)
        assertEquals(quat, state.quaternion)
        assertEquals(scale, state.scale)
        assertFalse(state.isVisible)
        assertTrue(state.isEditable)
        assertFalse(state.isTouchable)
    }

    @Test
    fun `data class equals works correctly`() {
        val a = NodeState(position = Float3(1f, 0f, 0f))
        val b = NodeState(position = Float3(1f, 0f, 0f))
        val c = NodeState(position = Float3(2f, 0f, 0f))

        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = NodeState(
            position = Float3(1f, 2f, 3f),
            isVisible = true,
            isEditable = false
        )
        val copied = original.copy(isEditable = true)

        assertEquals(original.position, copied.position)
        assertEquals(original.isVisible, copied.isVisible)
        assertTrue(copied.isEditable)
    }

    @Test
    fun `data class hashCode is consistent with equals`() {
        val a = NodeState(position = Float3(5f, 5f, 5f), scale = Float3(2f, 2f, 2f))
        val b = NodeState(position = Float3(5f, 5f, 5f), scale = Float3(2f, 2f, 2f))

        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `copy with all flags toggled`() {
        val state = NodeState()
        val toggled = state.copy(
            isVisible = false,
            isEditable = true,
            isTouchable = false
        )

        assertFalse(toggled.isVisible)
        assertTrue(toggled.isEditable)
        assertFalse(toggled.isTouchable)
    }

    @Test
    fun `different quaternion values are not equal`() {
        val a = NodeState(quaternion = Quaternion(0f, 0f, 0f, 1f))
        val b = NodeState(quaternion = Quaternion(0f, 1f, 0f, 0f))

        assertNotEquals(a, b)
    }

    @Test
    fun `scale with uniform value matches Float3 with same components`() {
        // Scale(1f) creates Float3(1f, 1f, 1f)
        val state = NodeState(scale = Float3(1f))
        assertEquals(Float3(1f, 1f, 1f), state.scale)
    }
}
