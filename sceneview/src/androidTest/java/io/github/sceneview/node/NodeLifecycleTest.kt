package io.github.sceneview.node

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.Utils
import io.github.sceneview.createEglContext
import io.github.sceneview.createEngine
import io.github.sceneview.safeDestroy
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [Node] lifecycle — creation, parent/child relationships, transform propagation,
 * visibility, and destruction.
 */
@RunWith(AndroidJUnit4::class)
class NodeLifecycleTest {

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

    // ── Creation ──────────────────────────────────────────────────────────────

    @Test
    fun node_defaultValues_areCorrect() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)

            assertNull("name should be null by default", node.name)
            assertTrue("isVisible should be true", node.isVisible)
            assertFalse("isEditable should be false", node.isEditable)
            assertTrue("isTouchable should be true", node.isTouchable)
            assertTrue("childNodes should be empty", node.childNodes.isEmpty())
            assertNull("parent should be null", node.parent)

            node.destroy()
        }
    }

    // ── Parent / child ───────────────────────────────────────────────────────

    @Test
    fun addChildNode_setsParentRelationship() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            val child = Node(engine)

            parent.addChildNode(child)

            assertEquals("child's parent should be parent", parent, child.parent)
            assertTrue("parent should contain child", parent.childNodes.contains(child))

            child.destroy()
            parent.destroy()
        }
    }

    @Test
    fun removeChildNode_clearsParentRelationship() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            val child = Node(engine)

            parent.addChildNode(child)
            parent.removeChildNode(child)

            assertNull("child's parent should be null", child.parent)
            assertFalse("parent should not contain child", parent.childNodes.contains(child))

            child.destroy()
            parent.destroy()
        }
    }

    @Test
    fun clearChildNodes_removesAllChildren() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            val child1 = Node(engine)
            val child2 = Node(engine)

            parent.addChildNode(child1)
            parent.addChildNode(child2)
            assertEquals("should have 2 children", 2, parent.childNodes.size)

            parent.clearChildNodes()
            assertTrue("children should be empty", parent.childNodes.isEmpty())

            child1.destroy()
            child2.destroy()
            parent.destroy()
        }
    }

    @Test
    fun settingParent_removesFromOldParent() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent1 = Node(engine)
            val parent2 = Node(engine)
            val child = Node(engine)

            child.parent = parent1
            assertTrue(parent1.childNodes.contains(child))

            child.parent = parent2
            assertFalse("child removed from old parent", parent1.childNodes.contains(child))
            assertTrue("child added to new parent", parent2.childNodes.contains(child))

            child.destroy()
            parent2.destroy()
            parent1.destroy()
        }
    }

    // ── Transform ────────────────────────────────────────────────────────────

    @Test
    fun position_setter_updatesTransform() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)

            node.position = Position(1f, 2f, 3f)

            assertEquals(1f, node.position.x, 0.01f)
            assertEquals(2f, node.position.y, 0.01f)
            assertEquals(3f, node.position.z, 0.01f)

            node.destroy()
        }
    }

    @Test
    fun scale_setter_updatesTransform() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)

            node.scale = Scale(2f)

            assertEquals(2f, node.scale.x, 0.01f)
            assertEquals(2f, node.scale.y, 0.01f)
            assertEquals(2f, node.scale.z, 0.01f)

            node.destroy()
        }
    }

    @Test
    fun setScale_uniformHelper_works() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)

            node.setScale(0.5f)

            assertEquals(0.5f, node.scale.x, 0.01f)
            assertEquals(0.5f, node.scale.y, 0.01f)
            assertEquals(0.5f, node.scale.z, 0.01f)

            node.destroy()
        }
    }

    // ── Visibility ───────────────────────────────────────────────────────────

    @Test
    fun visibility_parentHiddenHidesChild() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            val child = Node(engine)
            parent.addChildNode(child)

            parent.isVisible = false
            assertFalse("child should be invisible when parent is hidden", child.isVisible)

            parent.isVisible = true
            assertTrue("child should be visible when parent is shown", child.isVisible)

            child.destroy()
            parent.destroy()
        }
    }

    // ── onChildAdded / onChildRemoved callbacks ──────────────────────────────

    @Test
    fun onChildAdded_firesWhenChildAdded() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            val child = Node(engine)
            var callbackFired = false

            parent.onChildAdded += { callbackFired = true }
            parent.addChildNode(child)

            assertTrue("onChildAdded callback should have fired", callbackFired)

            child.destroy()
            parent.destroy()
        }
    }

    @Test
    fun onChildRemoved_firesWhenChildRemoved() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            val child = Node(engine)
            var callbackFired = false

            parent.addChildNode(child)
            parent.onChildRemoved += { callbackFired = true }
            parent.removeChildNode(child)

            assertTrue("onChildRemoved callback should have fired", callbackFired)

            child.destroy()
            parent.destroy()
        }
    }

    // ── Destroy ──────────────────────────────────────────────────────────────

    @Test
    fun destroy_detachesFromParent() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            val child = Node(engine)
            parent.addChildNode(child)

            child.destroy()
            assertFalse("parent should no longer contain destroyed child",
                parent.childNodes.contains(child))

            parent.destroy()
        }
    }

    // ── Name ─────────────────────────────────────────────────────────────────

    @Test
    fun name_canBeSetAndRetrieved() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)

            node.name = "TestNode"
            assertEquals("TestNode", node.name)

            node.destroy()
        }
    }
}
