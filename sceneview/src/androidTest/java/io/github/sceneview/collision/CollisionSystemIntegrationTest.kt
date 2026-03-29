package io.github.sceneview.collision

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.Utils
import io.github.sceneview.SceneNodeManager
import io.github.sceneview.createEglContext
import io.github.sceneview.createEngine
import io.github.sceneview.createView
import io.github.sceneview.math.Position
import io.github.sceneview.node.Node
import io.github.sceneview.safeDestroy
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the [CollisionSystem] — verifying that colliders are properly
 * registered and unregistered as nodes enter and leave the scene, and that overlap tests work.
 */
@RunWith(AndroidJUnit4::class)
class CollisionSystemIntegrationTest {

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

    // ── Collider registration ────────────────────────────────────────────────

    @Test
    fun addNode_setsCollisionSystemOnNode() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            assertNull("collisionSystem should be null before addNode", node.collisionSystem)

            nodeManager.addNode(node)
            assertNotNull("collisionSystem should be set after addNode", node.collisionSystem)

            nodeManager.removeNode(node)
            node.destroy()
        }
    }

    @Test
    fun removeNode_clearsCollisionSystem() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            nodeManager.addNode(node)
            nodeManager.removeNode(node)

            assertNull("collisionSystem should be null after removeNode", node.collisionSystem)
            node.destroy()
        }
    }

    @Test
    fun addNode_setsCollisionSystemOnChildren() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val parent = Node(engine)
            val child = Node(engine)
            parent.addChildNode(child)

            nodeManager.addNode(parent)
            assertNotNull("child should have collisionSystem", child.collisionSystem)

            nodeManager.removeNode(parent)
            child.destroy()
            parent.destroy()
        }
    }

    // ── Collision shape assignment ───────────────────────────────────────────

    @Test
    fun collisionShape_setCreatesCollider() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            assertNull("collider should be null initially", node.collider)

            node.collisionShape = Box(
                io.github.sceneview.collision.Vector3(0f, 0f, 0f),
                io.github.sceneview.collision.Vector3(1f, 1f, 1f)
            )
            assertNotNull("collider should be created", node.collider)

            node.collisionShape = null
            assertNull("collider should be null after clearing shape", node.collider)

            node.destroy()
        }
    }

    // ── Overlap test with no collision system ────────────────────────────────

    @Test
    fun overlapTest_withoutCollisionSystem_returnsNull() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            // Not added to scene, so no collision system
            val result = node.overlapTest()
            assertNull("overlapTest without collision system should return null", result)
            node.destroy()
        }
    }

    @Test
    fun overlapTestAll_withoutCollisionSystem_returnsEmptyList() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val node = Node(engine)
            val results = node.overlapTestAll()
            assertTrue("overlapTestAll should return empty list", results.isEmpty())
            node.destroy()
        }
    }
}
