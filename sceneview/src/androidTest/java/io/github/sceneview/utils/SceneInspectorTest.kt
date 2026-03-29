package io.github.sceneview.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.Utils
import io.github.sceneview.ExperimentalSceneViewApi
import io.github.sceneview.createEglContext
import io.github.sceneview.createEngine
import io.github.sceneview.math.Position
import io.github.sceneview.node.Node
import io.github.sceneview.safeDestroy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [inspectScene] and [snapshotNode].
 */
@OptIn(ExperimentalSceneViewApi::class)
@RunWith(AndroidJUnit4::class)
class SceneInspectorTest {

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

    @Test
    fun inspectScene_emptyList_returnsEmpty() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val snapshots = inspectScene(emptyList())
            assertTrue("should return empty list", snapshots.isEmpty())
        }
    }

    @Test
    fun snapshotNode_capturesProperties() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine).apply {
                name = "TestNode"
                position = Position(1f, 2f, 3f)
                isVisible = true
            }

            val snapshot = snapshotNode(node)

            assertEquals("TestNode", snapshot.name)
            assertEquals("Node", snapshot.type)
            assertEquals(1f, snapshot.position.x, 0.01f)
            assertEquals(2f, snapshot.position.y, 0.01f)
            assertEquals(3f, snapshot.position.z, 0.01f)
            assertTrue(snapshot.isVisible)
            assertEquals(0, snapshot.childCount)
            assertTrue(snapshot.children.isEmpty())

            node.destroy()
        }
    }

    @Test
    fun snapshotNode_capturesChildren() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine).apply { name = "Parent" }
            val child1 = Node(engine).apply { name = "Child1" }
            val child2 = Node(engine).apply { name = "Child2" }
            parent.addChildNode(child1)
            parent.addChildNode(child2)

            val snapshot = snapshotNode(parent)

            assertEquals(2, snapshot.childCount)
            assertEquals(2, snapshot.children.size)
            assertTrue(
                snapshot.children.any { it.name == "Child1" }
            )
            assertTrue(
                snapshot.children.any { it.name == "Child2" }
            )

            child1.destroy()
            child2.destroy()
            parent.destroy()
        }
    }

    @Test
    fun flatten_worksRecursively() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val root = Node(engine).apply { name = "Root" }
            val child = Node(engine).apply { name = "Child" }
            val grandchild = Node(engine).apply { name = "Grandchild" }
            root.addChildNode(child)
            child.addChildNode(grandchild)

            val snapshots = inspectScene(listOf(root))
            val flat = snapshots.flatten()

            assertEquals("should have 3 nodes", 3, flat.size)
            assertEquals("Root", flat[0].name)
            assertEquals("Child", flat[1].name)
            assertEquals("Grandchild", flat[2].name)

            grandchild.destroy()
            child.destroy()
            root.destroy()
        }
    }

    @Test
    fun snapshotNode_unnamedNode_getsGeneratedLabel() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine) // no name set

            val snapshot = snapshotNode(node)

            assertTrue("generated name should start with 'Node@'",
                snapshot.name.startsWith("Node@"))

            node.destroy()
        }
    }
}
