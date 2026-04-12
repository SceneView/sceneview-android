package io.github.sceneview.scene

import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.collision.Sphere
import io.github.sceneview.collision.Vector3
import io.github.sceneview.collision.Ray
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Transform
import io.github.sceneview.rendering.SceneNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Edge-case tests for [SceneGraph] covering guards and deep hierarchies not
 * exercised by [SceneGraphTest].
 */
class SceneGraphEdgeCasesTest {

    // ── Minimal test double ───────────────────────────────────────────────

    private class FakeNode(override var name: String? = null) : SceneNode {
        override var isVisible: Boolean = true
        override var isHittable: Boolean = true

        override var position: Position = Position()
        override var quaternion: Quaternion = Quaternion()
        override var rotation: Rotation = Rotation()
        override var scale: Scale = Scale(1f)
        override var transform: Transform = Transform()

        override var worldPosition: Position = Position()
        override var worldQuaternion: Quaternion = Quaternion()
        override var worldRotation: Rotation = Rotation()
        override var worldScale: Scale = Scale(1f)
        override var worldTransform: Transform = Transform()

        override var parent: SceneNode? = null
        private val _children = mutableSetOf<SceneNode>()
        override val childNodes: Set<SceneNode> get() = _children

        var addedCount = 0
        var removedCount = 0
        var frameCount = 0

        override fun addChildNode(node: SceneNode) {
            _children.add(node)
            (node as FakeNode).parent = this
        }

        override fun removeChildNode(node: SceneNode) {
            _children.remove(node)
            (node as FakeNode).parent = null
        }

        override fun lookAt(targetWorldPosition: Position, upDirection: Direction) {}
        override fun lookTowards(lookDirection: Direction, upDirection: Direction) {}
        override fun onAddedToScene() { addedCount++ }
        override fun onRemovedFromScene() { removedCount++ }
        override fun onFrame(deltaTime: Float) { frameCount++ }
        override fun destroy() {}
    }

    // ── Idempotency guard ────────────────────────────────────────────────

    @Test
    fun addSameNodeTwiceIsIdempotent() {
        val graph = SceneGraph()
        val node = FakeNode("once")
        graph.addNode(node)
        graph.addNode(node)  // second call must be a no-op

        assertEquals(1, graph.rootNodes.size, "Root list must not duplicate the node")
        assertEquals(1, node.addedCount, "onAddedToScene must fire exactly once")
    }

    @Test
    fun removeAbsentNodeIsNoOp() {
        val graph = SceneGraph()
        val node = FakeNode("absent")
        // Never added — removing it must not throw or corrupt state
        graph.removeNode(node)
        assertEquals(0, node.removedCount)
        assertEquals(0, graph.rootNodes.size)
    }

    // ── Child-node removal ───────────────────────────────────────────────

    @Test
    fun removeChildNodeDetachesFromParent() {
        val graph = SceneGraph()
        val parent = FakeNode("parent")
        val child = FakeNode("child")
        graph.addNode(parent)
        graph.addNode(child, parent)

        graph.removeNode(child)

        assertFalse(parent.childNodes.contains(child), "Child must be detached from parent")
        assertEquals(1, child.removedCount, "onRemovedFromScene must fire once")
        // Parent must remain in the graph
        assertEquals(1, graph.rootNodes.size)
        assertEquals(0, parent.removedCount)
    }

    // ── Deep (3-level) hierarchy ─────────────────────────────────────────

    @Test
    fun dispatchFramePropagatesThreeLevelsDeep() {
        val graph = SceneGraph()
        val root = FakeNode("root")
        val child = FakeNode("child")
        val grandchild = FakeNode("grandchild")

        graph.addNode(root)
        graph.addNode(child, root)
        graph.addNode(grandchild, child)

        graph.dispatchFrame(0.016f)

        assertEquals(1, root.frameCount, "root must receive frame")
        assertEquals(1, child.frameCount, "child must receive frame")
        assertEquals(1, grandchild.frameCount, "grandchild must receive frame")
    }

    @Test
    fun removeRootAlsoRemovesGrandchild() {
        val graph = SceneGraph()
        val root = FakeNode("root")
        val child = FakeNode("child")
        val grandchild = FakeNode("grandchild")
        graph.addNode(root)
        graph.addNode(child, root)
        graph.addNode(grandchild, child)

        graph.removeNode(root)

        assertEquals(0, graph.rootNodes.size)
        assertEquals(1, root.removedCount)
        assertEquals(1, child.removedCount)
        assertEquals(1, grandchild.removedCount)
    }

    // ── Hit-test: node without collision shape is skipped ────────────────

    @Test
    fun hitTestSkipsNodeWithNoCollisionShape() {
        val graph = SceneGraph()
        val node = FakeNode("shapeless")
        graph.addNode(node)
        // Intentionally do NOT call setCollisionShape

        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(0f, 0f, -1f))
        val results = graph.hitTest(ray)

        assertTrue(results.isEmpty(), "Node without a collision shape must be skipped")
    }

    @Test
    fun hitTestEmptyGraphReturnsEmptyList() {
        val graph = SceneGraph()
        val ray = Ray(Vector3(0f, 0f, 0f), Vector3(0f, 0f, -1f))
        assertTrue(graph.hitTest(ray).isEmpty())
    }
}
