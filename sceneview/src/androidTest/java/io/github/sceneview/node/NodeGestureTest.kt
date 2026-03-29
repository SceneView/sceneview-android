package io.github.sceneview.node

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.Utils
import io.github.sceneview.createEglContext
import io.github.sceneview.createEngine
import io.github.sceneview.safeDestroy
import io.github.sceneview.math.Scale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [Node] gesture-handling configuration — editable flags, scale ranges, and
 * gesture sensitivity.
 */
@RunWith(AndroidJUnit4::class)
class NodeGestureTest {

    private lateinit var engine: com.google.android.filament.Engine

    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Gltfio.init(); Filament.init(); Utils.init()
            val eglContext = createEglContext()
            engine = createEngine(eglContext)
        }
    }

    @After
    fun teardown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            engine.safeDestroy()
        }
    }

    // ── Editable flags ───────────────────────────────────────────────────────

    @Test
    fun isEditable_defaultFalse() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            assertFalse("isEditable should default to false", node.isEditable)
            node.destroy()
        }
    }

    @Test
    fun isPositionEditable_requiresIsEditable() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)

            // isPositionEditable defaults to false even when read
            assertFalse("isPositionEditable should be false", node.isPositionEditable)

            node.isEditable = true
            node.isPositionEditable = true
            assertTrue("should be true when both isEditable and isPositionEditable are true",
                node.isPositionEditable)

            node.isEditable = false
            assertFalse("should be false when isEditable is false",
                node.isPositionEditable)

            node.destroy()
        }
    }

    @Test
    fun isRotationEditable_requiresIsEditable() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)

            node.isEditable = true
            assertTrue("isRotationEditable should default to true when isEditable is true",
                node.isRotationEditable)

            node.isEditable = false
            assertFalse("should be false when isEditable is false",
                node.isRotationEditable)

            node.destroy()
        }
    }

    @Test
    fun isScaleEditable_requiresIsEditable() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)

            node.isEditable = true
            assertTrue("isScaleEditable should default to true when isEditable is true",
                node.isScaleEditable)

            node.isEditable = false
            assertFalse("should be false when isEditable is false",
                node.isScaleEditable)

            node.destroy()
        }
    }

    // ── Scale range ──────────────────────────────────────────────────────────

    @Test
    fun editableScaleRange_defaultIsReasonable() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)

            assertEquals("min should be 0.1", 0.1f, node.editableScaleRange.start, 0.001f)
            assertEquals("max should be 10.0", 10.0f, node.editableScaleRange.endInclusive, 0.001f)

            node.destroy()
        }
    }

    @Test
    fun editableScaleRange_canBeCustomized() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)

            node.editableScaleRange = 0.5f..5.0f
            assertEquals(0.5f, node.editableScaleRange.start, 0.001f)
            assertEquals(5.0f, node.editableScaleRange.endInclusive, 0.001f)

            node.destroy()
        }
    }

    // ── Gesture sensitivity ──────────────────────────────────────────────────

    @Test
    fun scaleGestureSensitivity_defaultIs05() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            assertEquals("default sensitivity should be 0.5", 0.5f, node.scaleGestureSensitivity, 0.001f)
            node.destroy()
        }
    }

    @Test
    fun scaleGestureSensitivity_canBeAdjusted() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            node.scaleGestureSensitivity = 0.25f
            assertEquals(0.25f, node.scaleGestureSensitivity, 0.001f)
            node.destroy()
        }
    }

    // ── isTouchable ──────────────────────────────────────────────────────────

    @Test
    fun isTouchable_defaultTrue() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            assertTrue("isTouchable should default to true", node.isTouchable)
            node.destroy()
        }
    }

    @Test
    fun isHittable_defaultTrue() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            assertTrue("isHittable should default to true", node.isHittable)
            node.destroy()
        }
    }
}
