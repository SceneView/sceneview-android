package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

/**
 * A coarsened surfel is drawn as wide as the region it stands for, unless a view caps its level:
 * the live view draws every surfel at its finest size.
 */
class DenseSurfelLevelTest {

    /** One surfel of [level], facing up. */
    private fun surfel(level: Int) = DenseCloud(
        positions = floatArrayOf(0f, 0f, 0f),
        colors = intArrayOf(0),
        normals = floatArrayOf(0f, 1f, 0f),
        levels = byteArrayOf(level.toByte()),
    )

    /** The side of the first quad of [mesh]: its first two corners apart. */
    private fun side(mesh: DebugMesh): Float {
        val p = mesh.positions
        val dx = p[3] - p[0]
        val dy = p[4] - p[1]
        val dz = p[5] - p[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    @Test
    fun `a coarse surfel is drawn as wide as its level`() {
        val side = side(DenseSurfels.mesh(surfel(LEVEL), VOXEL_M))
        assertEquals(FINE_SIDE * (1 shl LEVEL), side, EPS)
    }

    @Test
    fun `a coarse surfel capped at the finest level is drawn at the finest size`() {
        val side = side(DenseSurfels.mesh(surfel(LEVEL), VOXEL_M, maxLevel = 0))
        assertEquals(FINE_SIDE, side, EPS)
    }

    private companion object {
        const val VOXEL_M = 0.02f
        const val LEVEL = 2
        const val FINE_SIDE = 2f * VOXEL_M * DenseSurfels.HALF_SIDE_VOXELS
        const val EPS = 1e-5f
    }
}
