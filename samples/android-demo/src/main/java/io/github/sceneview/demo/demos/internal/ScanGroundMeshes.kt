package io.github.sceneview.demo.demos.internal

/**
 * [ScanGround] as geometry (#4719): the slab under the scan and its edge. Built off the main
 * thread; the layers upload them on it.
 *
 * - **Slab**: the footprint, holes included, one quad per run of cells along X, [SLAB_DROP_M]
 *   under the floor — under the scanned floor where there is one, so the scan stands on it rather
 *   than in it. A hole in the floor reads as the slab, never as a hatched tile: the result shows
 *   the room, and the parts worth scanning again are named by their own pill (#4736).
 * - **Edge**: the slab's sides, [SLAB_THICKNESS_M] deep, one quad per cell side on the footprint's
 *   outline: a base, as the dollhouse's plinth is, not a sheet of paper.
 */
class ScanGroundMeshes(val slab: DebugMesh, val edge: DebugMesh) {
    /** Triangles the two layers draw, for the frame-cost log. */
    val triangles: Int get() = slab.triangleCount + edge.triangleCount

    companion object {
        /** The slab's top, under the floor's height: the depth scatter of a floor stays above it. */
        const val SLAB_DROP_M = 0.03f

        /** How deep the slab's side runs. */
        const val SLAB_THICKNESS_M = 0.05f

        fun of(ground: ScanGround): ScanGroundMeshes {
            val slab = DebugMesh()
            val edge = DebugMesh()
            val s = ground.cellM
            val top = ground.floorY - SLAB_DROP_M
            val bottom = top - SLAB_THICKNESS_M
            for (r in 0 until ground.rows) {
                val z0 = ground.originZ + r * s
                val z1 = z0 + s
                var c = 0
                while (c < ground.columns) {
                    if (ground.at(c, r) == ScanGround.OUTSIDE) {
                        c++
                        continue
                    }
                    val start = c
                    while (c < ground.columns && ground.at(c, r) != ScanGround.OUTSIDE) c++
                    val x0 = ground.originX + start * s
                    val x1 = ground.originX + c * s
                    slab.quad(slab.vertex(x0, top, z0), slab.vertex(x0, top, z1), slab.vertex(x1, top, z1), slab.vertex(x1, top, z0))
                }
                for (col in 0 until ground.columns) {
                    if (ground.at(col, r) == ScanGround.OUTSIDE) continue
                    val x0 = ground.originX + col * s
                    val x1 = x0 + s
                    // The outline: a side wherever the neighbour is outside.
                    if (ground.at(col - 1, r) == ScanGround.OUTSIDE) side(edge, x0, z0, x0, z1, top, bottom)
                    if (ground.at(col + 1, r) == ScanGround.OUTSIDE) side(edge, x1, z0, x1, z1, top, bottom)
                    if (ground.at(col, r - 1) == ScanGround.OUTSIDE) side(edge, x0, z0, x1, z0, top, bottom)
                    if (ground.at(col, r + 1) == ScanGround.OUTSIDE) side(edge, x0, z1, x1, z1, top, bottom)
                }
            }
            return ScanGroundMeshes(slab, edge)
        }

        @Suppress("LongParameterList")
        private fun side(mesh: DebugMesh, ax: Float, az: Float, bx: Float, bz: Float, top: Float, bottom: Float) {
            mesh.quad(mesh.vertex(ax, top, az), mesh.vertex(bx, top, bz), mesh.vertex(bx, bottom, bz), mesh.vertex(ax, bottom, az))
        }
    }
}
