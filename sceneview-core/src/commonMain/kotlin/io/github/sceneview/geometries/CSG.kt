package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.rendering.Vertex

/**
 * Basic Constructive Solid Geometry (CSG) operations on meshes.
 *
 * These are approximate operations that work by classifying triangles relative
 * to the other mesh's bounding volume. For production-quality CSG, a BSP-tree
 * approach would be needed, but these provide useful results for simple shapes
 * and procedural generation.
 *
 * Each operation takes two [GeometryData] inputs and produces a new [GeometryData].
 */
object CSG {

    /**
     * A plane defined by a normal and distance from origin.
     */
    private data class Plane(val normal: Float3, val d: Float) {
        fun classify(point: Float3): Float = dot(normal, point) - d
    }

    /**
     * Union of two meshes: combines both geometries, keeping the outer surfaces.
     *
     * This is a simplified union that merges vertices and indices,
     * removing triangles from mesh A that are inside mesh B and vice versa.
     * For non-overlapping meshes, this is equivalent to concatenation.
     *
     * @param a First geometry.
     * @param b Second geometry.
     * @return Combined geometry.
     */
    fun union(a: GeometryData, b: GeometryData): GeometryData {
        val bbA = a.boundingBox()
        val bbB = b.boundingBox()

        // Keep triangles from A that are not inside B
        val aFiltered = filterTriangles(a) { centroid ->
            !isInsideBoundingBox(centroid, bbB)
        }

        // Keep triangles from B that are not inside A
        val bFiltered = filterTriangles(b) { centroid ->
            !isInsideBoundingBox(centroid, bbA)
        }

        return merge(aFiltered, bFiltered)
    }

    /**
     * Intersection of two meshes: keeps only the region where both overlap.
     *
     * @param a First geometry.
     * @param b Second geometry.
     * @return Geometry of the overlapping region.
     */
    fun intersection(a: GeometryData, b: GeometryData): GeometryData {
        val bbA = a.boundingBox()
        val bbB = b.boundingBox()

        // Keep triangles from A that are inside B
        val aFiltered = filterTriangles(a) { centroid ->
            isInsideBoundingBox(centroid, bbB)
        }

        // Keep triangles from B that are inside A
        val bFiltered = filterTriangles(b) { centroid ->
            isInsideBoundingBox(centroid, bbA)
        }

        return merge(aFiltered, bFiltered)
    }

    /**
     * Subtraction: removes geometry B from geometry A.
     *
     * Keeps triangles from A that are not inside B, and inverts triangles
     * from B that are inside A (to form the cavity wall).
     *
     * @param a Base geometry.
     * @param b Geometry to subtract.
     * @return A with B carved out.
     */
    fun subtract(a: GeometryData, b: GeometryData): GeometryData {
        val bbA = a.boundingBox()
        val bbB = b.boundingBox()

        // Keep triangles from A that are not inside B
        val aFiltered = filterTriangles(a) { centroid ->
            !isInsideBoundingBox(centroid, bbB)
        }

        // Invert and keep triangles from B that are inside A
        val bInside = filterTriangles(b) { centroid ->
            isInsideBoundingBox(centroid, bbA)
        }
        val bInverted = invertNormals(bInside)

        return merge(aFiltered, bInverted)
    }

    /**
     * Simple mesh merge: concatenates vertices and adjusts indices.
     */
    fun merge(a: GeometryData, b: GeometryData): GeometryData {
        val offset = a.vertices.size
        val vertices = a.vertices + b.vertices
        val indices = a.indices + b.indices.map { it + offset }
        return GeometryData(vertices, indices)
    }

    /**
     * Inverts the winding order and normals of a geometry (flips it inside-out).
     */
    fun invertNormals(geometry: GeometryData): GeometryData {
        val vertices = geometry.vertices.map { v ->
            v.copy(normal = v.normal?.let { Float3(-it.x, -it.y, -it.z) })
        }
        // Reverse triangle winding
        val indices = geometry.indices.chunked(3).flatMap { tri ->
            if (tri.size == 3) listOf(tri[0], tri[2], tri[1]) else tri
        }
        return GeometryData(vertices, indices)
    }

    // --- Internal helpers ---

    private fun filterTriangles(
        geometry: GeometryData,
        predicate: (Float3) -> Boolean
    ): GeometryData {
        val keptVertexIndices = mutableSetOf<Int>()
        val keptTriangles = mutableListOf<List<Int>>()

        for (i in geometry.indices.indices step 3) {
            if (i + 2 >= geometry.indices.size) break
            val i0 = geometry.indices[i]
            val i1 = geometry.indices[i + 1]
            val i2 = geometry.indices[i + 2]

            val v0 = geometry.vertices[i0].position
            val v1 = geometry.vertices[i1].position
            val v2 = geometry.vertices[i2].position
            val centroid = Float3(
                (v0.x + v1.x + v2.x) / 3f,
                (v0.y + v1.y + v2.y) / 3f,
                (v0.z + v1.z + v2.z) / 3f
            )

            if (predicate(centroid)) {
                keptVertexIndices.addAll(listOf(i0, i1, i2))
                keptTriangles.add(listOf(i0, i1, i2))
            }
        }

        if (keptTriangles.isEmpty()) return GeometryData(emptyList(), emptyList())

        // Remap indices to compact vertex array
        val sortedIndices = keptVertexIndices.sorted()
        val indexMap = sortedIndices.withIndex().associate { (newIdx, oldIdx) -> oldIdx to newIdx }
        val vertices = sortedIndices.map { geometry.vertices[it] }
        val indices = keptTriangles.flatMap { tri -> tri.map { indexMap[it]!! } }

        return GeometryData(vertices, indices)
    }

    private fun isInsideBoundingBox(point: Float3, bb: BoundingBox): Boolean =
        point.x >= bb.min.x && point.x <= bb.max.x &&
                point.y >= bb.min.y && point.y <= bb.max.y &&
                point.z >= bb.min.z && point.z <= bb.max.z
}
