package io.github.sceneview.rendering

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VertexTest {

    // ── Vertex data class ───────────────────────────────────────────────────

    @Test
    fun defaultVertexHasZeroPosition() {
        val v = Vertex()
        assertEquals(0f, v.position.x)
        assertEquals(0f, v.position.y)
        assertEquals(0f, v.position.z)
    }

    @Test
    fun defaultVertexHasNullOptionals() {
        val v = Vertex()
        assertEquals(null, v.normal)
        assertEquals(null, v.uvCoordinate)
        assertEquals(null, v.color)
    }

    @Test
    fun vertexWithAllFields() {
        val v = Vertex(
            position = Position(1f, 2f, 3f),
            normal = Direction(0f, 1f, 0f),
            uvCoordinate = Float2(0.5f, 0.5f),
            color = Color(1f, 0f, 0f, 1f)
        )
        assertEquals(1f, v.position.x)
        assertEquals(1f, v.normal?.y)
        assertEquals(0.5f, v.uvCoordinate?.x)
        assertEquals(1f, v.color?.x)
    }

    @Test
    fun vertexCopyModifiesField() {
        val v = Vertex(position = Position(1f, 2f, 3f))
        val v2 = v.copy(position = Position(4f, 5f, 6f))
        assertEquals(1f, v.position.x)
        assertEquals(4f, v2.position.x)
    }

    // ── Extension functions ─────────────────────────────────────────────────

    @Test
    fun hasNormals_falseForEmptyList() {
        assertFalse(emptyList<Vertex>().hasNormals)
    }

    @Test
    fun hasNormals_falseWhenAllNull() {
        val vertices = listOf(Vertex(), Vertex(), Vertex())
        assertFalse(vertices.hasNormals)
    }

    @Test
    fun hasNormals_trueWhenAnyHasNormal() {
        val vertices = listOf(
            Vertex(),
            Vertex(normal = Direction(0f, 1f, 0f)),
            Vertex()
        )
        assertTrue(vertices.hasNormals)
    }

    @Test
    fun hasUvCoordinates_falseForEmptyList() {
        assertFalse(emptyList<Vertex>().hasUvCoordinates)
    }

    @Test
    fun hasUvCoordinates_falseWhenAllNull() {
        val vertices = listOf(Vertex(), Vertex())
        assertFalse(vertices.hasUvCoordinates)
    }

    @Test
    fun hasUvCoordinates_trueWhenAnyHasUv() {
        val vertices = listOf(
            Vertex(),
            Vertex(uvCoordinate = Float2(0f, 1f))
        )
        assertTrue(vertices.hasUvCoordinates)
    }

    @Test
    fun hasColors_falseForEmptyList() {
        assertFalse(emptyList<Vertex>().hasColors)
    }

    @Test
    fun hasColors_falseWhenAllNull() {
        val vertices = listOf(Vertex(), Vertex())
        assertFalse(vertices.hasColors)
    }

    @Test
    fun hasColors_trueWhenAnyHasColor() {
        val vertices = listOf(
            Vertex(),
            Vertex(color = Float4(1f, 0f, 0f, 1f))
        )
        assertTrue(vertices.hasColors)
    }

    // ── getOffsets ──────────────────────────────────────────────────────────

    @Test
    fun getOffsetsEmptyList() {
        val offsets = emptyList<List<Int>>().getOffsets()
        assertTrue(offsets.isEmpty())
    }

    @Test
    fun getOffsetsSinglePrimitive() {
        val indices = listOf(listOf(0, 1, 2))
        val offsets = indices.getOffsets()
        assertEquals(1, offsets.size)
        assertEquals(0 until 3, offsets[0])
    }

    @Test
    fun getOffsetsMultiplePrimitives() {
        val indices = listOf(
            listOf(0, 1, 2),       // 3 indices
            listOf(3, 4, 5, 6),    // 4 indices
            listOf(7, 8)           // 2 indices
        )
        val offsets = indices.getOffsets()
        assertEquals(3, offsets.size)
        assertEquals(0 until 3, offsets[0])
        assertEquals(3 until 7, offsets[1])
        assertEquals(7 until 9, offsets[2])
    }
}
