package io.github.sceneview.collision

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OctreeTest {

    @Test
    fun octreeInsertAndQuery() {
        val bounds = AABB(Vector3(-10f, -10f, -10f), Vector3(10f, 10f, 10f))
        val octree = Octree(bounds)
        val objAabb = AABB(Vector3(-1f, -1f, -1f), Vector3(1f, 1f, 1f))
        octree.insert(objAabb, 0)
        assertEquals(1, octree.objectCount)
    }

    @Test
    fun octreeQueryFindsOverlap() {
        val bounds = AABB(Vector3(-10f, -10f, -10f), Vector3(10f, 10f, 10f))
        val octree = Octree(bounds)
        octree.insert(AABB(Vector3(-1f, -1f, -1f), Vector3(1f, 1f, 1f)), 0)
        octree.insert(AABB(Vector3(5f, 5f, 5f), Vector3(6f, 6f, 6f)), 1)

        val queryRegion = AABB(Vector3(-2f, -2f, -2f), Vector3(2f, 2f, 2f))
        val results = octree.query(queryRegion)
        assertTrue(results.contains(0))
        assertTrue(!results.contains(1))
    }

    @Test
    fun octreeQueryRay() {
        val bounds = AABB(Vector3(-10f, -10f, -10f), Vector3(10f, 10f, 10f))
        val octree = Octree(bounds)
        octree.insert(AABB(Vector3(-1f, -1f, -1f), Vector3(1f, 1f, 1f)), 0)
        octree.insert(AABB(Vector3(5f, 5f, 5f), Vector3(6f, 6f, 6f)), 1)

        val ray = Ray(Vector3(0f, 0f, -5f), Vector3(0f, 0f, 1f))
        val results = octree.queryRay(ray)
        assertTrue(results.contains(0))
    }

    @Test
    fun octreeClear() {
        val bounds = AABB(Vector3(-10f, -10f, -10f), Vector3(10f, 10f, 10f))
        val octree = Octree(bounds)
        octree.insert(AABB(Vector3(-1f, -1f, -1f), Vector3(1f, 1f, 1f)), 0)
        octree.clear()
        assertEquals(0, octree.objectCount)
    }

    @Test
    fun octreeSubdivides() {
        val bounds = AABB(Vector3(-10f, -10f, -10f), Vector3(10f, 10f, 10f))
        val octree = Octree(bounds, maxObjectsPerNode = 2)

        // Insert enough objects to trigger subdivision
        for (i in 0..5) {
            val offset = i.toFloat()
            octree.insert(
                AABB(Vector3(offset, offset, offset), Vector3(offset + 0.1f, offset + 0.1f, offset + 0.1f)),
                i
            )
        }

        assertEquals(6, octree.objectCount)
        assertTrue(octree.isSubdivided)
    }

    @Test
    fun octreeFromTriangles() {
        val triangles = listOf(
            MeshCollider.Triangle(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f)),
            MeshCollider.Triangle(Vector3(2f, 0f, 0f), Vector3(3f, 0f, 0f), Vector3(2f, 1f, 0f))
        )
        val octree = Octree.fromTriangles(triangles)
        assertEquals(2, octree.objectCount)
    }

    @Test
    fun acceleratedRayMeshWorks() {
        val triangles = listOf(
            MeshCollider.Triangle(
                Vector3(-1f, 0f, 0f), Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f)
            )
        )
        val octree = Octree.fromTriangles(triangles)
        val ray = Ray(Vector3(0f, 0.3f, -1f), Vector3(0f, 0f, 1f))
        val result = acceleratedRayMeshIntersection(ray, triangles, octree)
        assertTrue(result.hit)
        assertEquals(0, result.triangleIndex)
    }
}
