package io.github.sceneview.demo.demos.internal

/**
 * [ScanGround] as geometry (#4719): the slab under the scan, its edge, and the hatching of the
 * floor it did not see. Built off the main thread; the layers upload them on it.
 *
 * - **Slab**: the footprint, one quad per run of cells along X, [SLAB_DROP_M] under the floor —
 *   under the scanned floor where there is one, so the scan stands on it rather than in it.
 * - **Edge**: the slab's sides, [SLAB_THICKNESS_M] deep, one quad per cell side on the footprint's
 *   outline: a base, as the dollhouse's plinth is, not a sheet of paper.
 * - **Missing**: each missing cell's tile, inset, and its diagonal — cell after cell, the diagonals
 *   join into the 45° hatching of the live map's "still to scan".
 */
class ScanGroundMeshes(val slab: DebugMesh, val edge: DebugMesh, val missingFill: DebugMesh, val hatch: DebugMesh) {
    /** Triangles the four layers draw, for the frame-cost log. */
    val triangles: Int get() = slab.triangleCount + edge.triangleCount + missingFill.triangleCount + hatch.triangleCount

    companion object {
        /** The slab's top, under the floor's height: the depth scatter of a floor stays above it. */
        const val SLAB_DROP_M = 0.03f

        /** How deep the slab's side runs. */
        const val SLAB_THICKNESS_M = 0.05f

        /** The missing tiles and their hatching, a hair over the slab so neither fights it. */
        const val MISSING_LIFT_M = 0.004f
        const val HATCH_LIFT_M = 0.006f

        /** A missing tile's inset, share of a cell: the tiles read as a grid of squares to fill. */
        const val TILE_INSET = 0.08f

        /** Half the hatching's width, share of a cell. */
        const val HATCH_HALF_WIDTH = 0.07f

        @Suppress("NestedBlockDepth")
        fun of(ground: ScanGround): ScanGroundMeshes {
            val slab = DebugMesh()
            val edge = DebugMesh()
            val fill = DebugMesh()
            val hatch = DebugMesh()
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
                    val cell = ground.at(col, r)
                    if (cell == ScanGround.OUTSIDE) continue
                    val x0 = ground.originX + col * s
                    val x1 = x0 + s
                    // The outline: a side wherever the neighbour is outside.
                    if (ground.at(col - 1, r) == ScanGround.OUTSIDE) side(edge, x0, z0, x0, z1, top, bottom)
                    if (ground.at(col + 1, r) == ScanGround.OUTSIDE) side(edge, x1, z0, x1, z1, top, bottom)
                    if (ground.at(col, r - 1) == ScanGround.OUTSIDE) side(edge, x0, z0, x1, z0, top, bottom)
                    if (ground.at(col, r + 1) == ScanGround.OUTSIDE) side(edge, x0, z1, x1, z1, top, bottom)
                    if (cell != ScanGround.MISSING) continue
                    val inset = s * TILE_INSET
                    val y = top + MISSING_LIFT_M
                    fill.quad(
                        fill.vertex(x0 + inset, y, z0 + inset),
                        fill.vertex(x0 + inset, y, z1 - inset),
                        fill.vertex(x1 - inset, y, z1 - inset),
                        fill.vertex(x1 - inset, y, z0 + inset),
                    )
                    // The diagonal from (x0, z1) to (x1, z0), a ribbon across the cell.
                    val h = s * HATCH_HALF_WIDTH
                    val hy = top + HATCH_LIFT_M
                    hatch.quad(
                        hatch.vertex(x0 - h, hy, z1 - h),
                        hatch.vertex(x0 + h, hy, z1 + h),
                        hatch.vertex(x1 + h, hy, z0 + h),
                        hatch.vertex(x1 - h, hy, z0 - h),
                    )
                }
            }
            return ScanGroundMeshes(slab, edge, fill, hatch)
        }

        @Suppress("LongParameterList")
        private fun side(mesh: DebugMesh, ax: Float, az: Float, bx: Float, bz: Float, top: Float, bottom: Float) {
            mesh.quad(mesh.vertex(ax, top, az), mesh.vertex(bx, top, bz), mesh.vertex(bx, bottom, bz), mesh.vertex(ax, bottom, az))
        }
    }
}
