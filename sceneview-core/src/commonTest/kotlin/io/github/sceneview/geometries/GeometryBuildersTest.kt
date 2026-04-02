package io.github.sceneview.geometries

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.length
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for geometry builders not already fully covered by GeometryTest /
 * ExtendedGeometryTest: BoundingBox helpers, GeometryData properties,
 * center-offset variants, and edge-case validation.
 */
class GeometryBuildersTest {

    private fun assertClose(expected: Float, actual: Float, epsilon: Float = 1e-4f) {
        assertTrue(abs(expected - actual) < epsilon, "Expected $expected got $actual")
    }

    // ── BoundingBox helpers ──────────────────────────────────────────────────

    @Test
    fun boundingBoxMinEqualsCenter_minus_halfExtent() {
        val cube = generateCube(size = Float3(4f, 6f, 8f))
        val bb = cube.boundingBox()
        assertClose(-2f, bb.min.x)
        assertClose(-3f, bb.min.y)
        assertClose(-4f, bb.min.z)
    }

    @Test
    fun boundingBoxMaxEqualsCenter_plus_halfExtent() {
        val cube = generateCube(size = Float3(4f, 6f, 8f))
        val bb = cube.boundingBox()
        assertClose(2f, bb.max.x)
        assertClose(3f, bb.max.y)
        assertClose(4f, bb.max.z)
    }

    @Test
    fun boundingBoxOfEmptyGeometryThrows() {
        val empty = GeometryData(emptyList(), emptyList())
        assertFailsWith<IllegalArgumentException> { empty.boundingBox() }
    }

    @Test
    fun boundingBoxSingleVertex() {
        val vertex = io.github.sceneview.rendering.Vertex(position = Float3(3f, 5f, 7f))
        val geo = GeometryData(listOf(vertex), listOf(0))
        val bb = geo.boundingBox()
        assertClose(3f, bb.center.x)
        assertClose(5f, bb.center.y)
        assertClose(7f, bb.center.z)
        assertClose(0f, bb.halfExtent.x)
        assertClose(0f, bb.halfExtent.y)
        assertClose(0f, bb.halfExtent.z)
    }

    // ── GeometryData property helpers ────────────────────────────────────────

    @Test
    fun hasColors_falseForCubeWithNoColors() {
        assertFalse(generateCube().hasColors)
    }

    @Test
    fun hasColors_trueWhenAnyVertexHasColor() {
        val v = io.github.sceneview.rendering.Vertex(
            position = Float3(0f),
            color = dev.romainguy.kotlin.math.Float4(1f, 0f, 0f, 1f)
        )
        val geo = GeometryData(listOf(v), listOf(0))
        assertTrue(geo.hasColors)
    }

    @Test
    fun hasNormals_trueForSphere() {
        assertTrue(generateSphere().hasNormals)
    }

    @Test
    fun hasUvCoordinates_trueForPlane() {
        assertTrue(generatePlane().hasUvCoordinates)
    }

    // ── Cube center offset ───────────────────────────────────────────────────

    @Test
    fun cubeWithCenterOffset_boundingBoxCenterMatchesOffset() {
        val offset = Float3(5f, -3f, 2f)
        val cube = generateCube(size = Float3(1f), center = offset)
        val bb = cube.boundingBox()
        assertClose(offset.x, bb.center.x)
        assertClose(offset.y, bb.center.y)
        assertClose(offset.z, bb.center.z)
    }

    // ── Sphere center offset ─────────────────────────────────────────────────

    @Test
    fun sphereWithCenterOffset_verticesShiftedCorrectly() {
        val offset = Float3(10f, 0f, 0f)
        val sphere = generateSphere(radius = 1f, center = offset, stacks = 8, slices = 8)
        val xs = sphere.vertices.map { it.position.x }
        assertTrue(xs.min() > 8f, "All X positions should be > 8")
        assertTrue(xs.max() < 12f, "All X positions should be < 12")
    }

    // ── Cylinder dimensions ──────────────────────────────────────────────────

    @Test
    fun cylinderHeight_matchesParameter() {
        val height = 4f
        val cylinder = generateCylinder(radius = 1f, height = height, sideCount = 8)
        val ys = cylinder.vertices.map { it.position.y }
        assertClose(height, ys.max() - ys.min(), epsilon = 0.01f)
    }

