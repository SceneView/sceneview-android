package io.github.sceneview

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.Utils
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.node.Node
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SceneNodeManagerTest {

    private lateinit var engine: com.google.android.filament.Engine
    private lateinit var filamentScene: com.google.android.filament.Scene
    private lateinit var filamentView: com.google.android.filament.View
    private lateinit var collisionSystem: CollisionSystem
    private lateinit var nodeManager: SceneNodeManager

    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Gltfio.init(); Filament.init(); Utils.init()
            val eglContext = createEglContext()
            engine = createEngine(eglContext)
            filamentScene = engine.createScene()
            filamentView = createView(engine)
            collisionSystem = CollisionSystem(filamentView)
            nodeManager = SceneNodeManager(filamentScene, collisionSystem)
        }
    }

    @After
    fun teardown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            engine.destroyScene(filamentScene)
            engine.destroyView(filamentView)
            engine.safeDestroy()
        }
    }

    // ── addNode ──────────────────────────────────────────────────────────────

    @Test
    fun addNode_secondAdd_isIdempotent() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            nodeManager.addNode(node)
            nodeManager.addNode(node) // must not crash or double-register
            nodeManager.removeNode(node)
            node.destroy()
        }
    }

    @Test
    fun addNode_alsoAddsExistingChildNodes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            val child = Node(engine)
            parent.addChildNode(child)

            nodeManager.addNode(parent)

            // Child is now managed — a redundant addNode must be a no-op
            nodeManager.addNode(child)

            nodeManager.removeNode(parent)
            child.destroy()
            parent.destroy()
        }
    }

    @Test
    fun addNode_lateChildAdded_isAutoRegistered() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            nodeManager.addNode(parent)

            // Add child AFTER parent is already managed
            val child = Node(engine)
            parent.addChildNode(child)

            // Child should be auto-managed via onChildAdded listener
            // Removing parent should also clean up child without crashing
            nodeManager.removeNode(parent)
            child.destroy()
            parent.destroy()
        }
    }

    // ── removeNode ───────────────────────────────────────────────────────────

    @Test
    fun removeNode_secondRemove_isIdempotent() {
        // Regression for #646 — double-remove must be a no-op, not crash
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            nodeManager.addNode(node)
            nodeManager.removeNode(node)
            nodeManager.removeNode(node) // must not crash
            node.destroy()
        }
    }

    @Test
    fun removeNode_neverAdded_isIdempotent() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            nodeManager.removeNode(node) // never added — must not crash
            node.destroy()
        }
    }

    @Test
    fun removeNode_alsoRemovesChildren() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            val child = Node(engine)
            parent.addChildNode(child)

            nodeManager.addNode(parent)
            nodeManager.removeNode(parent)

            // Child was removed with parent — removing it again must be a no-op
            nodeManager.removeNode(child)

            child.destroy()
            parent.destroy()
        }
    }

    // ── replaceNode ──────────────────────────────────────────────────────────

    @Test
    fun replaceNode_swapsOldForNew() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val oldNode = Node(engine)
            val newNode = Node(engine)
            nodeManager.addNode(oldNode)

            nodeManager.replaceNode(oldNode, newNode)

            // Old node removed — removing again must be no-op
            nodeManager.removeNode(oldNode)
            // New node present — removing should work cleanly
            nodeManager.removeNode(newNode)

            oldNode.destroy()
            newNode.destroy()
        }
    }

    @Test
    fun replaceNode_nullOld_addsNew() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val newNode = Node(engine)
            nodeManager.replaceNode(null, newNode)
            nodeManager.removeNode(newNode)
            newNode.destroy()
        }
    }

    @Test
    fun replaceNode_nullNew_removesOld() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val oldNode = Node(engine)
            nodeManager.addNode(oldNode)
            nodeManager.replaceNode(oldNode, null)
            nodeManager.removeNode(oldNode) // must be no-op now
            oldNode.destroy()
        }
    }
}
