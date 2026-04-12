package io.github.sceneview.utils

import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ExperimentalSceneViewApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [NodeSnapshot] data class and [List.flatten] extension.
 *
 * [NodeSnapshot] is a pure data class with no Filament or Android dependencies,
 * so it is fully testable as a JVM unit test.
 *
 * Note: [inspectScene] and [snapshotNode] require live [Node] instances backed by a Filament
 * Engine and are not covered here. They are exercised by Android instrumented tests.
 */
@OptIn(ExperimentalSceneViewApi::class)
class NodeSnapshotTest {

    private fun makeSnapshot(
        name: String = "node",
        type: String = "Node",
        children: List<NodeSnapshot> = emptyList(),
        isVisible: Boolean = true,
        extras: Map<String, String> = emptyMap()
    ) = NodeSnapshot(
        name = name,
        type = type,
        position = Float3(0f, 0f, 0f),
        rotation = Float3(0f, 0f, 0f),
        scale = Float3(1f, 1f, 1f),
        isVisible = isVisible,
        childCount = children.size,
        children = children,
        extras = extras
    )

    // ── data class construction ──────────────────────────────────────────────────

    @Test
    fun `NodeSnapshot stores name and type correctly`() {
        val s = makeSnapshot(name = "root", type = "ModelNode")
        assertEquals("root", s.name)
        assertEquals("ModelNode", s.type)
    }

    @Test
    fun `NodeSnapshot stores position correctly`() {
        val pos = Float3(1f, 2f, 3f)
        val s = NodeSnapshot(
            name = "n",
            type = "Node",
            position = pos,
            rotation = Float3(0f, 0f, 0f),
            scale = Float3(1f, 1f, 1f),
            isVisible = true,
            childCount = 0,
            children = emptyList()
        )
        assertEquals(pos, s.position)
    }

    @Test
    fun `NodeSnapshot stores isVisible flag`() {
        val visible = makeSnapshot(isVisible = true)
        val hidden = makeSnapshot(isVisible = false)
        assertTrue(visible.isVisible)
        assertFalse(hidden.isVisible)
    }

    @Test
    fun `NodeSnapshot stores extras map`() {
        val extras = mapOf("animationCount" to "3", "playingAnimations" to "1")
        val s = makeSnapshot(extras = extras)
        assertEquals("3", s.extras["animationCount"])
        assertEquals("1", s.extras["playingAnimations"])
    }

    @Test
    fun `NodeSnapshot childCount matches children list size`() {
        val children = listOf(makeSnapshot("child1"), makeSnapshot("child2"))
        val s = makeSnapshot(children = children)
        assertEquals(2, s.childCount)
        assertEquals(2, s.children.size)
    }

    // ── data class equals / hashCode / copy ─────────────────────────────────────

    @Test
    fun `equal NodeSnapshots are equal`() {
        val a = makeSnapshot(name = "x", type = "Node")
        val b = makeSnapshot(name = "x", type = "Node")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `snapshots with different names are not equal`() {
        val a = makeSnapshot(name = "a")
        val b = makeSnapshot(name = "b")
        assertTrue(a != b)
    }

    @Test
    fun `copy changes only the specified field`() {
        val original = makeSnapshot(name = "orig", isVisible = true)
        val copy = original.copy(isVisible = false)
        assertEquals(original.name, copy.name)
        assertFalse(copy.isVisible)
    }

    // ── flatten extension ────────────────────────────────────────────────────────

    @Test
    fun `flatten of empty list returns empty list`() {
        val result = emptyList<NodeSnapshot>().flatten()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `flatten of single node with no children returns one element`() {
        val node = makeSnapshot(name = "root")
        val result = listOf(node).flatten()
        assertEquals(1, result.size)
        assertEquals("root", result[0].name)
    }

    @Test
    fun `flatten includes root and all children in depth-first order`() {
        val child1 = makeSnapshot(name = "child1")
        val child2 = makeSnapshot(name = "child2")
        val root = makeSnapshot(name = "root", children = listOf(child1, child2))
        val result = listOf(root).flatten()

        assertEquals(3, result.size)
        assertEquals("root", result[0].name)
        assertEquals("child1", result[1].name)
        assertEquals("child2", result[2].name)
    }

    @Test
    fun `flatten handles deeply nested nodes`() {
        val leaf = makeSnapshot(name = "leaf")
        val mid = makeSnapshot(name = "mid", children = listOf(leaf))
        val root = makeSnapshot(name = "root", children = listOf(mid))
        val result = listOf(root).flatten()

        assertEquals(3, result.size)
        assertEquals("root", result[0].name)
        assertEquals("mid", result[1].name)
        assertEquals("leaf", result[2].name)
    }

    @Test
    fun `flatten handles multiple root nodes`() {
        val rootA = makeSnapshot(name = "a", children = listOf(makeSnapshot("a1")))
        val rootB = makeSnapshot(name = "b")
        val result = listOf(rootA, rootB).flatten()

        assertEquals(3, result.size)
        assertEquals("a", result[0].name)
        assertEquals("a1", result[1].name)
        assertEquals("b", result[2].name)
    }

    @Test
    fun `flatten count matches total node count in tree`() {
        // Build a tree: root -> (branch1 -> leaf1, leaf2), branch2
        val leaf1 = makeSnapshot(name = "leaf1")
        val leaf2 = makeSnapshot(name = "leaf2")
        val branch1 = makeSnapshot(name = "branch1", children = listOf(leaf1, leaf2))
        val branch2 = makeSnapshot(name = "branch2")
        val root = makeSnapshot(name = "root", children = listOf(branch1, branch2))

        val result = listOf(root).flatten()
        assertEquals(5, result.size)
    }

    @Test
    fun `flatten preserves all fields of each node`() {
        val extras = mapOf("animationCount" to "2")
        val child = NodeSnapshot(
            name = "animated",
            type = "ModelNode",
            position = Float3(1f, 0f, 0f),
            rotation = Float3(0f, 90f, 0f),
            scale = Float3(2f, 2f, 2f),
            isVisible = false,
            childCount = 0,
            children = emptyList(),
            extras = extras
        )
        val root = makeSnapshot(name = "root", children = listOf(child))
        val flat = listOf(root).flatten()

        val flatChild = flat.first { it.name == "animated" }
        assertEquals("ModelNode", flatChild.type)
        assertEquals(Float3(1f, 0f, 0f), flatChild.position)
        assertFalse(flatChild.isVisible)
        assertEquals("2", flatChild.extras["animationCount"])
    }
}