    @Test
    fun cylinderRadius_verticesAtCorrectDistance() {
        val radius = 2f
        val cylinder = generateCylinder(radius = radius, height = 2f, sideCount = 12)
        for (v in cylinder.vertices) {
            val dist = kotlin.math.sqrt(v.position.x * v.position.x + v.position.z * v.position.z)
            // Side and cap vertices: side vertices should be at `radius`, cap center at 0
            if (dist > 0.01f) {
                assertTrue(dist <= radius + 0.01f, "Vertex too far from axis: dist=$dist")
            }
        }
    }

    // ── Sphere radius ────────────────────────────────────────────────────────

    @Test
    fun sphereWithRadius2_verticesAtRadius2() {
        val radius = 2f
        val sphere = generateSphere(radius = radius, stacks = 12, slices = 12)
        for (v in sphere.vertices) {
            val dist = length(v.position)
            assertClose(radius, dist, epsilon = 0.01f)
        }
    }

    // ── Torus dimensions ─────────────────────────────────────────────────────

    @Test
    fun torus_outerRadius_matchesParameter() {
        val major = 2f
        val minor = 0.5f
        val torus = generateTorus(majorRadius = major, minorRadius = minor, majorSegments = 24, minorSegments = 12)
        // Maximum distance from Y axis should be majorRadius + minorRadius
        val xzDists = torus.vertices.map { kotlin.math.sqrt(it.position.x * it.position.x + it.position.z * it.position.z) }
        assertClose(major + minor, xzDists.max(), epsilon = 0.05f)
    }

    @Test
    fun torus_innerRadius_matchesParameter() {
        val major = 2f
        val minor = 0.5f
        val torus = generateTorus(majorRadius = major, minorRadius = minor, majorSegments = 24, minorSegments = 12)
        val xzDists = torus.vertices.map { kotlin.math.sqrt(it.position.x * it.position.x + it.position.z * it.position.z) }
        assertClose(major - minor, xzDists.min(), epsilon = 0.05f)
    }

    // ── Heightmap edge cases ─────────────────────────────────────────────────

    @Test
    fun heightmap_segmentsX0_throws() {
        assertFailsWith<IllegalArgumentException> {
            generateHeightmap(segmentsX = 0, segmentsZ = 4)
        }
    }

    @Test
    fun heightmap_segmentsZ0_throws() {
        assertFailsWith<IllegalArgumentException> {
            generateHeightmap(segmentsX = 4, segmentsZ = 0)
        }
    }

    @Test
    fun heightmap_centerOffset() {
        val center = Float3(0f, 0f, 5f)
        val terrain = generateHeightmap(
            width = 2f, depth = 2f,
            center = center,
            segmentsX = 2, segmentsZ = 2
        )
        val zs = terrain.vertices.map { it.position.z }
        // Z values should all be around 5
        assertTrue(zs.min() >= 4f, "Z min should be around 5-1=4, got ${zs.min()}")
        assertTrue(zs.max() <= 6f, "Z max should be around 5+1=6, got ${zs.max()}")
    }

    // ── RoundedCube dimensions ───────────────────────────────────────────────

    @Test
    fun roundedCube_boundingBoxApproximatesSize() {
        val size = Float3(2f, 3f, 4f)
        val rc = generateRoundedCube(size = size, radius = 0.1f, segments = 2)
        val bb = rc.boundingBox()
        assertClose(1f, bb.halfExtent.x, epsilon = 0.15f)
        assertClose(1.5f, bb.halfExtent.y, epsilon = 0.15f)
        assertClose(2f, bb.halfExtent.z, epsilon = 0.15f)
    }

    // ── Icosphere center offset ───────────────────────────────────────────────

    @Test
    fun icosphere_centerOffset_shiftsBoundingBox() {
        val center = Float3(0f, 5f, 0f)
        val ico = generateIcosphere(radius = 1f, center = center, subdivisions = 1)
        val bb = ico.boundingBox()
        assertClose(5f, bb.center.y, epsilon = 0.1f)
    }

    // ── Plane dimensions ─────────────────────────────────────────────────────

    @Test
    fun plane_customSize_matchesBoundingBox() {
        // generatePlane lies in XY plane (Y varies for height, Z varies for depth)
        val plane = generatePlane(size = dev.romainguy.kotlin.math.Float2(4f, 6f))
        val bb = plane.boundingBox()
        assertClose(2f, bb.halfExtent.x, epsilon = 0.01f)
        assertClose(3f, bb.halfExtent.y, epsilon = 0.01f)
    }
}
